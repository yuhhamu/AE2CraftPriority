package com.yuuhamu.ae2craftpriority;

import net.minecraft.inventory.container.ContainerType;

import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.yuuhamu.ae2craftpriority.client.ClientSetup;
import com.yuuhamu.ae2craftpriority.menu.CraftPriorityStepContainer;

/**
 * MODエントリポイント。
 *
 * <p>本アドオンの優先度システム自体は全てAE2純正クラスへのMixinで完結しているが、
 * 唯一新規に追加するContainer種別({@link CraftPriorityStepContainer#TYPE}、README方法1の
 * 優先度入力画面)だけは、{@code ContainerTypeBuilder#build(...)}がレジストリ名を設定するのみで
 * Forgeレジストリへの実登録までは行わない(1.16.5実ソースで確認済み)ため、ここで明示的に
 * 登録する必要がある。</p>
 */
@Mod("ae2craftpriority")
public class AE2CraftPriorityMod {

    public AE2CraftPriorityMod() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addGenericListener(ContainerType.class, this::registerContainers);
        modEventBus.addListener(this::clientSetup);
    }

    private void registerContainers(final RegistryEvent.Register<ContainerType<?>> event) {
        event.getRegistry().register(CraftPriorityStepContainer.TYPE);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(ClientSetup::init);
    }
}
