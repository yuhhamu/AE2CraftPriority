package com.yuuhamu.ae2craftpriority.mixin.status;

import java.util.Iterator;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.yuuhamu.ae2craftpriority.priority.CraftingStatusPriorityControl;
import com.yuuhamu.ae2craftpriority.priority.PriorityReturnTarget;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.storage.ITerminalHost;
import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.container.implementations.PriorityContainer;
import appeng.container.me.crafting.CraftingCPURecord;
import appeng.container.me.crafting.CraftingStatusContainer;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.tile.crafting.CraftingTileEntity;

/**
 * 端末の「Crafting Status」タブ(README方法3)から、現在選択・追跡中のCrafting CPUの優先度を
 * 編集できるようにする。
 *
 * <p>1.16.5の{@code CraftingStatusContainer}には1.20.1の`CPUSelectionList`のような全CPU一覧は
 * 無く、{@code CraftingCPUCycler}で「稼働中のCPUだけを次へ/前へで1台ずつ切り替える」方式
 * (実ソースで確認済み)。現在表示中のCPUは自身が宣言する{@code onCPUSelectionChanged}
 * (68行目)への{@code @Inject(at = "HEAD")}で捕捉するのが、1.16.5の実際のUI遷移契機に忠実。</p>
 *
 * <p>優先度編集画面(バニラの{@code PriorityContainer})を開く際は、{@code WidgetContainer}の
 * 既存の{@code addOpenPriorityButton()}(常に{@code PriorityContainer.TYPE}を、現在開いている
 * Containerのロケータそのままで開こうとする設計)をそのまま使うことができない。端末のCrafting
 * Statusタブのロケータは端末自身を指しており、追跡中のCraftingCPUCluster(≒
 * {@code IPriorityHost}を実装する{@code CraftingTileEntity})を指してはいないためである。
 * そのため、このMixin自身が現在追跡中のCPUクラスタから代表{@code CraftingTileEntity}を1つ
 * 取り出し、そのロケータで直接{@code PriorityContainer}を開く専用のクライアントアクション
 * ({@code AEBaseContainer}の{@code registerClientAction}機構)を用意する。</p>
 *
 * <p>「戻る」でこのCrafting Statusタブへ正しく帰ってこられるようにするため、
 * {@link PriorityReturnTarget}へ戻り先(このContainer自身のTYPE・ロケータ)を記録しておく。
 * バニラの{@code SwitchGuisPacket}は常に「現在開いているContainer自身のロケータ」を再利用する
 * 設計であり、{@code PriorityContainer}を開いた時点のロケータは(端末ではなく)代表
 * {@code CraftingTileEntity}のものになってしまうため、この記録だけでは「戻る」ボタンの
 * 実際の遷移までは救えない(クライアント側の表示ラベル・アイコン決定までが本Mixinの責務)。
 * 実際に正しく戻るための遷移ロジックは、優先度画面側(Task #6の{@code PriorityScreenMixin})で
 * 別途対応する。</p>
 */
@Mixin(value = CraftingStatusContainer.class, remap = false)
public abstract class CraftingStatusContainerMixin implements CraftingStatusPriorityControl {

    private static final String ACTION_OPEN_PRIORITY = "ae2cp$openPrioritySettings";

    @Unique
    private CraftingCPUCluster ae2cp$currentCpu;

    @Shadow
    public abstract ContainerLocator getLocator();

    @Shadow
    public abstract PlayerInventory getPlayerInventory();

    @Shadow
    protected abstract boolean isClient();

    @Shadow
    protected abstract void registerClientAction(String name, Runnable callback);

    @Shadow
    protected abstract void sendClientAction(String action);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2cp$registerAction(int id, PlayerInventory ip, ITerminalHost te, CallbackInfo ci) {
        this.registerClientAction(ACTION_OPEN_PRIORITY, this::ae2cp$openPrioritySettings);
    }

    /**
     * 1.16.5実ソース68行目、{@code CraftingStatusContainer}自身が宣言する唯一の「表示中CPUが
     * 切り替わった」通知箇所。{@code cpuRecord}がnullの場合は選択解除(稼働中CPUが無くなった)。
     */
    @Inject(method = "onCPUSelectionChanged", at = @At("HEAD"))
    private void ae2cp$captureCurrentCpu(CraftingCPURecord cpuRecord, boolean cpusAvailable, CallbackInfo ci) {
        final ICraftingCPU cpu = cpuRecord != null ? cpuRecord.getCpu() : null;
        this.ae2cp$currentCpu = cpu instanceof CraftingCPUCluster ? (CraftingCPUCluster) cpu : null;
    }

    @Override
    public void ae2cp$openPrioritySettings() {
        if (this.isClient()) {
            final PlayerEntity localPlayer = this.getPlayerInventory().player;
            if (localPlayer != null) {
                // PriorityScreen構築(クライアント側)時にCraftingTileEntityMixin#getContainerType()
                // が読み取れるよう、サーバー往復を待たずここで先に記録しておく。
                PriorityReturnTarget.set(localPlayer.getUniqueID(), CraftingStatusContainer.TYPE,
                        this.getLocator());
            }
            this.sendClientAction(ACTION_OPEN_PRIORITY);
            return;
        }

        if (this.ae2cp$currentCpu == null) {
            return;
        }

        CraftingTileEntity representative = null;
        final Iterator<CraftingTileEntity> tiles = this.ae2cp$currentCpu.getTiles();
        if (tiles.hasNext()) {
            representative = tiles.next();
        }
        if (representative == null) {
            return;
        }

        final PlayerEntity player = this.getPlayerInventory().player;
        if (!(player instanceof ServerPlayerEntity)) {
            return;
        }

        // サーバー側でも同じ戻り先を独立に記録する(クライアント側の記録とは別プロセス/別インスタンス)。
        PriorityReturnTarget.set(player.getUniqueID(), CraftingStatusContainer.TYPE, this.getLocator());

        final ContainerLocator locator = ContainerLocator.forTileEntity(representative);
        ContainerOpener.openContainer(PriorityContainer.TYPE, player, locator);
    }
}
