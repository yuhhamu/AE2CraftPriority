package com.yuuhamu.ae2craftpriority.client;

import net.minecraft.client.gui.widget.button.Button.IPressable;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.IconButton;

/**
 * {@code CraftingCPUScreenMixin}が追加する優先度編集ボタン(レンチアイコン)の実装。
 *
 * <p>{@code IconButton}は{@code protected abstract Icon getIcon();}を持つabstractクラスであり、
 * 元は{@code CraftingCPUScreenMixin}内の匿名クラス({@code new IconButton(...) { ... }})として
 * 実装していた。しかし匿名クラスはMixinが対象クラス({@code CraftingCPUScreen})へマージする際に
 * 内部クラスの再配置(リネーム)が必要になり、実機起動時に
 * {@code NoClassDefFoundError: appeng/client/gui/me/crafting/CraftingCPUScreen$Anonymous$<hash>}
 * というクラスロード失敗を引き起こした(2026-07-22発見)。Mixin対象クラスに一切マージされない
 * 独立したトップレベルクラスとして切り出すことで回避する。詳細は
 * {@code Knowledge/mixin-anonymous-class-in-injected-method-breaks-classloading.md}参照。</p>
 */
public class PriorityWrenchButton extends IconButton {

    public PriorityWrenchButton(IPressable onPress) {
        super(onPress);
    }

    @Override
    protected Icon getIcon() {
        return Icon.WRENCH;
    }
}
