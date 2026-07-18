package com.yuuhamu.ae2craftpriority.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.TabButton;

@Mixin(value = TabButton.class, remap = false)
public interface TabButtonAccessor {

    @Accessor("icon")
    void ae2cp$setIcon(Icon icon);

    @Accessor("item")
    void ae2cp$setItem(ItemStack item);
}
