package com.yuuhamu.ae2craftpriority.mixin.status;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.yuuhamu.ae2craftpriority.priority.CraftingStatusPriorityControl;
import com.yuuhamu.ae2craftpriority.priority.PriorityReturnTarget;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import appeng.container.implementations.PriorityContainer;
import appeng.container.me.crafting.CraftingCPUContainer;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.SwitchGuisPacket;

/**
 * README方法2(単体Crafting CPU画面)・方法3(端末のCrafting Statusタブ)の両方で使われる
 * {@code CraftingCPUScreen<T extends CraftingCPUContainer>}に、優先度編集画面を開くレンチ
 * アイコンボタンを追加する。{@code CraftingStatusContainer}は{@code CraftingCPUContainer}を
 * 継承しているため、この1つのScreenクラス・Mixinで両方をカバーできる(1.16.5実ソースで確認済み)。
 *
 * <p>{@code this.container}が{@link CraftingStatusPriorityControl}を実装している場合(=方法3、
 * 端末のCrafting Statusタブ)は、そちらが提供する専用アクション
 * ({@code ae2cp$openPrioritySettings()})経由で「現在追跡中のCPU」の優先度画面を開く。
 * そうでない場合(=方法2、Crafting CPUブロックを直接右クリック)は、バニラの
 * {@code SwitchGuisPacket(PriorityContainer.TYPE)}をそのまま送る(現在開いているContainerの
 * ロケータがそのままCraftingTileEntityを指しているため、これで正しく解決できる)。</p>
 */
@Mixin(value = CraftingCPUScreen.class, remap = false)
public abstract class CraftingCPUScreenMixin extends AEBaseScreen<CraftingCPUContainer> {

    // Mixinの都合上のダミーコンストラクタ。実際にインスタンス化されることはない。
    private CraftingCPUScreenMixin(CraftingCPUContainer container, PlayerInventory playerInventory,
            ITextComponent title, ScreenStyle style) {
        super(container, playerInventory, title, style);
    }

    @Shadow
    @Final
    private Button cancel;

    @Unique
    private IconButton ae2cp$priorityButton;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2cp$onInit(CraftingCPUContainer container, PlayerInventory playerInventory,
            ITextComponent title, ScreenStyle style, CallbackInfo ci) {
        this.ae2cp$priorityButton = new IconButton(btn -> this.ae2cp$openPriority()) {
            @Override
            protected Icon getIcon() {
                return Icon.WRENCH;
            }
        };
        this.ae2cp$priorityButton.setMessage(GuiText.Priority.text());
        this.addButton(this.ae2cp$priorityButton);
    }

    @Unique
    private void ae2cp$openPriority() {
        if (this.container instanceof CraftingStatusPriorityControl) {
            ((CraftingStatusPriorityControl) this.container).ae2cp$openPrioritySettings();
            return;
        }

        // 方法2: 前回のCrafting Statusタブ(方法3)由来の戻り先記録が残っていれば破棄する。
        // ここでのアクセスは常に「直接右クリックで開いたCPU画面」であり、現在開いている
        // Container(CraftingCPUContainer)自身のロケータでPriorityContainerが正しく解決できる。
        if (this.minecraft != null && this.minecraft.player != null) {
            PriorityReturnTarget.clear(this.minecraft.player.getUniqueID());
        }
        NetworkHandler.instance().sendToServer(new SwitchGuisPacket(PriorityContainer.TYPE));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (this.ae2cp$priorityButton == null) {
            return;
        }
        this.ae2cp$priorityButton.x = this.cancel.x - 4 - 16;
        this.ae2cp$priorityButton.y = this.cancel.y + (this.cancel.height - 16) / 2;
    }
}
