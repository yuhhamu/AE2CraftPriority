# AE2 Crafting Priority ─ 1.12.2版

AE2(Applied Energistics 2)の自動クラフトに、ジョブ単位の実行優先度を追加するアドオンMOD。
1.20.1版(`AE2CraftPriority`)の Minecraft 1.12.2 / Forge 移植。

## 現在の状態(2026-07-22時点)

**ビルド直前まで準備完了。実際のビルド・実機起動テストは未実施(1.16.5移植でビルド環境を使用中のため)。**

- 設計書: [`design/PORT-DESIGN-1.12.2.md`](design/PORT-DESIGN-1.12.2.md)
  - 3〜10章: 2026-07-20作成の初版設計
  - 11章: 2026-07-22の追加調査。優先度UIの経路A/B分離、実装不要になった3 Mixin等
- `gradle.properties` / `build.gradle`: AE2 1.12.2本体の依存座標(CurseForge/CurseMaven、
  `appliedenergistics2-rv6-stable-7.jar`、2026-07-22にCurseForgeで確認済み)、
  MixinBooter 11.1(Forge1.12.2向けMixinランタイム、2026-07-22時点の最新1.12.2対応版)を設定済み。
- `ae2craftpriority.mixins.json`: **現時点ではPoC用の`core.PocBootstrapLogMixin`のみを登録**。
  AE2本体のメインクラス`appeng.core.AppEng#preInit()`末尾で1行ログを出すだけの検証用Mixin。
  design 11-3節で確定した9個の本実装Mixinは、このPoCで土台が動くことを確認してから
  1個ずつ追加していく(design 8章の推奨順序参照)。

## 未検証・要確認のTODO

- **Mixin導入のPoC自体が未検証**(design 8章手順1)。`gradlew build` または `runClient` を実行し、
  `core.PocBootstrapLogMixin`のログが起動ログに出るかを確認すること。
- MixinBooterのjarがそのままGradle依存として使える配布形態か(`deobfCompile`が必要か等)は未確認
  (`build.gradle`内のTODOコメント参照)。
- MixinBooterのForge modid・mcmod.infoでの依存宣言要否は未確認(現状mcmod.infoには追加していない)。

## 次にやること

1. `gradlew build`(または既存のGradle MCP/mod-test-runner経由)でPoCが通るか確認する。
2. 通れば、design 11-4節の優先順位(経路Bの`TileCraftingTileMixin`から着手)に従い、
   本実装Mixinを1個ずつ`mixins.json`に追加しながら実装・テスト・コミットしていく。

## 開発

1.20.1版と同じくAIアシスタントClaudeを活用して開発している。

問題を報告: https://github.com/yuhhamu/AE2CraftPriority/issues
