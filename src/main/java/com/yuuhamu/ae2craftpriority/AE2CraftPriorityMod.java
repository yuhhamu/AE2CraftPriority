package com.yuuhamu.ae2craftpriority;

import java.util.Objects;

import com.yuuhamu.ae2craftpriority.menu.CraftPriorityStepMenu;
import net.fabricmc.api.ModInitializer;

public class AE2CraftPriorityMod implements ModInitializer {

    public static final String MODID = "ae2craftpriority";

    @Override
    public void onInitialize() {
        // CraftPriorityStepMenu.TYPE(MenuType)をこの時点で確実にクラスロード・登録させる。
        Objects.requireNonNull(CraftPriorityStepMenu.TYPE);

        // 注意: AdvancedAE対応(compat.advancedae、Forge/NeoForge版に存在)はこのFabricターゲット
        // には含まれていない。2026-08時点でAdvancedAE(net.pedroksl.advanced_ae)にFabric版が
        // 存在しないため。AdvancedAEがFabricに対応した場合は、Forge版のcompat/advancedae一式
        // (AdvancedAeCompat/AdvancedAeCpuAdapter等)とae2craftpriority-advancedae.mixins.jsonを
        // 移植し、ここから条件付きで初期化する形に戻すこと。
    }
}
