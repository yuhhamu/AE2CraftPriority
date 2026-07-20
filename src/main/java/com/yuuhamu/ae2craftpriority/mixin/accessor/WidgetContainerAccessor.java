package com.yuuhamu.ae2craftpriority.mixin.accessor;

import java.util.Map;

import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import appeng.client.gui.WidgetContainer;

@Mixin(value = WidgetContainer.class, remap = false)
public interface WidgetContainerAccessor {

    @Accessor("widgets")
    Map<String, AbstractWidget> ae2cp$getWidgets();
}
