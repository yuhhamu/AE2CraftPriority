package com.yuuhamu.ae2craftpriority.compat.advancedae.mixin;

import com.yuuhamu.ae2craftpriority.compat.advancedae.AdvCraftingPriorityCpuHost;
import com.yuuhamu.ae2craftpriority.compat.advancedae.AdvancedAeMenuTypes;
import com.yuuhamu.ae2craftpriority.priority.CraftingPriorityHostMarker;
import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;
import com.yuuhamu.ae2craftpriority.priority.PriorityReturnTarget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import net.pedroksl.advanced_ae.common.entities.AdvCraftingBlockEntity;
import net.pedroksl.advanced_ae.gui.quantumcomputer.QuantumComputerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import appeng.helpers.IPriorityHost;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftingCPUMenu;

/**
 * AdvancedAEのQuantum Computerブロック({@code AdvCraftingBlockEntity})にAE2バニラの
 * {@code IPriorityHost} を実装させ、ブロック本体(レンチアイコンの優先度ボタン)から
 * 直接優先度設定画面({@code PriorityMenu})を開けるようにする。
 *
 * <p><b>優先度の実体は「タスク単位」({@code AdvCraftingCPU})</b>。AE2バニラの
 * {@code IPriorityHost#getPriority()}/{@code #setPriority(int)} は引数を取らないため、
 * このブロック自身に「今どのタスクの優先度を編集中か」({@link #ae2cp$activePriorityCpu})を
 * 覚えさせておく必要がある。この値は優先度画面を開く直前に {@code CraftingStatusMenuMixin} や
 * {@code QuantumComputerMenuMixin} から {@link AdvCraftingPriorityCpuHost#ae2cp$setActivePriorityCpu}
 * 経由でセットされる。</p>
 */
@Mixin(value = AdvCraftingBlockEntity.class, remap = false)
public abstract class AdvCraftingBlockEntityMixin
        implements IPriorityHost, CraftingPriorityHostMarker, AdvCraftingPriorityCpuHost {

    @Unique
    private AdvCraftingCPU ae2cp$activePriorityCpu;

    @Override
    public void ae2cp$setActivePriorityCpu(AdvCraftingCPU cpu) {
        this.ae2cp$activePriorityCpu = cpu;
    }

    @Override
    public AdvCraftingCPU ae2cp$getActivePriorityCpu() {
        return this.ae2cp$activePriorityCpu;
    }

    @Override
    public int getPriority() {
        return PriorityHolder.getPriorityOrDefault(this.ae2cp$activePriorityCpu);
    }

    @Override
    public void setPriority(int newValue) {
        if (this.ae2cp$activePriorityCpu != null) {
            PriorityHolder.setPriority(this.ae2cp$activePriorityCpu, newValue);
        }
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        var target = PriorityReturnTarget.take(player.getUUID());
        if (target != null && MenuOpener.returnTo(target.menuType(), player, target.locator())) {
            return;
        }
        // このブロックはQuantum Computer(AdvancedAE)専用であり、個別CPU画面は常に
        // QuantumComputerMenuで開かれる。バニラのCraftingCPUMenu.TYPEへ戻そうとすると
        // ホスト型不一致で失敗する("does not implement class CraftingBlockEntity")ため、
        // QuantumComputerMenuMixinが実行時にキャプチャした実際のMenuTypeへ戻す。
        MenuType<QuantumComputerMenu> quantumType = AdvancedAeMenuTypes.quantumComputerMenuTypeOrNull();
        if (quantumType != null
                && MenuOpener.returnTo(quantumType, player, MenuLocators.forBlockEntity(ae2cp$self()))) {
            return;
        }
        // フォールバック(理論上到達しないはず: QuantumComputerMenuが一度も生成されていない場合のみ)
        MenuOpener.returnTo(CraftingCPUMenu.TYPE, player, MenuLocators.forBlockEntity(ae2cp$self()));
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(ae2cp$self().getUnitBlock());
    }

    @Unique
    private AdvCraftingBlockEntity ae2cp$self() {
        return (AdvCraftingBlockEntity) (Object) this;
    }
}
