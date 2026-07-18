package com.yuuhamu.ae2craftpriority.compat.advancedae;

import net.minecraft.world.inventory.MenuType;
import net.pedroksl.advanced_ae.gui.quantumcomputer.QuantumComputerMenu;

/**
 * AdvancedAEの {@code QuantumComputerMenu} の {@link MenuType} をキャッシュする。
 *
 * <p>{@code QuantumComputerMenu} のメニュー種別({@code AAEMenus.QUANTUM_COMPUTER}、
 * {@code net.pedroksl.ae2addonlib.registry.MenuRegistry} 経由の {@code Supplier}
 * ラッパー)への直接アクセス手段が不明瞭だったため、コンパイル時に確実に存在が保証される
 * {@code AbstractContainerMenu#getType()}(バニラMinecraft継承メソッド)を使い、実際に
 * 生成されたメニューインスタンスから実行時に取得してキャッシュする方式にした
 * ({@link com.yuuhamu.ae2craftpriority.compat.advancedae.mixin.QuantumComputerMenuMixin}
 * が {@code <init>} 末尾で {@link #capture(MenuType)} を呼ぶ)。</p>
 */
public final class AdvancedAeMenuTypes {

    private static volatile MenuType<QuantumComputerMenu> quantumComputerMenuType;

    private AdvancedAeMenuTypes() {
    }

    public static void capture(MenuType<QuantumComputerMenu> type) {
        if (quantumComputerMenuType == null) {
            quantumComputerMenuType = type;
        }
    }

    /** 未取得(QuantumComputerMenuが一度も生成されていない)ならnull。 */
    public static MenuType<QuantumComputerMenu> quantumComputerMenuTypeOrNull() {
        return quantumComputerMenuType;
    }
}
