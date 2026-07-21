package com.yuuhamu.ae2craftpriority.mixin.accessor;

import java.util.Map;

import net.minecraft.client.gui.widget.Widget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import appeng.client.gui.WidgetContainer;

/**
 * {@code WidgetContainer}のprivateフィールド{@code widgets}(1.16.5実ソースで確認済み、
 * {@code Map<String, Widget>})への読み取りアクセサ。{@code PriorityScreenMixin}が、
 * {@code AESubScreen#addBackButton("back", widgets)}で追加された「戻る」ボタンを
 * IDから引き当てるために使う。
 */
@Mixin(value = WidgetContainer.class, remap = false)
public interface WidgetContainerAccessor {

    @Accessor("widgets")
    Map<String, Widget> ae2cp$getWidgets();
}
