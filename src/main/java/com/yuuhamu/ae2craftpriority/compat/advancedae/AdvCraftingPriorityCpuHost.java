package com.yuuhamu.ae2craftpriority.compat.advancedae;

import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;

/**
 * {@code AdvCraftingBlockEntity}(Quantum Computerのブロック本体)に、「今まさに優先度編集画面
 * (AE2バニラの {@code PriorityMenu})が対象にしているタスク」を一時的に覚えさせるための橋渡し。
 *
 * <p>AE2バニラの {@code IPriorityHost#getPriority()}/{@code #setPriority(int)} は引数を一切
 * 取らない(呼び出し元プレイヤーやタスクの情報を渡す手段が無い)。一方、Quantum Computer上の
 * タスク({@code AdvCraftingCPU})は複数存在しうるため、優先度画面を開く直前(サーバー側で
 * 「今どのタスクが選択されていたか」が分かる唯一のタイミング)に、対象タスクをこのホストへ
 * セットしておき、{@code IPriorityHost} の実装({@link
 * com.yuuhamu.ae2craftpriority.compat.advancedae.mixin.AdvCraftingBlockEntityMixin})が
 * それを読み書きの実体として使う。</p>
 *
 * <p>既知の制約: 同一のQuantum Computerに対し複数プレイヤーが同時に別々のタスクの優先度画面を
 * 開いた場合、後から開いた方のタスクが優先される(ブロック本体1つにつき1つの値しか
 * 覚えられないため)。プレイヤー単位で厳密に分離するにはAE2本体の {@code PriorityMenu} 自体へ
 * Mixinで介入する必要があり、コスト対効果を鑑みて見送っている。</p>
 */
public interface AdvCraftingPriorityCpuHost {

    void ae2cp$setActivePriorityCpu(AdvCraftingCPU cpu);

    AdvCraftingCPU ae2cp$getActivePriorityCpu();
}
