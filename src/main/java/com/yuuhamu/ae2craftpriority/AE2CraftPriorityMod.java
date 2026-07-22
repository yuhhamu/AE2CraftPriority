package com.yuuhamu.ae2craftpriority;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * AE2 Crafting Priority ─ 1.12.2版エントリポイント。
 *
 * TODO: これはプロジェクト骨格の雛形であり、実装は未着手(design/PORT-DESIGN-1.12.2.md参照)。
 * Mixin導入(MixinBooter経由 or 自前coremod、design 6章)のPoCが完了するまで、
 * 実際のMixin適用は動作未確認。
 */
@Mod(modid = AE2CraftPriorityMod.MODID, name = "AE2 Crafting Priority", version = "@VERSION@",
        dependencies = "required-after:appliedenergistics2")
public class AE2CraftPriorityMod {

    public static final String MODID = "ae2craftpriority";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // TODO: 実装
    }
}
