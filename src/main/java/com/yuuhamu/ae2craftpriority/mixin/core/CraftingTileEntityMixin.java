package com.yuuhamu.ae2craftpriority.mixin.core;

import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.yuuhamu.ae2craftpriority.client.PriorityReturnTargetClient;
import com.yuuhamu.ae2craftpriority.priority.CraftingPriorityHostMarker;
import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;

import appeng.container.me.crafting.CraftingCPUContainer;
import appeng.helpers.IPriorityHost;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.tile.crafting.CraftingTileEntity;

/**
 * README方法2(Crafting CPUのブロックを直接右クリック)で、AE2純正の{@code WidgetContainer}の
 * レンチボタン({@code addOpenPriorityButton()})からバニラの優先度画面({@code PriorityContainer}、
 * {@code ContainerTypeBuilder.create(PriorityContainer::new, IPriorityHost.class)})を開けるように、
 * {@code CraftingTileEntity}へ{@link IPriorityHost}を実装させる。
 *
 * <p>優先度の実体は{@code CraftingCPUCluster}側(①{@code CraftingCPUClusterMixin})が保持しており、
 * このMixinは{@link CraftingTileEntity#getCluster()}(直接宣言・public、212行目)経由で単に委譲する。
 * クラスタがまだ形成されていない({@code getCluster() == null})間は既定値0を返し、setPriorityは
 * 何もしない(バニラの{@code IPriorityHost}実装各所も同様にnullガードしている)。</p>
 *
 * <p>{@link #getContainerType()}は、優先度画面から「戻る」ボタンの遷移先を決めるための値
 * (AE2純正{@code AESubScreen}のコンストラクタが呼ぶ、クライアント側専用の呼び出し — 1.16.5実
 * ソースで他の用途が無いことを確認済み)。{@link PriorityReturnTargetClient}にクライアント側で
 * 記録済みの戻り先があればそれを優先し(README方法3、端末のCrafting Statusタブ経由)、無ければ
 * 既定で{@code CraftingCPUContainer.TYPE}(方法2、右クリックで直接開いたCPU画面)を返す。</p>
 *
 * <p><b>2026-07-22発見・修正:</b> このMixinは{@code ae2craftpriority.mixins.json}の共通
 * ("mixins")配列に登録されており、Dedicated Serverでも適用される。以前は本メソッドが
 * {@code World#isRemote}ガードの内側とはいえ{@code net.minecraft.client.Minecraft}を直接
 * 参照していたため、Mixin(SpongePowered Mixin 0.8.2)のバイトコード前処理が
 * {@code ClassMetadataNotFoundException: net.minecraft.client.Minecraft}を投げてサーバー
 * 起動がクラッシュしていた(実行時にその分岐へ到達するかどうかに関わらず、Mixin対象クラス
 * へマージされるメソッド自身のバイトコードが参照する型は前処理時に解決されてしまうため)。
 * {@code Minecraft}への参照は{@link PriorityReturnTargetClient}という独立した非Mixinクラス
 * (通常のJVM遅延クラスロードに従う)へ切り出し、このMixinクラス自身のバイトコードからは
 * {@code Minecraft}型への参照を完全に除去することで解決した。</p>
 */
@Mixin(value = CraftingTileEntity.class, remap = false)
public abstract class CraftingTileEntityMixin implements IPriorityHost, CraftingPriorityHostMarker {

    @Shadow
    public abstract CraftingCPUCluster getCluster();

    @Shadow
    protected abstract ItemStack getItemFromTile(Object obj);

    /** {@code CraftingTileEntity}自身は{@code getWorld()}を宣言していない
     * ({@code CraftingTileEntity} → {@code AENetworkTileEntity} → {@code AEBaseTileEntity} →
     * {@code net.minecraft.tileentity.TileEntity}の3階層上で宣言・javapで確認済み)。
     * このMixin構成({@code remap = false})の{@code @Shadow}は、Mixin適用対象クラス自身が
     * 直接宣言するメンバーしか解決できず、継承のみのメンバーは
     * {@code InvalidMixinException}になる(本プロジェクトの{@code CraftingCPUContainerMixin}の
     * コメントにある「1.20.1版で発生したCraftingCPUMenuMixin新設と同一の罠」と同種の問題)。
     * {@code getWorld()}は{@code TileEntity}上でpublic宣言されているため、@Shadowを使わず
     * {@code Object}経由の安全なダウンキャストで直接呼び出す(クライアント/サーバー判定は
     * {@code World#isRemote}publicフィールド経由)。 */
    private World ae2cp$getWorld() {
        return ((TileEntity) (Object) this).getWorld();
    }

    @Override
    public int getPriority() {
        final CraftingCPUCluster cluster = this.getCluster();
        return cluster != null ? PriorityHolder.getPriorityOrDefault(cluster) : PriorityHolder.DEFAULT_PRIORITY;
    }

    @Override
    public void setPriority(int newValue) {
        final CraftingCPUCluster cluster = this.getCluster();
        if (cluster != null) {
            PriorityHolder.setPriority(cluster, newValue);
        }
    }

    @Override
    public ItemStack getItemStackRepresentation() {
        return this.getItemFromTile(this);
    }

    @Override
    public ContainerType<?> getContainerType() {
        // 1.16.5実ソース上、IPriorityHost#getContainerType()の唯一の呼び出し元はAESubScreenの
        // コンストラクタ(appeng.client.gui.implementations.AESubScreen)であり、これは常に
        // クライアント側のPriorityScreen構築時にのみ呼ばれる(サーバー側のSwitchGuisPacket
        // ハンドラは「現在開いているContainer自身のロケータ」を再利用するのみで、
        // getContainerType()自体は参照しない)。そのためクライアント専用の戻り先解決は
        // PriorityReturnTargetClient(このMixinとは別の非Mixinクラス)へ完全に委譲し、
        // このメソッド自身のバイトコードにはnet.minecraft.client.Minecraftへの参照を
        // 一切含めない(理由は上記クラスコメント、および本Mixinのクラスコメント参照)。
        if (this.ae2cp$getWorld().isRemote) {
            final ContainerType<?> clientTarget = PriorityReturnTargetClient.peekReturnContainerType();
            if (clientTarget != null) {
                return clientTarget;
            }
        }
        return CraftingCPUContainer.TYPE;
    }
}
