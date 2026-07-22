package com.yuuhamu.ae2craftpriority;

import java.util.Objects;

import com.yuuhamu.ae2craftpriority.client.ClientSetup;
import com.yuuhamu.ae2craftpriority.compat.advancedae.AdvancedAeCompat;
import com.yuuhamu.ae2craftpriority.menu.CraftPriorityStepMenu;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(AE2CraftPriorityMod.MODID)
public class AE2CraftPriorityMod {

    public static final String MODID = "ae2craftpriority";

    public AE2CraftPriorityMod(IEventBus modEventBus) {
        Objects.requireNonNull(CraftPriorityStepMenu.TYPE);

        if (ModList.get().isLoaded(AdvancedAeCompat.MOD_ID)) {
            AdvancedAeCompat.init();
        }

        modEventBus.addListener(
                (final FMLClientSetupEvent event) -> event.enqueueWork(() -> ClientSetup.init()));
    }
}
