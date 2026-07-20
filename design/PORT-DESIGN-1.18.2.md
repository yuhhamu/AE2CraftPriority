# AE2 Crafting Priority — Minecraft 1.18.2版 実装設計書 v2

作成日: 2026-07-20
対象: 実装担当者(1.20.1版の完成済みソース + 本設計書を見ながら実装する開発者)
前提: 本設計書は実装ではなく設計のみ。コード生成は別タスク。

## 0. v1からの変更点(重要)

v1(`F:\claude\MinecraftMods\AE2CraftPriority\design\PORT-DESIGN-1.18.2.md`)は、移行前の古いコピー(`F:\claude\MinecraftMods\AE2CraftPriority`)を元に書かれており、現在の本体(`Z:\Claude\Projects\MinecraftMods\AE2CraftPriority`)とは以下の点で大きく食い違っていた。本v2はこれを全面的に書き直したものである。

1. **パッケージ構成が刷新済み**: `mixin/`直下フラット12クラスではなく、`mixin/core`・`mixin/craft`・`mixin/status`・`mixin/accessor`の4パッケージに再編されている。
2. **`api/`パッケージが新規追加**: 他Modから優先度を読み書きする公開API(`CraftPriorityApi`/`PriorityAdapter`)。
3. **`compat/advancedae/`パッケージが新規追加**されているが、**AdvancedAE Mod自体がMinecraft 1.18.2版を配布していない**(Modrinth確認: 1.20.1と1.21.1のみ)ため、**本移植の対象範囲から除外**する(ユーザー承認済み)。
4. **`priority/CraftingPriorityHostMarker.java`が新規追加**。
5. **v1の技術的結論のうち誤っていた点**: v1は`CraftingServiceMixin`について「`@Redirect` at `@At("NEW")`方式」を推奨していたが、現行実装は`@Shadow @Final @Mutable`によるフィールド名直接指定方式を採用している(他Modが同じコンストラクタに独自の`new HashSet<>()`を追加してもordinalのズレで誤爆しないようにするための意図的choice)。この方式は1.18.2でもそのまま踏襲できることを実ソース比較で確認済み(5章参照)。
6. **v1が縣念していたリスクの多くが実際には無関係と判明**: `cancel()`/`cancelJob()`の改名、`trySubmitJob`/`submitJob`の抽象・default逆転、`PoseStack`→`GuiGraphics`のAPI刷新、`CraftingCpuListEntry`のフィールド構成差異——これらはAE2の1.18.2/1.20.1間で実在する差異ではあるが、**現行の実装(12 Mixin)はいずれの箇所にも触れていない**ため、本移植には影響しない(2章で個別に確認結果を記載)。
7. **確認された唯一の実質的な差異**: `CraftAmountMenu#confirm`の引数が2個(1.18.2)→3個(1.20.1)。`CraftAmountMenuMixin`の対象シグネチャを1.18.2向けに書き換える必要がある(2-6参照)。

## 0.1 検証方法

本設計書は以下を実際に取得・突き合わせて作成した。

- 現行ソース: `Z:\Claude\Projects\MinecraftMods\AE2CraftPriority\src\main\java\com\yuuhamu\ae2craftpriority\` 配下の全35 Javaファイル(mixin 12・api 2・priority 7・menu 1・client 3・mod本体1・compat/advancedae 9)を全文読了。
- AE2公式リポジトリ: `https://github.com/AppliedEnergistics/Applied-Energistics-2` の `forge/1.18.2` ブランチ(コミット `923cc06`)と `forge/1.20.1` ブランチ(コミット `b4b08d9`、2025-10-25)を実際にclone(`--depth 1`)し、本MODが対象とする全クラス・メソッド・フィールドを`grep`/`diff`で直接突き合わせた。
- AE2 1.18.2向けForgeビルドの実配布(Modrinth `ae2` 11.7.6、Minecraft 1.18.2向け)を確認済み。
- AdvancedAEはModrinthで1.20.1・1.21.1のみ配布されており1.18.2版が存在しないことを確認済み。

## 1. 移植対象範囲

- `mixin/core/`(5): `CraftingBlockEntityMixin`, `CraftingCPUClusterMixin`, `CraftingCPUMenuMixin`, `CraftingCpuLogicMixin`, `CraftingServiceMixin`
- `mixin/craft/`(2): `CraftAmountMenuMixin`, `CraftConfirmMenuMixin`
- `mixin/status/`(3): `CraftingCPUScreenMixin`, `CraftingStatusMenuMixin`, `PriorityScreenMixin`
- `mixin/accessor/`(2): `TabButtonAccessor`, `WidgetContainerAccessor`
- `api/`(2): `CraftPriorityApi`, `PriorityAdapter`
- `priority/`(7): `CraftingCPUMenuGridAccess`, `CraftingPriorityHostMarker`, `CraftingStatusPriorityControl`, `PendingCraftPriority`, `PriorityHolder`, `PriorityOrderedHashSet`, `PriorityReturnTarget`
- `menu/CraftPriorityStepMenu.java`, `client/ClientSetup.java`, `client/CraftPriorityStepScreen.java`, `client/PriorityBackIconOverride.java`
- `AE2CraftPriorityMod.java`(ただし`AdvancedAeCompat`初期化呼び出しは削除する)

**対象外**: `compat/advancedae/`(4トップレベル + 5 Mixin、計9ファイル)。AdvancedAE自体が1.18.2をサポートしないため、依存Mavenアーティファクトが存在せず`compileOnly`依存すら組めない。`AE2CraftPriorityMod`コンストラクタ内の`ModList.get().isLoaded(AdvancedAeCompat.MOD_ID)`分岐、`build.gradle`の`ae2craftpriority-advancedae.mixins.json`登録、`mods.toml`の`advanced_ae`任意依存宣言も削除する。

## 2. Mixin/Accessor別 設計表(実ソース確認済み)

### 2-1. core.CraftingCPUClusterMixin

| 項目 | 内容 |
|---|---|
| 対象クラス | `appeng.me.cluster.implementations.CraftingCPUCluster`(`public final class implements IAECluster, ICraftingCPU`) |
| 対象メンバー | `public void writeToNBT(CompoundTag data)` / `public void readFromNBT(CompoundTag data)` |
| 1.18.2での確認結果 | 両メソッドとも1.18.2(260/278行目)・1.20.1(261/279行目)でシグネチャ完全一致。`@Inject(method="writeToNBT", at=@At("TAIL"))`/`readFromNBT`のTAIL Injectはそのまま移植可能。 |
| 改名の影響 | `cancel()`(1.18.2)→`cancelJob()`(1.20.1)の改名を確認したが、**本Mixinはこのメソッドに一切触れていないため無関係**。 |
| 変更要否 | **変更不要**(パッケージ宣言の`mixin.core`のみそのまま)。 |

### 2-2. core.CraftingServiceMixin

| 項目 | 内容 |
|---|---|
| 対象クラス | `appeng.me.service.CraftingService`(`public class implements ICraftingService, IGridServiceProvider`) |
| 対象メンバー | `private final Set<CraftingCPUCluster> craftingCPUClusters = new HashSet<>();`(1.18.2で114行目、1.20.1で116行目)。コンストラクタ`CraftingService(IGrid grid, IStorageService storageGrid, IEnergyService energyGrid)`(両バージョン完全一致)。 |
| 実装方式 | 現行実装は`@Shadow @Final @Mutable private Set<CraftingCPUCluster> craftingCPUClusters;` + `@Inject(method="<init>", at=@At("TAIL"))`でフィールドの実体を`PriorityOrderedHashSet`へ差し替える方式。フィールド名・型・コンストラクタシグネチャが1.18.2/1.20.1で完全一致することを確認したため、**この方式は1.18.2でもそのまま機能する**。 |
| 変更要否 | **変更不要**。 |

### 2-3. core.CraftingCpuLogicMixin

| 項目 | 内容 |
|---|---|
| 対象クラス | `appeng.crafting.execution.CraftingCpuLogic`(`public class`) |
| 対象メンバー | `final CraftingCPUCluster cluster;`(package-private、1.18.2/1.20.1とも同一)。`public ICraftingSubmitResult trySubmitJob(IGrid grid, ICraftingPlan plan, IActionSource src, @Nullable ICraftingRequester requester)`(シグネチャ完全一致)。`private void finishJob(boolean success)`(private、1.18.2/1.20.1とも同一)。 |
| 改名の影響 | `ICraftingService`インターフェースの`trySubmitJob`/`submitJob`抽象・default逆転を確認したが、**本Mixinは`CraftingCpuLogic#trySubmitJob`(インターフェースではなくクラス自身のメソッド)に`@Inject`しているだけで、`ICraftingService`型経由の呼び出しには一切触れていないため無関係**。 |
| 変更要否 | **変更不要**。 |

### 2-4. craft.CraftConfirmMenuMixin

| 項目 | 内容 |
|---|---|
| 対象クラス | `appeng.menu.me.crafting.CraftConfirmMenu`(`public class extends AEBaseMenu implements ISubMenu`) |
| 対象メンバー | `public boolean autoStart`(フィールド、両バージョン一致)。`public void startJob()`(1.18.2で260行目、1.20.1で256行目、シグネチャ一致)。`public void goBack()`(1.18.2で365行目、1.20.1で361行目、シグネチャ一致)。 |
| goBack内の`CraftAmountMenu.open`呼び出し | `CraftAmountMenu.open(serverPlayer, getLocator(), whatToCraft, amount)` — 1.18.2/1.20.1で完全に同一の呼び出し(4引数、型も同一)。現行Mixinの`@Redirect`ターゲットはこの4引数版そのものであり**変更不要**。 |
| startJob内の`trySubmitJob`/`submitJob`呼び分け | 1.18.2は`cc.trySubmitJob(...)`、1.20.1は`cc.submitJob(...)`と呼び方が異なることを確認したが、**本Mixinは`startJob()`の`@At("HEAD")`/`@At("RETURN")`にInjectしているだけで、メソッド内部の`trySubmitJob`/`submitJob`呼び出しには一切触れていないため無関係**。 |
| 変更要否 | **変更不要**。 |

### 2-5. core.CraftingBlockEntityMixin

| 項目 | 内容 |
|---|---|
| 対象クラス | `appeng.blockentity.crafting.CraftingBlockEntity`(`public class extends AENetworkBlockEntity`) |
| 対象メンバー | `public CraftingCPUCluster getCluster()`(1.18.2で203行目、1.20.1で204行目、同一)。`public AbstractCraftingUnitBlock<?> getUnitBlock()`(両バージョン同一)。 |
| 実装インターフェース | `appeng.helpers.IPriorityHost`(`extends ISubMenuHost`、`getPriority()`/`setPriority(int)`)。`appeng.api.storage.ISubMenuHost`の`returnToMainMenu(Player, ISubMenu)`/`getMainMenuIcon()`。いずれも1.18.2/1.20.1でシグネチャ完全一致を確認。 |
| 変更要否 | **変更不要**。 |

### 2-6. craft.CraftAmountMenuMixin(唯一の実質的な変更箇所・実装済み)

| 項目 | 内容 |
|---|---|
| 対象クラス | `appeng.menu.me.crafting.CraftAmountMenu` |
| **確定した差異** | `confirm`メソッドのシグネチャが**1.18.2: `public void confirm(int amount, boolean autoStart)`(2引数) / 1.20.1: `public void confirm(int amount, boolean craftMissingAmount, boolean autoStart)`(3引数)**。1.18.2には`craftMissingAmount`関連の分岐(既存在庫を差し引く処理、amount<=0時に前画面へ戻る処理)が存在せず、`confirm`の実装自体がより単純。 |
| `MenuOpener.open`呼び出し | 1.18.2の`confirm`内: `MenuOpener.open(CraftConfirmMenu.TYPE, player, locator)`(戻り値なしで呼び捨て、3引数)。`MenuOpener.open(MenuType<?>, Player, MenuLocator)`は`public static boolean`で両バージョン一致。 |
| `whatToCraft`フィールド | `private AEKey whatToCraft;`(1.18.2で66行目、1.20.1で63行目、型・名前とも同一)。 |
| **実施した変更** | `@Redirect`のRedirectメソッド本体の引数リストから`boolean craftMissingAmount`を削除済み: `private boolean ae2cp$openPriorityStep(MenuType<?> type, Player player, MenuLocator locator, int amount, boolean autoStart)`(6引数→5引数)。 |
| 変更要否 | **変更済み**(本プロジェクトへのコピー時に適用済み)。ビルド時に曖昧性エラーが出た場合は`@Desc`でシグネチャを明示する。 |

### 2-7. core.CraftingCPUMenuMixin

| 項目 | 内容 |
|---|---|
| 対象クラス | `appeng.menu.me.crafting.CraftingCPUMenu`(`public class extends AEBaseMenu`) |
| 対象メンバー | `IGrid getGrid()`(package-private、1.18.2で170行目、1.20.1で168行目、同一)。コンストラクタ`CraftingCPUMenu(MenuType<?> menuType, int id, Inventory ip, Object te)`(両バージョン完全一致)。 |
| 変更要否 | **変更不要**。 |

### 2-8. status.CraftingStatusMenuMixin

| 項目 | 内容 |
|---|---|
| 対象クラス | `appeng.menu.me.crafting.CraftingStatusMenu`(`public class extends CraftingCPUMenu implements ISubMenu`) |
| 対象メンバー | コンストラクタ`CraftingStatusMenu(int id, Inventory ip, ITerminalHost host)`(両バージョン完全一致)。`protected void setCPU(ICraftingCPU c)`(両バージョン完全一致)。`private CraftingCpuList createCpuList()`(両バージョン完全一致、内部で`cpu.getName()`を呼ぶ構造も同一)。`private int getOrAssignCpuSerial(ICraftingCPU cpu)`(両バージョン完全一致)。 |
| `@Redirect`対象 | `ICraftingCPU#getName()`(`appeng.api.networking.crafting`パッケージの公開API、シグネチャ`Component getName()`で両バージョン一致)。`createCpuList()`内の呼び出し位置も同一。 |
| `CraftingCpuListEntry`のフィールド構成差異について | v1が懸念していた`totalItems`+`progress`(long) vs `progress`(float)の差異を確認したが、**現行実装はこのrecordに一切触れていない**ため無関係。 |
| broadcastChanges相当のトリガーについて | 現行の`ae2craftpriority.mixins.json`には`ContainerMenuMixin`は存在せず、`CraftingStatusMenuMixin`の`@Redirect`は「AE2が既に`createCpuList()`を呼ぶタイミング(=`broadcastChanges`経由を含む)」に相乗りしているだけで、専用のトリガーMixinは不要という設計。**DEVELOPMENT.md本文には「12. ContainerMenuMixin」の説明が残っているが、実際の`ae2craftpriority.mixins.json`には登録されていない(ドキュメントの記載漏れ)。**この点はZ:\側のDEVELOPMENT.mdの修正が別途望ましいが、1.18.2移植そのものへの影響はない。 |
| 変更要否 | **変更不要**。 |

### 2-9. status.CraftingCPUScreenMixin

| 項目 | 内容 |
|---|---|
| 対象クラス | `appeng.client.gui.me.crafting.CraftingCPUScreen<T extends CraftingCPUMenu>`(`public class extends AEBaseScreen<T>`) |
| 対象メンバー | `private final Button cancel;`(両バージョン完全一致)。コンストラクタ`CraftingCPUScreen(T menu, Inventory playerInventory, Component title, ScreenStyle style)`(両バージョン完全一致)。`protected void updateBeforeRender()`(両バージョン存在、シグネチャ一致)。 |
| GuiGraphics/PoseStack刷新の影響 | AE2自身の`CraftingCPUScreen`本体は1.18.2で`PoseStack`ベースのdraw系メソッドを使っているが、**本MixinはdrawFG/render等の描画メソッドを一切オーバーライドしておらず**、`addRenderableWidget`でIconButtonを追加し`updateBeforeRender`内で`setX`/`setY`するだけのため無関係。 |
| `IconButton`について | `appeng.client.gui.widgets.IconButton`のコンストラクタ`IconButton(OnPress onPress)`、抽象メソッド`protected abstract Icon getIcon()`は両バージョンで完全一致(1.18.2はAE2内部実装がPoseStackベースだが公開コンストラクタ・抽象メソッドのシグネチャに変更なし)。 |
| 変更要否 | **変更不要**。 |

### 2-10. status.PriorityScreenMixin

| 項目 | 内容 |
|---|---|
| 対象クラス | `appeng.client.gui.implementations.PriorityScreen`(`public class extends AEBaseScreen<PriorityMenu>`) |
| 対象メンバー | コンストラクタ`PriorityScreen(PriorityMenu menu, Inventory playerInventory, Component title, ScreenStyle style)`(両バージョン完全一致)。 |
| `WidgetContainerAccessor`/`TabButtonAccessor`経由のアクセス | `WidgetContainer.widgets`(`Map<String, AbstractWidget>`)、`TabButton.icon`(`Icon`)、`TabButton.item`(`ItemStack`)いずれも1.18.2に同名・同型で存在(2-11, 2-12参照)。 |
| 変更要否 | **変更不要**。 |

### 2-11. mixin.accessor.TabButtonAccessor

| 項目 | 内容 |
|---|---|
| 対象クラス | `appeng.client.gui.widgets.TabButton`(`public class extends Button implements ITooltip`) |
| 対象フィールド | `private Icon icon = null;`(1.18.2で37行目、1.20.1で36行目)。`private ItemStack item;`(1.18.2で38行目、1.20.1で37行目)。フィールド名・型とも完全一致。 |
| 補足 | 1.18.2の`TabButton`は`private final ItemRenderer itemRenderer;`という追加フィールドを持つ(1.20.1では削除)が、本Accessorはこのフィールドを対象にしていないため無関係。 |
| 変更要否 | **変更不要**。 |

### 2-12. mixin.accessor.WidgetContainerAccessor

| 項目 | 内容 |
|---|---|
| 対象クラス | `appeng.client.gui.WidgetContainer`(`public class`) |
| 対象フィールド | `private final Map<String, AbstractWidget> widgets = new LinkedHashMap<>();`(1.18.2で64行目、1.20.1で63行目)。フィールド名・型とも完全一致。 |
| 変更要否 | **変更不要**。 |

## 3. api/・priority/・menu/・client/ の移植方針

これらはAE2の内部実装ではなく、以下のAE2公開APIのみに依存する純粋なMOD側コード。全て1.18.2/1.20.1でシグネチャ一致を確認済みのため、**パッケージ宣言以外は変更不要**。

| クラス | 依存するAE2 API | 1.18.2確認結果 |
|---|---|---|
| `menu.CraftPriorityStepMenu` | `appeng.menu.implementations.MenuTypeBuilder`(`create`/`withInitialData`)、`appeng.api.storage.ISubMenuHost`、`appeng.api.networking.crafting.CalculationStrategy`、`appeng.menu.me.crafting.CraftConfirmMenu` | 全クラス存在・シグネチャ一致 |
| `client.ClientSetup` | `appeng.init.client.InitScreens#register(MenuType<M>, ...)` | シグネチャ一致(1.18.2で193行目、1.20.1で190行目) |
| `client.CraftPriorityStepScreen` | `appeng.client.gui.AEBaseScreen`、`appeng.client.gui.NumberEntryType`、`appeng.client.gui.implementations.AESubScreen#addBackButton`、`appeng.client.gui.widgets.NumberEntryWidget` | 全クラス存在・公開API一致(`NumberEntryWidget`内部実装はGuiComponent継承の有無で差異あるが公開メソッドは同一) |
| `mixin.status.CraftingCPUScreenMixin`(再掲) | `appeng.core.localization.GuiText`(`Priority`/`CraftingStatus`/`CPUs`)、`appeng.client.gui.Icon`(`WRENCH`/`CRAFT_HAMMER`)、`appeng.core.sync.network.NetworkHandler`、`appeng.core.sync.packets.SwitchGuisPacket`、`appeng.menu.implementations.PriorityMenu` | 全て1.18.2に同名で存在確認済み |

## 4. `compat/advancedae/` の扱い(除外)

AdvancedAE Mod(modId: `advanced_ae`)は、Modrinth上でMinecraft 1.20.1・1.21.1向けのビルドのみ配布されており、1.18.2版は存在しない(2026-07-20時点で確認)。したがって:

- `compat/advancedae/`配下の9ファイルは1.18.2版プロジェクトにコピーしない(実施済み)。
- `AE2CraftPriorityMod`コンストラクタから`AdvancedAeCompat`関連の呼び出しを削除する(実施済み)。
- `build.gradle`から`compileOnly fg.deobf("maven.modrinth:advancedae:...")`と`config "ae2craftpriority-advancedae.mixins.json"`を削除する(実施済み)。
- `gradle.properties`から`advancedae_version`を削除する(実施済み)。
- `mods.toml`から`advanced_ae`の任意依存宣言を削除する(実施済み)。
- `ae2craftpriority-advancedae.mixins.json`はコピーしない(実施済み)。

## 5. ツールチェイン設定

| 設定項目 | 1.20.1版(現行) | 1.18.2版(適用済み) | 根拠 |
|---|---|---|---|
| `minecraft_version` | `1.20.1` | `1.18.2` | |
| `minecraft_version_range` | `[1.20.1,1.20.2)` | `[1.18.2,1.19.0)` | |
| `forge_version` | `47.4.6` | `40.1.60`(AE2 1.18.2ブランチのgradle.propertiesが要求する最小値。実際の導入Forgeビルドに合わせて更新可) | AE2公式`forge/1.18.2`ブランチの`gradle.properties`実測 |
| `forge_version_range` | `[47,)` | `[40.1.60,41.0.0)` | |
| `mapping_channel`/`mapping_version` | `official`/`1.20.1` | `official`/`1.18.2` | |
| ForgeGradleプラグイン | `[6.0,6.2)` | `[5.1,5.2)` | AE2公式`forge/1.18.2`ブランチの`build.gradle`実測(`ForgeGradle` `5.1.+`) |
| `mixin_version` | `0.8.5` | `0.8.5`のまま維持(AE2本体は`0.8.4`だが上位互換) | |
| `mixingradle_version` | `0.7-SNAPSHOT` | 変更なし | |
| Java言語バージョン | 17 | 17(変更なし) | |
| AE2依存関係 | `maven.modrinth:ae2:15.4.10` | `maven.modrinth:ae2:11.7.6`(Modrinthで1.18.2 Forge向け配布を確認済み。実装時に最新パッチが出ていないか再確認すること) | Modrinth実配布確認済み |
| AdvancedAE依存関係 | `maven.modrinth:advancedae:1.3.3-1.20.1` | **削除**(1.18.2版が存在しないため) | Modrinth確認済み |

すべて`gradle.properties`/`build.gradle`/`mods.toml`/`AE2CraftPriorityMod.java`に適用済み。

## 6. 推奨実装順序

1. `AE2CraftPriority-1.18.2`プロジェクトを作成し、`compat/advancedae/`を除く現行ソース一式をコピーする。(**完了**)
2. `gradle.properties`/`build.gradle`/`mods.toml`/`AE2CraftPriorityMod.java`を5章・4章に従い1.18.2向けに変更する。(**完了**)
3. `CraftAmountMenuMixin`の2引数対応を適用する。(**完了**、2-6参照)
4. `git init`してプロジェクトの初期状態をコミットする。
5. まず`./gradlew compileJava`が通ること(Mixin適用前)を確認する。AE2 11.7.6のMaven解決に失敗する場合はCurseForge Maven等の代替経路を検討する。
6. `runClient`でMixin適用まで含めた起動確認を行う(Mixin適用エラーが出た場合、本設計書2章の該当項目を再確認する)。
7. 実機動作確認(7章のチェックリスト)を実施する。

各ステップ完了時点でクライアント起動(`runClient`)まで通すことを推奨。ソースコード自体はほぼ無変更(1ファイルの引数1個削除のみ)のため、本移植の主な作業はツールチェイン設定と依存解決の確認に比重が置かれる見込み。

## 7. 実機動作確認チェックリスト(1.18.2向け)

### 7.1 起動確認
- [ ] `runClient`でクラッシュせず起動する(`Mixin apply failed`/`MixinApplyError`が出ていないか確認)。
- [ ] `runServer`でも同様にクラッシュしないことを確認する。
- [ ] AE2本体の基本機能(ネットワーク構築、Pattern Providerでのクラフト)が本MOD導入後も正常に動作する。

### 7.2 優先度スケジューリング(core.CraftingCPUClusterMixin・CraftingServiceMixin・CraftingCpuLogicMixin)
- [ ] 複数のCrafting CPUクラスタで優先度の異なるジョブを同時投入し、優先度の高いジョブが優先的にPattern Providerを確保できることを確認する。
- [ ] 優先度設定をワールド再読み込み後も保持している(NBT保存/読み込み)。
- [ ] ジョブ完了時に優先度がデフォルト(0)へリセットされる。

### 7.3 クラフト確認画面(craft.CraftConfirmMenuMixin・status.PriorityScreenMixin・craft.CraftAmountMenuMixin)
- [ ] README「方法1」のフロー(個数設定→優先度設定→CPU選択・開始)が1.18.2でも同じ順序で遷移する。
- [ ] `CraftAmountMenuMixin`の2引数版`confirm`への対応が正しく動作する(2-6の変更箇所の重点確認)。
- [ ] クラフト確認画面に優先度入力UI(-/+ボタン等)が正しい位置に表示される。
- [ ] `goBack()`で優先度設定画面へ正しく戻り、優先度の値が引き継がれる。

### 7.4 Crafting Statusメニュー(status.CraftingStatusMenuMixin・core.CraftingCPUMenuMixin・status.CraftingCPUScreenMixin)
- [ ] 端末の「Crafting Status」タブでCPU一覧に優先度(`@N`)が表示される。
- [ ] Crafting CPUブロックを右クリックして開く個別画面でも優先度ボタンが正しく機能する。
- [ ] 「戻る」タブのアイコン・ラベル上書き(`PriorityScreenMixin`)が正しく機能する。

### 7.5 その他
- [ ] AdvancedAE関連コードが一切残っていないこと(build.gradle・mods.toml・ソースコードを再確認)。
- [ ] デバッグ用ログ(`AE2CraftPriority: ...`接頭辞)を整理する。

## 8. 既知のリスク・不確実性のまとめ

| # | リスク | 影響度 | 対応方針 |
|---|---|---|---|
| 1 | `CraftAmountMenuMixin`の2引数化(2-6)が実装時に想定と異なるMixinエラーを起こす可能性 | 中 | 最初にビルド・起動確認を行い、`method = "confirm"`だけで曖昧性エラーが出た場合は`@Desc`でディスクリプタを明示する。 |
| 2 | AE2 11.7.6以降で1.18.2向けにさらにパッチが出ている可能性(実装時点で要再確認) | 低 | 実装着手時にModrinthで最新の1.18.2向けビルドを再確認する。 |
| 3 | DEVELOPMENT.md本文に残る「12. ContainerMenuMixin」の説明が実装(mixins.json)と食い違っている(Z:\側の既存プロジェクトのドキュメント不整合) | 低(1.18.2移植自体には影響しないが、Z:\本体側のドキュメント修正が別途望ましい) | 本タスクの範囲外。気づいた点としてユーザーに報告する。 |
| 4 | AE2 1.18.2ブランチと11.7.6ビルドの間に本設計書で洗い出せていない差異が残っている可能性(ブランチHEADと実配布ビルドのズレ) | 低 | 実装時に実際に導入する11.7.6のソース(逆コンパイルまたは公式ソースjar)で最終確認する。 |

---
以上。
