package com.yuuhamu.ae2craftpriority.mixin.status;

import net.minecraft.client.gui.widget.Widget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.yuuhamu.ae2craftpriority.mixin.accessor.TabButtonAccessor;
import com.yuuhamu.ae2craftpriority.mixin.accessor.WidgetContainerAccessor;
import com.yuuhamu.ae2craftpriority.priority.CraftingPriorityHostMarker;
import com.yuuhamu.ae2craftpriority.priority.PriorityReturnTarget;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.PriorityScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.TabButton;
import appeng.container.implementations.PriorityContainer;
import appeng.container.me.crafting.CraftingStatusContainer;
import appeng.core.localization.GuiText;

/**
 * バニラの優先度編集画面({@code PriorityScreen})が、Crafting CPU(README方法2・方法3)の
 * 優先度編集のために開かれたときの見た目を微調整する。
 *
 * <p>実際の「戻る」遷移先の決定自体は{@code CraftingTileEntityMixin#getContainerType()}
 * (このMixinのコンストラクタが呼ばれるより前、{@code AESubScreen}のコンストラクタ内で
 * {@link PriorityReturnTarget#peek}済み)で完結している。このMixinはその同じ記録を
 * {@link PriorityReturnTarget#take}で最終消費し、方法3(端末のCrafting Statusタブ)経由だった
 * 場合にのみ、「戻る」ボタンのラベルを「Crafting Status」に上書きして、戻り先が
 * (方法2のような単体CPU画面ではなく)端末のタブであることを視覚的に示す。</p>
 *
 * <p>クライアント側のみのMixin({@code ae2craftpriority.mixins.json}の{@code client}リストに
 * 属する)であるため、対応Mod(このアドオン)を導入していない環境の心配は無い。ただし
 * {@code this.container}(=バニラの{@code PriorityContainer})のホストが本アドオンの
 * {@link CraftingPriorityHostMarker}を実装しているかどうかは毎回instanceofで判定し、
 * ストレージバス等バニラ本来の優先度画面には一切影響しない。</p>
 */
@Mixin(value = PriorityScreen.class, remap = false)
public abstract class PriorityScreenMixin extends AEBaseScreen<PriorityContainer> {

    // Mixinの都合上のダミーコンストラクタ。実際にインスタンス化されることはない。
    private PriorityScreenMixin(PriorityContainer container, PlayerInventory playerInventory, ITextComponent title,
            ScreenStyle style) {
        super(container, playerInventory, title, style);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2cp$onInit(PriorityContainer container, PlayerInventory playerInventory, ITextComponent title,
            ScreenStyle style, CallbackInfo ci) {
        if (!(container.getPriorityHost() instanceof CraftingPriorityHostMarker)) {
            return;
        }
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        final PriorityReturnTarget.Target target = PriorityReturnTarget.take(this.minecraft.player.getUniqueID());
        if (target == null || target.getContainerType() != CraftingStatusContainer.TYPE) {
            return;
        }

        final Widget backWidget = ((WidgetContainerAccessor) this.widgets).ae2cp$getWidgets().get("back");
        if (backWidget instanceof TabButton) {
            final TabButton backButton = (TabButton) backWidget;
            backButton.setMessage(GuiText.CraftingStatus.text());
            // AESubScreenは既定でCraftingTileEntity#getItemStackRepresentation()の
            // アイテム外観(クラフトユニット)をアイコンに使うが、方法3経由と分かっている
            // ここでは「一覧に戻る」ことがより伝わるアイコンに差し替える。
            final TabButtonAccessor accessor = (TabButtonAccessor) backButton;
            accessor.ae2cp$setItem(ItemStack.EMPTY);
            accessor.ae2cp$setIcon(Icon.SEARCH_MANUAL);
        }
    }
}
