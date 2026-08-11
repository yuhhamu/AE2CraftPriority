# AE2 Crafting Priority (Fabric 1.20.1)

> This mod was developed with the help of Anthropic's AI assistant "Claude".

## What does this mod do?

An addon mod that adds per-job **"priority"** to Applied Energistics 2 (AE2)'s auto-crafting. This is a forward-port (Beta) of the Forge/1.20.1 version.

In AE2's auto-crafting, multiple crafting jobs can end up competing for the same machine (a compressor, furnace, etc. connected via a Pattern Provider). This mod lets you set a priority per job, so when a machine frees up, the higher-priority job gets to use it first. It never interrupts a machine that's already processing something, and this is a best-effort, network-wide adjustment rather than a strict guarantee.

## Requirements

- Minecraft: 1.20.1
- ModLoader: Fabric Loader 0.19.3+
- Dependencies: Fabric API 0.88.1+1.20.1 (pinned, required — newer builds crash on startup with AE2 15.4.10), Team Reborn Energy API 3.0.0+ (required), Applied Energistics 2 for Fabric 1.20.1, 15.x series (required — tested with 15.4.10, the "AE2 15.4.10 [FABRIC]" build on Modrinth)

## Installation

1. Install Fabric Loader.
2. Place the dependency mods above into your `mods` folder.
3. Place this mod's jar file into your `mods` folder.

## Known Limitations

- "Complete jobs in priority order as much as possible" is best-effort; completion order may not exactly follow priority in every situation.
- Priority is only visible when multiple crafting jobs are competing for the same machine (Pattern Provider).
- Fabric API is pinned to 0.88.1+1.20.1; a newer build combined with AE2 15.4.10 will crash at startup.
- AdvancedAE is not supported, as it has no Fabric build.
- This is a beta-stage forward-port; human verification of priority ordering under real crafting contention is still ongoing.

## Credits

- Developed by yuuhamu

## License

This mod is licensed under MIT. See [LICENSE](https://github.com/yuhhamu/AE2CraftPriority/blob/main/LICENSE).

Copyright (c) 2026 yuuhamu

---

# AE2 Crafting Priority (Fabric 1.20.1)

> 本MODの開発にはAnthropicのAIアシスタント「Claude」を活用しています。

## これは何をするMODですか?

AE2(Applied Energistics 2)の自動クラフトに、ジョブごとの「優先度」を追加するアドオンMODです。Forge/1.20.1版からのフォワードポート(Beta)です。

AE2の自動クラフトでは、複数のクラフトジョブが同じ機械(圧縮機・溶鉱炉など、Pattern Providerを経由して繋がった実機械)を取り合うことがあります。本MODを導入すると、ジョブごとに優先度を設定でき、機械が空いたときに優先度の高いジョブから先にその機械を使えるようになります。処理中の機械から横取りすることはなく、あくまでネットワーク全体に対するベストエフォートの調整です。

## 必要なもの

- Minecraft: 1.20.1
- ModLoader: Fabric Loader 0.19.3以降
- 依存Mod: Fabric API 0.88.1+1.20.1(固定・必須。これより新しいバージョンはAE2 15.4.10との組み合わせで起動時にクラッシュします)、Team Reborn Energy API 3.0.0以降(必須)、Applied Energistics 2 for Fabric、Minecraft 1.20.1版・15.x系列(必須。15.4.10で動作確認、Modrinthでは「AE2 15.4.10 [FABRIC]」表記のビルド)

## 導入方法

1. Fabric Loaderを導入する。
2. 上記の依存Modを`mods`フォルダに配置する。
3. 本MODの jar ファイルを`mods`フォルダに配置する。

## 既知の制限

- 「できるだけ優先度順に完了させる」はベストエフォートです。状況によっては優先度通りにならないことがあります。
- 優先度の効果は、複数のクラフトジョブが同じ機械(Pattern Provider)を取り合っている場合のみ目に見える形で現れます。
- Fabric APIは0.88.1+1.20.1に固定しています。AE2 15.4.10と組み合わせてこれより新しいバージョンを使うと起動時にクラッシュします。
- AdvancedAEはFabric版が存在しないため非対応です。
- 本バージョンはベータ段階のフォワードポートです。実際のクラフトジョブでの優先度制御そのものの人手による検証は継続中です。

## クレジット

- 開発: yuuhamu

## ライセンス

本MODは MIT ライセンスの下で公開されています。詳細は [LICENSE](https://github.com/yuhhamu/AE2CraftPriority/blob/main/LICENSE) を参照してください。

Copyright (c) 2026 yuuhamu
