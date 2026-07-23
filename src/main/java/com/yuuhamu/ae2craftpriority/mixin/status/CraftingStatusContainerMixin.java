package com.yuuhamu.ae2craftpriority.mixin.status;

import java.util.Iterator;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.yuuhamu.ae2craftpriority.api.CraftPriorityApi;
import com.yuuhamu.ae2craftpriority.mixin.accessor.AEBaseContainerAccessor;
import com.yuuhamu.ae2craftpriority.mixin.accessor.CraftingCPURecordAccessor;
import com.yuuhamu.ae2craftpriority.priority.CraftingStatusPriorityControl;
import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;
import com.yuuhamu.ae2craftpriority.priority.PriorityReturnTarget;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.storage.ITerminalHost;
import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.container.guisync.GuiSync;
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
 * 実際に正しく戻るための遷移ロジックは、優先度画面側({@code PriorityScreenMixin}/
 * {@code PriorityContainerMixin})で別途対応する。</p>
 */
@Mixin(value = CraftingStatusContainer.class, remap = false)
public abstract class CraftingStatusContainerMixin implements CraftingStatusPriorityControl {

    private static final String ACTION_OPEN_PRIORITY = "ae2cp$openPrioritySettings";

    @Unique
    private CraftingCPUCluster ae2cp$currentCpu;

    /**
     * 現在追跡中のCPUの優先度。{@code appeng.container.guisync.DataSynchronization}が
     * リフレクションで{@code CraftingStatusContainer}(Mixin適用後の実クラス)の全フィールドを
     * 走査して{@code @GuiSync}アノテーションを見つける仕組みのため、Mixinで追加したこの
     * フィールドも他のAE2純正フィールド(noCPU=6, cpuName=7)と全く同じようにクライアントへ
     * 自動同期される(追加のパケット実装は不要)。IDは6・7と衝突しない8を使用。
     */
    @Unique
    @GuiSync(8)
    public int ae2cp$priority = PriorityHolder.DEFAULT_PRIORITY;

    /** {@code getLocator()}/{@code getPlayerInventory()}/{@code isClient()}/
     * {@code registerClientAction()}/{@code sendClientAction()}はいずれも
     * {@code CraftingStatusContainer}(および親の{@code CraftingCPUContainer})自身ではなく、
     * さらに上の{@code AEBaseContainer}が直接宣言しているため@Shadow不可。
     * {@link AEBaseContainerAccessor}経由で呼び出す(詳細は
     * {@code Knowledge/mixin-shadow-cannot-target-inherited-methods.md}参照)。 */

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2cp$registerAction(int id, PlayerInventory ip, ITerminalHost te, CallbackInfo ci) {
        ((AEBaseContainerAccessor) (Object) this).ae2cp$invokeRegisterClientAction(ACTION_OPEN_PRIORITY,
                this::ae2cp$openPrioritySettings);
    }

    /**
     * 1.16.5実ソース68行目、{@code CraftingStatusContainer}自身が宣言する唯一の「表示中CPUが
     * 切り替わった」通知箇所。{@code cpuRecord}がnullの場合は選択解除(稼働中CPUが無くなった)。
     */
    @Inject(method = "onCPUSelectionChanged", at = @At("HEAD"))
    private void ae2cp$captureCurrentCpu(CraftingCPURecord cpuRecord, boolean cpusAvailable, CallbackInfo ci) {
        final ICraftingCPU cpu = cpuRecord != null
                ? ((CraftingCPURecordAccessor) (Object) cpuRecord).ae2cp$invokeGetCpu()
                : null;
        this.ae2cp$currentCpu = cpu instanceof CraftingCPUCluster ? (CraftingCPUCluster) cpu : null;
        this.ae2cp$priority = CraftPriorityApi.getPriority(this.ae2cp$currentCpu);
    }

    @Override
    public int ae2cp$getPriority() {
        return this.ae2cp$priority;
    }

    @Override
    public void ae2cp$openPrioritySettings() {
        final AEBaseContainerAccessor accessor = (AEBaseContainerAccessor) (Object) this;
        if (accessor.ae2cp$invokeIsClient()) {
            final PlayerEntity localPlayer = accessor.ae2cp$invokeGetPlayerInventory().player;
            if (localPlayer != null) {
                // PriorityScreen構築(クライアント側)時にCraftingTileEntityMixin#getContainerType()
                // が読み取れるよう、サーバー往復を待たずここで先に記録しておく。
                PriorityReturnTarget.set(localPlayer.getUniqueID(), CraftingStatusContainer.TYPE,
                        accessor.ae2cp$invokeGetLocator());
            }
            accessor.ae2cp$invokeSendClientAction(ACTION_OPEN_PRIORITY);
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

        final PlayerEntity player = accessor.ae2cp$invokeGetPlayerInventory().player;
        if (!(player instanceof ServerPlayerEntity)) {
            return;
        }

        // サーバー側でも同じ戻り先を独立に記録する(クライアント側の記録とは別プロセス/別インスタンス)。
        PriorityReturnTarget.set(player.getUniqueID(), CraftingStatusContainer.TYPE,
                accessor.ae2cp$invokeGetLocator());

        final ContainerLocator locator = ContainerLocator.forTileEntity(representative);
        ContainerOpener.openContainer(PriorityContainer.TYPE, player, locator);
    }
}
