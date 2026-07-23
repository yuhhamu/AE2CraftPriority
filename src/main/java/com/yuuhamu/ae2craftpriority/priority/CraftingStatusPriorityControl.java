package com.yuuhamu.ae2craftpriority.priority;

/**
 * {@code CraftingStatusContainer}(端末のCrafting Statusタブ)が実装するインターフェース。
 * レンチボタン押下時に、汎用の{@code SwitchGuisPacket}経路ではなく、現在選択中のCPUに対応する
 * 優先度編集画面を明示的に開くための専用クライアントアクションを提供する。
 */
public interface CraftingStatusPriorityControl {

    void ae2cp$openPrioritySettings();

    /**
     * 現在追跡中のCrafting CPUの優先度(未選択時は{@link PriorityHolder#DEFAULT_PRIORITY}）。
     * クライアント側の{@code CraftingStatusScreenMixin}が「CPU: #1@{優先度}」表示のために使う。
     * サーバー側で{@code @GuiSync}フィールドとして自動同期される値をそのまま返す。
     */
    int ae2cp$getPriority();
}
