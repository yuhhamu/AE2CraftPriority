package com.yuuhamu.ae2craftpriority.priority;

/**
 * クラフトCPUの優先度編集画面(AE2バニラの{@code PriorityContainer}/{@code IPriorityHost})を
 * 開くためのホストであることを示す共通マーカー。
 *
 * <p>クライアント側の{@code PriorityScreenMixin}は、対応Modのクラスを直接参照できない
 * (常時ロードされるメインMixin設定から任意依存Modのクラスを直接instanceofすると、
 * そのMod未導入環境で{@code NoClassDefFoundError}になるため)、代わりにこの共通マーカーで
 * 判定する。{@code CraftingTileEntityMixin}がこのマーカーと{@code IPriorityHost}を
 * 併せて実装する。</p>
 */
public interface CraftingPriorityHostMarker {
}
