package com.yuuhamu.ae2craftpriority.menu;

import java.util.concurrent.Future;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ContainerType;

import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.container.implementations.ContainerTypeBuilder;
import appeng.container.me.crafting.CraftAmountContainer;
import appeng.container.me.crafting.CraftConfirmContainer;

/**
 * README記載の「方法1」(個数を確定すると優先度画面が開く)を実現するための、
 * クラフト開始フロー専用の優先度入力画面。
 *
 * <p>AE2バニラの{@code PriorityContainer}(ストレージバス等が使う汎用の優先度画面)とは
 * 別の、専用のContainer/Screenペアとして実装している。1.16.5にはAE2 1.18.2/1.20.1のような
 * {@code ISubMenuHost}/{@code ISubMenu}という「サブ画面から元の画面へ戻る」統一抽象化が
 * 存在しないため(1.16.5実ソースで確認済み)、「戻る」「次へ」の遷移はこのクラス自身が
 * 明示的に処理する。</p>
 *
 * <p>1.16.5の{@code CraftAmountContainer#confirm()}は、{@code CraftConfirmContainer}を開く
 * *前*にジョブ計算(Future)を完了させる設計(1.18.2/1.20.1は逆に、先に{@code CraftConfirmContainer}
 * を開いてから{@code planJob()}で計算する設計)であるため、このContainerは既に計算済みの
 * {@link Future}をそのまま保持し、優先度確定後に{@code CraftConfirmContainer}へ引き渡す。</p>
 */
public class CraftPriorityStepContainer extends AEBaseContainer {

    private static final String ACTION_CONFIRM = "ae2cp$confirmPriority";
    private static final String ACTION_BACK = "ae2cp$back";

    /**
     * {@code open(...)}呼び出しからこの画面を開く直前に渡された初期優先度を、
     * {@link ContainerTypeBuilder#withInitialData}のシリアライザへ橋渡しするための一時変数。
     * このContainerのホスト({@code ITerminalHost})自体は優先度を持たないため、初期値を
     * Container構築前にクライアントへ渡す手段が無く、この静的フィールド経由で受け渡す。
     * {@code ContainerOpener.openContainer(...)}呼び出しからシリアライザ実行までは同一スレッド・
     * 同一呼び出しの中で完結するため、ThreadLocalを使うまでもない(1.18.2版と同じ設計)。
     */
    private static int pendingInitialPriority = PriorityHolder.DEFAULT_PRIORITY;

    public static final ContainerType<CraftPriorityStepContainer> TYPE = ContainerTypeBuilder
            .create(CraftPriorityStepContainer::new, ITerminalHost.class)
            .requirePermission(SecurityPermissions.CRAFT)
            .withInitialData(
                    (host, buffer) -> buffer.writeVarInt(pendingInitialPriority),
                    (host, container, buffer) -> container.priorityValue = buffer.readVarInt())
            .build("ae2craftpriority_craft_priority_step");

    private IAEItemStack whatToCreate;
    private Future<ICraftingJob> job;
    private boolean autoStart;
    private int priorityValue = PriorityHolder.DEFAULT_PRIORITY;

    public CraftPriorityStepContainer(int id, PlayerInventory ip, ITerminalHost host) {
        super(TYPE, id, ip, host);
        registerClientAction(ACTION_CONFIRM, Integer.class, this::confirmPriority);
        registerClientAction(ACTION_BACK, this::goBack);
    }

    /** クライアント側の画面初期表示用。サーバー側では未使用(常に既定値のまま)。 */
    public int getPriorityValue() {
        return this.priorityValue;
    }

    /**
     * @param job 既に{@code cg.beginCraftingJob(...)}で開始済みのジョブ計算。優先度確定後、
     *            そのまま{@code CraftConfirmContainer}へ引き渡す。
     */
    public static void open(ServerPlayerEntity player, ContainerLocator locator, IAEItemStack whatToCreate,
            Future<ICraftingJob> job, boolean autoStart, int initialPriority) {
        pendingInitialPriority = initialPriority;
        ContainerOpener.openContainer(TYPE, player, locator);
        if (player.openContainer instanceof CraftPriorityStepContainer) {
            CraftPriorityStepContainer container = (CraftPriorityStepContainer) player.openContainer;
            container.whatToCreate = whatToCreate;
            container.job = job;
            container.autoStart = autoStart;
        } else if (job != null) {
            job.cancel(true);
        }
    }

    public void confirmPriority(int priority) {
        if (isClient()) {
            sendClientAction(ACTION_CONFIRM, Integer.valueOf(priority));
            return;
        }

        ContainerLocator locator = getLocator();
        if (locator == null || this.whatToCreate == null) {
            return;
        }

        // 先にローカル変数へ退避しnullへ落としておく。ContainerOpener.openContainer(...)は
        // このContainerを閉じる副作用(onContainerClosed経由のジョブキャンセル)を持つため、
        // 退避しないと引き渡す前にキャンセルされてしまう。
        Future<ICraftingJob> jobToHandOff = this.job;
        this.job = null;

        PlayerEntity player = getPlayerInventory().player;
        ContainerOpener.openContainer(CraftConfirmContainer.TYPE, player, locator);
        if (player.openContainer instanceof CraftConfirmContainer) {
            CraftConfirmContainer confirm = (CraftConfirmContainer) player.openContainer;
            confirm.setAutoStart(this.autoStart);
            confirm.setItemToCreate(this.whatToCreate);
            confirm.setJob(jobToHandOff);
            PriorityHolder.setPriority(confirm, priority);
            detectAndSendChanges();
        } else if (jobToHandOff != null) {
            jobToHandOff.cancel(true);
        }
    }

    /**
     * バニラの{@code CraftConfirmContainer#goBack()}と同じ設計。要求数設定画面
     * ({@code CraftAmountContainer})へ戻る。
     */
    public void goBack() {
        PlayerEntity player = getPlayerInventory().player;
        if (player instanceof ServerPlayerEntity && this.whatToCreate != null) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            CraftAmountContainer.open(serverPlayer, getLocator(), this.whatToCreate,
                    (int) this.whatToCreate.getStackSize());
        } else if (!isServer()) {
            sendClientAction(ACTION_BACK);
        }
    }

    @Override
    public void onContainerClosed(PlayerEntity player) {
        super.onContainerClosed(player);
        // 優先度確定前(次へ/戻る どちらも押さずに画面を閉じた)場合、計算中のジョブが
        // リークしないようキャンセルする。confirmPriority()で正常に引き渡した後は
        // this.jobは既にnullになっているため二重キャンセルは起きない。
        if (this.job != null) {
            this.job.cancel(true);
            this.job = null;
        }
    }
}
