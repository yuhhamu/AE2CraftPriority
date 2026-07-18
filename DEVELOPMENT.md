# AE2 Crafting Priority — 開発者向け情報

> **本MODはAnthropicのAIアシスタント「Claude」を利用して開発されました。**

プレイヤー向けの使い方は [README.md](README.md) を参照してください。このファイルは実装・
ビルドに関する技術的な情報のみを扱います。

設計の背景・決定事項はObsidian Vaultの以下も参照:

- `Projects/AE2-CraftingPriority-Addon.md`
- `Decisions/2026-07-15-ae2-priority-addon-scope.md`
- `Knowledge/ae2-crafting-cpu-scheduling.md`
- `Knowledge/ae2craftpriority-windows-build.md`(Windows環境でのビルド時に踏んだ問題と解決策)

## 技術的な仕組み(Mixin)

AE2には「複数ジョブが同じ機械を取り合う際の実行優先度」という概念自体が存在しない
(AE2本体でも長年の未実装要望: [Issue #520](https://github.com/AppliedEnergistics/Applied-Energistics-2/issues/520))。
公式APIにもこのレベルのスケジューリングを制御する拡張点は無いため、実際のAE2ソース
(`forge/1.20.1` ブランチ)を確認した上で、必要最小限のMixinで実現している。

**スケジューリング本体(低リスク・確度高):**

1. `appeng.me.cluster.implementations.CraftingCPUCluster` に優先度フィールドを追加
   (`CraftingCPUClusterMixin`)。NBTの読み書き末尾に1キー追記するだけの変更。
2. `appeng.me.service.CraftingService#onServerEndTick()` は、稼働中の全Crafting CPUを
   `craftingCPUClusters` という素の `HashSet`(優先度と無関係な反復順)でtickしている。
   `CraftingServiceMixin` は、このフィールドを `@Shadow @Final @Mutable` でフィールド名
   指定により直接取得し、コンストラクタ末尾(`@Inject(method = "<init>", at = @At("TAIL"))`)で
   `PriorityOrderedHashSet`(`HashSet` のサブクラスで `iterator()` のみオーバーライド、優先度
   降順で反復)へ実体そのものを差し替える。これにより `onServerEndTick()` 内でこのフィールドを
   反復する箇所が何箇所あっても、AE2側の実装が将来変わっても、常に優先度降順の反復順が保証される。
   Pattern Provider側の内部ロジックには一切手を加えていない。
   （2026-07-16に `@Redirect(@At("NEW"))` 方式へ設計変更。旧方式・変更理由は下記「実機で
   『GUI以外全機能していない』と報告された調査」の項を参照。その後2026-07-18、AdvancedAE
   対応の過程で `@Redirect(@At("NEW"))` 方式自体にも同種の脆さ(他Modが同じコンストラクタへ
   独自の `new HashSet<>()` を追加すると、何番目の生成呼び出しかという ordinal がずれて誤った
   フィールドを差し替える恐れ)があると判断し、フィールド名を直接指定する現行の
   `@Shadow @Final @Mutable` 方式へ再度変更した。詳細は下記「AdvancedAE / ExtendedAE / Mega
   Cells / Applied Mekanistics 対応」の項を参照)

**クラフト確認画面への統合(相対的に高リスク・要実機確認):**

3. `CraftConfirmMenuMixin` — クラフト確認画面のメニューに優先度フィールドを追加。
   `startJob()` の実行区間だけ、その値を `PendingCraftPriority`(ThreadLocal)に流し込む。
4. `CraftingCpuLogicMixin` — `CraftingCpuLogic#trySubmitJob(...)` は、ジョブがどの
   `CraftingCPUCluster` に実際に割り当てられたかが確定する場所。提出成功時に
   `PendingCraftPriority` の値があれば、そのCPUへ適用する。Export Bus/Interface経由の
   自動クラフトなど確認画面を通らない提出には一切影響しない(値が未設定のまま)。
5. `CraftConfirmScreenMixin` — クライアント側の確認画面に -/+ ボタンと数値入力欄(`EditBox`)を
   追加し、専用パケットで優先度をサーバー側メニューへ反映する。座標はハードコードではなく、
   AE2自身が確定させた `cancel`/`start` ボタンの実座標を実行時に取得し、その間の隙間
   (クラフト確認画面の下部、「キャンセル」と「スタート」の間)に毎フレーム動的にフィットさせる方式。
   `start` は通常の `@Shadow @Final` で参照できるが、`cancel` はAE2側でフィールドに保持されていない
   ため、`WidgetContainer#addButton(...)` 呼び出し自体を `@Redirect` で横取りして実体を取得している
   (詳細は Obsidian `Knowledge/ae2-crafting-cpu-scheduling.md` および
   `Decisions/2026-07-15-ae2-priority-addon-scope.md` を参照)。
   Mixinの「対象クラスの祖先クラスをコンパイル時だけ継承する」という定石パターンを使っている。

**AE2純正「Crafting Status」画面への統合(低リスク・要実機確認):**

6. `CraftingStatusMenuMixin` — `appeng.menu.me.crafting.CraftingStatusMenu`(端末のCrafting Status
   タブのメニュー。CPU一覧・稼働状況表示・CPU選択を既に持っている)に、選択中CPUの優先度を
   `@GuiSync` フィールドで同期し、`registerClientAction`/`sendClientAction` というAE2本体の
   クライアントアクション機構(`AEBaseMenu`)経由で変更できるようにする。
   `CraftingCPUMenu extends AEBaseMenu` を「見せかけの継承」してこれらのprotectedメンバーに
   コンパイル時アクセスしている。AE2本体のクラスや既存フィールドは一切変更していない
   (完全に新規追加のフィールド・メソッドのみ)。優先度の再計算処理(`ae2cp$refreshPriorityFromCpu()`)
   自体は毎tick呼ぶ必要があるが、`CraftingStatusMenu` や `AEBaseMenu` など**AE2側のクラスへ
   直接 `broadcastChanges` をInjectすると導入されているAE2バージョン次第で起動クラッシュする**
   ことが2回の実機クラッシュで判明したため(下記参照)、実際の呼び出しはAE2の外、Vanilla本体
   から行う `ContainerMenuMixin` から行っている。
7. `ContainerMenuMixin` — **AE2ではなくMinecraft Vanilla本体**の
   `net.minecraft.world.inventory.AbstractContainerMenu#broadcastChanges()` に注入し、
   `this instanceof CraftingStatusPriorityControl` の場合のみ `ae2cp$refreshPriorityFromCpu()`
   を呼ぶ。AE2のクラス階層(`CraftingStatusMenu` → `CraftingCPUMenu` → `AEBaseMenu`)は
   バージョンによってどの段階でこのメソッドをオーバーライドしているか変わり得ることが実機で
   2回確認されたため、AE2のバージョンに一切依存しないVanillaの基底クラスまで遡っている。
   他のAE2メニュー(クラフト確認画面・各種端末画面など)には一切影響しない。
   **Vanillaクラスが対象のため、他のMixinと異なり `remap = false` を付けていない**
   (SRG/MCPマッピング解決が必要なため)。
8. `CraftingCPUScreenMixin` — -/+ ボタンと数値入力欄(`EditBox`)を追加し、
   `CraftingStatusMenuMixin` が実装する duck interface `CraftingStatusPriorityControl` 経由で
   優先度を変更する。**対象は `CraftingStatusScreen` ではなく、その親クラス
   `appeng.client.gui.me.crafting.CraftingCPUScreen`**。「キャンセル」ボタン(`cancel`)は
   `CraftingStatusScreen` 自身ではなくこの親クラスのコンストラクタで作られたprivateフィールド
   であり、`CraftConfirmScreenMixin` の `start`/`cancel` 取得と同じ理由で、フィールドを宣言している
   クラス自体をMixin対象にする必要がある。`CraftingCPUScreen` は個別Crafting CPU画面にも使われる
   共通クラスのため、`this.menu instanceof CraftingStatusPriorityControl` で「Crafting Status」
   タブの場合だけに限定している。ウィジェットの座標は `CraftConfirmScreenMixin` と同じ手法で、
   `cancel` ボタンの実座標(`getX()`/`getY()`/`getHeight()`)を基準に毎フレーム
   (`updateBeforeRender`)再配置している。
9. `CraftingStatusMenuMixin`(2026-07-16拡張、`CPUSelectionListMixin`は廃止・削除) — 当初は
   クライアント側の `CPUSelectionListMixin` が選択中CPUの表示名にだけ `@<優先度>` を追記していたが、
   「選択中しか優先度が見えない」制限をなくすため、**サーバー側の `createCpuList()`
   (CPU一覧をクライアントへ同期するデータを作る、`CraftingStatusMenu` のprivateメソッド)内で
   `ICraftingCPU#getName()` の呼び出しを `@Redirect` で横取りし、選択中かどうかに関わらず
   全CPU分の表示名へ `@<優先度>` を追記する**方式に一本化した。`ICraftingCPU#getName()` は
   `appeng.api.networking.crafting` パッケージの**公開API**であり、他のMixinが依存している
   AE2内部実装より変わりにくい。名前が未設定(null)のCPUについては、クライアント側の従来の
   フォールバック表示("CPUs #N")と同じ書式をサーバー側で再現してから優先度を追記するため、
   `getOrAssignCpuSerial(ICraftingCPU)`(private)も追加で`@Shadow`している。この
   private依存は、既にこのMixinが依存している `selectedCpu` フィールド・コンストラクタ構造
   (いずれもrequire=1、既定の厳格設定)と同程度のリスクと判断し、同じく既定の`require = 1`
   のままにしている(AE2側でこのメソッドが消える規模の変更が起きた場合、どのみち
   `selectedCpu`等の既存shadowも道連れで壊れる可能性が高いため、新たなリスクカテゴリを
   増やしているわけではないという判断。詳細はコード内のjavadocを参照)。
   クライアント側では名前が常に非nullで届くようになるため、追加の処理は一切不要になった。

対象クラスはAE2(サードパーティMod)のクラスがほとんどで、Vanilla向けのSRGマッピングが存在しない
ため、AE2クラスを対象とする `@Mixin` には全て `remap = false` を付けている(付けないと
`Unable to locate obfuscation mapping for @Inject target ...` エラーになる)。**例外は
`ContainerMenuMixin` のみ**で、これはMinecraft Vanilla本体の `AbstractContainerMenu` が対象
なので、逆にSRG/MCPマッピング解決が必要な通常のMixin(`remap` 省略 = デフォルトtrue)になっている。

**`@GuiSync` がMixin追加フィールドでも機能する理由:**

AE2の同期機構(`appeng.menu.guisync.DataSynchronization`)は、メニューの**実行時クラス**の
全フィールドを `host.getClass().getDeclaredFields()` でリフレクション走査して `@GuiSync` 付き
フィールドを収集する(コンパイル時のコード生成ではない)。Mixinはクラスロード時にバイトコードへ
フィールドを注入するため、リフレクション走査が行われる時点(メニュー生成時)には既にMixinが
適用済みで、追加したフィールドも本来からある物と同様に検出される。

## 開発者向けAPI(`api` パッケージ)

他Modから優先度を読み書きできる公開APIを2026-07-18に追加した。

- `api.CraftPriorityApi`(公開ファサード): `static int getPriority(Object)` /
  `static void setPriority(Object, int)` / `static boolean isSupported(Object)` /
  `static void registerAdapter(PriorityAdapter)` の4メソッドのみ。まず内部の
  `PriorityHolder`(Mixinで追加した既存フィールド)を優先してチェックし、該当しなければ
  `CopyOnWriteArrayList<PriorityAdapter>` として保持しているアダプタ登録を順に照会する。
  どちらにも該当しない場合は `PriorityHolder.DEFAULT_PRIORITY`(0)を返す。
- `api.PriorityAdapter`(拡張ポイント): `supports(Object)` / `getPriority(Object)` /
  `setPriority(Object, int)` の3メソッドを実装したクラスを `CraftPriorityApi.registerAdapter(...)`
  で登録すると、AE2純正の `CraftingCPUCluster` 以外の第三者Mod製CPU/クラスタ型にも本MODの優先度
  システムを適用できる。既存の `CraftingStatusMenuMixin`(CPU一覧への `@優先度` 表記)も内部で
  `CraftPriorityApi.getPriority(...)` 経由に統一したため、アダプタ登録さえ済ませれば表示にも
  自動的に反映される。
- この仕組みは「開発者向けAPI」という要件と「AdvancedAE等サードパーティCPU型への対応」という
  要件を1つのレジストリパターンで同時に満たす設計になっている(既存の `PriorityHolder` ベースの
  内部経路は一切変更していないため、Vanilla CPUに対する既存動作への回帰リスクはゼロ)。

## AdvancedAE / ExtendedAE / Mega Cells / Applied Mekanistics 対応

2026-07-18、AdvancedAE・ExtendedAEが追加するCraftingProvider/CPUへの対応と、Applied Mekanistics・
Mega Cells・AE2 Utility・Not Enough Energistics導入時の非クラッシュ確認を行った(ユーザー要望)。
このうち「AE2 Utility」「Not Enough Energistics」はMinecraft 1.20.1 Forge向けビルドが存在しない
(前者はNeoForge 1.21.1のみ、後者はForge 1.12.2止まりで開発終了)ため、ユーザー確認の上でスキップした。

### AdvancedAE(Quantum Computer)

AdvancedAEは独自の `AdvCraftingCPUCluster`(`IAECluster` 実装、Vanillaの `CraftingCPUCluster` を
継承していない別クラス)と `AdvCraftingCPU`(`ICraftingCPU` 実装)、および `CraftingService` に
`@Unique Set<AdvCraftingCPUCluster> advancedAE$advCraftingCPUClusters` を追加する独自Mixinを
持っており、Vanillaの `craftingCPUClusters` とは別の反復ループ・CPU選択ロジックで動作する。
そのため本MODの優先度システムを効かせるには専用の対応コードが必要だった。

- `mixin/compat/advancedae/AdvCraftingCPUClusterMixin` — `AdvCraftingCPUCluster` に
  `PriorityHolder` を実装させ、`writeToNBT`/`readFromNBT` の末尾で優先度をNBT永続化する
  (既存の `CraftingCPUClusterMixin` と全く同じパターン)。
- `mixin/compat/advancedae/AdvCraftingCPUAccessor` — `AdvCraftingCPU` の private `cluster`
  フィールドを読むための `@Accessor`。
- `mixin/compat/advancedae/CraftingServiceAdvancedAeMixin` — `@Mixin(priority = 2000)` で
  AdvancedAE自身のMixinより後に適用されるようにした上で、`advancedAE$advCraftingCPUClusters`
  を `@Shadow(remap = false) @Final @Mutable` し、`PriorityOrderedHashSet` へ差し替える
  (Vanilla側の `CraftingServiceMixin` と同じ方式)。
- `compat/advancedae/AdvancedAeCpuAdapter implements PriorityAdapter` — `AdvCraftingCPU` を
  `AdvCraftingCPUAccessor` 経由でクラスタへ辿り、優先度を読み書きする。
- `compat/advancedae/AdvancedAeCompat.init()` — `Mixins.addConfiguration(...)` と
  `CraftPriorityApi.registerAdapter(...)` を行う。**必ず `ModList.isLoaded("advanced_ae")` で
  ガードされた経路からのみ呼び出すこと**(クラス参照自体がAdvancedAE未導入時に `NoClassDefFoundError`
  を招くため)。

**優先度が及ぶ範囲(2026-07-18、ユーザー確認済みの設計判断)**: 優先度はAdvancedAEのQuantum
Computer同士の実行順序のみを制御し、Vanilla CPUとQuantum Computerのどちらを使うかという
AdvancedAE自身のCPU選択ロジック(Vanilla CPUよりQuantum Computerを優先する等)には一切介入しない。
「上書きしない」がユーザーの選択(AskUserQuestionで確認)。

**重大なビルド落とし穴(2026-07-18、実機で発見・解決済み)**: `build.gradle` の `mixin { }` DSL
ブロックに `config "ae2craftpriority-advancedae.mixins.json"` を列挙すると、MixinGradleが
jarのマニフェストに自動登録情報を書き込み、`AE2CraftPriorityMod` の `ModList.isLoaded(...)`
ガードより前に**無条件でロードされてしまう**。この状態でAdvancedAE未導入の環境で起動すると、
`CraftingServiceAdvancedAeMixin` が `advancedAE$advCraftingCPUClusters` という存在しないフィールドを
`@Shadow` しようとして `InvalidMixinException` で即クラッシュする(`AdvCraftingCPUClusterMixin`/
`AdvCraftingCPUAccessor` は対象クラス自体が無いだけなので警告止まりだが、`CraftingService` は
常に存在するAE2本体のクラスのため、こちらは致命的)。**対処**: `build.gradle` の `mixin{}` ブロックからは
このconfigを完全に除去し、`AdvancedAeCompat.init()` 内の `Mixins.addConfiguration(...)`
(`ModList` ガード付き)のみで登録する。この設定ファイルはAE2CraftPriority本体の
`ae2craftpriority.mixins.json` とは別ファイル(`ae2craftpriority-advancedae.mixins.json`)にし、
`mods.toml` の `[[mixins]]` にも列挙しない。

**動作確認(2026-07-18、実機)**:

1. AdvancedAE未導入(ベースライン): 上記の修正後、正常起動を確認(`RESULT: SUCCESS`)。
2. AdvancedAE 1.3.3-1.20.1 + 依存のGeckoLib 4.8.3導入: 正常起動を確認。ログに
   `advanced_ae`/Mixin適用失敗は一切出ていない。
3. 実際にQuantum Computerを設置して優先度を変更する対話的な確認(ゲーム内でのブロック設置・
   GUI操作)は今回のセッションでは未実施。ログレベルでのMixin適用成功と起動確認までが完了範囲。

### Mega Cells

調査の結果、Mega CellsはVanillaの `appeng.block.crafting.AbstractCraftingUnitBlock` を継承した
独自の `ICraftingUnitType`(`MEGACraftingUnitType`)を追加しているだけで、専用の
CraftingCPUCluster/CraftingServiceクラスは持たない(GitHubのMixinディレクトリを確認したところ
`AbstractCraftingUnitBlockMixin` のみで、レシピ/アップグレード互換用)。つまり「MEGA Crafting CPU」は
内部的にVanillaの `CraftingCPUCluster`/`CraftingService` をそのまま使っているため、既存の
`CraftingServiceMixin`(`PriorityOrderedHashSet` への差し替え)が無改修で効く。追加のMixinは不要。
2026-07-18の実機確認で、Mega Cells 2.4.6導入時も正常起動を確認済み(ログに
`[MEGA Cells/]: Initialised ...` の各初期化ログが出ており、クラッシュ・Mixinエラーなし)。

### ExtendedAE

ExtendedAEは標準AE2公開APIの範囲で36スロットPattern Providerを追加しているのみで、
CraftingService/CraftingCPUClusterへのMixinは持たない。追加コード不要。2026-07-18の実機確認で、
ExtendedAE 1.20-1.4.12-forge(および必須の codelib である Glodium)導入時も正常起動を確認済み。

### Applied Mekanistics

標準AE2公開APIの範囲でMekanism連携パーツ(Pattern Provider等)を追加するアドオンで、
CraftingService/CraftingCPUClusterへのMixinは持たない。追加コード不要。2026-07-18の実機確認で、
Applied Mekanistics 1.4.3導入時も正常起動を確認済み(`[appmek] Found status: UP_TO_DATE` を確認)。

### 複合導入テスト(2026-07-18)

AE2CraftPriority + AdvancedAE 1.3.3 + GeckoLib 4.8.3 + ExtendedAE 1.20-1.4.12 + Glodium 1.20-1.5 +
Applied Mekanistics 1.4.3 + Mega Cells 2.4.6 の7Mod(+既存の13companion mod)を同時導入した状態での
起動確認を実施し、クラッシュ・Mixin適用エラーともになし(`RESULT: SUCCESS`)。Mega Cellsのログには
「Applied Mekanistics integration」「AE2WT integration」の初期化ログも出ており、Mega Cells側の
他Mod連携機構も正常に動作していることを確認した。

## 対応バージョン範囲の根拠

READMEには「AE2 15.x系列(Minecraft 1.20.1向け)」とだけ書いているが、これは特定の1バージョンに
決め打ちせず、実際にこのMODが依存しているAE2内部APIを洗い出した上で判断したもの。

**このMODが依存しているAE2の内部クラス・メソッド一覧**(すべて `appeng.api.*` 配下の公開APIではなく、
AE2の内部実装。[公式Javadoc](https://appliedenergistics.org/javadoc/)には載っていない):

| Mixin | 対象クラス | 依存しているメンバー |
|---|---|---|
| `CraftingCPUClusterMixin` | `appeng.me.cluster.implementations.CraftingCPUCluster` | `writeToNBT` / `readFromNBT` |
| `CraftingServiceMixin` | `appeng.me.service.CraftingService` | コンストラクタ内 `craftingCPUClusters` フィールドの `new HashSet<>()` 初期化(1箇所目のHashSet生成) |
| `CraftingCpuLogicMixin` | `appeng.crafting.execution.CraftingCpuLogic` | `trySubmitJob` |
| `CraftConfirmMenuMixin` | `appeng.menu.me.crafting.CraftConfirmMenu` | `startJob` |
| `CraftConfirmScreenMixin` | `appeng.client.gui.me.crafting.CraftConfirmScreen` | コンストラクタ、`updateBeforeRender`、`appeng.client.gui.WidgetContainer#addButton` |
| `CraftingStatusMenuMixin` | `appeng.menu.me.crafting.CraftingStatusMenu` | コンストラクタ、`selectedCpu` フィールド、`CraftingCPUMenu` 継承構造、`createCpuList()`、private `getOrAssignCpuSerial(...)` |
| `CraftingCPUScreenMixin` | `appeng.client.gui.me.crafting.CraftingCPUScreen` | コンストラクタ、`updateBeforeRender`、`cancel` フィールド |
| `ContainerMenuMixin` | `net.minecraft.world.inventory.AbstractContainerMenu`(**Vanilla**) | `broadcastChanges` |

**判断の根拠**:

- 上記のうち `ContainerMenuMixin` 以外は全てAE2の内部実装(非公開API)に依存しており、AE2自身は
  これらのクラス構成・メソッド名についてバージョン間の互換性を一切保証していない。ただし
  `CraftingStatusMenuMixin` の `@Redirect` 対象そのもの(`ICraftingCPU#getName()`)は
  `appeng.api.networking.crafting` パッケージの公開APIであり、この1点に限れば他の内部依存より
  変わりにくい(横取り箇所を「探すための入れ物」である `createCpuList()` の存在自体は内部依存のまま)。
- ただし、これらのクラスは全て `appeng.menu.me.crafting.*` / `appeng.client.gui.me.crafting.*` /
  `appeng.me.cluster.implementations.*` / `appeng.me.service.*` / `appeng.crafting.execution.*`
  という、AE2がMinecraft 1.20.1向けに使っている `forge/1.20.1` ブランチ固有のパッケージ構成に
  属しており、この構成は他のMinecraftバージョン向けブランチとは異なる(AE2はMCバージョンごとに
  内部実装を作り直しており、`15.x` という版数自体がこの1.20.1向けブランチに紐付いている)。
- 実際に、AE2 15.4.10(実機で動作確認済み)と、`forge/1.20.1` ブランチのより新しいコミット
  (2026-07-16時点のHEAD、`guideme` 依存が20.1.7→実機導入版20.1.15相当まで進んでいることから
  15.4.10より後のバージョンと推定される)の両方を実際にソースで突き合わせた結果、上記表の
  メソッド名・シグネチャ・コンストラクタ引数はすべて一致していた。**唯一の例外が
  `broadcastChanges` のオーバーライド有無**で、これが2026-07-16の2回の実機クラッシュの原因
  だった(`Knowledge/ae2-crafting-cpu-scheduling.md` 参照)。この1点についてはAE2バージョンに
  依存しないVanilla本体への注入(`ContainerMenuMixin`)に切り替えることで、依存自体を解消した。
- 以上から、「AE2 15.4.10単体でのみ動作確認済み」ではあるが、「AE2 15.x系列(Minecraft 1.20.1
  向けの `forge/1.20.1` ブランチから作られたビルド)全体で構造的に互換性がある可能性が高い」と
  判断できる。これはあくまでソースコードの突き合わせに基づく推定であり、実機での動作確認は
  15.4.10のみである点に注意。
- `CraftingCPUScreenMixin`(2026-07-16追加)・`CPUSelectionListMixin`(同日追加)は、上記の
  クラッシュ修正よりも後に `forge/1.20.1` ブランチHEADのソースと突き合わせて実装したが、
  15.4.10での実機動作はまだ確認できていない。特に `CPUSelectionListMixin` が対象にしている
  `getCpuName` はprivateな内部ヘルパーであり最も不確実性が高いと判断したため、
  `@Redirect` に `require = 0` を付けて「対象が見つからなければ黙ってこの1機能だけ無効化する」
  形にし、万一signatureが異なっていてもゲーム全体がクラッシュしないようにしてある。

**新しいAE2バージョンで問題が起きた場合の確認手順**: 上記表の対象クラス・メンバーを、実際に
導入されているAE2バージョンの `forge/1.20.1` ブランチの該当タグ(またはjarの逆コンパイル)で
1つずつ確認する。GitHubのデフォルトブランチ(HEAD)は先行開発中でリリース版より新しい場合が
あるため、ソース確認だけで判断せず、疑わしい箇所は実機で検証すること(2026-07-16の教訓、
`Knowledge/mistakes.md` 参照)。

## セットアップ・ビルド方法

Windows環境では専用スクリプトが使える(Java/Gradleの検出・インストールを自動化):

```
cd AE2CraftPriority
powershell -ExecutionPolicy Bypass -File build.ps1
```

素のGradleでも可:

```
cd AE2CraftPriority
gradle build
```

初回は Forge MDK と AE2(`maven.modrinth:ae2:15.4.10`)がダウンロードされる。
`gradle runClient` で開発環境の起動確認ができる。

### ビルドで実際に踏んだ問題(2026-07-16、解決済み)

- javacがWindows既定のANSIコードページでソースを読み込み、日本語コメントで大量エラーになる
  → `build.gradle` に `tasks.withType(JavaCompile) { options.encoding = 'UTF-8' }`
- `processResources` の `expand()` によるmods.tomlへのトークン置換が同じ理由で文字化けする
  → `processResources { filteringCharset = 'UTF-8' }`(↑とは別設定、両方必要)
- Mixin対象がすべてAE2(非Vanilla)クラスのため `Unable to locate obfuscation mapping` エラー
  → 全Mixinに `remap = false`
- Forge 1.20.1には `RegisterMenuScreensEvent` が存在しない
  → `FMLClientSetupEvent#enqueueWork` 内で `MenuScreens.register(...)`
- `ForgeRegistries.CREATIVE_MODE_TABS` は存在しない(1.20.1ではVanilla側のレジストリ)
  → `net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB`

詳細は `Knowledge/ae2craftpriority-windows-build.md`(Obsidian)を参照。

### 実機で踏んだ起動クラッシュ(2026-07-16、2回発生・解決済み)

1回目: `CraftingStatusMenuMixin` が `CraftingStatusMenu` へ直接
`@Inject(method = "broadcastChanges")` していたが、実際に導入されたAE2 15.4.10では
`CraftingStatusMenu` がこのメソッドをオーバーライドしておらず、`MixinApplyError`
(対象クラスにメソッドが存在しない)で起動クラッシュした。

2回目: 1回目の対処として注入先を基底クラス `appeng.menu.AEBaseMenu` に変更したところ、
**全く同じ理由で再度クラッシュした**。導入されているAE2 15.4.10では `AEBaseMenu` 自体も
`broadcastChanges()` をオーバーライドしていなかった。GitHubの `forge/1.20.1` ブランチ(HEAD、
先行開発中)で見えた実装はリリース版より新しく、参照した実装が実在しなかったことが2回とも
根本原因。

最終的な解決: AE2のクラス階層を辿るのをやめ、**Minecraft Vanilla本体**の
`net.minecraft.world.inventory.AbstractContainerMenu#broadcastChanges()` に直接注入する
`ContainerMenuMixin` に変更した(AE2のバージョンに一切依存しない)。詳細は
`Knowledge/ae2-crafting-cpu-scheduling.md` / `Knowledge/mistakes.md`(Obsidian)を参照。

なお1回目のクラッシュ時のログには巻き添えでMekanism側の
`NullPointerException: Registry Object not present: mekanism:basic_smelting_factory` も
出ていたが、ae2craftpriority関連のスタックトレースは含まれておらず、Mixinクラッシュで
Mod初期化が異常終了した副作用と考えられる。

### 実機で「GUI以外全機能していない」と報告された調査(2026-07-16)

起動クラッシュ修正・Crafting Status統合がすべて実機ビルドを通過した後、実際のプレイで
「優先度を設定してクラフトを開始しても優先的な搬入が行われない」「Crafting Status画面の
優先度設定が機能していない」「GUI以外すべて機能していない」という報告を受けた。

**調査手順**: `PriorityHolder`/`CraftingCPUClusterMixin`(優先度の保持)、
`CraftConfirmMenuMixin`→`PendingCraftPriority`→`CraftingCpuLogicMixin`(クラフト開始時の
優先度受け渡し)、`CraftingStatusMenuMixin`(Crafting Status画面での変更)は、いずれも
コードを読む限り論理的に正しく繋がっていることを確認した。一方、実際にAE2のソース
(`appeng.me.service.CraftingService#onServerEndTick()`、`appeng.crafting.execution.CraftingCpuLogic`、
`appeng.me.cluster.implementations.CraftingCPUCluster`)を精読した結果、優先度の高いジョブが
「先にtickされることで、同じPattern Providerを取り合った際に先にパターンをpushできる」という
本MODのスケジューリング原理そのものは妥当であることも確認できた。

**疑わしいと判断した箇所**: 旧 `CraftingServiceMixin` は `onServerEndTick()` 内の
「2箇所の `Set#iterator()` 呼び出し」を出現順(`ordinal = 0` / `ordinal = 1`)で個別に
狙い撃ちする実装だった。この方式は、Mixin適用自体は成功する(=「何らかの `Set#iterator()`」に
マッチしさえすれば `require = 1` を満たしてしまう)にもかかわらず、狙った意図通りのループに
命中しているとは限らないという弱点がある。特に `onServerEndTick()` は条件分岐・複数の
ローカル変数を含む比較的複雑なメソッドで、`Set` 型と推論される他の反復箇所が万一混入すると
ordinalがずれ、**クラッシュせずに黙って無効化**されてしまう。これは今回の症状
(「起動はする、GUIも動く、しかし効果が一切ない」)と矛盾しない。

**対処**: ordinalに依存しない、より頑健な方式へ変更した。`craftingCPUClusters` フィールド自体の
実体を、コンストラクタでの生成箇所(`new HashSet<>()`)を `@Redirect`(`@At("NEW")`)で横取りして
`PriorityOrderedHashSet`(優先度降順でしか反復できない `HashSet` サブクラス)に差し替える方式にした
(`priority/PriorityOrderedHashSet.java` 新規作成)。この方式なら、このフィールドを反復する箇所が
`onServerEndTick()` 内に何箇所あっても、将来AE2側の実装が変わって反復箇所が増減しても、
自動的に優先度順が保証される。差し替え対象は「コンストラクタ内で最初に生成される `HashSet`」
(フィールド宣言順で `craftingCPUClusters` が最初のHashSetフィールドであることをソースで確認済み、
`ordinal = 0`)。

**検証用ログ**: 差し替えが実際に行われた場合、初回のみサーバーログに
`AE2CraftPriority: craftingCPUClusters を優先度順Setに差し替えました` というINFOログが出る。
再ビルド後、まずこのログが出ているかを確認すること。出ていない場合は `@Redirect` 自体が
Mixin適用時に失敗している(`require = 1` なので本来は起動クラッシュになるはずだが、
念のためログでも確認できるようにしてある)。

**今回の教訓**: Mixinで「複数箇所ある同一シグネチャの呼び出しをordinalで個別に狙い撃ちする」方式は、
対象メソッドが単純(呼び出し回数が少なく、今後も変化しにくい)な場合に限って安全。今回のように
比較的複雑で今後も変化しうるメソッドに対しては、**フィールドの実体そのものを差し替える**、
**呼び出し対象を1メソッドに集約してから内部で処理する**など、ordinalの本数に依存しない設計を
優先すべき(`Knowledge/mistakes.md` へ記録予定)。

### 実機動作確認で確認すること(優先順)

1. **起動できるか**: 上記クラッシュ修正後、まず正常に起動するか確認。
2. **`CraftingStatusMenuMixin` の `@Shadow private ICraftingCPU selectedCpu;`**: Mixin適用の
   最初のエラーだけが報告される仕様上、このフィールドの存在は2回とも未検証のまま
   (`ContainerMenuMixin` 自体はVanilla対象なのでこの問題は起きないが、
   `CraftingStatusMenuMixin` 側の `@Shadow` は引き続きAE2依存)。起動できても優先度表示が
   おかしい場合はここを疑う。
3. **`CraftConfirmScreenMixin` の -/+ ボタン・テキストボックス**: クラフト確認画面を開き、
   「キャンセル」と「スタート」の間にちょうど収まって表示されるか確認する(2026-07-16に
   実座標追従方式へ変更済みだが、実機未確認)。
4. **`CraftingCPUScreenMixin` の -/+ ボタン・数値入力欄**(2026-07-16追加): 「Crafting Status」
   タブを開き、「キャンセル」ボタンのすぐ左に表示され、他の要素と重ならないか確認する。
   入力欄に直接数値を打ち込んで優先度が変わるか、-/+ ボタンでも変わるかの両方を確認。
5. **CPU一覧の "@優先度" 表記**(2026-07-16追加・同日中に全CPU表示へ拡張): CPU一覧の
   **全CPU**(選択中に限らない)の名前が "CPU #1@1000" のような表記になっているか確認する。
   `CraftingStatusMenuMixin` の `@Redirect`/`@Shadow` は既定の `require = 1`(厳格)のため、
   万一AE2側の実装が対応していない場合はゲームクラッシュという形で気づく設計にしてある
   (このMixinは既に `selectedCpu` 等でAE2内部依存を抱えているため、新たに `require = 0` の
   緩衝を設けるより、他の既存機能と一蓮托生で厳格に倒す方が一貫性があると判断した)。
6. **AE2の依存バージョン**: `gradle.properties` の `ae2_version` は2026-07-15時点でModrinthから確認した
   最新の1.20.1 Forge向けバージョン。開発時点でさらに新しいバージョンが出ていれば追従を検討。
7. **`CraftingServiceMixin` の実際の効果**(2026-07-16再設計後): サーバーログに
   `AE2CraftPriority: craftingCPUClusters を優先度順Setに差し替えました` が出ているか確認する。
   その上で、**同一のPattern Provider機械を2台以上のCrafting CPUが取り合うシナリオ**
   (例: 1台の圧縮機/溶鉱炉だけをPattern Providerに繋いだ状態で、優先度の異なる2つの
   クラフトジョブを別々のCrafting CPUへほぼ同時に投入する)で、優先度の高い方が先にその機械を
   使い続けるか(=優先度の低い方の投入が後回しになるか)を確認する。Pattern Providerが
   機械ごとに複数台あって余裕がある場合、そもそも「取り合い」が発生しないため優先度による
   違いが観測できない点に注意。
8. **Mixinターゲットのシグネチャずれ**: AE2側のアップデートで対象メソッド
   (`CraftingCPUCluster.writeToNBT/readFromNBT`、`CraftingService`のコンストラクタ(`craftingCPUClusters`
   フィールド初期化順)、`CraftingCpuLogic.trySubmitJob`、`CraftConfirmMenu.startJob`、`CraftConfirmScreen`のコンストラクタ、
   `CraftingStatusMenu`のコンストラクタ、`createCpuList()`、`getOrAssignCpuSerial(...)`、
   `CraftingCPUScreen`のコンストラクタ)の実装が変わっていないか。**GitHubの `forge/1.20.1` ブランチ
   (HEAD)は先行開発中で、実際にリリースされているバージョンより新しい場合があるため、疑わしい
   場合はソース確認だけで判断せず実機で検証すること**(今回のクラッシュの根本原因)。

### 実機で「優先度設定そのものが反映されない」不具合の調査(2026-07-17)

上記「GUI以外全機能していない」調査(2026-07-16)の対処後も、実機ログ
(`https://mclo.gs/Fp8j5DV`)と実プレイ報告で「優先度表示以外は変わっていない」
「Providerの奪い合いで優先度0/1どちらも表示上0になる」「Crafting Statusタブで優先度変更が
できない」ことが確認された。static codeレビューだけでは原因を特定できなかったため、
提出チェーンの各段階(`CraftConfirmMenu#startJob` HEAD/RETURN →
`CraftingService#submitJob` → `CraftingCPUCluster#submitJob` HEAD →
`CraftingCpuLogic#trySubmitJob` RETURN)すべてに `"AE2CraftPriority: ..."` 接頭辞の
診断ログを仕込み、ユーザーに再現・ログ提出してもらう方式で切り分けを進めた。

**判明した事実(ログ`https://mclo.gs/oab2vTs`より)**:

- `CraftConfirmMenuMixin#ae2cp$onStartJobHead` は正しい優先度で毎回発火し、
  `PendingCraftPriority` への書き込みも正しく動いている(クライアント→サーバーの
  ウィジェット→パケット→メニューの経路は正常)。
- `CraftingCpuLogicMixin#ae2cp$onTrySubmitJob` は**一度も発火しない**。すなわち
  `CraftingCpuLogic#trySubmitJob(...)` 自体が呼ばれていない可能性が高い。
- `CraftingStatusMenuMixin` の旧実装(`@Shadow private ICraftingCPU selectedCpu;` を読む方式)は
  「selectedCpuが未選択だったため自動選択します (serial=1)」というログを1ティックごとに
  延々と出し続け、`selectCpu(1)` を呼んでも `selectedCpu` が一切非nullにならないことが確認された。

**`selectedCpu` が更新されない根本原因(2026-07-17に特定・解決済み)**:
AE2の実ソース(`forge/1.20.1` ブランチ、`appeng/menu/me/crafting/CraftingStatusMenu.java`)を
改めて全文取得して精読した結果、`private ICraftingCPU selectedCpu = null;` フィールドは
宣言されているだけで、**クラス内のどこからも代入されていない**ことが判明した
(`selectCpu(int)` 内では比較にのみ使われ、実際に選択中CPUを保持しているのは親クラス
`CraftingCPUMenu` 側のprivateフィールド `cpu` と、`CraftingStatusMenu` 自身の
`selectedCpuSerial` の2つ)。つまりこれはAE2のバージョン差異の問題ではなく、そもそも
本MOD側が「更新されない/意味を持たないフィールド」を読み取っていたという設計ミスだった。

**対処**: `CraftingStatusMenuMixin` から `selectedCpu` フィールドへの依存を完全に撤廃した。
代わりに、このクラスが直接オーバーライドしている `protected void setCPU(ICraftingCPU c)`
(`CraftingCPUMenu` の同名メソッドを `CraftingStatusMenu` 側で上書きしている、実ソースで確認済み)
に `@Inject(at = @At("HEAD"))` を仕込み、渡されたCPUを独自のUniqueフィールド
`ae2cp$currentCpu` へ保存する方式にした。`setCPU` はAE2純正のCPU一覧クリック
(`selectCpu(int)` 経由)、`broadcastChanges()` の自動選択ロジック、こちらの
`ae2cp$ensureCpuSelected()`(何も選択されていない場合に `getGrid().getCraftingService().getCpus()`
から先頭のCPUを選ぶフォールバック)のいずれの経路で呼ばれても必ず通過するため、
どの経路でCPU選択が変わっても確実に追従できる。これにより「Crafting Statusタブで優先度の
変更ができない」不具合は解消される見込み(2026-07-17時点、再ビルド後の実機確認待ち)。

**未解決(このセクション時点)**: `CraftingCpuLogic#trySubmitJob(...)` が一度も呼ばれない理由は
まだ特定できていない。`CraftConfirmMenu#startJob()` の実ソースでは
`if (this.result != null && !this.result.simulation())` を満たさない限り
`ICraftingService#submitJob(...)` 自体を呼ばないため、この条件を直接ログで確認する診断
(`willSubmit` の算出、`CraftConfirmMenuMixin#ae2cp$onStartJobHead`)と、
`CraftingService#submitJob(...)` から `CraftingCPUCluster#submitJob(...)` までの中間地点を
確認する新規診断(`CraftingCPUClusterMixin#ae2cp$onSubmitJobHead`)を追加した。次回のログで
これらが発火するかどうかにより、(a) `startJob()` 内の条件分岐で弾かれている、
(b) `ICraftingService#submitJob` → `CraftingCPUCluster#submitJob` の間で経路がずれている、
(c) `CraftingCPUCluster#submitJob` は呼ばれるが `trySubmitJob` 側のMixin適用自体に問題がある、
のいずれかに切り分けられる想定。

### 3回目の起動クラッシュ修正(2026-07-17): @Shadowは継承しているだけのメンバーを対象にできない

上記の `selectedCpu` 修正を実装した直後、再ビルド・再起動のログで新たな起動クラッシュが判明した:

```
InvalidMixinException: @Shadow method getGrid in ae2craftpriority.mixins.json:CraftingStatusMenuMixin
was not located in the target class appeng.menu.me.crafting.CraftingStatusMenu.
```

`getGrid()` は `CraftingCPUMenu`(親クラス)が宣言しているメソッドで、`CraftingStatusMenu`
(このMixinの実際のターゲット)はオーバーライドせずそのまま継承しているだけだった。Javaの
コンパイル時アクセス制御では `this.getGrid()` は問題なく書けるが、Mixinの `@Shadow` は
コンパイル時のアクセス制御とは別の仕組みで、`@Mixin(value = ...)` で指定した**1クラスの
実バイトコードに直接宣言されているメンバー**しか解決できない。詳細は Obsidian
`Knowledge/mixin-shadow-requires-exact-declaring-class.md` を参照。

**対処**: `getGrid()` の宣言元クラスである `CraftingCPUMenu` 自身を直接ターゲットにした新規
Mixin `CraftingCPUMenuMixin` を追加し、そこで `@Shadow` する。duck interface
`priority/CraftingCPUMenuGridAccess`(`ae2cp$getGrid()`)を実装させ、`CraftingStatusMenuMixin`
側はこのインターフェース経由(`CraftingCPUMenuGridAccess.getGridOrNull(this)`)で呼び出す方式に
変更した。Mixinで親クラスに実装させたインターフェースは、実行時のサブクラスインスタンスにも
継承されるため、`CraftingStatusMenu` の実インスタンスからも正しく呼び出せる。

### `trySubmitJob` が呼ばれない謎の追加診断(2026-07-17)

AE2の実ソース(`appeng/me/service/CraftingService.java`)を精読し、`submitJob(...)` が
`CraftingCPUCluster#submitJob(...)` に到達せず早期returnする経路が3つあることを確認した:

1. `job.simulation()` が `true` → `CraftingSubmitResult.INCOMPLETE_PLAN`
2. `target`(`CraftConfirmMenu` の `selectedCpu`。確認画面で「Automatic」のままなら `null`)が
   指定されておらず、`findSuitableCraftingCPU(...)` が候補CPUを1つも見つけられない
   → `NO_CPU_FOUND` または `noSuitableCpu(...)`(内訳: オフライン/稼働中(`isBusy()`)/
   容量不足/`canBeAutoSelectedFor(src)` による除外のいずれか)
3. (見つかった場合)`cpuCluster.submitJob(...)` へ委譲 → 正常経路

`CraftingCPUClusterMixin` のHEADログが出ない場合、上記のどちらで早期returnしているかを
直接特定するため、`CraftingServiceMixin` に `submitJob` のRETURN診断
(`ae2cp$onSubmitJobReturn`。返り値の `successful()`/`errorCode()` をログ出力)を追加した。

### 実機動作確認で確認すること(2026-07-17追加分)

1. **Crafting Statusタブでの優先度変更**: CPUを選択していない状態でタブを開いても
   自動的に先頭CPUが選ばれ、-/+ ボタン・数値入力欄で優先度が変わり、サーバーログに
   `AE2CraftPriority: setCPU(...)` および `AE2CraftPriority: 優先度を...に変更しました` が
   出ることを確認する。
2. **`willSubmit` 診断ログ**: クラフト確認画面で「スタート」を押した際、サーバーログに
   `AE2CraftPriority: startJob HEAD ... willSubmit=true` が出るか確認する。`willSubmit=false`
   の場合は `CraftConfirmMenu#startJob()` 自身がジョブ未提出と判断している(=シミュレーション
   結果が無い、またはUI操作がそもそも `startJob()` を呼べていない)ことになる。
3. **`CraftingCPUCluster#submitJob` HEAD診断ログ**: `AE2CraftPriority: CraftingCPUCluster#submitJob
   HEAD ...` が出るか確認する。(2)が`true`なのにこれが出ない場合、`ICraftingService#submitJob`
   の内部(CPU自動選択 `findSuitableCraftingCPU` 等)で失敗している可能性が高い。
4. **`trySubmitJob` RETURN診断ログ**: `AE2CraftPriority: trySubmitJob RETURN ...` が出るか確認する。
   (3)が出ているのにこれが出ない場合、`CraftingCpuLogicMixin` 側のMixin適用そのものを疑う。
5. **`CraftingService#submitJob` RETURN診断ログ**(2026-07-17追加): `AE2CraftPriority:
   CraftingService#submitJob RETURN ... successful=... errorCode=...` を確認する。
   `successful=false` の場合、`errorCode` の値(`INCOMPLETE_PLAN`/`NO_CPU_FOUND`/
   `NO_SUITABLE_CPU_FOUND`等)で早期returnの原因が特定できる。(2)の `CraftingCPUCluster#submitJob`
   HEADログが出ないのにこちらは出る場合、CPU自動選択(`findSuitableCraftingCPU`)の失敗が
   確定する。

## 既知の技術的制約

- ネットワーク全体での「できるだけ優先度順に完了させる」はベストエフォートであり、厳密な保証はない
- `CraftConfirmScreenMixin` / `CraftingCPUScreenMixin` のウィジェット表示位置は実機での
  目視確認がまだ済んでいない(座標計算自体は実ボタンの実座標を基準にしているため、AE2側の
  スタイル変更には自動追従するはずだが、初回の見た目調整が必要になる可能性はある)
- CPU一覧の "@優先度" 表記は、2026-07-16の再設計により選択中・未選択に関わらず全CPUに表示される
  (サーバー側 `createCpuList()` で名前に焼き込んでから同期するため)
