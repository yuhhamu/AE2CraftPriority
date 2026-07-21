package com.yuuhamu.ae2craftpriority.mixin.craft;

import java.util.concurrent.Future;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.yuuhamu.ae2craftpriority.menu.CraftPriorityStepContainer;
import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.ContainerLocator;
import appeng.container.me.crafting.CraftAmountContainer;
import appeng.core.AELog;

/**
 * 「個数を確定する({@code confirm}する)」操作を横取りし、バニラのように直接
 * {@code CraftConfirmContainer}(CPU選択画面)を開くのではなく、その手前に優先度入力画面
 * ({@link CraftPriorityStepContainer})を挟む。
 *
 * <p>1.16.5実ソース({@code CraftAmountContainer.java} confirm()メソッド、131行目付近)は、
 * {@code CraftConfirmContainer}を開く*前*に{@code cg.beginCraftingJob(...)}でジョブ計算
 * ({@link Future})を完了させる設計であるため、この計算結果を{@code CraftPriorityStepContainer}
 * へそのまま引き渡す必要がある。{@code Future}はメソッドローカル変数であり、Mixinの
 * {@code @Redirect}では(ローカル変数キャプチャの仕組み無しには)このメソッド呼び出しの
 * 引数以外の形で安全に橋渡しできない上、仮に呼び出しだけを{@code @Redirect}で差し替えても
 * バニラ側で既に計算済みの{@code futureJob}が誰にも渡されず宙に浮いてキャンセルもされない
 * (計算だけ無駄に走り続ける)リークになってしまう。そのため
 * {@code @Inject(at = "HEAD", cancellable = true)}でサーバー側実行時のみメソッド全体を
 * 差し替える方式を採る。</p>
 *
 * <p>クライアント側実行(パケット送信のみ)はバニラのまま素通しする(横取りしない)。</p>
 */
@Mixin(value = CraftAmountContainer.class, remap = false)
public abstract class CraftAmountContainerMixin {

    @Shadow
    private IAEItemStack itemToCreate;

    @Shadow
    protected abstract boolean isServer();

    @Shadow
    public abstract Object getTarget();

    @Shadow
    public abstract ContainerLocator getLocator();

    @Shadow
    public abstract PlayerInventory getPlayerInventory();

    @Shadow
    public abstract IGrid getGrid();

    @Shadow
    public abstract World getWorld();

    @Shadow
    public abstract IActionSource getActionSrc();

    @Shadow
    public abstract void detectAndSendChanges();

    @Inject(method = "confirm", at = @At("HEAD"), cancellable = true)
    private void ae2cp$openPriorityStepInstead(int amount, boolean autoStart, CallbackInfo ci) {
        if (!this.isServer()) {
            // クライアント側実行(ConfirmAutoCraftPacketの送信のみ)はバニラのまま。
            return;
        }
        ci.cancel();

        final Object target = this.getTarget();
        if (!(target instanceof IActionHost)) {
            return;
        }
        final IActionHost ah = (IActionHost) target;
        final IGridNode gn = ah.getActionableNode();
        if (gn == null) {
            return;
        }
        final IGrid g = gn.getGrid();
        if (g == null || this.itemToCreate == null) {
            return;
        }

        this.itemToCreate.setStackSize(amount);

        Future<ICraftingJob> futureJob = null;
        try {
            final ICraftingGrid cg = g.getCache(ICraftingGrid.class);
            futureJob = cg.beginCraftingJob(this.getWorld(), this.getGrid(), this.getActionSrc(), this.itemToCreate,
                    null);

            final ContainerLocator locator = this.getLocator();
            final PlayerEntity playerEntity = this.getPlayerInventory().player;
            if (locator != null && playerEntity instanceof ServerPlayerEntity) {
                CraftPriorityStepContainer.open((ServerPlayerEntity) playerEntity, locator, this.itemToCreate.copy(),
                        futureJob, autoStart, PriorityHolder.DEFAULT_PRIORITY);
                this.detectAndSendChanges();
            } else {
                futureJob.cancel(true);
            }
        } catch (final Throwable e) {
            if (futureJob != null) {
                futureJob.cancel(true);
            }
            AELog.info(e);
        }
    }
}
