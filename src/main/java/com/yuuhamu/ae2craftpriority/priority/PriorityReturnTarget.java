package com.yuuhamu.ae2craftpriority.priority;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.inventory.container.ContainerType;

import appeng.container.ContainerLocator;

/**
 * 端末のCrafting Statusタブ(方法3)から優先度編集画面を開いたとき、「戻る」で正しい画面
 * (CraftingStatusContainer)に戻れるようにするための、プレイヤーごとの一時的な戻り先記録。
 *
 * <p>1.16.5の{@code IPriorityHost#getContainerType()}はホスト(ブロックエンティティ)単位の
 * 固定値しか返せないため、「右クリックで直接CPU画面を開いた場合(方法2)」と「端末のCrafting
 * Statusタブ経由(方法3)」を区別できない。この記録がある場合はそれを優先して使う。</p>
 */
public final class PriorityReturnTarget {

    public static final class Target {
        private final ContainerType<?> containerType;
        private final ContainerLocator locator;

        public Target(ContainerType<?> containerType, ContainerLocator locator) {
            this.containerType = containerType;
            this.locator = locator;
        }

        public ContainerType<?> getContainerType() {
            return containerType;
        }

        public ContainerLocator getLocator() {
            return locator;
        }
    }

    private static final Map<UUID, Target> TARGETS = new ConcurrentHashMap<UUID, Target>();

    private PriorityReturnTarget() {
    }

    public static void set(UUID playerId, ContainerType<?> containerType, ContainerLocator locator) {
        TARGETS.put(playerId, new Target(containerType, locator));
    }

    /**
     * 削除せずに覗き見る。{@code CraftingTileEntityMixin#getContainerType()}
     * (クライアント側、{@code AESubScreen}のコンストラクタから呼ばれる)が使う。
     * この時点ではまだ消費し切らない。最終的な消費(削除)は{@code PriorityScreenMixin}の
     * コンストラクタ末尾({@code AESubScreen}構築より後)で{@link #take(UUID)}が行う。
     */
    public static Target peek(UUID playerId) {
        return TARGETS.get(playerId);
    }

    /** 記録を取り出して削除する。無ければnull。 */
    public static Target take(UUID playerId) {
        return TARGETS.remove(playerId);
    }

    public static void clear(UUID playerId) {
        TARGETS.remove(playerId);
    }
}
