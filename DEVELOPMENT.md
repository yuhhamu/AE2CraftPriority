# AE2 Crafting Priority — 開発者向け情報

> 本MODの開発にはAnthropicのAIアシスタント「Claude」を活用しています。

プレイヤー向けの使い方は [README.md](README.md) を参照してください。

## パッケージ構成

`src/main/java/com/yuuhamu/ae2craftpriority/` 配下は役割ごとに分かれている。

- `api/` — 他Modから優先度を読み書きするための公開API(`CraftPriorityApi`/`PriorityAdapter`)
- `priority/` — 優先度そのもののデータモデル(duck interface・保持用フィールド・並び替え済みSet等)。
  UI・Mixinのどちらにも依存しない
- `menu/` / `client/` — 優先度設定用の独自画面(`CraftPriorityStepMenu`/`CraftPriorityStepScreen`)
- `mixin/` — Vanilla AE2本体を対象にしたMixin。さらに以下に分かれる:
  - `mixin/core/` — 優先度の保持・スケジューリング本体(CPU/クラスタ/CraftingServiceへの変更。
    UIには依存しない)
  - `mixin/craft/` — クラフト開始フロー(個数設定→優先度設定→CPU選択)
  - `mixin/status/` — AE2純正「Crafting Status」画面・優先度画面へのUI統合
  - `mixin/accessor/` — AE2 GUI内部のprivateフィールドを読み書きする汎用Accessor
- `compat/<addonMod>/` — サードパーティAddon Mod別の対応コード(例: `compat/advancedae/`)。
  Mixinは `compat/<addonMod>/mixin/` にまとめ、Addon本体が未導入でも安全なように
  `ModList.isLoaded(...)` ガード付きの実行時登録専用の `*.mixins.json` を持つ
  (詳細は下記「サードパーティAddon対応」を参照)。新しいAddon Modへ対応する場合も、
  同じ階層構成で `compat/<新しいAddon名>/` を追加する想定。

## 技術的な仕組み(Mixin)

AE2には「複数ジョブが同じ機械を取り合う際の実行優先度」という概念自体が存在しない
(AE2本体でも長年の未実装要望: [Issue #520](https://github.com/AppliedEnergistics/Applied-Energistics-2/issues/520))。
公式APIにもこのレベルのスケジューリングを制御する拡張点は無いため、実際のAE2ソース
(`forge/1.20.1` ブランチ)を確認した上で、必要最小限のMixinで実現している。

**スケジューリング本体(`mixin/core/`):**

1. `appeng.me.cluster.implementations.CraftingCPUCluster` に優先度フィールドを追加
   (`CraftingCPUClusterMixin`)。NBTの読み書き末尾に1キー追記するだけの変更。
2. `appeng.me.service.CraftingService#onServerEndTick()` は、稼働中の全Crafting CPUを
   `craftingCPUClusters` という素の `HashSet`(優先度と無関係な反復順)でtickしている。
   `CraftingServiceMixin` は、このフィールドを `@Shadow @Final @Mutable` でフィールド名
   指定により直接取得し、コンストラクタ末尾(`@Inject(method = "<init>", at = @At("TAIL"))`)で
   `PriorityOrderedHashSet`(`HashSet` のサブクラスで `iterator()` のみオーバーライド、優先度
   降順で反復)へ実体そのものを差し替える。これにより `onServerEndTick()` 内でこのフィールドを
   反復する箇所が何箇所あっても、AE2側の実装が将来変わっても、常に優先度降順の反復順が保証される。
   Pattern Provider側の内部ロジックには一切手を加えていない。フィールドの実体そのものを
   `@Redirect(@At("NEW"))` ではなくフィールド名を直接指定する `@Shadow @Final @Mutable` 方式に
   しているのは、他Modが同じコンストラクタへ独自の `new HashSet<>()` を追加した場合でも
   ordinalのずれで誤ったフィールドを差し替えないようにするため。
3. `CraftingCpuLogicMixin` — `CraftingCpuLogic#trySubmitJob(...)` は、ジョブがどの
   `CraftingCPUCluster` に実際に割り当てられたかが確定する場所。提出成功時に
   `PendingCraftPriority`(ThreadLocal)の値があれば、そのCPUへ適用する。Export Bus/Interface
   経由の自動クラフトなど確認画面を通らない提出には一切影響しない(値が未設定のまま)。
4. `CraftingCPUMenuMixin` — `appeng.menu.me.crafting.CraftingCPUMenu` が持つ
   protectedメソッド `getGrid()` を `@Shadow` し、duck interface `CraftingCPUMenuGridAccess`
   経由でサブクラス(`CraftingStatusMenuMixin` 等)から呼び出せるようにする橋渡し役。
5. `CraftingBlockEntityMixin` — Crafting CPUのブロックエンティティに `IPriorityHost`
   (AE2純正の`PriorityScreen`が汎用的に読み書きするインターフェース)を実装させ、
   優先度画面から直接読み書きできるようにする。

**クラフト開始フロー(`mixin/craft/`):**

6. `CraftAmountMenuMixin` — 個数設定画面で「次へ」を押した際、AE2純正のCPU選択画面
   (`CraftConfirmMenu`)を直接開く呼び出しを `@Redirect` で横取りし、代わりに専用の優先度設定
   画面(`menu.CraftPriorityStepMenu` / `client.CraftPriorityStepScreen`)を開く。これにより
   クラフト開始フローは「個数設定→優先度設定→CPU選択」の3ステップになる。優先度設定画面は
   独立した `AEBaseMenu` 実装で、AE2純正の `PriorityMenu` と同じ `+1/+10/+100/+1000` ボタン付き
   数値入力ウィジェットを流用している。画面を開き直す際の初期値の受け渡しには、AE2純正の
   `PriorityMenu` と同じ `MenuTypeBuilder#withInitialData` 機構(メニュー生成と同時にサーバーから
   クライアントへ初期データを渡す仕組み)を使っている。
7. `CraftConfirmMenuMixin` — CPU選択・開始画面のメニュー(`CraftConfirmMenu`)にジョブ優先度
   フィールドを追加して保持する。`startJob()` の実行区間だけ、その値を `PendingCraftPriority`
   に流し込む。またバニラの `goBack()` は「キャンセル時は個数設定画面(`CraftAmountMenu`)へ戻る」
   を決め打ちしているが、本MODは個数設定とCPU選択の間に優先度設定画面を挟んでいるため、この
   戻り先を優先度設定画面へ `@Redirect` で差し替えている(その際、現在保持している優先度値も
   一緒に引き継ぐ)。

**AE2純正「Crafting Status」画面への統合(`mixin/status/`):**

8. `CraftingStatusMenuMixin` — `appeng.menu.me.crafting.CraftingStatusMenu`(端末のCrafting Status
   タブのメニュー。CPU一覧・稼働状況表示・CPU選択を既に持っている)に、選択中CPUの優先度画面を
   開く機能を追加する。`registerClientAction`/`sendClientAction` というAE2本体のクライアント
   アクション機構(`AEBaseMenu`)経由で変更できるようにし、`CraftingCPUMenu extends AEBaseMenu`
   を「見せかけの継承」してこれらのprotectedメンバーにコンパイル時アクセスしている。AE2本体の
   クラスや既存フィールドは一切変更していない(完全に新規追加のフィールド・メソッドのみ)。
   選択中CPUの取得は、このクラスが直接オーバーライドしている
   `protected void setCPU(ICraftingCPU c)` に `@Inject(at = @At("HEAD"))` を仕込み、渡された
   CPUを独自のUniqueフィールド `ae2cp$currentCpu` へ保存する方式にしている。`setCPU` はAE2純正の
   CPU一覧クリック・`broadcastChanges()` の自動選択ロジック・本Mod側のフォールバック選択の
   いずれの経路で呼ばれても必ず通過するため、どの経路でCPU選択が変わっても確実に追従できる。
9. `CraftingCPUScreenMixin` — 「キャンセル」ボタンのすぐ左に、優先度画面を開くレンチアイコンの
   ボタンを追加する。**対象は `CraftingStatusScreen` ではなく、その親クラス
   `appeng.client.gui.me.crafting.CraftingCPUScreen`**。「キャンセル」ボタン(`cancel`)は
   `CraftingStatusScreen` 自身ではなくこの親クラスのコンストラクタで作られたprivateフィールド
   であり、Mixinの `@Shadow` はフィールドを宣言しているクラス自体を対象にしないと参照できない
   ため、あえて子クラスではなく親クラスをMixin対象にしている。`CraftingCPUScreen` は個別
   Crafting CPU画面にも使われる共通クラスのため、`this.menu instanceof
   CraftingStatusPriorityControl` で「Crafting Status」タブの場合だけに限定している。ボタンの
   座標は `cancel` ボタンの実座標(`getX()`/`getY()`/`getHeight()`)を基準に毎フレーム
   (`updateBeforeRender`)再配置している。
10. `PriorityScreenMixin` — AE2純正の優先度画面(`PriorityScreen`)を、本MODが追加した優先度
    ホスト(`CraftingPriorityHostMarker`)から開いた場合にヒント文言を差し替え、「戻る」タブの
    アイコン・ラベルを「クラフト状況」に見えるよう上書きする(`WidgetContainerAccessor`/
    `TabButtonAccessor` という `mixin/accessor/` のAccessor経由でAE2内部のprivateフィールドを
    直接読み書きしている)。
11. CPU一覧の "@優先度" 表記(`CraftingStatusMenuMixin` 内) — **サーバー側の
    `createCpuList()`**(CPU一覧をクライアントへ同期するデータを作る、`CraftingStatusMenu` の
    privateメソッド)内で `ICraftingCPU#getName()` の呼び出しを `@Redirect` で横取りし、選択中
    かどうかに関わらず全CPU分の表示名へ `@<優先度>` を追記する方式。`ICraftingCPU#getName()` は
    `appeng.api.networking.crafting` パッケージの**公開API**であり、他のMixinが依存している
    AE2内部実装より変わりにくい。名前が未設定(null)のCPUについては、クライアント側の従来の
    フォールバック表示("CPUs #N")と同じ書式をサーバー側で再現してから優先度を追記するため、
    `getOrAssignCpuSerial(ICraftingCPU)`(private)も `@Shadow` している。
12. `ContainerMenuMixin` — **AE2ではなくMinecraft Vanilla本体**の
    `net.minecraft.world.inventory.AbstractContainerMenu#broadcastChanges()` に注入し、
    `this instanceof CraftingStatusPriorityControl` の場合のみ優先度の再計算処理を呼ぶ。AE2の
    クラス階層に直接Injectすると、導入されているAE2バージョン次第でそのメソッドをオーバーライド
    していない場合に起動クラッシュしうるため、AE2のバージョンに一切依存しないVanillaの基底クラス
    に注入することでこれを回避している。他のAE2メニュー(クラフト確認画面・各種端末画面など)には
    一切影響しない。**Vanillaクラスが対象のため、他のMixinと異なり `remap = false` を付けていない**
    (SRG/MCPマッピング解決が必要なため)。

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

他Modから優先度を読み書きできる公開APIを提供している。

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
  `CraftPriorityApi.getPriority(...)` 経由に統一しているため、アダプタ登録さえ済ませれば表示にも
  自動的に反映される。
- この仕組みは「開発者向けAPI」という要件と「AdvancedAE等サードパーティCPU型への対応」という
  要件を1つのレジストリパターンで同時に満たす設計になっている。

## サードパーティAddon対応

以下のAE2アドオンMODとの併用を確認済み: AdvancedAE / Mega Cells / ExtendedAE / Applied Mekanistics。

### AdvancedAE(Quantum Computer) — `compat/advancedae/`

AdvancedAEは独自の `AdvCraftingCPUCluster`(`IAECluster` 実装、Vanillaの `CraftingCPUCluster` を
継承していない別クラス)と `AdvCraftingCPU`(`ICraftingCPU` 実装)、および `CraftingService` に
`@Unique Set<AdvCraftingCPUCluster> advancedAE$advCraftingCPUClusters` を追加する独自Mixinを
持っており、Vanillaの `craftingCPUClusters` とは別の反復ループ・CPU選択ロジックで動作する。
そのため専用の対応コードを用意している。

- `compat/advancedae/mixin/AdvCraftingCPUClusterMixin` — `AdvCraftingCPUCluster` に
  `PriorityHolder` を実装させ、`writeToNBT`/`readFromNBT` の末尾で優先度をNBT永続化する
  (`mixin/core/CraftingCPUClusterMixin` と同じパターン)。
- `compat/advancedae/mixin/AdvCraftingCPUMixin` — `AdvCraftingCPU` の private `cluster`
  フィールドを読むための `@Accessor`。
- `compat/advancedae/mixin/AdvCraftingCPULogicMixin` — AdvancedAE独自のCPUロジックへの対応
  (Vanillaの `mixin/core/CraftingCpuLogicMixin` に相当)。
- `compat/advancedae/mixin/AdvCraftingBlockEntityMixin` — AdvancedAE独自のブロックエンティティへの
  対応(Vanillaの `mixin/core/CraftingBlockEntityMixin` に相当)。
- `compat/advancedae/mixin/QuantumComputerMenuMixin` — Quantum ComputerのCPU一覧(クラフト状況
  タブのコピー画面)にも優先度(`@N`)を表示させる(Vanillaの `CraftingStatusMenuMixin` の
  該当処理に相当)。
- `compat/advancedae/AdvancedAeCpuAdapter implements PriorityAdapter` — `AdvCraftingCPU` を
  クラスタへ辿り、優先度を読み書きする。
- `compat/advancedae/AdvancedAeCompat.init()` — `Mixins.addConfiguration(...)` と
  `CraftPriorityApi.registerAdapter(...)` を行う。**必ず `ModList.isLoaded("advanced_ae")` で
  ガードされた経路からのみ呼び出すこと**(クラス参照自体がAdvancedAE未導入時に `NoClassDefFoundError`
  を招くため)。同じ理由で、`ae2craftpriority-advancedae.mixins.json` は `build.gradle` の
  `mixin{}` DSLや `mods.toml` の `[[mixins]]` には列挙せず、上記の `ModList` ガード付き経路
  (`Mixins.addConfiguration(...)`)からのみ実行時に登録する。DSL側に列挙するとAdvancedAE
  未導入環境でも無条件ロードされ、存在しないフィールドへの `@Shadow` でクラッシュする。

優先度はAdvancedAEのQuantum Computer同士の実行順序のみを制御し、Vanilla CPUとQuantum Computerの
どちらを使うかというAdvancedAE自身のCPU選択ロジックには介入しない設計にしている。

## 対応バージョン範囲の根拠

READMEには「AE2 15.x系列(Minecraft 1.20.1向け)」とだけ書いているが、これは特定の1バージョンに
決め打ちせず、実際にこのMODが依存しているAE2内部APIを洗い出した上で判断したもの。

**このMODが依存しているAE2の内部クラス・メソッド一覧**(すべて `appeng.api.*` 配下の公開APIではなく、
AE2の内部実装。[公式Javadoc](https://appliedenergistics.org/javadoc/)には載っていない):

| Mixin | 対象クラス | 依存しているメンバー |
|---|---|---|
| `core.CraftingCPUClusterMixin` | `appeng.me.cluster.implementations.CraftingCPUCluster` | `writeToNBT` / `readFromNBT` |
| `core.CraftingServiceMixin` | `appeng.me.service.CraftingService` | コンストラクタ内 `craftingCPUClusters` フィールドの `new HashSet<>()` 初期化(1箇所目のHashSet生成) |
| `core.CraftingCpuLogicMixin` | `appeng.crafting.execution.CraftingCpuLogic` | `trySubmitJob` |
| `craft.CraftAmountMenuMixin` | `appeng.menu.me.crafting.CraftAmountMenu` | `confirm` 内のCPU選択画面オープン呼び出し |
| `craft.CraftConfirmMenuMixin` | `appeng.menu.me.crafting.CraftConfirmMenu` | `startJob`、`goBack` |
| `status.CraftingStatusMenuMixin` | `appeng.menu.me.crafting.CraftingStatusMenu` | コンストラクタ、`setCPU`、`CraftingCPUMenu` 継承構造、`createCpuList()`、private `getOrAssignCpuSerial(...)` |
| `status.CraftingCPUScreenMixin` | `appeng.client.gui.me.crafting.CraftingCPUScreen` | コンストラクタ、`updateBeforeRender`、`cancel` フィールド |
| `status.PriorityScreenMixin` | `appeng.client.gui.implementations.PriorityScreen` | コンストラクタ |
| `ContainerMenuMixin` | `net.minecraft.world.inventory.AbstractContainerMenu`(**Vanilla**) | `broadcastChanges` |

**判断の根拠**:

- 上記のうち `ContainerMenuMixin` 以外は全てAE2の内部実装(非公開API)に依存しており、AE2自身は
  これらのクラス構成・メソッド名についてバージョン間の互換性を一切保証していない。ただし
  `CraftingStatusMenuMixin` の `@Redirect` 対象そのもの(`ICraftingCPU#getName()`)は
  `appeng.api.networking.crafting` パッケージの公開APIであり、この1点に限れば他の内部依存より
  変わりにくい。
- これらのクラスは全て `appeng.menu.me.crafting.*` / `appeng.client.gui.me.crafting.*` /
  `appeng.me.cluster.implementations.*` / `appeng.me.service.*` / `appeng.crafting.execution.*`
  という、AE2がMinecraft 1.20.1向けに使っている `forge/1.20.1` ブランチ固有のパッケージ構成に
  属しており、AE2はMCバージョンごとに内部実装を作り直しているため、`15.x` という版数自体が
  この1.20.1向けブランチに紐付いている。
- 実際に、AE2 15.4.10(実機で動作確認済み)と `forge/1.20.1` ブランチのより新しいコミットを
  ソースで突き合わせた結果、上記表のメソッド名・シグネチャ・コンストラクタ引数は一致していた。
- 以上から、「AE2 15.4.10単体でのみ動作確認済み」ではあるが、「AE2 15.x系列(Minecraft 1.20.1
  向けの `forge/1.20.1` ブランチから作られたビルド)全体で構造的に互換性がある可能性が高い」と
  判断できる。これはあくまでソースコードの突き合わせに基づく推定であり、実機での動作確認は
  15.4.10のみである点に注意。
