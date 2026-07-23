# AE2 Crafting Priority

> This mod's development leverages Anthropic's AI assistant, Claude.
> 本MODの開発にはAnthropicのAIアシスタント「Claude」を活用しています。

An addon mod for Applied Energistics 2 (AE2) that adds crafting **priority** to its auto-crafting system.
AE2(Applied Energistics 2)の自動クラフトに「優先度」を追加するアドオンMODです。

## Supported Versions / 対応バージョン

This branch is for **Minecraft 1.18.2**. See below for other versions.

このブランチは **Minecraft 1.18.2** 向けです。他のバージョンは以下を参照してください。

| Version | Support |
|---|---|
| 1.20.1 | 🟢 Full Support |
| 1.18.2 | 🟡 Bug Fix Only |
| 1.16.5 | 🟡 Bug Fix Only |
| 1.12.2 | 🚧 In Development |

See the README on the repository's top (`main` branch) for details on the support policy.

サポートポリシーの詳細はリポジトリのトップ(mainブランチ)のREADMEを参照してください。

## What Does This Mod Do? / これは何をするMODですか?

In AE2's auto-crafting, multiple crafting jobs can end up competing for the same machine (a compressor, furnace, etc. connected via a Pattern Provider). This mod lets you set a **priority** per job, so when a machine frees up, the higher-priority job gets to use it first.

- It never interrupts a machine that's already processing something — it only decides which job goes next once a machine becomes free.
- This is a best-effort, network-wide adjustment, not a strict guarantee.

AE2の自動クラフトでは、複数のクラフトジョブが同じ機械(圧縮機・溶鉱炉など、Pattern Provider経由で繋がった実機械)を取り合うことがあります。このMODを入れると、ジョブごとに「優先度」を設定でき、機械が空いたときに優先度の高いジョブから先にその機械を使えるようになります。

- 処理中の機械から横取りすることはありません。あくまで「次に空いたときにどちらを優先するか」を制御します
- ネットワーク全体に対するベストエフォートの調整であり、厳密な保証があるわけではありません

## Requirements / 必要なもの

- Minecraft 1.18.2
- Minecraft Forge 40.1.60 or later
- Applied Energistics 2 for Minecraft 1.18.2 (tested with forge-11.7.6)

This mod includes code that directly relies on AE2's internal implementation (not its public API). Any AE2 build for Minecraft 1.18.2 shares the same internal structure, so other versions may work too — but only forge-11.7.6 has actually been verified to launch and run correctly. AE2 builds for other Minecraft versions (which use a different internal structure) are not supported.

このMODはAE2の内部実装(公開APIではない部分)を直接利用しているコードを含みます。Minecraft 1.18.2向けのAE2であれば同じ内部構造を使っているため他のバージョンでも動作する可能性がありますが、実際に起動・動作確認できているのはforge-11.7.6のみです。他のMinecraftバージョン向けのAE2(内部構造が異なります)には対応していません。

## Installation / 導入方法

1. Install Forge.
2. Put the Applied Energistics 2 jar in your `mods` folder.
3. Put this mod's (AE2 Crafting Priority) jar in your `mods` folder as well.
4. Launch Minecraft.

1. Forgeをインストールする
2. Applied Energistics 2 の jar を `mods` フォルダに入れる
3. このMOD(AE2 Crafting Priority)の jar も `mods` フォルダに入れる
4. Minecraftを起動する

## Usage / 使い方

Priority starts at 0; the higher the number, the higher the priority. It's set from AE2's own "Priority" screen (the same one used for storage buses etc., with `+1/+10/+100/+1000` buttons and a number field).

There are three ways to set it.

優先度は初期値0、数値が高いほど優先されます。設定はAE2純正の「優先度」画面(ストレージバス等と同じ、`+1/+10/+100/+1000` ボタンと数値入力欄のある画面)で行います。

設定方法は3通りあります。

### Method 1: Set it when starting a craft / 方法1: クラフト開始時に設定する

The crafting request flow becomes "set amount → **set priority** → select CPU & start." Once you confirm the amount, the priority screen opens; enter a priority and press "Next" to move on to the normal CPU selection/start screen (pressing "Next" without changing anything keeps priority at 0, same as before). The priority you set is applied to whichever Crafting CPU actually ends up being selected.

アイテムのクラフト要求フローが「個数設定 → **優先度設定** → CPU選択・開始」に変わります。個数を確定すると優先度画面が開くので、優先度を入力して「次へ」を押すと通常のCPU選択・開始画面に進みます(そのまま「次へ」を押せば優先度0で従来通りです)。設定した優先度は、開始時に実際に選ばれたCrafting CPUへ適用されます。

### Method 2: Change it from the Crafting CPU screen / 方法2: Crafting CPUの画面から変更する

Right-click a Crafting CPU block to open its screen; a wrench-icon tab button has been added at the top right. Press it to open that CPU's priority screen (the back button returns you to the CPU screen).

Crafting CPUのブロックを右クリックしてCPU画面を開くと、画面右上にレンチアイコンのタブボタンが追加されています。これを押すとそのCPUの優先度画面が開きます(戻るボタンでCPU画面へ戻れます)。

### Method 3: Change it from the terminal's "Crafting Status" tab / 方法3: 端末の「クラフト状況(Crafting Status)」タブから変更する

In an AE2 terminal's Crafting Status tab, select the target CPU from the list on the left, then press the wrench-icon button added to the left of the "Cancel" button to open that CPU's priority screen (the back button returns you to the Crafting Status tab).

In the CPU list, **only CPUs that are currently running a job (active)** show their priority after their name, like "CPU #1@1000" (idle CPUs keep AE2's standard display).

AE2端末のクラフト状況タブで、左側のCPU一覧から対象CPUを選択し、「キャンセル」ボタンの左に追加されたレンチアイコンのボタンを押すと、選択中CPUの優先度画面が開きます(戻るボタンでクラフト状況タブへ戻れます)。

CPU一覧では**ジョブを実行中(アクティブ)のCPUのみ**「CPU #1@1000」のように優先度が名前の後ろに表示されます(アイドル状態のCPUはAE2標準の表示のままです)。

### Priority auto-reset / 優先度の自動リセット

Priority is a per-job setting. When a crafting task ends (whether completed or cancelled), that CPU's priority automatically resets to 0.

優先度はジョブ単位の設定です。クラフトタスクが終了(完了・キャンセルとも)すると、そのCPUの優先度は自動的に0へ戻ります。

## Compatible Addon Mods / 対応アドオンMOD

On this branch (1.18.2), compatibility with other AE2 addon mods (AdvancedAE, Mega Cells, etc.) has not been tested — only AE2 by itself has been verified on real hardware. (The 1.20.1 version has been verified alongside several companion mods; see the README on the `main` branch for details.)

このブランチ(1.18.2)では、AE2本体以外の連携MOD(AdvancedAE、Mega Cells等)を組み合わせた動作確認は行っていません。AE2単体との組み合わせでのみ実機確認済みです。(1.20.1版では複数の連携MODとの併用を確認済みです。詳細はmainブランチのREADMEを参照してください)

## Developer API / 開発者向けAPI

A public API is provided so other mods can read and write this mod's priority values. See the "Developer API" section of [DEVELOPMENT.md](DEVELOPMENT.md) for details.

他のMODから本MODの優先度を読み書きするための公開APIを提供しています。詳細は[DEVELOPMENT.md](DEVELOPMENT.md) の「開発者向けAPI」の項を参照してください。

## Known Limitations / 既知の制限

- "Complete jobs in priority order as much as possible" is best-effort. Depending on the situation, jobs may not finish exactly in priority order.
- Priority only has a visible effect **when multiple crafting jobs are competing for the same machine (Pattern Provider)**. If machines have enough spare capacity that no contention occurs, changing priority won't produce any visible difference — since there's no contention, there's nothing to prioritize between in the first place.

- 「できるだけ優先度順に完了させる」はベストエフォートです。状況によっては優先度通りにならないことがあります
- 優先度の効果は、**複数のクラフトジョブが同じ機械(Pattern Provider)を取り合っている場合のみ**目に見える形で現れます。機械に十分な余裕があり取り合いが起きていない場合は、優先度を変えても見た目上の違いは出ません(取り合いが発生していないので、そもそも優先順位をつける必要がない状態です)

## Credits / クレジット

Author: yuuhamu
Applied Energistics 2 (AE2) is a separate required dependency. This mod does not bundle AE2.

作者: yuuhamu
Applied Energistics 2 (AE2) は別途必要な依存MODです。このMODはAE2を同梱していません。

## License / ライセンス

This mod is released under the [MIT License](LICENSE).

本MODは [MIT License](LICENSE) の下で公開されています。

---

For development and build instructions, see [DEVELOPMENT.md](DEVELOPMENT.md).
開発・ビルド方法など技術的な情報は [DEVELOPMENT.md](DEVELOPMENT.md) を参照してください。
