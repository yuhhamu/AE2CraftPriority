package com.yuuhamu.ae2craftpriority.mixin.craft;

import java.util.concurrent.Future;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.yuuhamu.ae2craftpriority.menu.CraftPriorityStepContainer;
import com.yuuhamu.ae2craftpriority.priority.PendingCraftPriority;
import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.ContainerLocator;
import appeng.container.me.crafting.CraftAmountContainer;
import appeng.container.me.crafting.CraftConfirmContainer;
import appeng.core.AELog;

/**
 * CPU選択・開始画面のContainer({@code CraftConfirmContainer})にジョブ優先度を保持させる。
 *
 * <p>値は優先度設定ステップ({@link CraftPriorityStepContainer})から{@code PriorityHolder.setPriority}
 * 経由で引き渡される。ジョブ提出({@code startJob()})の実行区間だけ{@link PendingCraftPriority}に
 * 流し込み、提出の確定地点({@code CraftingCpuLogicMixin})で読み取って、実際に選ばれたCPUへ適用する。</p>
 */
@Mixin(value = CraftConfirmContainer.class, remap = false)
public abstract class CraftConfirmContainerMixin implements PriorityHolder {

    @Unique
    private int ae2cp$priority = PriorityHolder.DEFAULT_PRIORITY;

    @Shadow
    public boolean autoStart;

    /** {@code getLocator()}/{@code getPlayerInventory()}は{@code CraftConfirmContainer}自身では
     * なく親クラス{@code AEBaseContainer}が直接宣言しているため@Shadow不可(未使用のため削除。
     * 詳細は{@code Knowledge/mixin-shadow-cannot-target-inherited-methods.md}参照)。 */

    @Shadow
    public abstract World getWorld();

    /** {@code CraftConfirmContainer}自身が持つ private メソッドを直接Shadowする。
     * Javaの言語仕様上 private と abstract は併用できないため、パッケージプライベート
     * (修飾子なし)の abstract として宣言する。 */
    @Shadow
    abstract IGrid getGrid();

    @Shadow
    abstract IActionSource getActionSrc();

    @Override
    public int ae2cp$getPriority() {
        return this.ae2cp$priority;
    }

    @Override
    public void ae2cp$setPriority(int priority) {
        this.ae2cp$priority = priority;
    }

    /**
     * バニラの{@code goBack()}は「CPU選択画面をキャンセルしたら要求数設定画面
     * ({@code CraftAmountContainer})へ戻る」を決め打ちしている。このMODは要求数設定とCPU選択の間に
     * 優先度設定({@link CraftPriorityStepContainer})を挟んでいるため、そのままでは優先度設定が
     * スキップされて要求数設定まで戻ってしまう(既に入力した優先度も失われる)。ここで戻り先を
     * 優先度設定画面に差し替える。
     *
     * <p>この画面まで到達した時点で、要求数設定画面から引き継いだ{@code Future<ICraftingJob>}は
     * 既に計算完了して{@code this.result}へ格納済み(=元のFutureは消費済み)であるのが通常のため
     * ({@code detectAndSendChanges()}が完了検知後に{@code setJob(null)}する)、戻る際は
     * {@code CraftAmountContainer#confirm()}と同じ手順でジョブ計算をやり直してから優先度設定画面へ
     * 引き渡す。計算に失敗した場合はジョブ無しのまま優先度設定画面を開く(バニラの元エラー処理と
     * 同様、クラッシュはさせない)。</p>
     */
    @Redirect(method = "goBack", at = @At(value = "INVOKE",
            target = "Lappeng/container/me/crafting/CraftAmountContainer;"
                    + "open(Lnet/minecraft/entity/player/ServerPlayerEntity;Lappeng/container/ContainerLocator;"
                    + "Lappeng/api/storage/data/IAEItemStack;I)V"))
    private void ae2cp$backToPriorityStep(ServerPlayerEntity player, ContainerLocator locator,
            IAEItemStack itemToCraft, int initialAmount) {
        Future<ICraftingJob> futureJob = null;
        try {
            final IGrid g = this.getGrid();
            if (g != null) {
                final ICraftingGrid cg = g.getCache(ICraftingGrid.class);
                futureJob = cg.beginCraftingJob(this.getWorld(), g, this.getActionSrc(), itemToCraft, null);
            }
        } catch (final Throwable e) {
            if (futureJob != null) {
                futureJob.cancel(true);
            }
            futureJob = null;
            AELog.info(e);
        }

        CraftPriorityStepContainer.open(player, locator, itemToCraft, futureJob, this.autoStart,
                this.ae2cp$getPriority());
    }

    @Inject(method = "startJob", at = @At("HEAD"))
    private void ae2cp$onStartJobHead(CallbackInfo ci) {
        PendingCraftPriority.set(this.ae2cp$priority);
    }

    @Inject(method = "startJob", at = @At("RETURN"))
    private void ae2cp$onStartJobReturn(CallbackInfo ci) {
        PendingCraftPriority.clear();
    }
}
