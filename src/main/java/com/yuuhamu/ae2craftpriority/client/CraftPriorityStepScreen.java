package com.yuuhamu.ae2craftpriority.client;

import com.yuuhamu.ae2craftpriority.menu.CraftPriorityStepMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.NumberEntryWidget;

public class CraftPriorityStepScreen extends AEBaseScreen<CraftPriorityStepMenu> {

    private final NumberEntryWidget priority;
    private final IconButton nextButton;
    private final IconButton backButton;

    public CraftPriorityStepScreen(CraftPriorityStepMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);

        // NOTE: 以前はここで AESubScreen.addBackButton(menu, "back", widgets) を呼び、
        // AE2標準のISubMenu用「戻る」タブボタンを流用していたが、そのボタンは
        // ホスト(クラフトターミナル等)の本来のメインメニューへ戻る挙動
        // (SwitchGuisPacket.returnToParentMenu())になっており、
        // このMODのウィザードの前段(数量設定 = CraftAmountMenu)には戻れず、
        // かつボタンのツールチップがホストのメインメニュー名(例:クラフトターミナル)を
        // 表示してしまう問題があった。専用のbackButtonに置き換える。

        this.priority = widgets.addNumberEntryWidget("priority", NumberEntryType.UNITLESS);
        this.priority.setTextFieldStyle(style.getWidget("priorityInput"));
        this.priority.setMinValue(Integer.MIN_VALUE);
        this.priority.setLongValue(menu.getPriorityValue());
        this.priority.setOnConfirm(this::confirm);

        this.nextButton = new IconButton(b -> confirm()) {
            @Override
            protected Icon getIcon() {
                return Icon.ENTER;
            }
        };
        this.nextButton.setMessage(Component.translatable("gui.ae2craftpriority.next"));

        this.backButton = new IconButton(b -> back()) {
            @Override
            protected Icon getIcon() {
                return Icon.BACK;
            }
        };
        this.backButton.setMessage(Component.translatable("gui.ae2craftpriority.back"));

        setTextContent("priority_insertion_hint",
                Component.translatable("gui.ae2craftpriority.priority_hint_high"));
        setTextHidden("priority_extraction_hint", true);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(this.nextButton);
        addRenderableWidget(this.backButton);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.nextButton.setX(this.leftPos + this.imageWidth - 4 - this.nextButton.getWidth());
        this.nextButton.setY(this.topPos + this.imageHeight - 4 - this.nextButton.getHeight());
        this.backButton.setX(this.leftPos + 4);
        this.backButton.setY(this.topPos + this.imageHeight - 4 - this.backButton.getHeight());
    }

    private void confirm() {
        var value = this.priority.getIntValue();
        if (value.isPresent()) {
            this.menu.confirmPriority(value.getAsInt());
        }
    }

    private void back() {
        this.menu.goBack();
    }
}
