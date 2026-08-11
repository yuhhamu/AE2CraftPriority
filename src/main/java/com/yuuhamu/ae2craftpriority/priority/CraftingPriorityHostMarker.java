package com.yuuhamu.ae2craftpriority.priority;

/**
 * クラフトCPUの優先度編集画面(AE2バニラの {@code PriorityMenu}/{@code IPriorityHost})を
 * 開くためのホストであることを示す共通マーカー。
 *
 * <p>バニラの {@code CraftingBlockEntity} 用({@code CraftingBlockEntityMixin})・
 * AdvancedAEの {@code AdvCraftingBlockEntity} 用({@code AdvCraftingBlockEntityMixin})の
 * どちらも {@code IPriorityHost} と併せてこのマーカーを実装する。クライアント側の
 * {@code PriorityScreenMixin} は、対応Modのクラスを直接参照できない(常時ロードされる
 * メインMixin設定から、任意依存Modのクラスを直接instanceofするとそのMod未導入環境で
 * {@code NoClassDefFoundError} になるため)、代わりにこの共通マーカーで判定する。</p>
 */
public interface CraftingPriorityHostMarker {
}
