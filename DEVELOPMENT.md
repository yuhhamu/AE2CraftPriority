# AE2 Crafting Priority — 1.18.2 移植 開発記録

## 概要
- 移植元: `AE2CraftPriority` (Minecraft 1.20.1 / Forge 47.4.10 / AE2 15.x)
- 移植先: 本プロジェクト (Minecraft 1.18.2 / Forge 40.1.60 / AE2 forge-11.7.6)
- ビルド環境: Windows側 gradle-mcp-server はネットワーク/ワーカーデーモンのソケット制限によりcompileJava実行不可のため、
  desktop-commander-z-onlyコンテナ(Alpine + 手動インストールJDK17/Gradle7.3)でビルドを実施。
  依存解決キャッシュはコンテナからZ:\Claude\Tools\GradleUserHomeへコピーし、Windows側gradle-mcp-serverにも
  `-g Z:\Claude\Tools\GradleUserHome`で共有(deploy系タスクはWindows側で実行するため)。

## 実施したポーティング修正 (すべて実AE2 forge/1.18.2ブランチソースで検証済み)
1. `build.gradle`: FG5.1のプラグイン宣言を`plugins{}`DSLから`buildscript{}`+`apply plugin:`形式に変更
2. `gradle.properties`: `ae2_version`をModrinthの実際のMaven version_number `forge-11.7.6`に修正(素の`11.7.6`ではNG)
3. `CraftPriorityStepScreen.java`: `Button.builder().build()`→`new Button(...)`コンストラクタ、
   `Component.translatable()`→`new TranslatableComponent()`、`nextButton.getWidth()/getHeight()`は存在するが
   `setX/setY`は無いため`.x`/`.y`フィールド直接代入に変更
4. `CraftingCPUMenuMixin.java`: `player.level().isClientSide()`(メソッド)→`player.level.isClientSide`(フィールド)
5. `CraftingCPUScreenMixin.java`: 同上、ウィジェット座標をフィールド直接代入に変更
6. `PriorityScreenMixin.java`: `Component.translatable()`→`TranslatableComponent`、
   AE2 11.7.6の`Icon`enumに`CRAFT_HAMMER`が存在しないため`VIEW_MODE_CRAFTING`で代替

## ビルド状況
- `compileJava` / `build -x test` ともにBUILD SUCCESSFUL (コンテナ側ビルド)
- 生成物: `build/libs/ae2craftpriority-0.1.0.jar` (38077 bytes)

## 実機起動確認 (進行中)
- mod-test-runnerの`craftpriority118`プロファイルでPrismインスタンス
  `AE2 CraftPriority 1.18.2` (MC1.18.2 / Forge 40.1.60) をZ:\で新規作成し、
  `D:\Program Files\PrismLauncher\instances\`へ配置(既存の動作実績ある1.20.1版instance.cfg/mmc-pack.jsonを雛形に作成)
- AE2本体jar (`appliedenergistics2-forge-11.7.6.jar`, Modrinthより取得) を手動でmods/へ配置する必要があった
  (deployLaunchタスクは自Mod jarのみ配置する仕様のため)
- Mod読み込み・Mixin適用(ae2craftpriority側12個・AE2側20個、計31 mixin)までは正常に完了を確認(mclo.gsログ解析)
- しかし起動時にクラッシュ: LWJGLネイティブライブラリのアーキテクチャ不一致
  ```
  [LWJGL] Platform/architecture mismatch detected for module: org.lwjgl
  JVM platform: Windows amd64 17.0.15
  Platform available on classpath: windows/arm64
  java.lang.UnsatisfiedLinkError: Failed to locate library: lwjgl.dll
  ```
  instance.cfgのJavaArchitecture/JavaRealArchitectureはamd64で正しく、
  mmc-pack.jsonのLWJGLバージョン(3.3.1)も動作実績のある1.20.1版と同一のため、
  Mod側/設定値側の誤りではなく、新規インスタンスの初回ライブラリダウンロード時に
  何らかの理由でarm64向けネイティブが取得された環境側の問題と推測。
  → ユーザー側でPrismの起動構成(インスタンス)を作成し直していただくことになった。

## 次のステップ
1. ユーザー側でPrism起動構成の準備が完了次第、`deployLaunch`で再起動確認
2. 起動確認後、design/所定の推奨実装順序に沿って各機能を実機でテスト
3. 安定後、デバッグ用の`AE2CraftPriority: ...`ログを整理
4. 最終報告 (GitHubへのpushはユーザーの明示的指示があるまで行わない)

## 追記: 起動構成再作成後も同じLWJGLクラッシュが再発 (2026-07-21)
- ユーザーがPrismの起動構成を作成し直し、mods/にAE2本体・JEI・Mekanism等の追加Modも含む形で再構築
- 再度`deployLaunch`→実機確認したが、**全く同じ**エラーで再クラッシュ(mclo.gs: https://mclo.gs/ExnV9hj)
  ```
  [LWJGL] Platform/architecture mismatch detected
  JVM platform: Windows amd64 17.0.15
  Platform available on classpath: windows/arm64
  java.lang.UnsatisfiedLinkError: Failed to locate library: lwjgl.dll
  NoClassDefFoundError: Could not initialize class com.mojang.blaze3d.systems.RenderSystem
  ```
  Mod読み込みは今回も全modが正常(AE2CraftPriority含む)。
- `C:\Users\yuuse\AppData\Roaming\PrismLauncher\libraries\org\lwjgl\`を確認したところ、
  `lwjgl-natives-windows`(amd64用)・`lwjgl-natives-windows-arm64`・`lwjgl-natives-windows-x86`の
  3種類とも存在していた。にもかかわらず実行時にarm64版がクラスパスに載っている。
  → ライブラリファイル自体は揃っており、**Prism Launcher側のアーキテクチャ判定ロジックが
  誤ってarm64を選択している**可能性が高い。instance.cfg/mmc-pack.json側の設定値の問題ではない。
- 仮説: このPCがWindows on ARM環境で、Prismがホスト側CPUアーキテクチャ(ARM64)を見て
  ネイティブを選択しており、実際に使われているJava(java-runtime-gamma, amd64エミュレーション動作)の
  ビット数とズレている可能性がある。動作実績のある1.20.1版instanceが「今も」正常起動するか
  再テストすることで、ライブラリキャッシュ共有部分が壊れたのか、新規instance固有の問題かを切り分け可能。
- Mod/コード側の追加修正は不要と判断。環境(Prism Launcher / Windows側)の切り分け・対応が必要なため、
  ユーザーへ状況報告し指示待ちとする。

## 追記: LWJGLバージョン修正で実機起動成功 (2026-07-21)
- ユーザーがPrism側でLWJGLを3.3.1→3.2.2(1.18.2本来のバージョン)に変更し、起動確認に成功
- 実機テスト開始。バグ報告1件目: 数量設定画面(CraftPriorityStepScreen)で「次へ」ボタンが押せない
  → 調査中

## 追記: バグ1「数量設定画面の次へが押せない」の根本原因と修正 (2026-07-21)
- 実機ログ(latest.log)を確認したところ、クラッシュ等は無く、以下のWARNのみ発見:
  `[Render thread/WARN] [net.minecraft.client.gui.screens.MenuScreens/]: Trying to open invalid screen with name: `
  (name が空)。サーバー側は動いている(CraftAmountMenuMixinのリダイレクトも実際には発火していた)が、
  クライアント側で画面ファクトリが見つからず開けていない、という症状と一致。
- 実AE2 1.18.2ソース(`appeng.menu.implementations.MenuTypeBuilder#build`)を確認したところ、
  `build(id)`は`setRegistryName(id)`を呼ぶのみで、**Forgeレジストリへの実登録(`event.getRegistry().register(...)`)は
  一切行っていない**ことが判明。
- AE2本体は`appeng.core.AppEngBase`で`modEventBus.addGenericListener(MenuType.class, this::registerContainerTypes)`
  → `appeng.init.InitMenuTypes.init(registry)`により、**AE2自身が定義する全MenuTypeを明示的に列挙してregisterAllしている**
  (ハードコードリストで、サードパーティModのMenuTypeは含まれない)。
- つまり `CraftPriorityStepMenu.TYPE` はAE2にもこのMod自身にも一切Forgeレジストリへ登録されておらず、
  サーバー→クライアントの画面オープンパケットでMenuTypeのレジストリIDが解決できず(未登録=ID不明)、
  クライアント側で空名の警告とともに画面が開かなかった。
- **これは1.18.2移植で生じたものではなく、移植元(1.20.1版)ソースにも同様に登録コードが存在しない
  (grep で確認済み)ため、同種の問題が1.20.1側にも潜在している可能性がある。** ただし1.20.1側の修正は
  本タスクの範囲外のため、Z:\側のオリジナルプロジェクトには手を加えていない。ユーザーへ別途報告要。
- **修正**: `AE2CraftPriorityMod.java`のコンストラクタに、AE2本体と同じパターンで
  `modEventBus.addGenericListener(MenuType.class, event -> event.getRegistry().register(CraftPriorityStepMenu.TYPE))`
  を追加し、明示的にForgeレジストリへ登録するよう修正。
- コンテナ側ビルド(`compileJava`/`build -x test`)ともにBUILD SUCCESSFUL。実機で再検証中。
