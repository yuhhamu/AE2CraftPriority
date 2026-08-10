package com.yuuhamu.ae2craftpriority.client;

import com.yuuhamu.ae2craftpriority.menu.CraftPriorityStepMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.NumberEntryWidget;

public class CraftPriorityStepScreen extends AEBaseScreen<CraftPriorityStepMenu> {

    private final NumberEntryWidget priority;

    public CraftPriorityStepScreen(CraftPriorityStepMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.priority = widgets.addNumberEntryWidget("priority", NumberEntryType.UNITLESS);
        this.priority.setTextFieldStyle(style.getWidget("priorityInput"));
        this.priority.setMinValue(Integer.MIN_VALUE);
        this.priority.setLongValue(menu.getPriorityValue());
        this.priority.setOnConfirm(this::confirm);

        // Native AE2 buttons: widgets.addButton(...) constructs an appeng.client.gui.widgets.AE2Button,
        // which renders using AE2's own texture atlas (ae2:widget/button, ae2:widget/button_highlighted,
        // ae2:widget/button_disabled) and has built-in auto-scroll overflow handling for labels wider
        // than the button - unlike a vanilla Button. This replaces the earlier vanilla-Button-based
        // CompactTextButton shrink-to-fit hack, which never matched AE2's native visual style.
        // Position and size are resolved automatically from this screen's style JSON ("next"/"cancel"
        // widget entries in assets/ae2/screens/ae2craftpriority_priority.json) by WidgetContainer.add(...),
        // so no manual updateBeforeRender() positioning override is needed.
        widgets.addButton("next", Component.translatable("gui.ae2craftpriority.next"), btn -> confirm());
        widgets.addButton("cancel", Component.translatable("gui.ae2craftpriority.back"), btn -> back());

        setTextContent("priority_insertion_hint",
                Component.translatable("gui.ae2craftpriority.priority_hint_high"));
        setTextHidden("priority_extraction_hint", true);
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
