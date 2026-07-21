package com.yuuhamu.ae2craftpriority.mixin.accessor;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.TabButton;

/**
 * {@code TabButton}のprivateフィールド{@code icon}/{@code item}(1.16.5実ソースで確認済み、
 * どちらも1.18.2版と完全に同名)への書き込みアクセサ。{@code PriorityScreenMixin}が、
 * {@code AESubScreen#addBackButton}が生成した「戻る」ボタンの見た目を、
 * 遷移元(方法2/方法3)に応じて上書きする際に使う。
 */
@Mixin(value = TabButton.class, remap = false)
public interface TabButtonAccessor {

    @Accessor("icon")
    void ae2cp$setIcon(Icon icon);

    @Accessor("item")
    void ae2cp$setItem(ItemStack item);
}
