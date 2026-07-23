# AE2 Crafting Priority

> This mod's development leverages Anthropic's AI assistant, Claude.
> 本MODの開発にはAnthropicのAIアシスタント「Claude」を活用しています。

An addon mod for Applied Energistics 2 (AE2) that adds crafting **priority** to its auto-crafting system.
AE2(Applied Energistics 2)の自動クラフトに「優先度」を追加するアドオンMODです。

## Supported Versions / 対応バージョン

This branch is the **Minecraft 1.12.2** port, currently in development. It is **not yet buildable or playable** — see "Current Status" below. See other versions below.

このブランチは **Minecraft 1.12.2** 向けの移植版で、現在開発中です。**まだビルド・プレイはできません**(詳細は下記「現在の状態」参照)。他のバージョンは以下を参照してください。

| Version | Support |
|---|---|
| 1.20.1 | 🟢 Full Support |
| 1.18.2 | 🟡 Bug Fix Only |
| 1.16.5 | 🟡 Bug Fix Only |
| 1.12.2 | 🚧 In Development (this branch) |

See the README on the repository's top (`main` branch) for details on the support policy, what this mod does, installation, and usage — they will be the same for 1.12.2 once released.

サポートポリシー・MODの機能説明・導入方法・使い方の詳細はリポジトリのトップ(mainブランチ)の
READMEを参照してください(1.12.2版がリリースされた際も、これらは同じ内容になる予定です)。

## Current Status (as of 2026-07-23) / 現在の状態(2026-07-23時点)

**The Mixin bootstrap PoC has been built and launch-tested successfully** (1.16.5 development
wrapped up, so the build/test tooling switched over to this branch). `gradlew build` and an
actual PrismLauncher launch both succeeded; `core.PocBootstrapLogMixin`'s log line was confirmed
in `latest.log`, and Forge reported "Forge Mod Loader has successfully loaded 11 mods" with no
crash. Fixed several legacy ForgeGradle 2.3 / Gradle 4.8 build issues found along the way (see
below) and confirmed that MixinBooter must be deployed as its own jar in the instance's `mods/`
folder (a compile-time dependency alone is not picked up as a coremod).

**Mixinブートストラップ PoC のビルド・実機起動テストに成功しました**(1.16.5の開発が終了した
ため、ビルド・テスト環境をこのブランチに切り替え)。`gradlew build`・実際のPrismLauncher起動の
両方が成功し、`latest.log`で`core.PocBootstrapLogMixin`のログ出力を確認、Forgeも
「Forge Mod Loader has successfully loaded 11 mods」とクラッシュなしで報告。途中で見つかった
旧ForgeGradle 2.3 / Gradle 4.8特有のビルド不具合を複数修正した(下記参照)ほか、MixinBooterは
コンパイル時依存として宣言するだけでは不十分で、実機のインスタンスの`mods/`フォルダに
jarとして別途配置する必要があることを確認した(コアmodとして認識されないため)。

- Design document: [`design/PORT-DESIGN-1.12.2.md`](design/PORT-DESIGN-1.12.2.md)
  - Sections 3–10: initial design written 2026-07-20.
  - Section 11: additional research from 2026-07-22 — splitting the priority-UI navigation into
    routes A/B, confirmed against the actual AE2 1.12.2 source.
  - Section 12: GUI design revisited on 2026-07-23. Cross-checked
    `Knowledge/ae2-priorityscreen-gui-design-reference.md` against the real AE2 1.12.2 source
    (`/tmp/ae2-1122`, AE2's `rv6-1.12` branch), which corrected some of section 11's conclusions:
    the back-navigation fix needs a new `PacketSwitchGuisMixin`, while the back button's icon can
    just use `GuiTabButton`'s built-in `ItemStack` icon support — no low-level draw hack needed.
- `gradle.properties` / `build.gradle`: configured with AE2 1.12.2's dependency coordinates
  (via CurseForge/CurseMaven, `appliedenergistics2-rv6-stable-7.jar`, confirmed on CurseForge on
  2026-07-22) and MixinBooter 11.1 (the Mixin runtime for Forge 1.12.2, the latest version
  supporting 1.12.2 as of 2026-07-22).
- `ae2craftpriority.mixins.json`: **currently registers only the PoC mixin, `core.PocBootstrapLogMixin`.**
  As of design section 12.4, the planned real-implementation mixins are (to be added one at a time
  once the PoC is confirmed working, per the recommended order in design section 8):
  - Ported as-is (9): see design section 3.
  - New (2): `TileCraftingTileMixin` (route B, implements `IPriorityHost`, design section 11.1.3)
    and `PacketSwitchGuisMixin` (route B, back-navigation bypass, design section 12.1.1).
  - Confirmed unnecessary (2): equivalents of `TabButtonAccessor` / `WidgetContainerAccessor`
    (design section 12.2 — not needed because `GuiTabButton` already supports `ItemStack` icons
    natively).

- 設計書: [`design/PORT-DESIGN-1.12.2.md`](design/PORT-DESIGN-1.12.2.md)
  - 3〜10章: 2026-07-20作成の初版設計
  - 11章: 2026-07-22の追加調査。優先度UIの経路A/B分離、実ソース確認結果
  - 12章: 2026-07-23のGUI設計見直し。`Knowledge/ae2-priorityscreen-gui-design-reference.md`を
    実ソース(`/tmp/ae2-1122`、AE2 `rv6-1.12`ブランチ)で裏取りし、11章の一部結論を訂正。
    戻り導線(バック機能)には`PacketSwitchGuisMixin`(新規)が必要と判明した一方、
    戻るボタンのアイコン差し替えは`GuiTabButton`のItemStackアイコン標準サポートにより
    低レベル描画ハック不要と確定。
- `gradle.properties` / `build.gradle`: AE2 1.12.2本体の依存座標(CurseForge/CurseMaven、
  `appliedenergistics2-rv6-stable-7.jar`、2026-07-22にCurseForgeで確認済み)、
  MixinBooter 11.1(Forge1.12.2向けMixinランタイム、2026-07-22時点の最新1.12.2対応版)を設定済み。
- `ae2craftpriority.mixins.json`: **現時点ではPoC用の`core.PocBootstrapLogMixin`のみを登録**。
  design 12.4節時点で確定している本実装Mixinは以下の構成(PoCで土台が動くことを確認してから
  1個ずつ追加していく、design 8章の推奨順序参照):
  - そのまま移植(9個): design 3章参照
  - 新規(2個): `TileCraftingTileMixin`(経路B、`IPriorityHost`実装、design 11章1.3節)、
    `PacketSwitchGuisMixin`(経路B、戻り導線バイパス、design 12.1.1節)
  - Mixin不要と確定(2個): `TabButtonAccessor`・`WidgetContainerAccessor`相当
    (design 12.2節、`GuiTabButton`のItemStackアイコン標準サポートのため)

## Unverified / To-Do Items / 未検証・要確認のTODO

- ~~The Mixin bootstrap PoC itself has not been verified yet~~ **Verified 2026-07-23**: built and
  launch-tested successfully via mod-test-runner (profile `craftpriority112`). See "Current
  Status" above and the build-fix notes below.
- Build fixes discovered while getting the PoC to build/launch (2026-07-23, all applied in this
  commit):
  - The project was missing `gradlew` / `gradlew.bat` / `gradle/wrapper/gradle-wrapper.jar`
    (only `gradle-wrapper.properties` existed) — without them, mod-test-runner silently fell back
    to whatever Gradle happened to be cached globally (6.8.3), which is incompatible with
    ForgeGradle 2.3. Regenerated the wrapper scripts/jar (version-agnostic bootstrap; the target
    Gradle 4.8 comes from `gradle-wrapper.properties`, unchanged).
  - `mixingradle_version` must be `0.6-SNAPSHOT`, not `0.7-SNAPSHOT` — 0.7-SNAPSHOT explicitly
    refuses to apply on projects with a `genSrgs` task (i.e. any ForgeGradle 2.x project).
  - `dependencies { minecraft "net.minecraftforge:forge:..." }` doesn't exist in ForgeGradle 2.x —
    that's ForgeGradle 3+ syntax. The `minecraft { version = "..." }` extension block alone is
    sufficient (confirmed by cross-checking AE2's own 1.12.2 `build.gradle`).
  - The `content { includeGroup '...' }` repository content-filtering DSL doesn't exist in
    Gradle 4.8 (added in a much later Gradle version) — removed from the CurseMaven repository
    block.
  - MixinBooter must be deployed as a standalone jar inside the target instance's `mods/` folder.
    Declaring it as a `compile` dependency only puts it on the runtime classpath — Forge's coremod
    discovery scans actual files under `mods/`, so without a physical jar there, no coremod (and
    therefore no Mixin) gets loaded at all, even though the build succeeds and the game boots
    without error.
- It's unconfirmed whether MixinBooter needs to be declared as a Forge mod dependency in
  `mcmod.info` (it hasn't been added there yet).
- The exact field layout for the synthetic `IPriorityHost` implementation (for CPU crafting jobs)
  hasn't been finalized (design section 12.5, item 6).
- Whether the back button's icon should use a newly registered dedicated item or reuse an existing
  one hasn't been decided (design section 12.5, item 7).

- ~~Mixin導入のPoC自体が未検証~~ **2026-07-23検証済み**: mod-test-runner(プロファイル
  `craftpriority112`)経由でビルド・実機起動テストに成功。詳細は上記「現在の状態」および
  下記のビルド不具合修正メモを参照。
- PoCのビルド・起動にこぎつけるまでに見つかったビルド不具合(2026-07-23、いずれもこのコミットで修正済み):
  - プロジェクトに`gradlew`/`gradlew.bat`/`gradle/wrapper/gradle-wrapper.jar`が欠けていた
    (`gradle-wrapper.properties`のみ存在)。これが無いと、mod-test-runnerはグローバルに
    たまたまキャッシュされていたGradle(6.8.3)へ黙ってフォールバックしてしまい、
    ForgeGradle 2.3と非互換のため失敗する。wrapperスクリプト/jarを再生成した
    (このjar自体はバージョン非依存のブートストラップで、実際に使うGradle 4.8は
    `gradle-wrapper.properties`側の設定のまま変更なし)。
  - `mixingradle_version`は`0.7-SNAPSHOT`ではなく`0.6-SNAPSHOT`が必要。0.7-SNAPSHOTは
    `genSrgs`タスク(=ForgeGradle 2.x系プロジェクト全般)を検出すると明示的に適用を拒否する。
  - `dependencies { minecraft "net.minecraftforge:forge:..." }`はForgeGradle 2.xには存在しない
    (ForgeGradle 3+の書式)。`minecraft { version = "..." }`拡張ブロックだけで十分
    (AE2本体の1.12.2版build.gradleでも同様の構成であることを確認済み)。
  - リポジトリのコンテンツフィルタリングDSL`content { includeGroup '...' }`はGradle 4.8には
    存在しない(もっと新しいGradleバージョンで追加された機能)。CurseMavenリポジトリ設定から削除。
  - MixinBooterは対象インスタンスの`mods/`フォルダに単独のjarとして配置する必要がある。
    `compile`依存として宣言するだけではランタイムのクラスパスに乗るだけで、Forgeのコアmod検出は
    `mods/`配下の実ファイルを走査する方式のため、物理的なjarが無いとコアmod(ひいてはMixin)が
    一切ロードされない(ビルドは成功し、ゲームもエラーなく起動してしまうため気づきにくい)。
- MixinBooterのForge modid・mcmod.infoでの依存宣言要否は未確認(現状mcmod.infoには追加していない)。
- 合成`IPriorityHost`実装(CPUクラフトジョブ用)の具体的なフィールド構成が未確定(design 12.5節#6)。
- 戻るボタン用アイコンの専用アイテム新規登録 or 既存アイテム流用の判断が未確定(design 12.5節#7)。

## Next Steps / 次にやること

1. Confirm the PoC builds via `gradlew build` (or the existing Gradle MCP / mod-test-runner setup).
2. Once that passes, follow the priority order in design section 12.5 (detailed design of the
   synthetic `IPriorityHost` implementation, alongside a `GuiBridge` enum PoC) and implement the
   real mixins one at a time, adding each to `mixins.json` as you go, testing and committing along
   the way.

1. `gradlew build`(または既存のGradle MCP/mod-test-runner経由)でPoCが通るか確認する。
2. 通れば、design 12.5節の優先順位(`GuiBridge` enum PoCと並行して合成`IPriorityHost`実装の
   詳細設計)に従い、本実装Mixinを1個ずつ`mixins.json`に追加しながら実装・テスト・コミットしていく。

## Development / 開発

Like the 1.20.1 version, this port is developed with the help of the AI assistant Claude.

1.20.1版と同じくAIアシスタントClaudeを活用して開発している。

## Report an Issue / 問題を報告

https://github.com/yuhhamu/AE2CraftPriority/issues
