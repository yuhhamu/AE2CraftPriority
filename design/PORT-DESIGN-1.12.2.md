# AE2 Crafting Priority ─ Minecraft 1.12.2 移植 実装設計書

作成日: 2026-07-20
対象読者: 本MODを1.12.2向けに実装する開発者
ステータス: **設計のみ。コードは未実装。**

前提ソース:
- AE2 1.12.2版: `/tmp/ae2-1.12.2` (branch `rv6-1.12`, worktree)
- AE2 1.20.1版(現行・比較用): `/tmp/ae2-1.20.1` (branch `forge/1.20.1`, worktree)
- 本MODの1.20.1版DEVELOPMENT.md記載の8 Mixin + 追加調査で判明した3 Mixin、という前提で調査を開始したが、**その後、1.20.1版の実際の`ae2craftpriority.mixins.json`を入手できたため、これを実装対象の正とする**。実装対象は以下の**計12個**。

```json
{
  "mixins": [
    "CraftAmountMenuMixin", "CraftConfirmMenuMixin", "CraftingBlockEntityMixin",
    "CraftingCPUMenuMixin", "CraftingStatusMenuMixin", "CraftingCPUClusterMixin",
    "CraftingServiceMixin", "CraftingCpuLogicMixin"
  ],
  "client": [
    "CraftingCPUScreenMixin", "PriorityScreenMixin", "TabButtonAccessor", "WidgetContainerAccessor"
  ]
}
```

この正の一覧と、DEVELOPMENT.md記載の8項目+推測3項目という当初の前提との**差分**は以下の通り。

1. **`ContainerMenuMixin`(DEVELOPMENT.md項目8、Vanilla `AbstractContainerMenu#broadcastChanges`対象)はこのリストに存在しない**。DEVELOPMENT.md記載時点から設計が変わり、この仕組み自体が廃止されたか、別のMixin(例えば`CraftingCPUClusterMixin`や`CraftingServiceMixin`)に統合された可能性が高い。→ 3.8節を参照(注記として残してある)。
2. **`CraftConfirmScreenMixin`という独立クラスは存在せず、`PriorityScreenMixin`のみが存在する**。3.5節で立てていた「両者は統合された可能性が高い」という推測が、ファイル一覧レベルで裏付けられた形になる。
3. **`TabButtonAccessor`と`WidgetContainerAccessor`という、クライアント側Mixinが2つ漏れていた**。名前から判断して、SpongePowered Mixinの`@Accessor`/`@Invoker`パターン(非公開フィールド/メソッドへリフレクションなしで型安全にアクセスするインターフェース)を使っていると推測される。3.12・3.13節を参照。
4. DEVELOPMENT.md記載の8項目のうち、`CraftConfirmMenuMixin` / `CraftingBlockEntityMixin` / `CraftingCPUMenuMixin` / `CraftingStatusMenuMixin` / `CraftingCPUClusterMixin` / `CraftingServiceMixin` / `CraftingCpuLogicMixin` / `CraftingCPUScreenMixin`の8個は、正の一覧にもそのまま含まれており、当初の前提通りで問題ない。`CraftAmountMenuMixin`も含めれば9個が「名称としては確定」している。

> **重要な限定事項**: `CraftAmountMenuMixin` / `CraftingBlockEntityMixin` / `PriorityScreenMixin` / `TabButtonAccessor` / `WidgetContainerAccessor`の5個は、**1.20.1版addon MODの実ソース本文を確認できていません**(確認できたのはクラス名の一覧〔ファイル一覧・mixins.json〕のみ)。これらの節の内容は「1.20.1 AE2本体のソース」「README記載の3ステップフロー」「クラス名から推測される役割」から逆算した**推測**であり、信頼度は「低」です。実装着手前に必ずこれら5クラスの実ソースを読み、本書の推測が正しいか検証してください。それ以外の7 Mixin(実ソースまで確認できた分)についても、1.20.1側の実ソースと1.12.2側の実ソースを本書の記述と突き合わせてから着手することを強く推奨します。GitHubブランチのHEADは先行開発中でリリース版より新しい場合があるため、**実際に導入するAE2バイナリのバージョンで最終確認**してください。

---

## 1. 概要

### 1.1 前回調査の要約

- 1.12.2にも「Crafting CPU」による自動クラフト機構は存在し、しかも**クラスタ管理層(`appeng.me.cluster.implementations.CraftingCPUCluster`)は1.20.1と完全に同じ完全修飾クラス名**で存在する。
- CPU集合を保持する層は1.12.2では`appeng.me.cache.CraftingGridCache`(1.20.1は`appeng.me.service.CraftingService`)で、内部フィールド構造(`Set<CraftingCPUCluster> craftingCPUClusters`)はほぼ同一。
- ただし1.20.1の`CraftingCpuLogic`(CPU単位のジョブ実行ロジックを分離した専用クラス)に相当する**独立クラスは1.12.2には存在しない**。CPU選定〜ジョブ投入は`CraftingGridCache#submitJob(...)`という1メソッドに集約されている。
- UI層(Container/GuiScreen)は1.12.2がvanilla旧APIのため、**クラス構造(継承関係)は近いが実装(特にウィジェット追加API)は別物**。1.20.1の`WidgetContainer#addButton`のようなAE2独自の抽象化は1.12.2に存在せず、vanilla `GuiScreen.buttonList`への直接addで実装されている。
- ツールチェインはJava8 / ForgeGradle 2.3-SNAPSHOT / Gradle 4.8 / MCPマッピング(`snapshot_20171003`, SRGベース) / Forge `14.23.5.2847`。AE2自体はMixinを使わずAccess Transformer(`appeng_at.cfg`)方式。

### 1.2 今回追加で判明した重要な構造差(実装者が最初にハマりやすい点)

**「個数設定→優先度設定→CPU選択」の3ステップ遷移の実装方式が1.12.2と1.20.1で全く別のレイヤーにある。**

1.20.1では、個数設定メニュー`appeng.menu.me.crafting.CraftAmountMenu`自身が`confirm(int amount, boolean craftMissingAmount, boolean autoStart)`という**メニュークラスのメソッド**を持ち、その中で`MenuOpener.open(CraftConfirmMenu.TYPE, player, locator)`を呼んで次画面を開いている(`/tmp/ae2-1.20.1/src/main/java/appeng/menu/me/crafting/CraftAmountMenu.java` 109行目〜)。したがって`CraftAmountMenuMixin`はおそらく**このメニュークラスのメソッドを対象にしている**。

一方1.12.2では、個数設定コンテナ`appeng.container.implementations.ContainerCraftAmount`は単なるデータ保持クラス(`itemToCreate`, `craftingItem`スロットのみ)であり、**次画面への遷移ロジックを一切持たない**。実際の遷移ロジックは、GUI(クライアント)側の「Next」ボタン押下 → `NetworkHandler.instance().sendToServer(new PacketCraftRequest(amount, isShiftKeyDown()))`でサーバへ送信 → **`appeng.core.sync.packets.PacketCraftRequest#serverPacketData(...)`(パケットハンドラクラス)** がサーバ側で`Platform.openGUI(player, te, side, GuiBridge.GUI_CRAFTING_CONFIRM)`を呼んで次のGUI(`ContainerCraftConfirm`)を開く、という**完全に別レイヤー(ネットワークパケットハンドラ)**で実装されている。

つまり:
- 1.20.1: `CraftAmountMenu#confirm()`(メニュークラス自身のメソッド)を Mixin
- 1.12.2の対応候補: **`ContainerCraftAmount`ではなく`appeng.core.sync.packets.PacketCraftRequest#serverPacketData(...)`** を Mixinする必要がある可能性が高い

この構造差は、実ソース未確認の`CraftAmountMenuMixin`の移植方針に直接影響するため、**最優先で実ソース確認すべき箇所**として3章に詳細を記載する。

### 1.3 全体方針

- 各Mixinは「1.20.1で対象にしていたクラス・メンバーの役割」を1.12.2側の対応クラス・メンバーに読み替えて実装する。役割対応表は3章を参照。
- サーバ側ロジック(CPUクラスタ・スケジューリング・NBT)は構造が近いため先に実装し、UI層は後回しにする(8章「推奨実装順序」参照)。
- UIウィジェットは1.12.2では新規実装になるため、座標はハードコードせず、AE2既存ボタンの実座標を実行時に取得して動的配置する方針を維持する(5章)。
- Mixin導入はAE2自体が採用していないため、1.12.2でMixinを使う定番手法(`MixinBooter`系ライブラリ利用、または自前coremod)を6章にまとめる。

---

## 2. 全体アーキテクチャ対応図

```
[1.20.1]                                          [1.12.2]
appeng.me.service.CraftingService                 appeng.me.cache.CraftingGridCache
  └ craftingCPUClusters: Set<CraftingCPUCluster>     └ craftingCPUClusters: Set<CraftingCPUCluster>  (同名フィールド)
  └ findSuitableCraftingCPU(...)                      └ submitJob(...) 内にCPU選定ロジックが同居
appeng.crafting.execution.CraftingCpuLogic          (該当クラスなし。CraftingCPUCluster.submitJob() に統合)
  └ trySubmitJob(...)
appeng.me.cluster.implementations.CraftingCPUCluster  appeng.me.cluster.implementations.CraftingCPUCluster (完全一致パッケージ・クラス名)
  └ writeToNBT / readFromNBT (Saveable経由)             └ writeToNBT(NBTTagCompound) / readFromNBT(NBTTagCompound)

appeng.menu.me.crafting.CraftAmountMenu#confirm()    appeng.core.sync.packets.PacketCraftRequest#serverPacketData()  ※推測・要検証
appeng.menu.me.crafting.CraftConfirmMenu#startJob()  appeng.container.implementations.ContainerCraftConfirm#startJob()
appeng.client.gui.me.crafting.CraftConfirmScreen     appeng.client.gui.implementations.GuiCraftConfirm
appeng.menu.me.crafting.CraftingStatusMenu           appeng.container.implementations.ContainerCraftingStatus
appeng.menu.me.crafting.CraftingCPUMenu              appeng.container.implementations.ContainerCraftingCPU
appeng.client.gui.me.crafting.CraftingCPUScreen      appeng.client.gui.implementations.GuiCraftingCPU
appeng.blockentity.crafting.CraftingBlockEntity      appeng.tile.crafting.TileCraftingTile  ※推測・要検証(下記3.10参照)
net.minecraft.world.inventory.AbstractContainerMenu  net.minecraft.inventory.Container
  #broadcastChanges()                                  #detectAndSendChanges()
```

---

## 3. Mixin対応表(12個、`ae2craftpriority.mixins.json`実物ベース)

各項目は以下のフォーマットで記載します。

- **目的**: 1.20.1版でこのMixinが何をしているか(DEVELOPMENT.md記載内容の要約)
- **1.12.2対象クラス**: 完全修飾クラス名
- **対象メンバー**: メソッド/フィールドのシグネチャと、そのメンバーが対象クラス自身の宣言か親クラスからの継承かを明記(`@Shadow`は対象クラス自身の宣言にしか使えないため重要)
- **実装方針**: 具体的なMixinアノテーション種別と挿入点の設計案
- **信頼度**: 高(実ソースで完全確認)/中(構造から強く推測できるが1.20.1側addonソース未確認)/低(推測の域を出ない、実ソース確認必須)
- **チェックリスト**: 実装前に目視確認すべき項目(教訓1対応)

---

### 3.1 CraftingCPUClusterMixin

**目的**: `writeToNBT`/`readFromNBT`の末尾に優先度フィールドを1キー追記する。

**1.12.2対象クラス**: `appeng.me.cluster.implementations.CraftingCPUCluster`(パッケージ・クラス名は1.20.1と完全一致)

**対象メンバー**:
- `public void writeToNBT(final NBTTagCompound data)` ─ **クラス自身に直接宣言**(1.12.2ソース1145行目)。オーバーライドではなく`CraftingCPUCluster`自身の独自メソッド(インターフェース由来ではない)。
- `public void readFromNBT(final NBTTagCompound data)` ─ **クラス自身に直接宣言**(1215行目)。

**実装方針**:
- 1.20.1と同様、両メソッドの末尾に`@Inject(method = "writeToNBT", at = @At("TAIL"))` / `@Inject(method = "readFromNBT", at = @At("TAIL"))`で優先度フィールドの読み書きを追加する。
- NBT APIは1.12.2の`NBTTagCompound`(`net.minecraft.nbt.NBTTagCompound`)を使用。`setInteger("craftPriority", ...)` / `getInteger("craftPriority")`(4章のAPI早見表参照)。
- このMixinは`appeng.*`パッケージのクラスを対象とするため、**`remap = false`**(6章参照)。
- メソッドはオーバーロードがなく、シグネチャの曖昧さはない。`@Redirect`ではなく`@Inject`で完結するため、ordinal問題(教訓3)は発生しない。

**信頼度**: 高(1.12.2ソースで両メソッドの存在・シグネチャ・末尾位置まで直接確認済み)

**チェックリスト**:
- [ ] `NBTTagCompound`のimportパスが`net.minecraft.nbt.NBTTagCompound`であることを確認(1.20.1の`CompoundTag`と混同しないこと)
- [ ] 優先度フィールドを保持するクラス側フィールド(このMixinが`@Shadow`ではなく自前で追加する新規フィールド)の初期値・型を1.20.1版と揃える
- [ ] `readFromNBT`が呼ばれるタイミング(`done()`メソッド内、コアブロック復帰時)でも正しく優先度が復元されることを確認する。`done()`(1199行目)は`this.readFromNBT(core.getPreviousState())`を呼んでいるため、優先度の永続化はここを経由する経路も通る。

---

### 3.2 CraftingServiceMixin

**目的**: コンストラクタ内`craftingCPUClusters`フィールド(`new HashSet<>()`)を優先度順反復可能なSet実装に`@Redirect`で差し替える。

**1.12.2対象クラス**: `appeng.me.cache.CraftingGridCache`(1.20.1の`CraftingService`に相当。クラス名が異なる点に注意)

**対象メンバー**:
- `private final Set<CraftingCPUCluster> craftingCPUClusters = new HashSet<>();` ─ **フィールド初期化子**(1.12.2ソース104行目)。コンストラクタ本体内の明示的な代入文ではなく、フィールド宣言時の初期化式である点は1.20.1と同じ。javac変換後は`<init>`バイトコード内の`NEW java/util/HashSet`命令になるため、`@Redirect(method = "<init>", at = @At(value = "NEW", target = "java/util/HashSet"))`は1.20.1と同じ要領で機能する見込み。

**実装方針**:
- 1.20.1と同じ`@At("NEW")`パターンを踏襲する(教訓3: ordinal非依存の設計を優先、という方針そのものが両バージョンで有効)。
- コンストラクタ`public CraftingGridCache(final IGrid grid)`(118行目)内で`HashSet`の`NEW`が他に出現しないか要確認(他のフィールドは`craftingProviders`, `emitableItems`も`new HashSet<>()`を使っているが、これらは別フィールドの初期化式であり、`@At("NEW")`は対象記述で`target`をフィールド型やコンストラクタ引数型で絞り込むか、`ordinal`を使わずに済むよう「代入先のフィールド」で一意に特定できるかを確認すること。複数の`HashSet`生成がある場合は、教訓3の「ordinal依存の危険性」が直接該当するため、`@ModifyVariable`や`@Redirect`の`target`をより厳密に(コンストラクタディスクリプタ全体で)指定するか、フィールド代入自体を`@Redirect`する設計(例: `@Redirect`で`Fieldに対するPUTFIELD`を狙う)を検討する。
- このMixinもAE2内部クラス対象のため`remap = false`。

**信頼度**: 高(フィールド宣言のリテラル一致まで確認済み)。ただし同一コンストラクタ内に複数の`new HashSet<>()`が存在する点(104,105,110行目)は1.20.1にも同様の並びがあり(116,117,... 行目)、**Redirectのターゲット特定方法は1.20.1版の実装を先に確認し、同じ手法(フィールド型やコンストラクタ引数を使った絞り込み)を踏襲するのが安全**。

**チェックリスト**:
- [ ] `CraftingGridCache`コンストラクタ内の`new HashSet<>()`出現箇所を全て列挙し(104, 105, 110行目)、1.20.1版Mixinがどの絞り込み方法(型パラメータ・変数名・ordinal等)を使っているか確認する
- [ ] 1.20.1版が万一ordinalで絞り込んでいる場合は、1.12.2でも同じ出現順になっているか(このMixinの実装のためだけに)必ず目視で数え直す。教訓3の通り、ずれると**クラッシュせず黙って無効化される**ため特に危険。
- [ ] 差し替え後のSet実装がAE2側の他のロジック(`ImmutableSet.copyOf(new ActiveCpuIterator(this.craftingCPUClusters))`等、567行目)から見て正しくIterableとして振る舞うか確認

---

### 3.3 CraftingCpuLogicMixin

**目的**: `trySubmitJob(...)`(ジョブがどのCrafting CPU Clusterに割り当てられたか確定する箇所)に優先度を適用する。

**1.12.2対象クラス**: **該当する独立クラスは存在しない。** 1.20.1の`CraftingCpuLogic`(CPU単位の実行ロジックを`CraftingCPUCluster`から分離した専用クラス)に相当するものは1.12.2にはなく、その責務は`appeng.me.cache.CraftingGridCache#submitJob(...)`(CPU選定)と`appeng.me.cluster.implementations.CraftingCPUCluster#submitJob(...)`(選定後の実投入)の2メソッドに分散している。

**対象メンバー**:
- `appeng.me.cache.CraftingGridCache#submitJob(final ICraftingJob job, final ICraftingRequester requestingMachine, final ICraftingCPU target, final boolean prioritizePower, final IActionSource src)` ─ **クラス自身に直接宣言**(505行目)。CPU候補のフィルタリング(`isActive() && !isBusy() && availableStorage >= job.getByteTotal()`)と`Collections.sort(validCpusClusters, (a, b) -> {...})`によるソートを行い、`validCpusClusters.get(0)`を選ぶ。**優先度を注入する最有力ポイントはこのComparatorラムダ**。
- `appeng.me.cluster.implementations.CraftingCPUCluster#submitJob(final IGrid g, final ICraftingJob job, final IActionSource src, final ICraftingRequester requestingMachine)` ─ **クラス自身に直接宣言**(868行目)。CPU確定後の実ジョブ投入。1.20.1の`CraftingCpuLogic#trySubmitJob`が持つ「アイテム抽出・CraftingLink生成」等の責務はこちら側にある。

**実装方針**:
- 優先度によるCPU選定順序の変更は、`CraftingGridCache#submitJob`内の`Collections.sort(validCpusClusters, ( firstCluster, nextCluster ) -> {...})`ラムダを`@Redirect`で丸ごと差し替える(`@Redirect(method = "submitJob", at = @At(value = "INVOKE", target = "Ljava/util/Collections;sort(Ljava/util/List;Ljava/util/Comparator;)V"))`)のが最も素直。1.20.1で複数クラス(`CraftingServiceMixin`の`craftingCPUClusters`差し替え + `CraftingCpuLogicMixin`のtrySubmitJob改変)に分かれていた責務が、1.12.2では**この1メソッドの1呼び出し箇所に統合できる**可能性が高い。
- `submitJob`の呼び出し元シグネチャに`ICraftingJob job, ICraftingRequester requestingMachine, ICraftingCPU target, boolean prioritizePower, IActionSource src`という同名メソッドは他にオーバーロードが存在しないか確認要(なければ`method = "submitJob"`のみで一意)。
- 1.20.1側で「優先度」をどこに保持しているか(`CraftingCPUCluster`インスタンスのフィールドとして持つ設計であれば、Comparatorの比較キーに追加するだけで済む)を確認し、同じ設計を1.12.2の`CraftingCPUCluster`(3.1で優先度フィールドを追加済み)に対して行う。

**信頼度**: 中(1.12.2側の対応箇所・実装方針は実ソースで具体的に特定できたが、「1.20.1の2クラス構成をどう1メソッドに統合するか」の設計判断は実装者の裁量に委ねられる部分がある)

**チェックリスト**:
- [ ] `CraftingGridCache#submitJob`が本当に1つしかオーバーロードを持たないか確認(505行目周辺で`submitJob`という名前のメソッドが他にないか`grep -n "submitJob" CraftingGridCache.java`で確認)
- [ ] `Collections.sort`呼び出しが対象メソッド内で1回しか出現しないか確認(複数回あれば`@Redirect`のordinal問題が発生するため、教訓3に従い型・変数名での絞り込みを検討)
- [ ] 3.1で追加した優先度フィールドに`CraftingCPUCluster`側からアクセスするためのgetterを、このMixinまたは3.1のMixinのどちらで宣言するか設計時に決めておく(重複宣言はMixin適用エラーの原因になる)

---

### 3.4 CraftConfirmMenuMixin

**目的**: `startJob()`の実行区間に優先度をThreadLocal経由で流し込む。

**1.12.2対象クラス**: `appeng.container.implementations.ContainerCraftConfirm`(1.20.1の`CraftConfirmMenu`に相当)

**対象メンバー**:
- `public void startJob()` ─ **クラス自身に直接宣言**(323行目、引数なし)。

**実装方針**:
- 1.20.1と同じくメソッド全体を`@Inject(at = @At("HEAD"))`でThreadLocalに優先度をセットし、`@Inject(at = @At("RETURN"))`または`@Inject(at = @At("TAIL"))`でクリアする2点挿入方式を踏襲できる。
- `startJob()`内部でどこまで処理が進むか(`ICraftingLink`生成やCPUへの`submitJob`呼び出しに到達するまでの経路)を事前に読み、ThreadLocalの値が3.3で改変した`CraftingGridCache#submitJob`/`CraftingCPUCluster#submitJob`の実行スタック内で確実に読み取れることを確認する。
- `remap = false`(AE2内部クラス対象)。

**信頼度**: 高(メソッド名・シグネチャ・宣言クラスまで確認済み)

**チェックリスト**:
- [ ] `startJob()`実行中に別スレッド(非同期タスク)へ処理が渡らないか確認する。1.12.2はメインスレッド同期実行が基本だが、`beginCraftingJob`が`Future<ICraftingJob>`を返す非同期設計(`PacketCraftRequest`参照)であるため、ThreadLocalを読み出すタイミングが**別スレッドの`Future`完了コールバック内にならないか**を必ず確認すること(1.20.1と挙動が異なる可能性がある重要な差分)
- [ ] `startJob()`内で例外がスローされるパスがある場合、ThreadLocalのクリア漏れ(try-finally化)を1.20.1版の実装と同様に行う

---

### 3.5 CraftConfirmScreenMixin (1.20.1では `PriorityScreenMixin` に相当する可能性が高い ─ 3.9参照)

> 本項はDEVELOPMENT.md記載の8項目としての`CraftConfirmScreenMixin`を扱う。ただしコーディネーターからの情報により、1.20.1版addonの実装では本Mixinが`PriorityScreenMixin`という名前にリネーム・統合されている可能性が高いとのことです。実装者は**まず`PriorityScreenMixin.java`の実ソースを確認し**、本項と3.9の内容を統合して読み替えてください。

**目的**: クラフト確認画面に優先度入力UIを追加する。コンストラクタ、`updateBeforeRender`、`WidgetContainer#addButton`の3箇所が対象。

**1.12.2対象クラス**: `appeng.client.gui.implementations.GuiCraftConfirm`(1.20.1の`CraftConfirmScreen`に相当)。`public class GuiCraftConfirm extends AEBaseGui`(58行目)、`AEBaseGui extends GuiContainer`(vanilla)。

**対象メンバー**:
- コンストラクタ(引数構成は`ContainerCraftConfirm`と同様のホスト引数を取る想定。実ソースの引数リストを要確認)。
- `drawScreen(final int mouseX, final int mouseY, final float btn)` ─ **クラス自身に直接宣言**(137行目)。1.20.1の`updateBeforeRender`に相当する描画前フック。vanilla `GuiScreen#drawScreen`のオーバーライドである点に注意(`GuiContainer`経由で継承される抽象メソッドではなく、`GuiCraftConfirm`自身が独自にオーバーライドしている)。
- ボタン追加: `this.buttonList.add(this.start)` / `this.buttonList.add(this.selectCPU)` / `this.buttonList.add(this.cancel)`(121, 126, 133行目)。**1.20.1の`WidgetContainer#addButton`に相当するAE2独自ラッパーメソッドは存在しない**。vanilla `GuiScreen`が持つ`protected List<GuiButton> buttonList`フィールドへの直接`add()`呼び出しである。

**実装方針**:
- 「`WidgetContainer#addButton`を`@Redirect`する」という1.20.1のアプローチはそのままでは使えない。代わりに以下のいずれかを設計する:
  - (A) コンストラクタ(またはGUI初期化メソッド`initGui()`)に`@Inject(at = @At("TAIL"))`し、独自の優先度入力ウィジェット(vanillaの`GuiTextField`または自作コンポーネント)を生成して`this.buttonList`(または独自の描画リストフィールド)に直接追加する。`buttonList`はvanilla `GuiScreen`の`protected`フィールドであり、`GuiCraftConfirm`からは継承経由でアクセス可能(親クラスの宣言のため、`GuiCraftConfirm`を対象にした`@Shadow`では「継承フィールド」扱いになる点に要注意。教訓2に従い、**`buttonList`を対象にする`@Shadow`は`GuiCraftConfirm`ではなく実際に宣言している`net.minecraft.client.gui.GuiScreen`を対象にした別Mixinを新設し、duck interface経由でアクセスする**設計が安全)。
  - (B) `drawScreen`に`@Inject(at = @At("TAIL"))`して、優先度ウィジェットの独自描画・入力処理を追加する。
- vanilla `GuiScreen`側の`mouseClicked`/`keyTyped`等の入力ハンドラにも独自ウィジェット用の`@Inject`が必要になる可能性が高い(5章で詳述)。
- `GuiCraftConfirm`自体は`appeng.*`パッケージなので`remap = false`。ただし`GuiScreen#buttonList`等vanillaメンバーを対象にした別Mixinを新設する場合は、そちらは`remap`省略(デフォルトtrue、SRG解決)。使い分けの詳細は6章。

**信頼度**: 中(1.12.2側の対象クラス・メソッド名・ボタン追加パターンは実ソースで確認済みだが、1.20.1版の実際のMixin設計〔`PriorityScreenMixin`〕を未確認のため、「何を優先度UIとして具体的に追加しているか」の対応付けは推測)

**チェックリスト**:
- [ ] `GuiCraftConfirm`のコンストラクタ引数リストを実ソースで確認(`ContainerCraftConfirm`または`ITerminalHost`等)
- [ ] `buttonList`が`GuiCraftConfirm`自身ではなく`GuiScreen`(vanilla)の宣言であることを踏まえ、`@Shadow`対象クラスの選定を誤らないこと(教訓2)
- [ ] `initGui()`(vanilla `GuiScreen`のGUI初期化メソッド、1.20.1の`init()`相当)が`GuiCraftConfirm`自身でオーバーライドされているか確認し、ウィジェット生成の注入先を`initGui()`かコンストラクタかを決める
- [ ] `PriorityScreenMixin.java`の実ソースを確認し、本項の内容と統合・訂正する

---

### 3.6 CraftingStatusMenuMixin + CraftingCPUMenuMixin

**目的**: 端末の「Crafting Status」タブのCPU一覧・選択中CPUに優先度UI・表示を統合。`createCpuList()`(private)内で公開API`ICraftingCPU#getName()`の呼び出しを`@Redirect`。

**1.12.2対象クラス**:
- `appeng.container.implementations.ContainerCraftingStatus extends ContainerCraftingCPU`(1.20.1の`CraftingStatusMenu`に相当。継承関係も同一パターン)
- `appeng.container.implementations.ContainerCraftingCPU extends AEBaseContainer`(1.20.1の`CraftingCPUMenu`に相当)

**対象メンバー**:
- `ContainerCraftingCPU#getNetwork()`(272行目、**パッケージプライベート**、`IGrid`を返す) ─ **クラス自身に直接宣言**。1.20.1の`getGrid()`に相当するがメソッド名が異なる点に注意(`getGrid`ではなく`getNetwork`)。
- `ContainerCraftingCPU#setCPU(final ICraftingCPU c)`(85行目、`protected`) ─ **クラス自身に直接宣言**。
- **`createCpuList()`という独立メソッドは1.12.2に存在しない。** CPU一覧構築ロジックは`ContainerCraftingStatus#detectAndSendChanges()`(53〜105行目)に直接インライン化されている。`ICraftingCPU#getName()`の呼び出しは、その中で生成される`appeng.container.implementations.CraftingCPURecord`のコンストラクタ内(`this.myName = server.getName();`)にある。呼び出し元は`detectAndSendChanges()`の94行目: `this.cpus.add( new CraftingCPURecord( c.getAvailableStorage(), c.getCoProcessors(), c ) );`。

**実装方針**:
- `getNetwork()` / `setCPU()`をターゲットにした優先度統合(1.20.1の`getGrid()/setCPU()`相当部分)はメソッド名を`getNetwork`に読み替えるだけで、ほぼ同じ設計で移植可能。
- `ICraftingCPU#getName()`の`@Redirect`は、1.20.1のように専用の`createCpuList()`メソッドを狙うのではなく、**`ContainerCraftingStatus#detectAndSendChanges()`を対象メソッドにして`@Redirect`する**必要がある。ただし`detectAndSendChanges()`は`super.detectAndSendChanges()`も呼ぶ大きめのメソッドであり、`getName()`呼び出しは`CraftingCPURecord`コンストラクタの**内部**にあるため、直接のメソッド呼び出し階層としては「`detectAndSendChanges()` → `new CraftingCPURecord(...)`(コンストラクタ呼び出し) → その中で`server.getName()`」となる。`@Redirect`で`ICraftingCPU#getName()`の呼び出しを差し替えるには、**対象メソッドを`detectAndSendChanges()`ではなく`CraftingCPURecord`のコンストラクタ自体(`<init>`)**にする設計の方が素直(1.20.1もおそらく`createCpuList()`ではなく、実際に`getName()`を呼んでいる末端の場所を対象にしているはずなので、1.12.2でも「`getName()`を実際に呼んでいる箇所」=`CraftingCPURecord`コンストラクタを対象にするのが構造的に自然)。
- 代替案として、`CraftingCPURecord`クラス自体に優先度フィールドを追加する新規Mixin(またはこのMixインの一部)を用意し、`Comparable<CraftingCPURecord>`実装(46〜51行目、`Long.compare(processors...)`)に優先度を比較キーとして組み込む設計が、1.12.2の実装(CPU一覧が`Collections.sort(this.cpus)`でソートされる、114行目)と自然に噛み合う。1.20.1側で優先度がCPU一覧のソート順にどう反映されているか(表示順ソートに使っているか、単なる表示のみか)を確認し、同じ挙動になるよう設計すること。

**信頼度**: 中(継承構造・`setCPU`の存在は高信頼度で確認済みだが、`createCpuList()`が存在しないため`@Redirect`の対象メソッドを`detectAndSendChanges()`か`CraftingCPURecord`コンストラクタかで設計判断が必要。1.20.1版addonの実際の`getName()`呼び出し文脈が未確認のため中程度とする)

**チェックリスト**:
- [ ] 1.20.1の`CraftingStatusMenu#createCpuList()`実ソースを確認し、`getName()`呼び出しの直前直後のコンテキスト(何のオブジェクトを生成しているか)を把握したうえで、1.12.2の`CraftingCPURecord`コンストラクタが構造的に同じ役割かを再確認する
- [ ] `ContainerCraftingCPU#getNetwork()`のアクセス修飾子がパッケージプライベートである点が、実装Mod側のMixinクラスをどのパッケージに置くかに影響しないか確認(Mixinはバイトコード合成のためJavaの通常アクセス制御の影響を受けにくいが、SpongePowered Mixin ASTの警告設定次第では`private`/パッケージプライベートメンバーの`@Shadow`にアクセスレベルの明示指定が必要な場合がある。`@Shadow`アノテーションの`remap`や可視性オプションを確認)
- [ ] `ContainerCraftingStatus`(子)側で`setCPU`をオーバーライドしているか、それとも純粋に親の`ContainerCraftingCPU`のメソッドをそのまま使っているかを確認し、Mixin対象クラスを子・親どちらにすべきか決定する

---

### 3.7 CraftingCPUScreenMixin

**目的**: コンストラクタ、`updateBeforeRender`、`cancel`ボタンフィールド ─ Crafting Status画面に優先度入力UIを追加。

**1.12.2対象クラス**: `appeng.client.gui.implementations.GuiCraftingCPU`(1.20.1の`CraftingCPUScreen`に相当)。`public class GuiCraftingCPU extends AEBaseGui implements ISortSource`(58行目)。

**対象メンバー**:
- コンストラクタ(引数は`ContainerCraftingCPU`ベースの想定。実ソース要確認)。
- `private GuiButton cancel;`(92行目) ─ **クラス自身に直接宣言**。1.20.1の`cancel`ボタンフィールドと同名。
- `protected void actionPerformed(final GuiButton btn) throws IOException`(120行目) ─ **クラス自身に直接宣言**。`this.cancel == btn`判定によるボタン処理分岐がある(124行目)。
- `public void drawScreen(final int mouseX, final int mouseY, final float btn)`(156行目) ─ **クラス自身に直接宣言**(1.20.1の`updateBeforeRender`相当)。
- ボタン初期化: `this.cancel = new GuiButton(0, this.guiLeft + CANCEL_LEFT_OFFSET, ...); this.buttonList.add(this.cancel);`(142, 144行目)。この`CANCEL_LEFT_OFFSET`等のオフセット定数の使い方は5章の「動的座標算出」設計の参考になる(=1.12.2のAE2自体が既に「GUIサイズからのオフセット計算」でボタン座標を決めている実例)。

**実装方針**:
- 3.5と同様、`buttonList`への直接add方式。`cancel`ボタンの実座標(`this.guiLeft + CANCEL_LEFT_OFFSET`, `this.guiTop + this.ySize - CANCEL_TOP_OFFSET`)を`@Inject`のタイミング(ボタン生成後)で読み取り、優先度ウィジェットをその近傍に動的配置する設計にする。
- `actionPerformed`は1.20.1のMixin対象に含まれていない点に注意(1.20.1表では「コンストラクタ、updateBeforeRender、cancelボタンフィールド」の3点のみが対象)。1.12.2でも同様に`actionPerformed`自体は改変不要かもしれないが、**優先度入力ウィジェット独自のクリック判定が必要な場合はここへの`@Inject`が別途必要になる**可能性がある(vanilla `GuiButton`ベースであれば`actionPerformed`経由でよいが、テキスト入力欄(`GuiTextField`)を使う場合は`mouseClicked`/`keyTyped`側の対応が必要、5章参照)。

**信頼度**: 高(対象クラス・フィールド・メソッドの存在とシグネチャは実ソースで確認済み)

**チェックリスト**:
- [ ] `GuiCraftingCPU`のコンストラクタ引数を実ソースで確認
- [ ] `CANCEL_LEFT_OFFSET` / `CANCEL_TOP_OFFSET` / `CANCEL_WIDTH` / `CANCEL_HEIGHT`等の定数値を確認し、優先度ウィジェットの配置基準に使えるか検討
- [ ] `ISortSource`インターフェース実装が優先度UIと関連しないか確認(CPUソート順に絡む可能性があるため、3.6のCPU一覧ソートと合わせて設計する)

---

### 3.8 ContainerMenuMixin (vanilla) ─ **【重要な注記】1.20.1現行実装の`ae2craftpriority.mixins.json`には存在しない**

> **この項目はDEVELOPMENT.md(設計ドキュメント)記載の8項目の一つとして調査したものだが、実際に入手した1.20.1版`ae2craftpriority.mixins.json`(8 mixins + 4 client mixinsの計12個、本章冒頭参照)には`ContainerMenuMixin`という名前のエントリが存在しない。** つまり、DEVELOPMENT.md記載時点(設計初期)ではこの仕組みが計画されていたか、あるいは開発初期には実在したものの、**その後の実装過程で廃止されたか、他のMixin(`CraftingCPUClusterMixin`や`CraftingServiceMixin`など、CPUクラスタ側で再計算を完結させる設計に変更された等)に統合・置き換えられた可能性が高い。**
>
> したがって、1.12.2移植にあたって本項目(vanilla `Container#detectAndSendChanges`へのinject)を独立したMixinとして実装する必要が本当にあるかどうかは、**現時点では確定していない**。以下は「もし必要だった場合」の設計案として残すが、**実装着手前に必ず以下を行うこと**:
> 1. 1.20.1版`ae2craftpriority.mixins.json`(本章冒頭に記載した内容)を確認し、`ContainerMenuMixin`相当のエントリが本当に存在しないことを再確認する
> 2. 存在しない場合、`broadcastChanges`相当のトリガーが「今の1.20.1実装で本当に必要ないのか」「別のMixin(`CraftingCPUClusterMixin`・`CraftingServiceMixin`等)が同等の役割を担っているのか」を、それら現存Mixinの実ソースを読んで確認する
> 3. 現存Mixinが役割を吸収していることが確認できれば、**1.12.2移植でも本項目は独立したMixinとして実装せず、その吸収先のMixin(3.1/3.2/3.3相当)の設計に優先度再計算トリガーを統合する**方針に切り替える
> 4. 万が一「本当にvanilla Menu層へのフックが今も必要」と判明した場合のみ、以下の設計(元のDEVELOPMENT.md記載内容に基づく)を使う

**目的(DEVELOPMENT.md記載、現行実装での採否は要確認)**: `broadcastChanges()`に優先度再計算処理のトリガーをinjectする。

**1.12.2対象クラス**: `net.minecraft.inventory.Container`(vanilla本体。1.20.1の`net.minecraft.world.inventory.AbstractContainerMenu`に相当)

**対象メンバー**:
- `void detectAndSendChanges()`(vanillaのメソッド名。1.20.1の`broadcastChanges()`に相当) ─ vanilla `Container`クラス自身の宣言。AE2の各種`AEBaseContainer`のサブクラス(`ContainerCraftingStatus`等)から`super.detectAndSendChanges()`が随所で呼ばれていることを確認済み。

**実装方針**:
- 1.20.1と同じ要領で`@Inject(method = "detectAndSendChanges", at = @At("HEAD"))`または`@At("TAIL")`で優先度再計算トリガーを仕込む。
- vanillaクラス対象のため**`remap`はデフォルト(true)のまま**、すなわちSRG名解決が必要(6章参照)。1.12.2はMCPマッピング(`snapshot_20171003`)の`detectAndSendChanges`という開発名がSRG名(`func_75142_b`等、実際のSRG番号は要確認)に解決される。

**信頼度**: 高(メソッドの存在は多数の呼び出し箇所から確認済み。vanilla公式クラスのため構造変化のリスクは低い)

**チェックリスト**:
- [ ] `net.minecraft.inventory.Container#detectAndSendChanges()`のSRG名を、使用するMCPマッピング(`snapshot_20171003`)のmapping csv/tsrgファイルから確認する(ForgeGradleの`genSrgs`等のタスクで生成される`mcp-srg.srg`または`notch-srg.srg`を参照。あるいはMixin Gradle Pluginのrefmap生成時に自動解決されるため、開発者が手動でSRG名を書く必要はない場合が多いが、ビルド設定(6章)が正しく機能しているかの検証観点として押さえておく)
- [ ] `AEBaseContainer`自体が`detectAndSendChanges()`をオーバーライドしているため(`appeng.container.AEBaseContainer`)、Mixinの注入がAE2側のオーバーライドとの間で意図した順序(vanilla `Container#detectAndSendChanges` HEAD → `AEBaseContainer`のオーバーライド実装 → 各サブクラスのオーバーライド)で発火するか確認する

---

### 3.9 PriorityScreenMixin(1.20.1で`CraftConfirmScreenMixin`からリネーム/統合されたクラスと確認できた) ─ **信頼度: 低〜中、本文未確認だが命名の裏付けは取れた**

**目的(推測)**: 3.5の`CraftConfirmScreenMixin`と同一の役割(クラフト確認画面への優先度入力UI追加)を、実装の進行過程で名称変更・統合したもの。

**裏付けが取れた点**: 実際の`ae2craftpriority.mixins.json`(client配列)には`CraftingCPUScreenMixin`と`PriorityScreenMixin`の2つしかクライアント側画面系Mixinが存在せず、DEVELOPMENT.md記載の`CraftConfirmScreenMixin`という名前のエントリは無い。したがって「`CraftConfirmScreenMixin`は`PriorityScreenMixin`にリネーム/統合された」という3.5節での推測は、**クラス一覧レベルでは確定した**と言える。ただし中身(具体的にどのメソッドを対象にしているか)はまだ未確認であるため、信頼度は「低〜中」に留める。

**1.12.2対象クラス(推測)**: 3.5と同じく`appeng.client.gui.implementations.GuiCraftConfirm`。ただし「`PriorityScreenMixin`」という名前が示す通り、`GuiCraftConfirm`だけでなく後述の3.11(`CraftAmountMenuMixin`)で挟み込まれる新設の優先度入力ステップ用画面クラス(独立した「Priority Screen」)を対象にしている可能性も残る。この場合、1.12.2側でも`GuiCraftConfirm`を改造するのではなく、**新規に独立したGuiScreenクラス(例: `GuiCraftPriorityStep`)を新設し、それを対象にしたMixin(というよりMixin不要、自MOD側のクラスなのでMixin自体が不要になる可能性すらある)という設計に転換する必要がある**。

**実装方針(推測)**: 対象が`GuiCraftConfirm`改造なら3.5の内容をそのまま適用。対象が独立新設画面なら、そもそもAE2側クラスへの侵襲的Mixinが不要になり、自MOD側で完結する通常のGuiScreen実装に置き換わる(その場合、1.12.2移植はむしろ楽になる)。

**信頼度**: 低〜中

**チェックリスト**:
- [ ] **最優先タスク**: 1.20.1版addonリポジトリの`PriorityScreenMixin.java`を開き、(a) 対象クラスが本当に`CraftConfirmScreen`なのか、それとも複数の画面(`CraftConfirmScreen`と`CraftingCPUScreen`両方など)を対象にした汎用Mixinなのか、(c) 独立した新規画面クラス(`CraftPriorityStepMenu`に対応する`PriorityScreen`のようなもの)を対象にしているのかを確認する
- [ ] 確認後、本書3.5・3.9を実態に合わせて1つの節に統合し直す
- [ ] 3.11(`CraftAmountMenuMixin`)の調査結果と合わせて、「`PriorityScreenMixin`が新設の独立ステップ画面を指しているか」を判断する(3.11の`CraftPriorityStepMenu`推測と表裏一体の関係にある)

---

### 3.10 CraftingBlockEntityMixin ─ **信頼度: 低、実ソース未確認**

**目的(推測)**: README「方法1」フロー(個数設定→優先度設定→CPU選択・開始)に関連し、ブロックエンティティ側の何らかのフック(メニュー生成箇所など)に関わっている、とコーディネーターから伝えられている。

**1.20.1側の実クラス確認結果**: `appeng.blockentity.crafting.CraftingBlockEntity`(`/tmp/ae2-1.20.1/src/main/java/appeng/blockentity/crafting/CraftingBlockEntity.java`)を実際に確認したところ、これは**Pattern Providerやクラフトターミナルのブロックエンティティではなく、「Crafting CPU」ブロック自体(コプロセッサ/加速機/ストレージ等のマルチブロック)のブロックエンティティ**である。`IAEMultiBlock<CraftingCPUCluster>`を実装し、`CraftingCPUCalculator`を保持してクラスタの再計算を行う役割を持つ。

**1.12.2側の対応候補**: `appeng.tile.crafting.TileCraftingTile`(`extends AENetworkTile implements IAEMultiBlock, IPowerChannelState`)が構造的に最も近い。`getCluster()`(244行目)、`isCoreBlock()/setCoreBlock()`(365, 370行目)等、`CraftingBlockEntity`と同種の責務を持つ。

**この推測に対する注意喚起**: コーディネーターの推測(「Pattern Provider/Crafting Terminal等のフック」)と、実際に1.20.1ソースを確認して判明した`CraftingBlockEntity`の役割(Crafting CPU自体のマルチブロックタイル)は**食い違っている**。すなわち`CraftingBlockEntityMixin`は、README「方法1」フローの个数設定〜CPU選択とは直接関係のない、**Crafting CPU自身のNBT保存/マルチブロック計算まわりの何か**(例えば3.1の優先度NBTの保存タイミングをタイルエンティティのライフサイクルにフックする、等)を対象にしている可能性がある。実ソース未確認のため、**このMixinの本当の役割自体を1.20.1ソースから再確認することが必須**。

**実装方針**: 実ソース確認後に設計を確定する。1.12.2側の対応クラスは`appeng.tile.crafting.TileCraftingTile`である可能性が高いことのみ、本書の推測として残す。

**信頼度**: 低

**チェックリスト**:
- [ ] **最優先タスク**: `CraftingBlockEntityMixin.java`の実ソースを確認し、対象メソッド・役割を特定する
- [ ] 役割が「Crafting CPU自身のNBT/マルチブロック関連」だった場合、3.1(`CraftingCPUClusterMixin`)との責務重複がないか確認する
- [ ] 役割が「个数設定→優先度設定フローのフック」だった場合、`TileCraftingTile`ではなく、Pattern Provider/インターフェース/クラフトターミナルに相当する別クラス(1.12.2では`appeng.tile.crafting`パッケージ内の別タイル、または`appeng.parts.reporting`配下のクラフトターミナルPartクラス等)を探し直す必要がある。1.12.2にはPattern Providerブロックという概念自体が存在しない(旧世代の「インターフェース」+「分子アセンブラチャンバー」構成であるため、パターン供給の仕組みそのものが1.20.1と異なる)点に留意する

---

### 3.11 CraftAmountMenuMixin ─ **信頼度: 低〜中(構造分析による強い推測あり)、実ソース未確認**

**目的(推測)**: README「方法1」フロー(個数設定→**優先度設定**→CPU選択・開始)を実現するため、個数設定メニュー(`CraftAmountMenu`)からの遷移先を横取りし、新設の`CraftPriorityStepMenu`(このMODが追加した独自メニューと推測される)へ挟み込んでいる。

**1.20.1側の実クラス確認結果**: `appeng.menu.me.crafting.CraftAmountMenu#confirm(int amount, boolean craftMissingAmount, boolean autoStart)`(1.20.1ソース109行目〜)を確認した。このメソッドは:
1. クライアント側で呼ばれた場合は`ConfirmAutoCraftPacket`をサーバへ送信して自分自身を再呼び出しする形にリダイレクトする
2. サーバ側では`MenuOpener.open(CraftConfirmMenu.TYPE, player, locator)`で次画面(`CraftConfirmMenu`)を直接開いている

という実装であり、**まさに「個数確定 → 次画面遷移」の唯一のポイント**である。したがって`CraftAmountMenuMixin`は非常に高い確度でこの`confirm(...)`メソッド(または`MenuOpener.open(CraftConfirmMenu.TYPE, ...)`呼び出し箇所)を`@Redirect`し、`CraftConfirmMenu.TYPE`を独自の`CraftPriorityStepMenu.TYPE`(仮称)に差し替えている、と推測できる。

**1.12.2側の対応候補(1.2節で詳述した通り、重要な構造差)**:
1.12.2には`CraftAmountMenu#confirm()`に相当する「メニュークラス自身のメソッド」は存在しない。個数確定〜次画面遷移は完全に**ネットワークパケットハンドラ**に実装されている:

- クライアント: `appeng.client.gui.implementations.GuiCraftAmount`の`next`ボタン押下 → `NetworkHandler.instance().sendToServer(new PacketCraftRequest(Integer.parseInt(this.amountToCraft.getText()), isShiftKeyDown()))`(233行目)
- サーバ: `appeng.core.sync.packets.PacketCraftRequest#serverPacketData(final INetworkInfo manager, final AppEngPacket packet, final EntityPlayer player)` ─ **クラス自身に直接宣言**。この中で`Platform.openGUI(player, te, cca.getOpenContext().getSide(), GuiBridge.GUI_CRAFTING_CONFIRM)`を呼んで次のGUI(`ContainerCraftConfirm`)を開いている(`appeng.util.Platform#openGUI`、`appeng.core.sync.GuiBridge`列挙型経由)。

**実装方針(推測、要検証)**:
- 最有力案: `PacketCraftRequest#serverPacketData(...)`を対象に`@Redirect`し、`Platform.openGUI(player, te, side, GuiBridge.GUI_CRAFTING_CONFIRM)`呼び出しの第4引数`GuiBridge.GUI_CRAFTING_CONFIRM`を、新設する`GuiBridge.GUI_CRAFTING_PRIORITY`(仮称)に差し替える。
- そのためには前提として、1.12.2の`appeng.core.sync.GuiBridge`列挙型(150〜209行目付近に`GUI_CRAFTING_CPU` / `GUI_CRAFTING_AMOUNT` / `GUI_CRAFTING_CONFIRM` / `GUI_CRAFTING_STATUS`のエントリが定義されている)に、新規の`GUI_CRAFTING_PRIORITY(ContainerCraftPriorityStep.class, ITerminalHost.class, GuiHostType.ITEM_OR_WORLD, SecurityPermissions.CRAFT)`のような列挙値を追加する必要がある。**`GuiBridge`はenumであり、Mixinでenum定数を追加するのは通常のJava/Mixinでは非常に困難**(enumはコンパイル時に固定される特殊クラスであり、実行時の値追加はASMでも高難度)。このため、現実的な実装案は次のいずれかになる:
  - **(案A)** `GuiBridge`をenumのままMixinで改変するのは避け、独自の`IGuiHandler`実装を追加のFML登録として用意し(`NetworkRegistry.INSTANCE.registerGuiHandler(...)`を自MODの`@Mod`初期化で呼ぶ)、`PacketCraftRequest#serverPacketData`の`Platform.openGUI(...)`呼び出し自体を`@Redirect`で潰して、代わりに自MOD独自の`player.openGui(自MODインスタンス, 独自ID, world, x, y, z)`相当の呼び出しに差し替える。次画面(`ContainerCraftPriorityStep`)側で優先度入力を受け付け、その後に`Platform.openGUI(player, te, side, GuiBridge.GUI_CRAFTING_CONFIRM)`を自前で呼んで本来の確認画面へ繋ぐ。
  - **(案B)** `GuiBridge.GUI_CRAFTING_CONFIRM`はそのまま素通りさせ、代わりに3.4(`CraftConfirmMenuMixin`相当、1.12.2では`ContainerCraftConfirm`)のコンストラクタまたは初期化時点で、優先度未設定なら`ContainerCraftConfirm`自体の中に優先度入力ステップを埋め込む(独立メニューを挟まず、確認画面の最初のサブ状態として実装する)設計に変更する。この場合`CraftPriorityStepMenu`相当の独立クラスは1.12.2版では新設しない、という設計判断になる。
- どちらの案を採るにせよ、**1.20.1が「新規MenuType」で実現している独立ステップを、1.12.2の「GuiBridge enum + IGuiHandler」という別のGUI管理方式にどう落とし込むか」が本Mixinの実装における最大の設計課題**であり、実装難易度は当初想定より高い可能性がある。

**信頼度**: 中(「個数確定→遷移」のロジックがどこにあるかという構造分析は高信頼度だが、1.20.1側addonの実際のMixin実装〔`CraftAmountMenuMixin`〕・新設メニュークラス〔`CraftPriorityStepMenu`〕の詳細が未確認のため、1.12.2での対応策は複数案からの選択が必要)

**チェックリスト**:
- [ ] **最優先タスク**: `CraftAmountMenuMixin.java`の実ソースを確認し、(a) 本当に`CraftAmountMenu#confirm()`を対象にしているか、(b) `CraftPriorityStepMenu`という独立クラスが実在するか、(c) 独立クラスだとすれば`MenuType`をどう登録しているか(1.12.2の`GuiBridge` enumのような制約が1.20.1側の`MenuType`システムにもあるか)を確認する
- [ ] `PacketCraftRequest#serverPacketData`内の`Platform.openGUI`呼び出しが本当にこの1箇所のみか確認する(`GuiBridge.GUI_CRAFTING_CONFIRM`という定数の使用箇所を`grep -rn "GUI_CRAFTING_CONFIRM"`で全て洗い出し、他の経路〔例えばショートカットキーやJEI連携などから直接`ContainerCraftConfirm`が開かれる別経路〕がないか確認する)
- [ ] `appeng.core.sync.GuiBridge` enumへの項目追加がMixinで可能か、あるいは案Aのような別registration方式が必要かを、実装着手前にPoCで検証する(この検証自体を独立したスパイク作業として最初にスケジュールすることを強く推奨)
- [ ] 3.10の`CraftingBlockEntityMixin`との役割分担(もし3.10が実は本項と関連していた場合)を再整理する

---

### 3.12 TabButtonAccessor ─ **信頼度: 低、実ソース未確認(ただし1.12.2側の対応クラスは特定できた)**

**目的(推測)**: SpongePowered Mixinの`@Accessor`/`@Invoker`パターンを用いて、AE2の`appeng.client.gui.widgets.TabButton`(1.20.1)クラスが持つprivateフィールド(`style`, `icon`, `item`, `selected`)に、リフレクションを使わず型安全にアクセスするためのインターフェースMixin。DEVELOPMENT.mdの8項目には無かった要素で、おそらく「Crafting Status端末画面のタブ」に優先度関連の表示(選択中タブのハイライト等)を統合するために、実装が進む過程で追加されたものと推測される。

**1.20.1側の実クラス確認結果**: `appeng.client.gui.widgets.TabButton`(`/tmp/ae2-1.20.1/src/main/java/appeng/client/gui/widgets/TabButton.java`)を確認した。`extends Button implements ITooltip`。privateフィールドは`private Style style = Style.BOX;` / `private Icon icon = null;` / `private ItemStack item;` / `private boolean selected;`(スタイルはCORNER/BOX/HORIZONTALの3種)。`selected`フィールドは主にHORIZONTALスタイルのタブのハイライト表示に使われている。

**1.12.2側の対応クラス**: `appeng.client.gui.widgets.GuiTabButton`(`/tmp/ae2-1.12.2/src/main/java/appeng/client/gui/widgets/GuiTabButton.java`、`extends GuiButton implements ITooltip`)が構造的に完全に対応する。**しかもこのクラスは`GuiCraftingStatus`(Crafting Status画面、1.12.2の`appeng.client.gui.implementations.GuiCraftingStatus`)が実際に使用していることを確認済み**(58行目 `private GuiTabButton originalGuiBtn;`、138行目 `this.originalGuiBtn = new GuiTabButton( this.guiLeft + 213, this.guiTop - 4, this.myIcon, this.myIcon.getDisplayName(), this.itemRender )`)。役割は「元のGUI(端末など)に戻るためのタブボタン」であり、Crafting Statusタブという概念そのものと直結している。

**1.12.2版`GuiTabButton`のフィールド一覧とアクセス可否**:

| フィールド | 1.12.2での宣言 | 既存の公開アクセサ | 備考 |
|---|---|---|---|
| `x`, `y`, `width`, `height`, `visible`, `enabled` | vanilla `GuiButton`(親クラス)の**public**フィールド | 不要(既にpublic) | 1.12.2 vanillaは1.20.1のようなカプセル化が薄く、座標系フィールドは素のpublicで直接読み書きできる |
| `hideEdge` | `GuiTabButton`自身のprivate int | `getHideEdge()`/`setHideEdge()`が**既に公開されている** | Accessor不要 |
| `myIcon`(int) | `GuiTabButton`自身のprivate int | なし | 読み取りが必要なら`@Accessor`が要る |
| `myItem`(ItemStack) | `GuiTabButton`自身のprivate ItemStack | なし | 同上 |
| `itemRenderer`, `message` | `GuiTabButton`自身のprivate final | なし | finalフィールドの`@Accessor`はgetterのみ生成可能(setterは不可) |
| `selected`相当のフィールド | **1.12.2には存在しない** | ─ | 1.20.1の`selected`(タブのハイライト状態)に相当する概念が1.12.2の`GuiTabButton`には無い。優先度UIで「選択中CPU/選択中タブ」のハイライトが必要な場合、1.12.2では独自にフィールドを追加する新規サブクラスまたはMixinでの新規フィールド追加が必要になる(3.7・5章のウィジェット設計と合わせて検討) |

**実装方針**:
- 1.12.2では座標系フィールドがそもそもpublicであるため、Accessorパターンの出番自体が1.20.1より少ない。必要になるのはおそらく`myIcon`/`myItem`の読み取り(表示状態の判定用)程度。
- `@Accessor`パターンの一般論として、対象フィールド名さえ特定できれば実装コストは非常に低い(インターフェースに`@Accessor("フィールド名") T getフィールド名(); @Accessor("フィールド名") void setフィールド名(T value);`を1〜2行書くだけで済む)。1.12.2移植でもこの点は変わらず、**このMixinは11個の中では最も移植コストが低い部類**になる見込み。
- ただし「`selected`相当の概念が1.12.2に存在しない」点だけは新規実装が必要になるため、1.20.1側で`selected`が実際にどう使われているか(優先度表示との関連有無)を必ず確認すること。

**信頼度**: 低(1.20.1側addonの`TabButtonAccessor.java`実ソース・使用箇所は未確認。ただし対応する1.12.2クラスの特定と、フィールドレベルの詳細調査は完了している)

**チェックリスト**:
- [ ] `TabButtonAccessor.java`の実ソースを確認し、どのフィールド/メソッドに対する`@Accessor`/`@Invoker`が定義されているか特定する
- [ ] `TabButtonAccessor`が実際にどこから使われているか(`PriorityScreenMixin`や`CraftingStatusMenuMixin`関連のクライアントコードから使われている可能性が高い)を確認し、優先度UIとの関連を明らかにする
- [ ] `selected`フィールド相当が必要と判明した場合、1.12.2の`GuiTabButton`に新規フィールドを追加する手段(Mixinでのフィールド追加、または`GuiTabButton`を継承した独自クラスに置き換える設計)を検討する

---

### 3.13 WidgetContainerAccessor ─ **信頼度: 低、実ソース未確認(ただし1.12.2に対応クラスが存在しないことは確認済み)**

**目的(推測)**: `appeng.client.gui.WidgetContainer`(1.20.1、AE2の画面が持つウィジェット管理コンテナ)のprivateフィールド/メソッドへ`@Accessor`/`@Invoker`でアクセスするためのインターフェースMixin。DEVELOPMENT.mdが説明していた「`WidgetContainer#addButton`呼び出し自体を`@Redirect`で横取りする」という手動実装(旧`CraftConfirmScreenMixin`の設計)から、より洗練されたAccessorパターンへ実装が進化したものと推測される。

**1.12.2側の重大な構造差**: **1.12.2には`WidgetContainer`に相当するクラスが存在しない**(3.5節・5章で既に確認済み)。1.12.2のGUIはvanilla `GuiScreen`が持つ`protected List<GuiButton> buttonList`フィールドへ各GUIクラスが直接`add()`する素朴な方式であり、AE2独自の「ウィジェット管理コンテナ」という抽象化層自体が無い。

**実装方針**:
- 1.20.1の`WidgetContainerAccessor`が本来アクセスしたいであろう対象(ボタンリスト、ウィジェット登録処理)は、1.12.2では**vanilla `GuiScreen`(またはその親`GuiContainer`)の`buttonList`フィールド**が直接の相当品になる。したがって1.12.2移植では、`WidgetContainer`という中間層を再現する必要はなく、**5.2節で既に設計していた「vanilla `GuiScreen`を対象にした`@Shadow`用の補助Mixin」を、`@Shadow`ではなく`@Accessor`パターンで書き直す**のが素直な移植方針になる。
- 具体的には、`net.minecraft.client.gui.GuiScreen`(vanilla)を対象にした以下のようなAccessorインターフェースを新設する設計を推奨する(擬似コード、実装ではない):
  ```java
  @Mixin(GuiScreen.class)
  public interface GuiScreenAccessor {
      @Accessor("buttonList")
      List<GuiButton> ae2cp$getButtonList();
  }
  ```
  この場合、対象がvanillaクラスのため`remap`はデフォルト(true、SRG解決)のままにする(6.3節の使い分けルールと整合)。
- 1.20.1版`WidgetContainerAccessor`の実ソースを確認した結果、もし単純な「ボタンリスト取得」以上の複雑な処理(ウィジェットの動的追加・削除・レイアウト管理等)を担っていることが判明した場合は、1.12.2側では**Accessorだけでは足りず、5.2節で述べた座標動的算出ロジックそのものをこのMixin相当の役割として自MOD側に新規実装する**必要が出てくる可能性がある。

**信頼度**: 低(1.20.1側`WidgetContainerAccessor.java`の実ソース・`WidgetContainer`自体が具体的に何を保持しているか未確認。ただし「1.12.2に直接対応するクラスが存在しない」という否定的事実は確認済み)

**チェックリスト**:
- [ ] `WidgetContainerAccessor.java`の実ソースと、対象クラス`appeng.client.gui.WidgetContainer`(1.20.1、`/tmp/ae2-1.20.1/src/main/java/appeng/client/gui/WidgetContainer.java`)を確認し、具体的にどのフィールド/メソッドにアクセスしているか特定する
- [ ] その用途が「単純なボタンリスト取得」で済むのか、「ウィジェットの動的管理」まで踏み込んでいるのかを判断し、上記の擬似コード案で足りるか、5章の設計をより作り込む必要があるかを決める
- [ ] 3.5・3.7(UI統合Mixin)がこのAccessorにどう依存しているかを確認し、実装順序(8章)に反映する

---

## 4. 1.12.2固有API早見表

| 用途 | 1.20.1 (現行) | 1.12.2 (移植先) | 備考 |
|---|---|---|---|
| NBTコンパウンド型 | `net.minecraft.nbt.CompoundTag` | `net.minecraft.nbt.NBTTagCompound` | |
| NBT longを書く | `tag.putLong(key, value)` | `tag.setLong(key, value)` | |
| NBT longを読む | `tag.getLong(key)` | `tag.getLong(key)` | メソッド名は同じ |
| NBT booleanを書く | `tag.putBoolean(key, value)` | `tag.setBoolean(key, value)` | |
| NBT intを書く | `tag.putInt(key, value)` | `tag.setInteger(key, value)` | **メソッド名が`setInteger`(`setInt`ではない)である点に注意** |
| NBT stringを書く | `tag.putString(key, value)` | `tag.setString(key, value)` | |
| キー存在確認 | `tag.contains(key)` | `tag.hasKey(key)` | |
| NBTリスト型 | `net.minecraft.nbt.ListTag` | `net.minecraft.nbt.NBTTagList` | |
| Menu基底クラス | `net.minecraft.world.inventory.AbstractContainerMenu` | `net.minecraft.inventory.Container` | |
| Menu変更通知メソッド | `broadcastChanges()` | `detectAndSendChanges()` | vanilla、SRG解決が必要(remapデフォルトtrueで対応) |
| Screen基底クラス | `net.minecraft.client.gui.screens.Screen` / `AbstractContainerScreen` | `net.minecraft.client.gui.GuiScreen` / `GuiContainer` | |
| ボタン型 | `net.minecraft.client.gui.components.Button`ほかWidget体系 | `net.minecraft.client.gui.GuiButton` | 1.12.2はWidget抽象化がほぼ無い |
| ボタンリスト管理 | AE2独自`WidgetContainer#addButton`等 | vanilla `GuiScreen#buttonList`(`List<GuiButton>`)への直接add | AE2側の抽象化なし |
| GUI初期化フック | `Screen#init()` | `GuiScreen#initGui()` | |
| 描画フック(毎フレーム) | `updateBeforeRender`(AE2独自) | `GuiScreen#drawScreen(int, int, float)`のオーバーライド | |
| ボタン押下ハンドラ | `Button.OnPress`ラムダ等 | `GuiScreen#actionPerformed(GuiButton)`(`throws IOException`) | |
| GUI遷移の仕組み | `MenuType` + `MenuOpener.open(...)` | `appeng.core.sync.GuiBridge` enum + `appeng.util.Platform#openGUI(...)` + `IGuiHandler` | **仕組み自体が別物**。3.11参照 |
| Crafting CPUクラスタ管理層 | `appeng.me.service.CraftingService` | `appeng.me.cache.CraftingGridCache` | クラス名変更のみ、構造は酷似 |
| CPU単位実行ロジック | `appeng.crafting.execution.CraftingCpuLogic`(独立クラス) | 該当なし(`CraftingCPUCluster`/`CraftingGridCache`に統合) | 3.3参照 |
| Crafting CPUクラスタ本体 | `appeng.me.cluster.implementations.CraftingCPUCluster` | 同一(パッケージ・クラス名一致) | |
| Crafting CPUタイル | `appeng.blockentity.crafting.CraftingBlockEntity` | `appeng.tile.crafting.TileCraftingTile` | 3.10参照(推測) |
| プレイヤーエンティティ型 | `net.minecraft.server.level.ServerPlayer` | `net.minecraft.entity.player.EntityPlayer`(サーバ/クライアント区別が薄い) | |
| ネットワークパケット送受信 | `NetworkHandler.instance().sendToServer(...)`(SimpleChannel系) | `appeng.core.sync.network.NetworkHandler.instance().sendToServer(...)` | AE2独自の仕組み自体は近い名称で存在 |

---

## 5. UI層新規実装方針(項目3.5・3.7の詳細)

1.12.2のGUIはvanilla `GuiScreen`/`GuiContainer`ベースであり、1.20.1のようなAE2独自ウィジェット抽象化(`WidgetContainer`等)が無いため、優先度入力UIはほぼゼロから設計する。以下を基本方針とする。

### 5.1 コンポーネント選定

- 数値入力: vanillaの`net.minecraft.client.gui.GuiTextField`をそのまま利用する(1.12.2の`GuiCraftAmount`が個数入力に`GuiTextField`(想定、実ソース要確認)を使っている実績がある。同クラスの入力フィールド実装パターンを参考にする)。
- 増減ボタン・確定ボタン: vanillaの`GuiButton`を使用し、既存の`this.buttonList.add(...)`パターンに倣う。

### 5.2 座標算出方針(ハードコード禁止)

1.12.2 AE2自体、既存ボタンの座標を「GUI原点(`guiLeft`, `guiTop`) + 固定オフセット定数」で算出している(例: `GuiCraftingCPU`の`this.cancel = new GuiButton(0, this.guiLeft + CANCEL_LEFT_OFFSET, this.guiTop + this.ySize - CANCEL_TOP_OFFSET, CANCEL_WIDTH, CANCEL_HEIGHT, ...)`)。したがって、この方針は完全な「動的取得」ではなく「GUIサイズに対する相対オフセット定数」である点に注意しつつ、以下のように設計する:

1. Mixinの`@Inject`挿入点(コンストラクタ末尾または`initGui()`末尾)で、**対象クラスの既存ボタンフィールド(`@Shadow`で取得できるものは取得し、できないものは`@Shadow`できる親クラス側に別Mixinを立てて中継する)の実座標(`.x`, `.y`, `.width`, `.height`)を読み取る**。
2. 読み取った座標を基準に、優先度ウィジェットを「既存ボタンの右隣」「既存ボタンの下」等、相対配置で計算する。
3. `guiLeft` / `guiTop` / `xSize` / `ySize`はvanilla `GuiContainer`(1.12.2)が持つ`protected`フィールドであり、`AEBaseGui`経由で継承されているため、**`GuiCraftConfirm`/`GuiCraftingCPU`自身を対象にした`@Shadow`ではアクセスできない可能性がある**(教訓2)。宣言元は`net.minecraft.client.gui.inventory.GuiContainer`(`guiLeft`, `guiTop`, `xSize`, `ySize`)であるため、これらを`@Shadow`する場合は`GuiContainer`(vanilla)を対象にした別Mixinを新設し、duck interfaceで`GuiCraftConfirm`側から呼び出す設計にする。

### 5.3 入力イベント処理

- `GuiTextField`を使う場合、`GuiScreen#mouseClicked(int, int, int)` と `GuiScreen#keyTyped(char, int)` の両方に対して、テキストフィールドへのフォーカス処理・入力転送を追加する`@Inject`が必要になる(vanilla `GuiTextField`は自動では呼ばれず、GUIクラス側で明示的に`textField.mouseClicked(...)`, `textField.textboxKeyTyped(...)`を呼ぶ設計が1.12.2 vanillaの標準パターン)。1.20.1の`Screen`が持つような自動フォーカス管理は無い。

### 5.4 デバッグログ

教訓6を踏襲し、UI実装中は`AE2CraftPriority: ...`接頭辞のINFOログ(座標算出結果、ウィジェット生成タイミング等)を仕込み、リリース前に整理する。

---

## 6. Mixin + Forge 1.12.2 の導入方法

AE2自体は1.12.2でMixinを採用していない(Access Transformer方式)ため、以下は**AE2非依存の、Forge 1.12.2環境でMixinを使う一般的な手順**として記載する。

### 6.1 導入方式の選択

Forge 1.12.2にはMixinの標準統合が無い(Forge 1.13以降で標準搭載された)。選択肢は以下の2つ:

- **(推奨) `MixinBooter`(Prospector作)のようなMixin初期化専用の共有ライブラリMOD経由**: 1.12.2コミュニティで広く使われている、Mixinのブートストラップ(`IFMLLoadingPlugin`実装・`MixinBootstrap`初期化・tweaker登録)を肩代わりしてくれるライブラリMOD。これに依存すれば、自MODは`mixins.<modid>.json`とrefmapを用意するだけでよくなる。**ただし本設計書では実際のMaven座標・最新バージョン・ライセンス条件をネットワーク経由で確認できていないため、実装着手時に配布元(CurseForge/Modrinth/GitHub)を確認し、依存として組み込めるか(再配布条件・同梱可否)を必ず確認すること。**
- **(代替) 自前で`IFMLLoadingPlugin`を実装**: 自MOD自身がcoremod(`IFMLLoadingPlugin`)として登録され、`MANIFEST.MF`に`FMLCorePlugin`属性を設定、その実装クラス内で`MixinBootstrap.init()`を呼び、`Mixins.addConfiguration("mixins.ae2craftpriority.json")`でMixin Configを登録する。tweaker登録(`net.minecraftforge.fml.relauncher.CoreModManager`経由の`SortingIndex`指定等)も自前で行う必要があり、実装・デバッグの手間が大きい。共有ライブラリMODに依存したくない場合の最終手段。

いずれの方式でも、**Mixin ConfigのJSON(`mixins.ae2craftpriority.json`)と、Mixin Annotation Processorが生成するrefmap(`ae2craftpriority.refmap.json`)が必要**な点は1.20.1と同じ。

### 6.2 build.gradleへのMixin組み込み(概要)

```gradle
buildscript {
    repositories {
        maven { url = "https://repo.spongepowered.org/repository/maven-public/" }
    }
    dependencies {
        classpath 'org.spongepowered:mixingradle:0.7-SNAPSHOT' // バージョンは要確認
    }
}

apply plugin: 'org.spongepowered.mixin'

dependencies {
    compile 'org.spongepowered:mixin:0.7.11-SNAPSHOT' // Forge1.12.2時代の実績があるバージョン。要最終確認
}

mixin {
    add sourceSets.main, "ae2craftpriority.refmap.json"
    // MCPマッピングとの連携設定(reobfSrgFile等)がForgeGradle2.x系のタスク名に合わせて必要
}
```

**注意**: 上記はあくまで「1.12.2時代に一般的だった構成」の概要であり、正確なバージョン番号・タスク連携設定はネットワーク環境で最新情報を確認しながら実装時に調整すること(本調査はネットワークアクセスなしで実施しているため、Maven座標の実在性・最新性は未検証)。

### 6.3 `remap`オプションの使い分け(教訓8への回答)

1.20.1と同じ考え方がそのまま1.12.2にも当てはまる:

- **AE2の`appeng.*`パッケージのクラスを対象にするMixin**(3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.9, 3.10, 3.11のAE2クラス部分): AE2自身は難読化されずに配布・コンパイルされる(ForgeGradleの`userdev`/deobf済みAE2ソース・成果物を使う)ため、メンバー名は既にMCP開発名(実名)そのものである。よって**`remap = false`を指定する**(1.20.1と同じ)。
- **vanilla `net.minecraft.*`パッケージのクラスを対象にするMixin**(3.8のみ): vanilla本体は難読化されたSRG名で配布されているため、MCP開発名で書いたメソッド/フィールド参照をSRG名へ解決する必要がある。よって**`remap`は指定しない(デフォルトtrue)**、すなわちMixin Annotation Processorがrefmap生成時にForgeGradleのMCPマッピングデータ(`snapshot_20171003`)を参照してSRG解決を行う設定にする(1.20.1がMojang公式マッピングを使うのに対し、1.12.2はMCPマッピングを使う、という「解決に使うマッピングデータベースの種類」が違うのみで、remapの要否判断ロジック自体は同じ)。
- 5.2で言及した「vanilla `GuiContainer`/`GuiScreen`を対象にした補助Mixin」も同様に`remap`はデフォルト(true)のまま使う。

---

## 7. ツールチェイン設定テンプレート

### 7.1 `gradle.properties`(抜粋・テンプレート)

```properties
# --- Minecraft/Forge ---
minecraft_version=1.12.2
mcp_mappings=snapshot_20171003
forge_version=14.23.5.2847

# --- Mixin (バージョンは実装時に最終確認) ---
mixin_version=0.7.11-SNAPSHOT

# --- 本MOD ---
mod_version=0.1.0
mod_group=com.example.ae2craftpriority
mod_id=ae2craftpriority
```

### 7.2 `build.gradle`(抜粋・テンプレート)

```gradle
buildscript {
    repositories {
        mavenLocal()
        jcenter()
        mavenCentral()
        maven { url = "https://files.minecraftforge.net/maven" }
        maven { url = "https://repo.spongepowered.org/repository/maven-public/" }
    }
    dependencies {
        classpath 'net.minecraftforge.gradle:ForgeGradle:2.3-SNAPSHOT'
        classpath 'org.spongepowered:mixingradle:0.7-SNAPSHOT' // 要最終確認
    }
}

apply plugin: 'net.minecraftforge.gradle.forge'
apply plugin: 'org.spongepowered.mixin'

sourceCompatibility = JavaVersion.VERSION_1_8
targetCompatibility = JavaVersion.VERSION_1_8

// 教訓7対応: Windows環境でjavacがANSIコードページでソースを読み込み、
// 日本語コメント等でエラーが出る問題への対策。
// ForgeGradle 2.x系でも tasks.withType(JavaCompile) は通常のGradle Javaプラグイン機構
// (ForgeGradleはJavaプラグインの上に乗っている)なのでそのまま使用可能と考えられる。
// ただし実際にForgeGradle 2.3のcompileJavaタスクがこの設定を正しく継承するか、
// 導入初期に必ずWindows環境でのビルドテストを行い確認すること。
compileJava.options.encoding = 'UTF-8'
tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
}

minecraft {
    version = "${minecraft_version}-${forge_version}"
    runDir = "run"
    mappings = mcp_mappings
}

mixin {
    add sourceSets.main, "${mod_id}.refmap.json"
}

dependencies {
    // AE2への依存: api限定jar(appeng/api/**のみを含む)を使う場合はcompile時にAPIしか見えない。
    // 本MODはAE2内部実装をMixinで直接いじるため、Mixin Annotation Processorが
    // 内部クラスの実バイトコード/ソースを解決できる必要がある。
    // 公開Mavenにapiのみのjarしか存在しない場合、内部クラス対象のMixinのrefmap生成が
    // 通らない可能性があるため、開発時はローカルにフルのdeobf済みAE2 jarを用意し、
    // flatDir経由で参照する等の対応を検討すること。
    // 例(実際のグループ/バージョンは配布形態を確認して調整):
    // deobfCompile "appeng:appliedenergistics2:rv6-stable-X.Y.Z"        // フルjar(内部クラス含む、要確認)
    // deobfCompile "appeng:appliedenergistics2:rv6-stable-X.Y.Z:api"    // API限定jar(参考: /tmp/ae2-1.12.2 の apiJar タスク定義を参照)

    compile "org.spongepowered:mixin:${mixin_version}"
}
```

**要検証事項**:
- `mixingradle`の実在バージョン・リポジトリURL(ネットワーク未接続のため確認不可)
- AE2 1.12.2(rv6)の配布形態(CurseForge公開Mavenの有無、フルjar/apiJarそれぞれの入手可否)。`/tmp/ae2-1.12.2/gradle/scripts/artifacts.gradle`に`apiJar`タスクの定義は存在するため、少なくともAE2ビルド成果物としてAPI限定jarを作れることは確認済みだが、公開Maven上での配布有無・座標は未確認。
- ForgeGradle 2.3系での`mixin` Gradle DSLブロックの正確なタスク連携(`reobf`との連携等)

---

## 8. 推奨実装順序

1.20.1開発時の経験(スケジューリング本体→UI統合の順が良い)を踏まえ、以下の順序を推奨する。

0. **正のMixin一覧の再確認**: 実装着手前に、`ae2craftpriority.mixins.json`(本章冒頭に記載した12個)と、実ソース未確認の5クラス(`CraftAmountMenuMixin` / `CraftingBlockEntityMixin` / `PriorityScreenMixin` / `TabButtonAccessor` / `WidgetContainerAccessor`)の実ソースを可能な限り入手する。特に`ContainerMenuMixin`(3.8)がリストに存在しない件は、他のMixinへの統合有無を先に確認しておくと後工程の手戻りを防げる。
1. **ビルド環境・Mixin導入のPoC**(6章): まず優先度ロジックとは無関係の「1行ログを出すだけのMixin」を1個作り、Forge1.12.2 + Mixinの組み合わせが正しくロード・適用されることを確認する。教訓1(エラーは1回に1個しか出ない)を踏まえ、**この段階を飛ばさないこと**が全体のデバッグ時間を最も削減する。
2. **3.12 `TabButtonAccessor` / 3.13 `WidgetContainerAccessor`(Accessorパターン系)**: 一般にAccessorパターンは対象フィールドさえ特定できれば実装コストが低く、他のUI系Mixin(3.5/3.7/3.9)が内部的にこれらのAccessorに依存している可能性が高い。UI系Mixinより先に、土台として最初に片付けておくと以降の手戻りが減る。ただし3.13は1.12.2に`WidgetContainer`相当のクラスが存在しないため、実質的には5.2節の設計(vanilla `GuiScreen`向けAccessor新設)に読み替えて着手する。
3. **3.1 `CraftingCPUClusterMixin`**(NBT永続化): 依存が少なく、動作確認もシングルプレイでNBT保存→再読込するだけで検証しやすい。
4. **3.2 `CraftingServiceMixin`**(`craftingCPUClusters`差し替え): 3.1で追加した優先度フィールドを読み書きできる状態が前提になるため3の後に。
5. **3.3(`CraftingGridCache#submitJob`のソート改変、`CraftingCpuLogicMixin`相当)**: 3, 4の上に乗る。ここまでで「優先度に応じてジョブが割り当てられるCPUが変わる」というコア機能が完成し、GUIなしでも(NBTエディタやログで確認しつつ)機能検証が可能になる。
6. **3.8 `ContainerMenuMixin`相当の要否判断**: 3.8節の注記の通り、現行`ae2craftpriority.mixins.json`には存在しないため、ここでは「実装するかどうかを判断する」工程として位置づける。3〜5で実装したMixin側に再計算トリガーが不要なら、この工程はスキップしてよい。
7. **3.4 `CraftConfirmMenuMixin`(`startJob`のThreadLocal)**: コア機能とUIの橋渡し。UIがまだ無くても、ハードコードした優先度値でThreadLocalをセットするテストコードで先行検証できる。
8. **3.11 `CraftAmountMenuMixin`(要実ソース確認)**: 最も設計判断の余地が大きい箇所(3.11参照)。着手前に必ずPoCで`GuiBridge`拡張方式(案A/案B)の実現可否を検証する。3.9(`PriorityScreenMixin`)が独立新設画面を指しているかの調査結果とセットで着手する。
9. **3.6 `CraftingStatusMenuMixin` + `CraftingCPUMenuMixin`**(CPU一覧のサーバ側ロジック): UIより先にサーバ側のデータ(優先度を含むCPU一覧)を仕上げる。
10. **3.5 / 3.9(クラフト確認画面UI)**、**3.7(Crafting Status画面UI)**: UI層は最後にまとめて実装する。5章の座標算出設計と2で実装したAccessorを前提に着手する。
11. **3.10 `CraftingBlockEntityMixin`(要実ソース確認)**: 役割が不明なため、他の全項目の実装完了後、最後に残った役割を1.20.1ソースと突き合わせて確定させ、対応する箇所が1.12.2側で未実装のままになっていないか確認する形で着手する。

---

## 9. 実機動作確認チェックリスト(1.12.2向け翻案)

1.20.1版DEVELOPMENT.mdの実機確認項目を1.12.2向けに翻案。

- [ ] 通常起動: Forge1.12.2 + AE2(rv6) + Mixin導入ライブラリ + 本MODを揃えた状態で`runClient`が正常起動する(Mixin適用エラーが出ないか、起動ログを最後まで確認)
- [ ] Crafting CPUを複数(異なる優先度で)設置し、同一Pattern供給元(1.12.2ではインターフェース+分子アセンブラチャンバー、またはCrafting CPU自体のパターン処理)を取り合うジョブを複数投入した際、優先度の高いCPUに優先的にジョブが割り当てられることを確認
- [ ] Crafting CPUを分解・再設置(マルチブロック再構成)した際、NBTに保存した優先度が正しく復元されることを確認(`TileCraftingTile#done()`経由の`readFromNBT`復帰パス、3.1参照)
- [ ] ワールド再読込(セーブ&ロード)後も優先度設定が保持されることを確認
- [ ] 個数設定画面→(優先度設定)→CPU選択→クラフト開始、のフロー一式をGUI操作で最初から最後まで実行し、意図通りの優先度でジョブが割り当てられることを確認(3.11の実装内容に応じて具体的な画面遷移を確認)
- [ ] Crafting Status端末画面でCPU一覧に優先度が正しく表示され、選択中CPUの切り替えでも表示が破綻しないことを確認
- [ ] 優先度入力ウィジェット(5章)がGUIの解像度・スケール設定(Minecraftのグラフィック設定「GUIの拡大率」)を変えても正しい位置に表示されることを確認
- [ ] マルチプレイ環境(専用サーバ)でクライアント・サーバ双方に本MOD+Mixinを導入した状態で同期・権限まわりに問題がないか確認(1.12.2はクライアント/サーバ共有コードが多いため、`Platform.isServer()`分岐の見落としがないか特に注意)
- [ ] 他の主要1.12.2 MOD(JEI、CraftTweaker等、AE2側`gradle.properties`に依存記載があるもの)と同時導入した際にMixin適用やGUIに競合が起きないか確認
- [ ] サーバ再起動・クラッシュ後の復旧時、進行中のクラフトジョブと優先度設定が破綻しないことを確認

---

## 10. 既知のリスク・不確実性まとめ

| リスク | 該当箇所 | 深刻度 | 対応方針 |
|---|---|---|---|
| `ContainerMenuMixin`が現行`ae2craftpriority.mixins.json`(12個)に存在せず、廃止されたのか他Mixinに統合されたのか未確認。誤って不要な実装を追加、または逆に必要な再計算トリガーを漏らすリスク | 3.8 | 中 | 3.1〜3.3(CraftingCPUClusterMixin/CraftingServiceMixin/CraftingCpuLogicMixin相当)の実ソースを確認し、吸収先の有無を判断してから着手する |
| `CraftAmountMenuMixin`の1.12.2対応策が複数案から選択が必要で、`GuiBridge` enumへの項目追加という技術的難所を含む | 3.11 | **高** | 実装着手前に独立したPoCスパイクを設ける(8章 手順8) |
| `CraftingBlockEntityMixin`の実際の役割が完全に未確認 | 3.10 | 高 | 1.20.1実ソース確認を最優先タスクとする |
| `PriorityScreenMixin`が3.5と同一役割か、独立新設画面かが未確認(`CraftAmountMenuMixin`の設計〔3.11〕次第で変わる) | 3.9 | 中 | 1.20.1実ソース確認後、3.5・3.11と合わせて統合整理 |
| `TabButtonAccessor`が対象とする1.20.1の`selected`フィールド相当が1.12.2の`GuiTabButton`に存在せず、新規実装が必要になる可能性 | 3.12 | 低〜中 | 実ソース確認で`selected`の用途(優先度UIとの関連有無)を先に判断する |
| `WidgetContainerAccessor`が対象とする`WidgetContainer`クラス自体が1.12.2に存在せず、Accessorパターンをそのまま移植できない(vanilla `GuiScreen`向けAccessorへの設計転換が必要) | 3.13 | 中 | 5.2節・3.13節の設計方針(vanilla `GuiScreen`対象のAccessor新設)で代替する |
| Mixin+Forge1.12.2の具体的ライブラリ(MixinBooter等)の最新バージョン・配布条件が未確認(ネットワークアクセスなしで調査したため) | 6章 | 中 | 実装着手時にネットワーク接続環境で最新情報を確認 |
| AE2 1.12.2のフルjar(内部クラス込み)の入手経路が未確認。API限定jarしか公開Mavenに無い場合、Mixin AP用の参照解決に支障が出る可能性 | 7章 | 中 | 開発環境構築時に確認。最悪の場合ローカルにdeobf済みjarを自前生成(`/tmp/ae2-1.12.2`を`gradle build`して生成物を利用する等) |
| `CraftingGridCache`コンストラクタ内に複数の`new HashSet<>()`があり、`@Redirect`のordinal依存リスクがある(教訓3) | 3.2 | 中 | 型・変数名での絞り込みを徹底し、ordinal使用を避ける |
| `ContainerCraftingCPU#getNetwork()`がパッケージプライベートであり、`@Shadow`のアクセス制御周りで想定外の挙動がないか未検証 | 3.6 | 低〜中 | 実装初期のPoCで確認 |
| vanilla `GuiScreen#buttonList`等、UIウィジェット関連の継承元フィールドを`@Shadow`/`@Accessor`する際に対象クラス選定を誤るリスク(教訓2が最も刺さりやすい箇所) | 3.5, 3.7, 3.13, 5章 | 中 | 本書のチェックリストに従い、宣言クラスを必ず個別確認してから対象を決める |
| GitHubブランチHEADが実際のリリース版と乖離している可能性(1.20.1・1.12.2双方) | 全体 | 中 | 実装時に実際に導入するAE2バイナリバージョンで再確認 |
| ForgeGradle2.3系での`UTF-8`エンコーディング設定の実効性が未検証(教訓7) | 7章 | 低 | 導入初期にWindows環境で日本語コメット入りソースのビルドテストを行う |

---

以上。

---

## 11. 追加調査(2026-07-22): 経路A/B分離と実ソース確認結果

本節は当初(2026-07-20)の低信頼度セクション(3.5/3.9/3.10/3.11/3.12/3.13)を、1.20.1版addonの実ソース(`Z:\Claude\Projects\MinecraftMods\AE2CraftPriority\src\main\java\com\yuuhamu\ae2craftpriority\`)と1.12.2版AE2本体の実ソース(GitHub `AppliedEnergistics/Applied-Energistics-2` branch `rv6-1.12`)を直接確認した結果に基づき更新するもの。3〜10章の該当箇所は本節の内容で読み替えること(本文自体は改変していない。矛盾する記述がある場合は本節を優先する)。

### 11-0. 最重要の発見: 優先度設定には「2つの独立した経路」がある

1.20.1版addonの実ソースを読んだ結果、当初「1つのUIをMixinで拡張する」という前提だった設計が誤りで、**実際は完全に独立した2つの経路**で構成されていることが判明した。

**経路A(クラフトフロー内): 個数設定→優先度設定→確認**
`CraftAmountMenuMixin`が`CraftAmountMenu#confirm()`内の`MenuOpener.open(CraftConfirmMenu.TYPE, ...)`呼び出しを`@Redirect`し、代わりに**MOD独自の新規メニュー`CraftPriorityStepMenu`+`CraftPriorityStepScreen`**(AE2への侵襲的Mixinではなく、MOD自身が完全に所有する普通のJavaクラス)を開く。ユーザーが数値入力→「次へ」ボタンで、初めて`CraftConfirmMenu`が開かれる。

**経路B(常設): 設置済みCrafting CPUを右クリック→優先度を直接編集**
`CraftingBlockEntityMixin`が`CraftingBlockEntity`(1.12.2では`TileCraftingTile`)に**AE2既存の`appeng.helpers.IPriorityHost`インターフェースを実装**させる。これにより、Interface・Storage Bus等の他のAE2デバイスと全く同じ仕組みで、**AE2が最初から持っている汎用の優先度編集画面**(1.20.1: `PriorityScreen`/`PriorityMenu`、1.12.2: `GuiPriority`/`ContainerPriority`、**両バージョンとも既存クラス、新規実装不要**)がCrafting CPUにもそのまま使えるようになる。`PriorityScreenMixin`はこの汎用画面に対する軽微な化粧直し(ヒントテキストの文言変更、戻るボタンのアイコン変更)のみを行う。

この2経路は互いに独立しており、実装・検証も別々に進められる。以下、各Mixin項目をこの2経路のどちらに属するか明記して再整理する。

---

### 11-1. 経路B(常設優先度編集)の1.12.2設計 ─ 当初想定より大幅に簡略化できる

### 1.1 1.12.2 AE2に「同型の仕組みが最初から存在する」ことを確認済み

`/tmp/ae2-1122`(1.12.2ソース、rv6-1.12ブランチ)を実際にcloneして確認した。以下の3クラスが**1.20.1版と完全に対応する形で最初から存在する**:

| 役割 | 1.12.2の実クラス | 備考 |
|---|---|---|
| インターフェース | `appeng.helpers.IPriorityHost`(`src/main/java/appeng/helpers/IPriorityHost.java`) | `int getPriority()` / `void setPriority(int)` / `ItemStack getItemStackRepresentation()` / `GuiBridge getGuiBridge()` の4メソッド。1.20.1と役割は同一だが、**戻り値型に`GuiBridge`(1.12.2独自のenum)を含む点が1.20.1と異なる**(1.20.1の`IPriorityHost`は恐らく`MenuType`等を返さない設計と推測されるため、この`getGuiBridge()`メソッドが1.12.2固有の追加要件になる) |
| コンテナ | `appeng.container.implementations.ContainerPriority` | 汎用。`IPriorityHost`を受け取るコンストラクタのみで動作する。改造不要 |
| 画面 | `appeng.client.gui.implementations.GuiPriority` | 汎用。数値直接入力ではなく **+1/+10/+100/+1000・-1/-10/-100/-1000ボタン**での増減UI(`AEConfig.instance().priorityByStacksAmounts(...)`で刻み幅を設定から取得)。改造不要 |
| GUI起動経路 | `appeng.core.sync.GuiBridge`の`GUI_PRIORITY( ContainerPriority.class, IPriorityHost.class, GuiHostType.WORLD, SecurityPermissions.BUILD )`(180行目) | **既に汎用エントリとして存在する**。`Platform.openGUI(player, te, side, GuiBridge.GUI_PRIORITY)`を呼ぶだけで、`te`が`IPriorityHost`を実装してさえいれば動作する |

**実装パターンの実例**として`appeng.tile.misc.TileInterface`(322〜366行目)を確認した:
```java
@Override
public int getPriority() { return this.duality.getPriority(); }
@Override
public void setPriority(final int newValue) { this.duality.setPriority(newValue); }
@Override
public ItemStack getItemStackRepresentation() {
    return AEApi.instance().definitions().blocks().iface().maybeStack(1).orElse(ItemStack.EMPTY);
}
@Override
public GuiBridge getGuiBridge() { return GuiBridge.GUI_INTERFACE; }
```
`getItemStackRepresentation()`は「戻るボタン」に表示するアイコン(=このデバイス自身のアイテム)、`getGuiBridge()`は「戻るボタン」を押したときに戻る先のGUIを返す。`GuiPriority`側(94〜99行目)はこの2つの値から**自動的に`GuiTabButton`(戻るボタン)を生成する**:
```java
final ItemStack myIcon = con.getPriorityHost().getItemStackRepresentation();
this.OriginalGui = con.getPriorityHost().getGuiBridge();
if (this.OriginalGui != null && !myIcon.isEmpty()) {
    this.buttonList.add(this.originalGuiBtn = new GuiTabButton(...));
}
```

### 1.2 結論: 3.12(TabButtonAccessor)・3.13(WidgetContainerAccessor)相当のMixinは1.12.2では**不要になる可能性が高い**

1.20.1版で`TabButtonAccessor`/`WidgetContainerAccessor`が必要だったのは、1.20.1の`PriorityScreen`が「戻るボタンのアイコン・ラベルを状況に応じて動的に上書きしたい」という化粧直し要求のためだった(3.9参照、`PriorityScreenMixin`のソース確認により裏付け済み: `back.setMessage(...)`, `accessor.ae2cp$setIcon(...)`等)。

1.12.2の`GuiPriority`では、戻るボタンは`getItemStackRepresentation()`/`getGuiBridge()`の**戻り値だけで完全に決まる**設計になっている。つまり「Crafting Status画面に戻るときだけアイコンをハンマーに変えたい」等の化粧直しをしたい場合も、**Accessor Mixinで既存ボタンを書き換える必要はなく、`TileCraftingTileMixin`(IPriorityHost実装)側の`getItemStackRepresentation()`/`getGuiBridge()`の返す値を状況に応じて切り替えるだけで実現できる**(例えば「クラフトフロー由来で開いた場合は`GuiBridge.GUI_CRAFTING_STATUS`+ハンマーアイコン、それ以外は通常のCPUブロックアイコン」のように出し分ける)。

**この場合、当初の設計書で最も実装コストが低いと見積もっていた3.12/3.13が、1.12.2では「実装自体が不要」になり、コストがゼロになる可能性が高い。** ただし、化粧直し自体を1.12.2版でも行うかどうか(1.20.1と完全に同じUXにするか、簡略化するか)はユーザーとの確認事項として残す。

### 1.3 経路Bの1.12.2実装方針(確定)

- **新規Mixin**: `TileCraftingTileMixin`(対象: `appeng.tile.crafting.TileCraftingTile`、`remap = false`)
  - `implements IPriorityHost`
  - `getPriority()`/`setPriority(int)`: 3.1で設計済みの「クラスタ単位で優先度を保持する」仕組み(`CraftingCPUClusterMixin`が`writeToNBT`/`readFromNBT`末尾に追記する優先度フィールド)に委譲する。1.20.1の`CraftingBlockEntityMixin`が`PriorityHolder.getPriorityOrDefault(ae2cp$self().getCluster())`という「クラスタをキーにした優先度保持クラス」経由で行っているのと同じ設計を踏襲する。
  - `getItemStackRepresentation()`: Crafting CPUブロックのItemStackを返す(`TileCraftingTile`が保持するブロック参照から生成。1.12.2の`AEApi.instance().definitions().blocks()...`経由の各種ブロック定義を確認して対応するCrafting CPU系ブロック定義を特定する必要がある)
  - `getGuiBridge()`: 戻り先。Crafting Status画面(`GuiBridge.GUI_CRAFTING_STATUS`)に戻すのが妥当と思われるが、右クリックで直接開いた場合の妥当な戻り先を要検討。
  - **注意点(要検証)**: `AEBaseTile`(1.12.2、`TileCraftingTile`の祖先)は**`instanceof IPriorityHost`であれば自動的に`writeToNBT`/`readFromNBT`で`"priority"`キーの読み書きを行う**(`AEBaseTile.java` 381行目・450行目で確認済み)。これは**タイル単体の自動永続化**であり、3.1で設計した「クラスタ単位」の永続化とは別レイヤーで動く。`TileCraftingTileMixin`を追加すると、この自動フックが**意図せず発火し**、`getPriority()`/`setPriority()`(クラスタへの委譲)経由で余計な読み書きが発生する可能性がある。特に、**クラスタがまだ形成されていない(マルチブロック未結合)タイミングで`readFromNBT`が呼ばれ`setPriority()`が実行されると、クラスタ未形成でNPEになるリスク**がある(3.1のチェックリストの「`done()`経由の`readFromNBT`」の懸念と直結)。実装時に`AEBaseTile`のreadFromNBT/writeToNBTの実処理タイミングと、クラスタ形成タイミングの前後関係を必ず確認すること。

### 1.4 GUI起動口(要設計、未確定)

経路Bを「設置済みCrafting CPUを右クリックして優先度を編集する」ためのUI導線をどこに置くか(Crafting Status画面にボタンを追加/専用のタブ/ブロック右クリック直後に直接GuiPriorityを開く、等)は1.20.1版addonの実際のUX(どのボタン・どの画面から`IPriorityHost`経由の優先度画面を開いているか)を確認していない。**次回セッションで1.20.1側の呼び出し元(`getGuiBridge()`実装や`CraftingBlockEntity`右クリックハンドラ)を確認し、1.12.2でも同じ導線を再現すること。**

---

### 11-2. 経路A(クラフトフロー内の優先度ステップ)の1.12.2設計

### 2.1 CraftAmountMenuMixin(3.11) ─ 実ソースで完全確認、当初推測と一致

1.20.1実ソース(`CraftAmountMenuMixin.java`):
```java
@Mixin(value = CraftAmountMenu.class, remap = false)
public abstract class CraftAmountMenuMixin {
    @Shadow private AEKey whatToCraft;

    @Redirect(method = "confirm", at = @At(value = "INVOKE",
            target = "Lappeng/menu/MenuOpener;open(...)Z"))
    private boolean ae2cp$openPriorityStep(MenuType<?> type, Player player, MenuLocator locator,
            int amount, boolean craftMissingAmount, boolean autoStart) {
        CraftPriorityStepMenu.open((ServerPlayer) player, locator, this.whatToCraft, amount, autoStart,
                PriorityHolder.DEFAULT_PRIORITY);
        return true;
    }
}
```
`MenuOpener.open(CraftConfirmMenu.TYPE, ...)`という呼び出し自体を`@Redirect`で丸ごと乗っ取り、代わりに`CraftPriorityStepMenu.open(...)`(MOD独自メソッド)を呼んでいる。**当初3.11で「最有力案」としていた「`Platform.openGUI`呼び出しの第4引数を差し替える」という設計方針は、1.20.1側の実装と完全に一致していたことが確認できた。**

1.12.2側は当初設計(1.2節)の通り、`appeng.core.sync.packets.PacketCraftRequest#serverPacketData(...)`内の`Platform.openGUI(player, te, side, GuiBridge.GUI_CRAFTING_CONFIRM)`を対象にする。**ただし対象が`GuiBridge`という列挙型である以上、「案A: 独自GuiBridge相当の仕組みを自前で用意する」が依然として必要**(2.2節参照)。この部分の技術的難所は当初評価(リスク「高」)のまま変わらない。

### 2.2 CraftPriorityStepMenu / CraftPriorityStepScreen ─ 新規発見、1.20.1では独立クラス(Mixin不要)

当初の設計書では「`PriorityScreenMixin`が独立ステップ画面を指している可能性」(3.9)として扱っていたが、実際には**`PriorityScreenMixin`とは無関係の、完全に独立したMOD所有クラス**として存在する:

- `com.yuuhamu.ae2craftpriority.menu.CraftPriorityStepMenu`(`AEBaseMenu implements ISubMenu`) ─ AE2への侵襲的Mixinではなく普通のサブクラス。`MenuTypeBuilder.create(...).withInitialData(...).build(...)`で独自`MenuType`を登録している(1.20.1は`MenuType`が動的登録可能なオブジェクトであるため、この形が可能)。
- `com.yuuhamu.ae2craftpriority.client.CraftPriorityStepScreen`(`AEBaseScreen<CraftPriorityStepMenu>`) ─ `NumberEntryWidget`で優先度を数値入力し、「次へ」ボタンで`menu.confirmPriority(value)`を呼ぶ。
- `CraftPriorityStepMenu#confirmPriority(int)`: サーバー側で`MenuOpener.open(CraftConfirmMenu.TYPE, ...)`→`PriorityHolder.setPriority(confirm, priority)`→`confirm.planJob(...)`→`broadcastChanges()`という順で、確認画面を開きつつ優先度をセットしてジョブ計画を実行している。

**1.12.2移植では、この2クラスに相当する完全新規クラス`ContainerCraftPriorityStep`+`GuiCraftPriorityStep`をMOD側にゼロから実装する必要がある**(AE2への侵襲的Mixinではないので、Mixin特有の制約〔`@Shadow`/`remap`等〕を考えずに済む点はむしろ簡単)。設計方針:
- `ContainerCraftPriorityStep extends AEBaseContainer`: `whatToCraft`・`amount`・`autoStart`を保持し、`GuiTextField`(1.12.2の`GuiCraftAmount`と同様の数値入力パターン)相当の値をパケット経由で受け取るフィールドを持つ。
- `GuiCraftPriorityStep extends AEBaseGui`: `GuiNumberBox`(`GuiPriority`が使っているのと同じウィジェットクラスが使えるはず、要確認)+「次へ」ボタン。
- 「次へ」ボタン押下時、新規パケット(例: `PacketCraftPriorityConfirm`)をサーバへ送信 → サーバ側ハンドラで`Platform.openGUI(player, te, side, GuiBridge.GUI_CRAFTING_CONFIRM)`を呼んで`ContainerCraftConfirm`を開き、優先度をそこに反映する(3.4の`ContainerCraftConfirm`側優先度受け渡し〔ThreadLocal等〕と接続する)。

このクラスの追加により、**当初の設計書(8章推奨実装順序)には無かった新規タスクが1つ増える**:「経路A用の`ContainerCraftPriorityStep`/`GuiCraftPriorityStep`を新規実装する」という、Mixinではなく純粋な新規クラス追加のタスク。これは技術的難易度としては低め(通常のContainer/GUI実装であり、Mixin特有の罠が無い)だが、作業量としては新規に増える。

### 2.3 CraftingBlockEntityMixin(3.10) ─ 当初の「NBT/マルチブロック」説は誤りと判明

実ソース(`CraftingBlockEntityMixin.java`)確認の結果、当初「Crafting CPU自身のNBT保存/マルチブロック計算まわりの何か」という推測(3.10の「注意喚起」段落)は**誤りだった**。実際の役割は1節で述べた**経路B(`IPriorityHost`実装)そのもの**:
```java
@Mixin(value = CraftingBlockEntity.class, remap = false)
public abstract class CraftingBlockEntityMixin implements IPriorityHost, CraftingPriorityHostMarker {
    @Override public int getPriority() { return PriorityHolder.getPriorityOrDefault(ae2cp$self().getCluster()); }
    @Override public void setPriority(int newValue) { PriorityHolder.setPriority(ae2cp$self().getCluster(), newValue); }
    @Override public void returnToMainMenu(Player player, ISubMenu subMenu) { ... }
    @Override public ItemStack getMainMenuIcon() { return new ItemStack(ae2cp$self().getUnitBlock()); }
}
```
`CraftingPriorityHostMarker`という独自マーカーインターフェースを介して、`PriorityScreenMixin`側が「このホストがクラフト優先度絡みか」を判定している(`menu.getHost() instanceof CraftingPriorityHostMarker`)。1.12.2では`TileCraftingTileMixin`に同様のマーカーインターフェース実装を追加するとよい(1.3節の設計に统合済み)。

**3.10は独立項目として扱う必要がなくなり、1節(経路B)の設計にそのまま統合される。**

### 2.4 PriorityScreenMixin(3.5・3.9) ─ 対象は「クラフト確認画面」ではなく「AE2既存の汎用優先度画面」

当初3.5は`appeng.client.gui.implementations.GuiCraftConfirm`(クラフト確認画面)を対象と推測していたが、これは**誤りだった**。実ソース(`PriorityScreenMixin.java`):
```java
@Mixin(value = PriorityScreen.class, remap = false)
public abstract class PriorityScreenMixin extends AEBaseScreen<PriorityMenu> {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2cp$onInit(...) {
        if (!(menu.getHost() instanceof CraftingPriorityHostMarker)) return;
        setTextContent("priority_insertion_hint", ...);
        setTextHidden("priority_extraction_hint", true);
        if (PriorityBackIconOverride.take() && ...) {
            // 戻るボタンのアイコン・ラベルを上書き
        }
    }
}
```
対象は`appeng.client.gui.implementations.PriorityScreen`という**AE2が最初から持つ汎用の優先度編集画面**(1.12.2での対応物が1節で確認した`GuiPriority`)。`menu.getHost() instanceof CraftingPriorityHostMarker`という条件分岐があることから、**この画面はCrafting CPU専用ではなく、Interface・Storage Bus等あらゆる`IPriorityHost`実装で共用されており、ホストがCrafting CPU(`CraftingPriorityHostMarker`)のときだけ文言・アイコンを差し替えている**ことが分かる。

1.12.2では、1.2節で述べた通り**`GuiPriority`自体を改造する必要はなく**、`TileCraftingTileMixin#getItemStackRepresentation()`/`getGuiBridge()`の返す値を出し分けるだけで同等のカスタマイズが可能(1.12.2の`GuiPriority`は「ヒントテキスト」を持たないシンプルなUIのため、ヒントテキスト差し替え自体は該当機能が無く不要)。**`PriorityScreenMixin`・`TabButtonAccessor`・`WidgetContainerAccessor`の3クラスはいずれも1.12.2移植では実装不要になる見込みが高い。**

**3.5(旧`CraftConfirmScreenMixin`)は完全に廃案とする。1.12.2の`GuiCraftConfirm`(クラフト確認画面)自体への優先度UI追加は、実際には発生しない設計だったことが判明したため。**

---

### 11-3. 12個のMixin一覧の再分類(確定)

| # | 1.20.1クラス名 | 所属経路 | 1.12.2での扱い |
|---|---|---|---|
| 1 | `CraftingCPUClusterMixin` | 共通基盤(NBT永続化) | そのまま移植(3.1、変更なし) |
| 2 | `CraftingServiceMixin` | 共通基盤(CPU集合差し替え) | そのまま移植(3.2、変更なし) |
| 3 | `CraftingCpuLogicMixin` | 共通基盤(CPU選定ソート) | そのまま移植(3.3、変更なし) |
| 4 | `CraftConfirmMenuMixin` | 経路A(startJob中に優先度をThreadLocal伝播) | そのまま移植(3.4、変更なし) |
| 5 | `CraftAmountMenuMixin` | 経路A(遷移先を横取り) | 実ソースで方針確認済み(2.1)。1.12.2は`PacketCraftRequest`対象、案A/B要PoC(変更なし、リスク「高」のまま) |
| 6 | `CraftingCPUMenuMixin` | 経路C(CPU一覧管理) | そのまま移植(3.6、変更なし) |
| 7 | `CraftingStatusMenuMixin` | 経路C(CPU一覧管理) | そのまま移植(3.6、変更なし) |
| 8 | `CraftingCPUScreenMixin` | 経路C(Status画面UI) | そのまま移植(3.7、変更なし) |
| 9 | `CraftingBlockEntityMixin` | **経路B(IPriorityHost実装)** | **設計を全面更新(2.3 = 1.3節に統合)**。1.12.2は`TileCraftingTileMixin`として新規設計 |
| 10 | `PriorityScreenMixin` | **経路B(汎用優先度画面の化粧直し)** | **1.12.2では実装不要の見込み**(2.4) |
| 11 | `TabButtonAccessor` | 経路B(化粧直し補助) | **1.12.2では実装不要の見込み**(1.2) |
| 12 | `WidgetContainerAccessor` | 経路B(化粧直し補助) | **1.12.2では実装不要の見込み**(1.2) |
| (新規) | `CraftPriorityStepMenu`/`Screen` | 経路A(新規UI) | **新規クラスとして`ContainerCraftPriorityStep`/`GuiCraftPriorityStep`を追加実装**(2.2) |
| (新規) | `TileCraftingTileMixin implements IPriorityHost` | 経路B | **新規Mixin追加**(1.3) |

**正味の見積もり変化**: 当初12個のMixin移植というタスク数だったが、実際には「9個のMixin移植 + 1個の新規Mixin(`TileCraftingTileMixin`) + 1組の新規Container/GUIクラスペア」に近い形になり、**3個(PriorityScreenMixin・TabButtonAccessor・WidgetContainerAccessor相当)が不要になる一方、新規実装(経路A用の独自GUI一式)が増える**。総工数としては大きくは変わらないが、実装の見通しは大幅にクリアになった(低信頼度セクションがゼロになった)。

---

### 11-4. 残る未確定事項(次に着手すべき順)

1. **`GuiBridge` enumへの新規GUI追加方式(案A/案B)のPoC** ─ 依然として最大の技術的難所(§2.1参照、変更なし)。実装着手前に必ず検証する。
2. **経路Bの起動導線**(1.4節) ─ 1.20.1側の実際の呼び出し元コードを確認し、1.12.2でのUI導線(Crafting Status画面のボタン追加か、ブロック右クリック直接か)を決定する。
3. **`AEBaseTile`の自動NBTフックとクラスタ単位永続化の整合性**(1.3節「注意点」) ─ クラスタ未形成時のNPEリスクを実装前に検証する。
4. **経路Aの新規Container/GUI実装**(2.2節) ─ Mixinではなく通常の新規クラス追加のため、技術的難易度は低いが、パケットハンドラ(`PacketCraftPriorityConfirm`等)を含めた実装が必要。

---

以上、2026-07-22セッションでの追加調査結果。次回セッションはこの内容を`PORT-DESIGN-1.12.2.md`本体にマージし、プロジェクトディレクトリ(`Z:\Claude\Projects\MinecraftMods\AE2CraftPriority-1.12.2`、Git初期化・GitHub非公開リポジトリ作成含む)の作成に進む。

---

## 12. GUI設計の見直し(2026-07-23): `Knowledge/ae2-priorityscreen-gui-design-reference.md`反映

### 12.0. 経緯と結論の要約

`Knowledge/ae2-priorityscreen-gui-design-reference.md`(および参照先の`Knowledge/ae2-priorityscreen-back-navigation-and-icon-fix.md`)を読み、11章時点の結論を実ソース(`/tmp/ae2-1122`、AE2 `rv6-1.12`ブランチ)で再検証した。

**11-3節の表で「1.12.2では実装不要の見込み」としていた`PriorityScreenMixin`・`TabButtonAccessor`・`WidgetContainerAccessor`相当の3項目のうち、「戻り導線(バック機能)の代替Mixin」は不要ではなく必要と判明したため、ここで訂正する。**

一方、「化粧直し(アイコン差し替え)」については1.12.2固有の事情により**1.16.5より大幅に簡単になる**ことも判明した。差分は以下の通り:

| 項目 | 11章時点の結論 | 12章での訂正後の結論 |
|---|---|---|
| 戻りボタンのクリック時導線(`PacketSwitchGuis`バイパス) | 不要の見込み | **必要。1.12.2にも同種のロケータ再利用バグが存在することを実ソースで確認**(12.1節) |
| 戻りボタンのアイコン差し替え(化粧直し) | 不要の見込み(≒`TabButtonAccessor`/`WidgetContainerAccessor`相当が不要) | **Mixin自体は不要のまま(結論は維持)。ただし理由が異なり、`GuiTabButton`がItemStackアイコンを標準サポートするためカスタム描画コード自体が原理的に不要**(12.2節) |
| `PriorityReturnTarget`相当(戻り先記録) | 未検討 | **移植が必要。ロジックはバージョン非依存でそのまま流用可能**(12.3節) |

### 12.1. 戻り導線の再検証: `PacketSwitchGuis`は1.12.2にも同種のバグを持つ

1.16.5の調査(`ae2-priorityscreen-back-navigation-and-icon-fix.md`)で特定された根本原因は、AE2純正の戻るボタンが「現在開いているコンテナ自身のロケータを再利用すれば元の画面に戻れる」という前提で実装されている点にあった。この前提は、優先度画面を素直な導線(ブロック右クリックなど)で開いた場合は成立するが、本MODのように「Crafting Statusタブのレンチボタン経由」という別ルートで開いた場合には成立しない。

`/tmp/ae2-1122`の実ソースを確認したところ、1.12.2でも**構造的に同一のバグパターン**が存在することを確認した。

- `appeng.client.gui.implementations.GuiPriority#actionPerformed`(`GuiPriority.java:122-125`)は、戻るボタン(`originalGuiBtn`)がクリックされた際、`NetworkHandler.instance().sendToServer(new PacketSwitchGuis(this.OriginalGui))`を送信するのみで、「元々どの画面のどの状態から遷移してきたか」の情報を一切含まない。`this.OriginalGui`は`con.getPriorityHost().getGuiBridge()`から取得した`GuiBridge`列挙値1個だけであり、それ以外の文脈(どのCPU/どのタブ/どのスクロール位置か等)は保持されない。
- サーバ側ハンドラ`appeng.core.sync.packets.PacketSwitchGuis#serverPacketData`(`PacketSwitchGuis.java:61-75`)は、`player.openContainer`(=現在開いている`ContainerPriority`自身)から`AEBaseContainer#getOpenContext()`を取得し、その`ContainerOpenContext#getTile()`(=現在の優先度コンテナが記録している元のTileEntity)を使って`Platform.openGUI(player, te, side, newGui)`を呼び出す。つまり**「現在開いている優先度コンテナ自身が持つ、コンテナ生成時点のロケータ」を再利用する**という、1.16.5と全く同じ設計上の前提がある。
- `appeng.container.implementations.ContainerPriority`のコンストラクタ(`ContainerPriority.java:47`)は`super(ip, (TileEntity)(te instanceof TileEntity ? te : null), (IPart)(te instanceof IPart ? te : null))`という形で、`IPriorityHost`実装(`te`)が`TileEntity`でも`IPart`でもない場合は`null`を渡す。さらに`ContainerOpenContext`のコンストラクタ(`ContainerOpenContext.java:40-44`)は、渡されたオブジェクトが`IPart`でも`TileEntity`でもない場合`isItem = true`とマークし、`getTile()`は常に`null`を返す(`ContainerOpenContext.java:46-53`)。

本MODの経路B(Crafting Statusタブ → レンチボタン → 優先度編集)で編集対象となる「優先度ホスト」は、現実のワールド上のブロック(インターフェース等)ではなく、**選択中のCPUに紐づくクラフトジョブを表す合成的な`IPriorityHost`実装**になる想定である(11章2.3/1.3節参照)。この合成ホストが`TileEntity`でも`IPart`でもない実装になった場合、`ContainerOpenContext.getTile()`は常に`null`を返し、`PacketSwitchGuis`のサーバ側ハンドラは`Platform.openGUI(player, null, side, newGui)`を呼ぶことになる。`GuiBridge.GUI_PRIORITY`は`GuiHostType.WORLD`として登録されている(`GuiBridge.java:180`)ため、`tile`が`null`のケースの挙動はワールド系GUIとしては本来想定されておらず、意図した「端末のCrafting Statusタブへ戻る」動作にはならない(たとえ合成ホストを無理に`TileEntity`実装にラップしたとしても、`Platform.openGUI`が開くのは「そのタイルの既定GUI」であり、端末の特定タブ・特定スクロール位置に戻ることはできない)。

**結論**: 1.16.5と同じ問題が1.12.2にも存在する。したがって11章での「戻り導線のMixinは不要」という結論を撤回し、以下の対処が必要と改める。

#### 12.1.1. 対処方針(1.12.2版)

1.16.5では「マーカーインターフェース + `PriorityContainerMixin` + `PriorityScreenMixin`書き換え」の3点セットが必要だったが、1.12.2は`PacketSwitchGuis`のサーバ側ハンドラが単一の集約ポイント(`serverPacketData`)になっているため、**サーバ側Mixin1本だけで完結できる見込み**であり、1.16.5より単純化できる。

- **マーカーインターフェース**: `IPriorityHost`を実装する合成ホスト(CPUクラフトジョブ用)に、戻り先情報を持つ独自インターフェース(仮称`IReturnableSyntheticPriorityHost`)を実装させる。
- **サーバ側Mixin(新規、`PacketSwitchGuisMixin`)**: `PacketSwitchGuis#serverPacketData`に対して`@Inject(at = @At("HEAD"), cancellable = true)`を張り、`player.openContainer instanceof ContainerPriority`かつ`((ContainerPriority) c).getPriorityHost() instanceof IReturnableSyntheticPriorityHost`の場合、`ci.cancel()`した上で本MOD独自の復帰処理(`PriorityReturnTarget`相当、12.3節)を実行する。それ以外(通常のワールド上`IPriorityHost`実装、たとえばインターフェース等の在来ケース)は素通りさせ、AE2純正の挙動を一切変更しない。
- クライアント側(`GuiPriority`/`originalGuiBtn`のクリックハンドラ)は改変不要。`PacketSwitchGuis`自体は従来どおり送信させ、その中身(=`this.OriginalGui`という`GuiBridge`列挙値)は今回の分岐では使用せず、サーバ側でPriorityReturnTargetの記録を優先する。
- `@Redirect`で送信パケット自体を差し替える案(クライアント側Mixin)も検討したが、判定に必要な情報(`ContainerPriority#getPriorityHost()`)はサーバ・クライアント双方から到達可能であり、より変更範囲が小さいサーバ側`@Inject`案を採用する。

### 12.2. アイコン差し替えの再検証: `GuiTabButton`はItemStackアイコンを標準サポートするため、1.16.5のBlitterハック相当は不要

1.16.5では、戻るボタンのアイコンをAE2純正の`Icon`列挙型/テクスチャアトラス経由でしか描画できず、移植元の(アトラスに存在しない)アイコンを表示するために`Blitter`ヘルパー経由の低レベル描画ハックが必要だった(`ae2-priorityscreen-back-navigation-and-icon-fix.md`参照)。

`/tmp/ae2-1122`の`appeng.client.gui.widgets.GuiTabButton`を確認したところ、1.12.2の`GuiTabButton`は最初から2種類のコンストラクタを持つことを確認した(`GuiTabButton.java:41-70`)。

- `GuiTabButton(x, y, int ico, String message, RenderItem ir)` ─ AE2純正アトラス(`states.png`)上のインデックスでアイコンを指定する版。
- `GuiTabButton(x, y, ItemStack ico, String message, RenderItem ir)` ─ **任意の`ItemStack`をアイコンとして使う版**。`drawButton`内部では`this.itemRenderer.renderItemAndEffectIntoGUI(this.myItem, ...)`という、標準のGUIアイテムレンダリングパイプラインをそのまま呼んでいるだけである(`GuiTabButton.java:105-113`)。

さらに`GuiPriority#initGui`(`GuiPriority.java:84-91`)を見ると、戻るボタン自体が最初から**ItemStack版コンストラクタで生成されている**ことを確認した。

```java
final ItemStack myIcon = con.getPriorityHost().getItemStackRepresentation();
this.OriginalGui = con.getPriorityHost().getGuiBridge();
if( this.OriginalGui != null && !myIcon.isEmpty() )
{
    this.buttonList.add( this.originalGuiBtn = new GuiTabButton( this.guiLeft + 154, this.guiTop, myIcon, myIcon.getDisplayName(), this.itemRender ) );
}
```

つまり、戻るボタンのアイコンは`IPriorityHost#getItemStackRepresentation()`が返す`ItemStack`をそのまま表示する仕組みであり、**AE2純正のアトラス・列挙型を経由しない**。したがって、本MODの合成`IPriorityHost`実装(12.1節のマーカーインターフェース)側で`getItemStackRepresentation()`が「戻る」を意味する任意のカスタムアイコン(たとえば独自登録した専用アイテム、あるいは既存アイテムの流用)を返しさえすれば、AE2純正の描画パイプラインがそのまま正しく表示してくれる。

**結論**: 11章の「化粧直し用Mixin(`TabButtonAccessor`/`WidgetContainerAccessor`相当)は不要」という結論自体は維持できるが、その理由は「1.12.2では対応する仕組みがそもそも存在しないから」ではなく、**「1.12.2の`GuiTabButton`はItemStackアイコンを標準サポートしており、独自Mixin/低レベル描画ハックなしで任意アイコンを表示できるから」**である。11章での説明はこの点で不正確だったため、ここで訂正する。実装時のTODOとしては、Mixinではなく通常のJavaコード(合成`IPriorityHost`実装の`getItemStackRepresentation()`)で完結する。

- **保留事項**: カスタムアイコン用の専用アイテムを新規登録するか、既存アイテム(バニラまたはAE2)を流用するかは未確定。専用アイテムを登録する場合、テクスチャ・モデル(`item/<id>.json`)・言語ファイルの追加が必要になる(実装コストは小さいが、13章以降で正式にタスク化する)。

### 12.3. `PriorityReturnTarget`相当の移植方針

1.16.5の`PriorityReturnTarget`(プレイヤーUUID単位で「どこに戻るか」を保持する静的マップ、`peek()`/`take()`の使い分けが必要)は、バージョン非依存のロジックであるため1.12.2でもほぼそのまま移植できる。

- データ構造: `Map<UUID, ReturnTarget>`(静的、`ReturnTarget`は`(GuiBridge, ワールド, BlockPos, AEPartLocation side)`程度の組を保持 ─ 1.12.2の`Platform.openGUI`のシグネチャ(`openGUI(EntityPlayer, TileEntity, AEPartLocation, GuiBridge)`)に合わせた形にする)。
- 記録タイミング: Crafting Statusタブのレンチボタン押下時(経路Bの起動時、11章1.4節で要設計としていた箇所)、優先度画面を開く直前に「今開いている端末の位置」を`PriorityReturnTarget`に記録する。
- 消費タイミング: 12.1節のサーバ側Mixin(`PacketSwitchGuisMixin`)内で、`take()`(消費・削除)を用いて記録を取り出し、その位置情報で端末画面を再度開く。
- シングルプレイヤーの罠(1.16.5で確認済みの`peek()`/`take()`使い分けの必要性)は、クライアント・統合サーバが同一JVM上で静的状態を共有する1.12.2でも同様に発生しうる。ただし1.12.2案では「UIチェック用の`peek()`呼び出し」自体が発生する箇所が(クライアント側Mixin不要のため)存在しない見込みであり、`take()`一発で完結できる可能性が高い。この点は実装時に要検証。

### 12.4. 11章の記載に対する訂正まとめ

11-3節の表の該当3行を以下のように読み替える。

| # | 対象 | 11章時点 | 12章での訂正 |
|---|---|---|---|
| 10 | `PriorityScreenMixin`相当 | 1.12.2では実装不要の見込み | **不要ではない。`PacketSwitchGuisMixin`(新規、サーバ側1本)が必要(12.1.1節)** |
| 11 | `TabButtonAccessor`相当 | 1.12.2では実装不要の見込み | **結論(不要)は維持。ただし理由を訂正: `GuiTabButton`のItemStackアイコン機構により、そもそもMixinで介入する必要がない(12.2節)** |
| 12 | `WidgetContainerAccessor`相当 | 1.12.2では実装不要の見込み | **結論(不要)は維持。理由は#11と同様(12.2節)** |

11-3節末尾の「正味の見積もり変化」も以下のように更新する。

- 新規Mixin: `TileCraftingTileMixin`(経路B、`IPriorityHost`実装、11章1.3節)に加えて、**`PacketSwitchGuisMixin`(戻り導線バイパス、12.1.1節)を追加**。
- 新規Javaクラス: 合成`IPriorityHost`実装(マーカーインターフェース`IReturnableSyntheticPriorityHost`を含む)、`PriorityReturnTarget`相当(12.3節)、`CraftPriorityStepMenu`/`Screen`(経路A、11章2.2節、変更なし)。
- Mixin不要と確定: `TabButtonAccessor`・`WidgetContainerAccessor`相当(理由は訂正、結論は維持)。

### 12.5. 次に着手すべき順序(11-4節の更新)

11-4節のリストに以下を追加する(優先度は既存項目と並列、実装フェーズでは`GuiBridge` enum PoC(11-4節#1)の直後が妥当)。

5. **`PacketSwitchGuisMixin`のPoC**(12.1.1節) ─ `PacketSwitchGuis#serverPacketData`への`@Inject(cancellable=true)`が意図通り機能するか、および`ContainerPriority#getPriorityHost()`へのアクセス(`public`メソッドのため`@Accessor`不要、通常のキャスト呼び出しで到達可能なことを確認済み)を実機で検証する。
6. **合成`IPriorityHost`実装の設計**(12.1節・12.3節) ─ CPUクラフトジョブを表す合成ホストの具体的なフィールド構成(どのCPU/どのクラスタを指すか)と、`IReturnableSyntheticPriorityHost`マーカーインターフェースの設計を、11章1.3節の`TileCraftingTileMixin`設計と合わせて確定する。
7. **戻るボタン用アイコンアイテムの要否判断**(12.2節「保留事項」) ─ 専用アイテム新規登録 or 既存アイテム流用のどちらにするか確定する。

以上、2026-07-23セッションでのGUI設計見直し結果。`Knowledge/ae2-priorityscreen-gui-design-reference.md`及び`Knowledge/ae2-priorityscreen-back-navigation-and-icon-fix.md`の内容を1.12.2の実ソース(`/tmp/ae2-1122`、AE2 `rv6-1.12`ブランチ)で裏取りし、11章の結論の一部(戻り導線Mixinの要否)を訂正した。次回セッションでは12.5節のリストに従い、まず`GuiBridge` PoCと並行して合成`IPriorityHost`実装の詳細設計に着手する。ビルド(`gradlew build`/`runClient`)は引き続き1.16.5側の検証完了までは着手しない。
