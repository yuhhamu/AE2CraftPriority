package com.yuuhamu.ae2craftpriority.mixin.status;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.yuuhamu.ae2craftpriority.mixin.accessor.AEBaseContainerAccessor;
import com.yuuhamu.ae2craftpriority.priority.PriorityBackNavigationControl;
import com.yuuhamu.ae2craftpriority.priority.PriorityReturnTarget;

import appeng.container.ContainerOpener;
import appeng.container.implementations.PriorityContainer;
import appeng.helpers.IPriorityHost;

/**
 * バニラの{@code PriorityContainer}に、README方法3(端末のCrafting Statusタブ経由)専用の
 * 「戻る」アクションを追加する。詳細な経緯・必要性は{@link PriorityBackNavigationControl}参照。
 *
 * <p>{@code CraftingStatusContainerMixin#ae2cp$openPrioritySettings()}のクライアント/サーバー
 * 判定パターンを踏襲: クライアント側から呼ばれた場合はサーバーへ転送するだけ、サーバー側では
 * 実際に{@link PriorityReturnTarget#take}で記録済みの戻り先(端末自身のロケータ)を取り出し、
 * {@code ContainerOpener.openContainer(...)}で直接開き直す。方法2(直接右クリック)や、
 * 本アドオン非対応の通常のストレージバス優先度編集等、{@link PriorityReturnTarget}に何も
 * 記録されていない場合は単に何もしない(このアクション自体が呼ばれるのは、
 * {@code PriorityScreenMixin}が方法3経由と判定して「戻る」ボタンを差し替えた場合のみのため、
 * 通常のバニラ用途に影響は無い)。実際の消費(削除)を行うのはここ1箇所のみ
 * ({@code PriorityScreenMixin}側は判定用にpeek()のみ使用、実機検証済み)。</p>
 */
@Mixin(value = PriorityContainer.class, remap = false)
public abstract class PriorityContainerMixin implements PriorityBackNavigationControl {

    private static final String ACTION_RETURN_TO_CRAFTING_STATUS = "ae2cp$returnToCraftingStatus";

    // PriorityContainer(int id, PlayerInventory ip, IPriorityHost te)。javap -pで直接宣言(public)を
    // 確認済み(appeng.container.implementations.PriorityContainer)。
    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2cp$registerReturnAction(int id, PlayerInventory ip, IPriorityHost te, CallbackInfo ci) {
        ((AEBaseContainerAccessor) (Object) this).ae2cp$invokeRegisterClientAction(ACTION_RETURN_TO_CRAFTING_STATUS,
                this::ae2cp$returnToCraftingStatus);
    }

    @Override
    public void ae2cp$returnToCraftingStatus() {
        final AEBaseContainerAccessor accessor = (AEBaseContainerAccessor) (Object) this;
        if (accessor.ae2cp$invokeIsClient()) {
            accessor.ae2cp$invokeSendClientAction(ACTION_RETURN_TO_CRAFTING_STATUS);
            return;
        }

        final PlayerEntity player = accessor.ae2cp$invokeGetPlayerInventory().player;
        if (!(player instanceof ServerPlayerEntity)) {
            return;
        }

        final PriorityReturnTarget.Target target = PriorityReturnTarget.take(player.getUniqueID());
        if (target == null) {
            // 方法2経由、または本アドオン非対応のバニラ優先度編集(ストレージバス等)の場合は
            // ここに何も記録されていない。専用の「戻る」ボタンはPriorityScreenMixinが方法3と
            // 判定した場合にしか差し替えないため、通常はここに来ない想定だが、念のため無視する。
            return;
        }
        ContainerOpener.openContainer(target.getContainerType(), player, target.getLocator());
    }
}
