# AE2 Crafting Priority (Fabric 1.20.1)

> This mod's development leverages Anthropic's AI assistant, Claude.
> 本MODの開発にはAnthropicのAIアシスタント「Claude」を活用しています。

An addon mod for Applied Energistics 2 (AE2) that adds crafting **priority** to its auto-crafting system — Fabric / Minecraft 1.20.1 port.
AE2(Applied Energistics 2)の自動クラフトに「優先度」を追加するアドオンMODです — Fabric / Minecraft 1.20.1 版。

## Status / 現在の状況

🧪 **Beta**: This is a forward-port from the Forge/1.20.1 version (see the [main repository](https://github.com/yuhhamu/AE2CraftPriority)). Core features are implemented, and the mod has been verified end-to-end via an automated server + client auto-join test running alongside its full companion mod set (AE2, Mouse Tweaks, Jade, JourneyMap, JEI) without crashes. The developer has confirmed this port has feature parity with the Forge/1.20.1 version. In-world testing of priority ordering under heavy crafting contention is still ongoing — feedback and bug reports are welcome.

🧪 **ベータ版**: Forge/1.20.1版([メインリポジトリ](https://github.com/yuhhamu/AE2CraftPriority))からのフォワードポートです。主要機能は実装済みで、対応する周辺MOD一式(AE2、Mouse Tweaks、Jade、JourneyMap、JEI)を揃えた状態でのサーバー起動・クライアント自動参加までを自動テストで確認しています。開発者本人により、Forge/1.20.1版との機能同等性も確認済みです。多数のクラフトジョブが競合する状況での優先度制御そのものについては引き続き検証中です。不具合等ありましたらご報告ください。

## What Does This Mod Do? / これは何をするMODですか?

In AE2's auto-crafting, multiple crafting jobs can end up competing for the same machine (a compressor, furnace, etc. connected via a Pattern Provider). This mod lets you set a **priority** per job, so when a machine frees up, the higher-priority job gets to use it first.

- It never interrupts a machine that's already processing something — it only decides which job goes next once a machine becomes free.
- This is a best-effort, network-wide adjustment, not a strict guarantee.

AE2の自動クラフトでは、複数のクラフトジョブが同じ機械(圧縮機や溶鉱炉など、Pattern Provider経由で繋がった実機械)を取り合うことがあります。このMODを入れると、ジョブごとに「優先度」を設定でき、機械が空いたときに優先度の高いジョブから先にその機械を使えるようになります。

- 処理中の機械から横取りすることはありません。あくまで「次に空いたときにどちらを優先するか」を制御します。
- ネットワーク全体に対するベストエフォートの調整であり、厳密な保証があるわけではありません。

## Requirements / 必要なもの

- Minecraft 1.20.1
- Fabric Loader 0.19.3 or later (tested with 0.19.3)
- **Fabric API 0.88.1+1.20.1 — pinned, do not use a newer build.** This mod is intentionally built against this exact Fabric API version to match what AE2 15.4.10 [FABRIC] itself was built with. A newer Fabric API causes the server to crash on startup with a `NoSuchMethodError` (FabricItemSettings-related) when AE2 initializes its items — confirmed on real hardware. Wait for this mod (or AE2) to officially raise the pin before upgrading.
- Team Reborn Energy API (required separately; AE2's Fabric build needs it for energy interop — tested with 3.0.0)
- Applied Energistics 2 for Fabric, Minecraft 1.20.1 (15.x series; tested with 15.4.10 — on Modrinth this is the **"AE2 15.4.10 [FABRIC]"** build specifically, since Forge and Fabric builds share the same version-number label there)

このMODはAE2の内部実装(公開APIではない部分)を直接利用しているコードを含みます。動作しない場合は、AE2をできるだけ最新の15.x系列(Fabric版)に揃えたうえで再度お試しください。

- Minecraft 1.20.1
- Fabric Loader 0.19.3以降(0.19.3で動作確認)
- **Fabric API 0.88.1+1.20.1(固定・これより新しいバージョンは使用不可)**: 本MODはAE2 15.4.10 [FABRIC]自身がビルドに使用しているFabric APIバージョンに意図的に合わせています。これより新しいFabric APIを使うと、AE2のアイテム初期化時に`NoSuchMethodError`(FabricItemSettings関連)でサーバー起動がクラッシュすることを実機確認済みです。本MODまたはAE2が正式にこの固定バージョンを引き上げるまでは、手動でのアップグレードは行わないでください。
- Team Reborn Energy API(別途導入が必要。AE2のFabric版がエネルギー相互運用のために必要とします。3.0.0で動作確認)
- Applied Energistics 2 for Fabric、Minecraft 1.20.1版(15.x系列、15.4.10で動作確認。ModrinthではForge版・Fabric版でバージョン番号表記が共用されているため、**「AE2 15.4.10 [FABRIC]」**という表記のビルドを選んでください)

## Installation / 導入方法

1. Install Fabric Loader.
2. Put the Fabric API jar (0.88.1+1.20.1 — see Requirements above) in your `mods` folder.
3. Put the Team Reborn Energy API jar in your `mods` folder.
4. Put the Applied Energistics 2 jar in your `mods` folder.
5. Put this mod's (AE2 Crafting Priority) jar in your `mods` folder as well.
6. Launch Minecraft.

1. Fabric Loaderをインストールする
2. Fabric API(0.88.1+1.20.1、上記「必要なもの」参照)の jar を `mods` フォルダに入れる
3. Team Reborn Energy API の jar を `mods` フォルダに入れる
4. Applied Energistics 2 の jar を `mods` フォルダに入れる
5. このMOD(AE2 Crafting Priority)の jar も `mods` フォルダに入れる
6. Minecraftを起動する

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

- **AdvancedAE**: Not available for Fabric as of this writing (AdvancedAE has no Fabric build), so this Fabric port does not include AdvancedAE compatibility code, unlike the Forge/NeoForge versions.
- **Mega Cells / ExtendedAE / Applied Mekanistics**: Confirmed compatible on the Forge/1.20.1 version via the standard Pattern Provider extension mechanism, so the same should hold here for architectural reasons — but these have not yet been individually verified on this Fabric port.

- **AdvancedAE**: 2026年8月時点でFabric版が存在しないため、Fabric版ではAdvancedAE対応コード自体を含んでいません(Forge/NeoForge版とはこの点が異なります)。
- **Mega Cells / ExtendedAE / Applied Mekanistics**: Forge/1.20.1版では標準のPattern Provider拡張として動作確認済みです。仕組み上は同様に動作すると見込まれますが、本Fabric版では個別の動作確認はまだ行っていません。

## Developer API / 開発者向けAPI

A public API is provided so other mods can read and write this mod's priority values. See the "Developer API" section of [DEVELOPMENT.md](DEVELOPMENT.md) for details.

他のMODから本MODの優先度を読み書きするための公開APIを提供しています。詳細は[DEVELOPMENT.md](DEVELOPMENT.md) の「開発者向けAPI」の項を参照してください。

## Known Limitations / 既知の制限

- "Complete jobs in priority order as much as possible" is best-effort. Depending on the situation, jobs may not finish exactly in priority order.
- Priority only has a visible effect **when multiple crafting jobs are competing for the same machine (Pattern Provider)**.
- The Fabric API version is intentionally pinned to 0.88.1+1.20.1 (see Requirements) — using a newer Fabric API alongside AE2 15.4.10 will crash the game at startup.
- This is a forward-port at beta stage. Build/boot and mod-compatibility have been verified via automated testing; human in-world verification of priority ordering under real crafting contention is still ongoing.

- 「できるだけ優先度順に完了させる」はベストエフォートです。状況によっては優先度通りにならないことがあります。
- 優先度の効果は、**複数のクラフトジョブが同じ機械(Pattern Provider)を取り合っている場合のみ**目に見える形で現れます。
- Fabric APIのバージョンは意図的に0.88.1+1.20.1に固定しています(「必要なもの」参照)。AE2 15.4.10と組み合わせてこれより新しいFabric APIを使うと起動時にクラッシュします。
- 本バージョンはフォワードポートのベータ段階です。ビルド・起動・他MODとの互換性は自動テストで確認済みですが、実際のクラフトジョブでの優先度制御そのものの人手による検証は引き続き行っています。

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
