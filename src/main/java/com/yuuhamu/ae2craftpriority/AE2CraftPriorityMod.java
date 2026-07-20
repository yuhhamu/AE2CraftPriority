package com.yuuhamu.ae2craftpriority;

import java.util.Objects;

import com.yuuhamu.ae2craftpriority.client.ClientSetup;
import com.yuuhamu.ae2craftpriority.menu.CraftPriorityStepMenu;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AE2CraftPriorityMod.MODID)
public class AE2CraftPriorityMod {

    public static final String MODID = "ae2craftpriority";

    public AE2CraftPriorityMod() {
        Objects.requireNonNull(CraftPriorityStepMenu.TYPE);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(
                (final FMLClientSetupEvent event) -> event.enqueueWork(() -> ClientSetup.init()));
    }
}
