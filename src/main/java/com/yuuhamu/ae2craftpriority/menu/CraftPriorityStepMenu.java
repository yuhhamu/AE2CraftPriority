package com.yuuhamu.ae2craftpriority.menu;

import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.stacks.AEKey;
import appeng.api.storage.ISubMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.locator.MenuLocator;
import appeng.menu.me.crafting.CraftConfirmMenu;

public class CraftPriorityStepMenu extends AEBaseMenu implements ISubMenu {

    private static final String ACTION_CONFIRM = "ae2cp$confirmPriority";

    private static int pendingInitialPriority = PriorityHolder.DEFAULT_PRIORITY;

    public static final MenuType<CraftPriorityStepMenu> TYPE = MenuTypeBuilder
            .create(CraftPriorityStepMenu::new, ISubMenuHost.class)
            .withInitialData(
                    (host, buffer) -> buffer.writeVarInt(pendingInitialPriority),
                    (host, menu, buffer) -> menu.priorityValue = buffer.readVarInt())
            .build("ae2craftpriority_craft_priority");

    private final ISubMenuHost host;

    private AEKey whatToCraft;
    private int amount;
    private boolean autoStart;
    private int priorityValue = PriorityHolder.DEFAULT_PRIORITY;

    public CraftPriorityStepMenu(int id, Inventory ip, ISubMenuHost host) {
        super(TYPE, id, ip, host);
        this.host = host;
        registerClientAction(ACTION_CONFIRM, Integer.class, this::confirmPriority);
    }

    @Override
    public ISubMenuHost getHost() {
        return this.host;
    }

    public int getPriorityValue() {
        return this.priorityValue;
    }

    public static void open(ServerPlayer player, MenuLocator locator, AEKey whatToCraft, int amount,
            boolean autoStart, int initialPriority) {
        pendingInitialPriority = initialPriority;
        MenuOpener.open(TYPE, player, locator);
        if (player.containerMenu instanceof CraftPriorityStepMenu menu) {
            menu.whatToCraft = whatToCraft;
            menu.amount = amount;
            menu.autoStart = autoStart;
        }
    }

    public void confirmPriority(int priority) {
        if (isClientSide()) {
            sendClientAction(ACTION_CONFIRM, Integer.valueOf(priority));
            return;
        }

        var locator = getLocator();
        var player = getPlayer();
        if (locator == null || this.whatToCraft == null) {
            this.host.returnToMainMenu(player, this);
            return;
        }

        MenuOpener.open(CraftConfirmMenu.TYPE, player, locator);
        if (player.containerMenu instanceof CraftConfirmMenu confirm) {
            confirm.setAutoStart(this.autoStart);
            PriorityHolder.setPriority(confirm, priority);
            confirm.planJob(this.whatToCraft, this.amount, CalculationStrategy.REPORT_MISSING_ITEMS);
            broadcastChanges();
        }
    }
}
