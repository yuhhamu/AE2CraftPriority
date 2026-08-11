package com.yuuhamu.ae2craftpriority.mixin.status;

import com.yuuhamu.ae2craftpriority.client.PriorityBackIconOverride;
import com.yuuhamu.ae2craftpriority.mixin.accessor.TabButtonAccessor;
import com.yuuhamu.ae2craftpriority.mixin.accessor.WidgetContainerAccessor;
import com.yuuhamu.ae2craftpriority.priority.CraftingPriorityHostMarker;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.PriorityScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.GuiText;
import appeng.menu.implementations.PriorityMenu;

@Mixin(value = PriorityScreen.class, remap = false)
public abstract class PriorityScreenMixin extends AEBaseScreen<PriorityMenu> {

    private PriorityScreenMixin(PriorityMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2cp$onInit(PriorityMenu menu, Inventory playerInventory, Component title, ScreenStyle style,
            CallbackInfo ci) {
        if (!(menu.getHost() instanceof CraftingPriorityHostMarker)) {
            return;
        }

        setTextContent("priority_insertion_hint",
                Component.translatable("gui.ae2craftpriority.priority_hint_high"));
        setTextHidden("priority_extraction_hint", true);

        if (PriorityBackIconOverride.take()
                && ((WidgetContainerAccessor) this.widgets).ae2cp$getWidgets().get("back") instanceof TabButton back) {
            var accessor = (TabButtonAccessor) back;
            accessor.ae2cp$setItem(null);
            accessor.ae2cp$setIcon(Icon.CRAFT_HAMMER);
            back.setMessage(GuiText.CraftingStatus.text());
        }
    }
}
