*This mod's development is assisted by Anthropic's AI assistant "Claude."*

# AE2 Crafting Priority (Fabric 1.20.1)

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
AE2's own "priority" screen (the one with `+1/+10/+100/+1000` buttons and a numeric input
field). There are three ways to set it.

- **Set it when starting a craft**: The item crafting request flow changes to
  "set amount → set priority → select CPU / start." The priority you set is applied to
  whichever Crafting CPU actually gets picked when the craft starts
- **Change it from the Crafting CPU screen**: A wrench-icon tab button at the top right
  lets you open that CPU's priority screen directly
- **Change it from the terminal's Crafting Status tab**: Select a CPU in AE2's Crafting
  Status tab, then use the wrench-icon button next to "Cancel" to change that CPU's priority

Any CPU that's currently active shows its priority next to its name, e.g. `CPU #1@1000`,
and the priority automatically resets to 0 once the crafting task ends.

## Compatible Addon Mods

- **AdvancedAE**: Not available for Fabric — AdvancedAE has no Fabric build, so this port
  does not include AdvancedAE compatibility code.
- Mega Cells, ExtendedAE, and Applied Mekanistics are confirmed compatible on the Forge/1.20.1
  version via the standard Pattern Provider extension mechanism, and are expected to work the
  same way here.

For requirements, installation steps, and development details, see the
[GitHub repository](https://github.com/yuhhamu/AE2CraftPriority) (`fabric/1.20.1` branch).

---

*本MODの開発にはAnthropicのAIアシスタント「Claude」を活用しています。*

# AE2 Crafting Priority (Fabric 1.20.1)

AE2(Applied Energistics 2)の自動クラフトに **「優先度」** を追加するアドオンMODです。

AE2の自動クラフトでは、複数のクラフトジョブが同じ機械(圧縮機・溶鉱炉など、Pattern
Provider経由で繋がった実機械)を取り合うことがあります。このMODを入れると、ジョブごとに
「優先度」を設定でき、機械が空いたときに優先度の高いジョブから先にその機械を使えるように
なります。

- 処理中の機械から横取りすることはありません。あくまで「次に空いたときにどちらを優先するか」を制御します
- ネットワーク全体に対するベストエフォートの調整であり、厳密な保証があるわけではありません

## 主な機能

優先度は初期値0、数値が高いほど優先されます。設定はAE2純正の「優先度」画面
(`+1/+10/+100/+1000` ボタンと数値入力欄のある画面)で行います。設定方法は3通りあります。

- **クラフト開始時に設定**: アイテムのクラフト要求フローが「個数設定 → 優先度設定 →
  CPU選択・開始」に変わります。設定した優先度は、開始時に実際に選ばれたCrafting CPUへ
  適用されます
- **Crafting CPU画面から変更**: 画面右上のレンチアイコンのタブボタンから、そのCPUの優先度
  画面を直接開けます
- **クラフト状況タブから変更**: AE2端末のクラフト状況タブでCPUを選択し、「キャンセル」
  ボタンの隣のレンチアイコンのボタンから優先度を変更できます

実行中(アクティブ)のCPUには `CPU #1@1000` のように優先度が表示され、クラフトタスクが
終了すると優先度は自動的に0へリセットされます。

## 対応アドオンMOD

- **AdvancedAE**: Fabric版が存在しないため対象外です。本Fabric版にはAdvancedAE対応コード
  自体を含んでいません。
- Mega Cells・ExtendedAE・Applied Mekanisticsは、Forge/1.20.1版で標準のPattern Provider拡張
  として動作確認済みです。仕組み上は同様に動作すると見込まれます。

必要環境・導入方法・開発情報は [GitHubリポジトリ](https://github.com/yuhhamu/AE2CraftPriority)
(`fabric/1.20.1` ブランチ)を参照してください。
