package com.yuuhamu.ae2craftpriority.mixin.core;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.yuuhamu.ae2craftpriority.priority.CraftingPriorityHostMarker;
import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;
import com.yuuhamu.ae2craftpriority.priority.PriorityReturnTarget;

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
 * ソースで他の用途が無いことを確認済み)。{@link PriorityReturnTarget}にクライアント側で記録済みの
 * 戻り先があればそれを優先し(README方法3、端末のCrafting Statusタブ経由)、無ければ既定で
 * {@code CraftingCPUContainer.TYPE}(方法2、右クリックで直接開いたCPU画面)を返す。</p>
 */
@Mixin(value = CraftingTileEntity.class, remap = false)
public abstract class CraftingTileEntityMixin implements IPriorityHost, CraftingPriorityHostMarker {

    @Shadow
    public abstract CraftingCPUCluster getCluster();

    @Shadow
    protected abstract ItemStack getItemFromTile(Object obj);

    @Shadow
    public abstract boolean isRemote();

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
        // getContainerType()自体は参照しない)。そのためMinecraft.getInstance()への参照は
        // isRemote()ガードの内側でのみ実行され、サーバー専用環境でこのメソッドが呼ばれることは
        // 無い前提で安全(サーバー専用jarでも、この行はクラス検証時ではなく実際に呼ばれたときに
        // 初めてリンクされるため、呼ばれない限り問題は起きない)。
        if (this.isRemote()) {
            final PlayerEntity localPlayer = Minecraft.getInstance().player;
            if (localPlayer != null) {
                // ここではまだ消費しない(peek)。PriorityScreenMixin側が「戻る」ボタンの
                // 見た目を上書きする際に同じ記録をもう一度参照するため、最終的な消費は
                // そちらのコンストラクタ末尾(このgetContainerType()呼び出しより後)で行う。
                final PriorityReturnTarget.Target target = PriorityReturnTarget.peek(localPlayer.getUniqueID());
                if (target != null) {
                    return target.getContainerType();
                }
            }
        }
        return CraftingCPUContainer.TYPE;
    }
}
