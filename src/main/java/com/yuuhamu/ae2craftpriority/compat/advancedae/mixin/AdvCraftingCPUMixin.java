package com.yuuhamu.ae2craftpriority.compat.advancedae.mixin;

import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;
import net.minecraft.nbt.CompoundTag;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 優先度の実体を {@code AdvCraftingCPUCluster}(物理的なQuantum Computer本体)ではなく、
 * {@code AdvCraftingCPU}(1クラフトタスク=1ジョブに対応する仮想CPU)側に持たせる。
 *
 * <p>AdvancedAEのQuantum Computerはバニラの「1 CraftingCPUCluster = 1ジョブ」という前提を破り、
 * 1台の物理クラスタから複数の {@code AdvCraftingCPU} を同時生成して並行ジョブを実行できる。
 * クラスタ側に優先度を持たせると、同一Quantum Computer上の並行タスクが優先度を共有してしまう
 * ため、タスク(ジョブ)自身に持たせる。</p>
 *
 * <p>{@code AdvCraftingCPU} はジョブ1件ごとに新規生成され、ジョブ完了時にオブジェクトごと
 * 破棄されるため、ここに優先度を持たせればジョブの生存期間=優先度の生存期間となり、バニラ側の
 * {@code CraftingCpuLogicMixin} が行っている「ジョブ完了時に優先度をデフォルトへリセットする」
 * 処理は不要(次のジョブは常に優先度 {@link PriorityHolder#DEFAULT_PRIORITY} の新しい
 * {@code AdvCraftingCPU} から始まる)。</p>
 */
@Mixin(value = AdvCraftingCPU.class, remap = false)
public abstract class AdvCraftingCPUMixin implements PriorityHolder {

    private static final String NBT_KEY = "ae2cp_priority";

    @Unique
    private int ae2cp$priority = PriorityHolder.DEFAULT_PRIORITY;

    @Override
    public int ae2cp$getPriority() {
        return this.ae2cp$priority;
    }

    @Override
    public void ae2cp$setPriority(int priority) {
        this.ae2cp$priority = priority;
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void ae2cp$onWriteToNBT(CompoundTag data, CallbackInfo ci) {
        data.putInt(NBT_KEY, this.ae2cp$priority);
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void ae2cp$onReadFromNBT(CompoundTag data, CallbackInfo ci) {
        this.ae2cp$priority = data.contains(NBT_KEY) ? data.getInt(NBT_KEY) : PriorityHolder.DEFAULT_PRIORITY;
    }
}
