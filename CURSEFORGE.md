*This mod's development is assisted by Anthropic's AI assistant "Claude."*

# AE2 Crafting Priority (NeoForge 1.21.1)

🚧 **In Development (alpha)**: This is a forward-port from the Forge/1.20.1 version. It builds and boots successfully, but in-world testing (actual crafting jobs, priority ordering under contention, addon-mod interoperability) is still limited.

An addon mod that adds **"priority"** to Applied Energistics 2 (AE2)'s autocrafting.

In AE2's autocrafting, multiple crafting jobs can end up competing for the same machine
(a compressor, furnace, etc. connected via a Pattern Provider). With this mod, you can set
a "priority" per job, so that when a machine becomes free, the job with the higher priority
gets to use it first.

- It never interrupts a machine that's already processing a job — it only controls which
  job gets priority the next time the machine becomes free
- This is a best-effort, network-wide adjustment, not a strict guarantee

## Key Features

Priority starts at 0 by default, and higher numbers mean higher priority. It's set using
AE2's own "priority" screen (the one with `+1/+16/+32/+64` buttons and a numeric input
field). There are three ways to set it.

- **Set it when starting a craft**: The item crafting request flow changes to
  "set amount → set priority → select CPU / start." The priority you set is applied to
  whichever Crafting CPU actually gets picked when the craft starts
- **Change it from the Crafting CPU screen**: An icon button next to the "Suspend"/"Cancel"
  buttons lets you change that CPU's priority directly
- **Change it from the terminal's Crafting Status tab**: Select a CPU in AE2's Crafting
  Status tab, then use the icon button next to "Suspend"/"Cancel" to change that CPU's priority

Any CPU that's currently active shows its priority next to its name, e.g. `CPU #1@1000`,
and the priority automatically resets to 0 once the crafting task ends.

## Compatible Addon Mods

- **AdvancedAE**: Dedicated compatibility code exists and has been verified at the source
  level against AdvancedAE `1.6.11-1.21.1`. In-world testing is still limited.

Mega Cells, ExtendedAE, and Applied Mekanistics are confirmed compatible on the 1.20.1
version and are expected to work the same way here, but have not yet been verified on 1.21.1.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.169 or later (tested with 21.1.169)
- Applied Energistics 2, 1.21.1 branch, 19.x series (tested with 19.2.17; AE2
  must be installed separately)

## Installation

1. Install NeoForge
2. Put the Applied Energistics 2 jar in your `mods` folder
3. Put this mod's jar in your `mods` folder too
4. Launch Minecraft

## Known Limitations

- "Complete jobs in priority order as much as possible" is best-effort — in some
  situations, completion order may not follow priority exactly
- The effect of priority is only visible when multiple crafting jobs are competing for
  the same machine (Pattern Provider)
- This is an early forward-port (alpha); in-world verification is still limited, only
  successful build and client/server boot have been confirmed so far

## Credits

Author: yuuhamu
Applied Energistics 2 (AE2) is a required separate dependency. This mod does not bundle AE2.

For source code and detailed documentation, see [GitHub](https://github.com/yuhhamu/AE2CraftPriority)
(`neoforge/1.21.1` branch). To report an issue, please use
[Issues](https://github.com/yuhhamu/AE2CraftPriority/issues).

---

*本MODの開発にはAnthropicのAIアシスタント「Claude」を活用しています。*

# AE2 Crafting Priority (NeoForge 1.21.1)

🚧 **開発中(alpha)**: Forge/1.20.1版からのフォワードポートです。ビルド・起動は成功していますが、実際のワールド内でのクラフトジョブ・優先度制御・他アドオンMODとの組み合わせについてはまだ検証が限定的です。

AE2(Applied Energistics 2)の自動クラフトに **「優先度」** を追加するアドオンMODです。

AE2の自動クラフトでは、複数のクラフトジョブが同じ機械(圧縮機・溶鉱炉など、Pattern
Provider経由で繋がった実機械)を取り合うことがあります。このMODを入れると、ジョブごとに
「優先度」を設定でき、機械が空いたときに優先度の高いジョブから先にその機械を使えるように
なります。

- 処理中の機械から横取りすることはありません。あくまで「次に空いたときにどちらを優先するか」を制御します
- ネットワーク全体に対するベストエフォートの調整であり、厳密な保証があるわけではありません

## 主な機能

優先度は初期値0、数値が高いほど優先されます。設定はAE2純正の「優先度」画面
(`+1/+16/+32/+64` ボタンと数値入力欄のある画面)で行います。設定方法は3通りあります。

- **クラフト開始時に設定**: アイテムのクラフト要求フローが「個数設定 → 優先度設定 →
  CPU選択・開始」に変わります。設定した優先度は、開始時に実際に選ばれたCrafting CPUへ
  適用されます
- **Crafting CPU画面から変更**: 「一時停止」「キャンセル」ボタンの隣のアイコンボタンから、
  そのCPUの優先度を直接変更できます
- **クラフト状況タブから変更**: AE2端末のクラフト状況タブでCPUを選択し、「一時停止」
  「キャンセル」ボタンの隣のアイコンボタンから優先度を変更できます

実行中(アクティブ)のCPUには `CPU #1@1000` のように優先度が表示され、クラフトタスクが
終了すると優先度は自動的に0へリセットされます。

## 対応アドオンMOD

- **AdvancedAE**: 本バージョン向けの専用対応コードがあり、AdvancedAE `1.6.11-1.21.1` に
  対してソースレベルでの整合性を確認済みです。実ワールドでの動作確認はまだ限定的です。

Mega Cells・ExtendedAE・Applied Mekanistics は1.20.1版で動作確認済みで、仕組み上は
同様に動作すると見込まれますが、1.21.1ではまだ検証していません。

## 必要環境

- Minecraft 1.21.1
- NeoForge 21.1.169以降(21.1.169で動作確認)
- Applied Energistics 2 for Minecraft 1.21.1(19.x系列、19.2.17で動作確認、AE2は別途導入が必要です)

## 導入方法

1. NeoForgeをインストールする
2. Applied Energistics 2 の jar を `mods` フォルダに入れる
3. 本MODの jar も `mods` フォルダに入れる
4. Minecraftを起動する

## 既知の制限

- 「できるだけ優先度順に完了させる」はベストエフォートです。状況によっては優先度通りに
  ならないことがあります
- 優先度の効果は、複数のクラフトジョブが同じ機械(Pattern Provider)を取り合っている場合のみ
  目に見える形で現れます
- 本バージョンはフォワードポートの初期段階(alpha)です。実ワールドでの検証はまだ限定的で、
  現時点ではビルド成功とクライアント/サーバーの起動確認までです

## クレジット

作者: yuuhamu
Applied Energistics 2 (AE2) は別途必要な依存MODです。このMODはAE2を同梱していません。

ソースコード・詳細なドキュメントは [GitHub](https://github.com/yuhhamu/AE2CraftPriority)
(`neoforge/1.21.1` ブランチ)を参照してください。問題を報告する場合は
[Issues](https://github.com/yuhhamu/AE2CraftPriority/issues) からお願いします。
