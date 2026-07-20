package com.yuuhamu.ae2craftpriority;

import java.util.Objects;

import com.yuuhamu.ae2craftpriority.client.ClientSetup;
import com.yuuhamu.ae2craftpriority.menu.CraftPriorityStepMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AE2CraftPriorityMod.MODID)
public class AE2CraftPriorityMod {

    public static final String MODID = "ae2craftpriority";

    public AE2CraftPriorityMod() {
        Objects.requireNonNull(CraftPriorityStepMenu.TYPE);

        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // CraftPriorityStepMenu.TYPEはAE2側のInitMenuTypes.init()には含まれない(AE2自身の
        // TYPEのみを列挙するハードコードリストのため)。MenuTypeBuilder#build()はsetRegistryName()
        // を呼ぶだけでForgeレジストリへの実登録は行わないため、ここで明示的に登録する。
        // 未登録のままだと画面オープンパケットでMenuTypeのレジストリIDが解決できず、
        // クライアント側で画面が開かない。
        modEventBus.addGenericListener(MenuType.class,
                (final RegistryEvent.Register<MenuType<?>> event) -> event.getRegistry()
                        .register(CraftPriorityStepMenu.TYPE));

        modEventBus.addListener(
                (final FMLClientSetupEvent event) -> event.enqueueWork(() -> ClientSetup.init()));
    }
}
