package com.yuuhamu.ae2craftpriority.client;

import com.yuuhamu.ae2craftpriority.menu.CraftPriorityStepMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.NumberEntryWidget;

public class CraftPriorityStepScreen extends AEBaseScreen<CraftPriorityStepMenu> {

    private final NumberEntryWidget priority;
    private final Button nextButton;

    public CraftPriorityStepScreen(CraftPriorityStepMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);
        AESubScreen.addBackButton(menu, "back", widgets);

        this.priority = widgets.addNumberEntryWidget("priority", NumberEntryType.UNITLESS);
        this.priority.setTextFieldStyle(style.getWidget("priorityInput"));
        this.priority.setMinValue(Integer.MIN_VALUE);
        this.priority.setLongValue(menu.getPriorityValue());
        this.priority.setOnConfirm(this::confirm);

        this.nextButton = new Button(0, 0, 50, 20, new TranslatableComponent("gui.ae2craftpriority.next"),
                b -> confirm());

        setTextContent("priority_insertion_hint",
                new TranslatableComponent("gui.ae2craftpriority.priority_hint_high"));
        setTextHidden("priority_extraction_hint", true);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(this.nextButton);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.nextButton.x = this.leftPos + this.imageWidth - 4 - this.nextButton.getWidth();
        this.nextButton.y = this.topPos + this.imageHeight - 4 - this.nextButton.getHeight();
    }

    private void confirm() {
        var value = this.priority.getIntValue();
        if (value.isPresent()) {
            this.menu.confirmPriority(value.getAsInt());
        }
    }
}
