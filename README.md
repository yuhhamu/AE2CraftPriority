# AE2 Crafting Priority ─ 1.12.2版

AE2(Applied Energistics 2)の自動クラフトに、ジョブ単位の実行優先度を追加するアドオンMOD。
1.20.1版(`AE2CraftPriority`)の Minecraft 1.12.2 / Forge 移植。

## 現在の状態(2026-07-22時点)

**プロジェクト骨格のみ。実装は未着手。**

- 設計書: [`design/PORT-DESIGN-1.12.2.md`](design/PORT-DESIGN-1.12.2.md)
  - 3〜10章: 2026-07-20作成の初版設計(12個のMixin対応表、API早見表、ツールチェイン設定等)
  - 11章: 2026-07-22の追加調査。1.20.1版addon実ソース・1.12.2版AE2本体実ソースを直接確認し、
    低信頼度だった項目(3.5/3.9/3.10/3.11/3.12/3.13)を確定情報に更新。
    優先度UIには「経路A(クラフトフロー内の新規GUI)」「経路B(`IPriorityHost`再利用)」の
    2系統があることが判明している。**3〜10章と11章で矛盾する記述がある場合は11章を優先すること。**

## 次にやること(design/PORT-DESIGN-1.12.2.md 8章 + 11-4節)

1. **Mixin導入のPoC**: Forge1.12.2にはMixinの標準統合が無い。MixinBooter等の共有ライブラリMOD経由か、
   自前coremod(`IFMLLoadingPlugin`)かを検証し、「1行ログを出すだけのMixin」が正しくロード・適用される
   ことを確認する。**この検証が済むまで、`build.gradle`のmixin関連設定・`gradle.properties`の
   `ae2_version`(TODO)は動作未確認。**
2. AE2 1.12.2(rv6)本体の配布形態・正確なMaven座標を確認する(`gradle.properties`の`ae2_version=rv6-stable-TODO`を確定させる)。
3. 経路B(`TileCraftingTileMixin`が`IPriorityHost`を実装)から着手するのが依存が少なく検証しやすい
   (詳細は設計書11-1・11-4節)。

## 開発

1.20.1版と同じくAIアシスタントClaudeを活用して開発している。

問題を報告: https://github.com/yuhhamu/AE2CraftPriority/issues
