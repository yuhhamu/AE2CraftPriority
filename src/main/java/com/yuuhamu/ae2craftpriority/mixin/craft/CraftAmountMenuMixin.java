package com.yuuhamu.ae2craftpriority.mixin.craft;

import com.yuuhamu.ae2craftpriority.menu.CraftPriorityStepMenu;
import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import appeng.api.stacks.AEKey;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.menu.me.crafting.CraftAmountMenu;

@Mixin(value = CraftAmountMenu.class, remap = false)
public abstract class CraftAmountMenuMixin {

    @Shadow
    private AEKey whatToCraft;

    @Redirect(method = "confirm", at = @At(value = "INVOKE",
            target = "Lappeng/menu/MenuOpener;open(Lnet/minecraft/world/inventory/MenuType;"
                    + "Lnet/minecraft/world/entity/player/Player;Lappeng/menu/locator/MenuLocator;)Z"))
    private boolean ae2cp$openPriorityStep(MenuType<?> type, Player player, MenuLocator locator,
            int amount, boolean autoStart) {
        CraftPriorityStepMenu.open((ServerPlayer) player, locator, this.whatToCraft, amount, autoStart,
                PriorityHolder.DEFAULT_PRIORITY);
        return true;
    }
}
