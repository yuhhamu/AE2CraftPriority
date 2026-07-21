package com.yuuhamu.ae2craftpriority.api;

import net.minecraft.tileentity.TileEntity;

/**
 * AE2CraftPriorityの優先度システムを、バニラの{@code CraftingCPUCluster}以外のオブジェクトにも
 * 対応させるための拡張点。
 *
 * <p>AE2本体の{@code CraftingCPUCluster}を継承しない独自のクラフトCPU実装を追加するModに
 * 対応する場合、このインターフェースを実装したアダプタを
 * {@link CraftPriorityApi#registerAdapter(PriorityAdapter)}で登録する。</p>
 *
 * <p>登録は早ければ早いほどよいが、少なくとも実際に優先度の読み書きが行われるより前
 * (Mod初期化時点)に済ませること。</p>
 */
public interface PriorityAdapter {

    /**
     * このアダプタが対象オブジェクトの優先度を扱えるかどうか。
     */
    boolean supports(Object target);

    /**
     * {@link #supports(Object)}が{@code true}を返したオブジェクトから優先度を取得する。
     */
    int getPriority(Object target);

    /**
     * {@link #supports(Object)}が{@code true}を返したオブジェクトへ優先度を設定する。
     */
    void setPriority(Object target, int priority);

    /**
     * 優先度編集画面(AE2バニラの{@code PriorityContainer}/{@code IPriorityHost})を開くための
     * 代表ブロックエンティティを1つ返す。バニラの{@code CraftingCPUCluster}はこの仕組みを
     * 経由せず{@code CraftingStatusContainerMixin}側で直接処理されるため、この既定実装
     * (常に{@code null})のままでよいアダプタも多い。対応する場合は、返すブロックエンティティが
     * {@code appeng.helpers.IPriorityHost}を実装していること(対応するMixinで実装しておくこと)。
     */
    default TileEntity getPriorityHostBlockEntity(Object target) {
        return null;
    }

    /**
     * {@link #getPriorityHostBlockEntity(Object)}が返したブロックエンティティで
     * {@code PriorityContainer}(AE2バニラ、{@code IPriorityHost})を開く直前に呼ばれる。
     * 既定実装は何もしない(1ブロック=1タスクのアダプタでは不要)。
     */
    default void prepareForPriorityEdit(Object target, TileEntity host) {
    }

}
