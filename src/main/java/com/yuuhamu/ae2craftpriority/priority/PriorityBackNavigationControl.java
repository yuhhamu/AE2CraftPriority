package com.yuuhamu.ae2craftpriority.priority;

/**
 * バニラの{@code PriorityContainer}(優先度編集画面)に、README方法3(端末のCrafting Statusタブ
 * 経由)で開いた場合専用の「戻る」アクションを追加するためのマーカーインターフェース。
 *
 * <p>{@code PriorityContainerMixin}が実装する。実際の呼び出しは{@code PriorityScreenMixin}が
 * (方法3経由と判明した場合にのみ)「戻る」ボタンへ差し替える新しい押下アクションから行う。</p>
 *
 * <p>バニラの{@code AESubScreen#goBack()}は常に{@code SwitchGuisPacket(previousContainerType)}
 * のみを送る設計で、これはサーバー側で「現在開いているContainer自身のContainerLocator」を
 * そのまま再利用して新しいContainerを開く仕組みになっている。方法3の場合、優先度画面
 * ({@code PriorityContainer})のロケータは(端末ではなく)代表{@code CraftingTileEntity}を
 * 指しているため、このロケータのまま{@code CraftingStatusContainer}を開こうとしても正しく
 * 解決できない(端末=ITerminalHostを期待する{@code CraftingStatusContainer}に、ブロック
 * エンティティのロケータを渡すことになるため)。この専用アクションは、
 * {@link PriorityReturnTarget}に記録しておいた「正しい戻り先(端末自身のロケータ)」を使って
 * {@code ContainerOpener.openContainer(...)}を直接呼び出すことで、この制約を回避する。</p>
 */
public interface PriorityBackNavigationControl {

    void ae2cp$returnToCraftingStatus();
}
