package com.yuuhamu.ae2craftpriority.mixin.accessor;

import net.minecraft.entity.player.PlayerInventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import appeng.container.AEBaseContainer;
import appeng.container.ContainerLocator;

/**
 * {@code appeng.container.AEBaseContainer}が直接宣言する({@code CraftAmountContainer}や
 * {@code CraftConfirmContainer}、{@code CraftingCPUContainer}/{@code CraftingStatusContainer}
 * 自身は継承しているだけで宣言していない)メンバーへの{@code @Invoker}ブリッジ。
 *
 * <p>SpongePowered Mixinの{@code @Shadow}は、Mixin適用対象クラス自身が直接宣言するメンバーしか
 * 解決できず、継承のみのメンバーを対象クラスと異なるMixinから直接{@code @Shadow}すると実機起動時に
 * {@code InvalidMixinException}になる(詳細は
 * {@code Knowledge/mixin-shadow-cannot-target-inherited-methods.md}参照)。このアクセサは、
 * {@code AEBaseContainer}を継承する各Container向けMixin(`CraftAmountContainerMixin`・
 * `CraftConfirmContainerMixin`・`CraftingStatusContainerMixin`等)から共通で使う橋渡し役。</p>
 */
@Mixin(value = AEBaseContainer.class, remap = false)
public interface AEBaseContainerAccessor {

    @Invoker("isClient")
    boolean ae2cp$invokeIsClient();

    @Invoker("isServer")
    boolean ae2cp$invokeIsServer();

    @Invoker("getTarget")
    Object ae2cp$invokeGetTarget();

    @Invoker("getLocator")
    ContainerLocator ae2cp$invokeGetLocator();

    @Invoker("getPlayerInventory")
    PlayerInventory ae2cp$invokeGetPlayerInventory();

    @Invoker("registerClientAction")
    void ae2cp$invokeRegisterClientAction(String name, Runnable callback);

    @Invoker("sendClientAction")
    void ae2cp$invokeSendClientAction(String action);
}
