package com.yuuhamu.ae2craftpriority.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.tileentity.TileEntity;

import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;

/**
 * 他Modから優先度を読み書きするための公開API。
 *
 * <p>クラフトCPU(AE2純正の{@code CraftingCPUCluster}や、その{@code ICraftingCPU}実装)の優先度は、
 * 初期値0・値が大きいほど優先度が高い整数として扱う。詳細な意味づけはREADME.md/DEVELOPMENT.mdを参照。</p>
 *
 * <p>バニラの{@code CraftingCPUCluster}は最初から対応済み。独自のクラフトCPU実装を追加するModへ
 * 対応させたい場合は、{@link PriorityAdapter}を実装して{@link #registerAdapter(PriorityAdapter)}で
 * 登録する。優先度が不明なオブジェクトを渡した場合は既定値0が返る(例外は投げない)。</p>
 */
public final class CraftPriorityApi {

    private static final List<PriorityAdapter> ADAPTERS = new CopyOnWriteArrayList<PriorityAdapter>();

    private CraftPriorityApi() {
    }

    /**
     * 対象オブジェクトの現在の優先度を取得する。対応するアダプタが無い場合は0。
     */
    public static int getPriority(Object target) {
        if (target instanceof PriorityHolder) {
            return ((PriorityHolder) target).ae2cp$getPriority();
        }
        for (PriorityAdapter adapter : ADAPTERS) {
            if (adapter.supports(target)) {
                return adapter.getPriority(target);
            }
        }
        return PriorityHolder.DEFAULT_PRIORITY;
    }

    /**
     * 対象オブジェクトの優先度を設定する。対応するアダプタが無い場合は何もしない。
     */
    public static void setPriority(Object target, int priority) {
        if (target instanceof PriorityHolder) {
            ((PriorityHolder) target).ae2cp$setPriority(priority);
            return;
        }
        for (PriorityAdapter adapter : ADAPTERS) {
            if (adapter.supports(target)) {
                adapter.setPriority(target, priority);
                return;
            }
        }
    }

    /**
     * このオブジェクトの優先度を本Modが把握できるかどうか(バニラ対応・アダプタ経由のいずれか)。
     */
    public static boolean isSupported(Object target) {
        if (target instanceof PriorityHolder) {
            return true;
        }
        for (PriorityAdapter adapter : ADAPTERS) {
            if (adapter.supports(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 対象オブジェクトの優先度編集画面を開くための代表ブロックエンティティを取得する。
     * 対応するアダプタが無い、またはそのアダプタがこの仕組みに未対応の場合は{@code null}。
     */
    public static TileEntity getPriorityHostBlockEntity(Object target) {
        for (PriorityAdapter adapter : ADAPTERS) {
            if (adapter.supports(target)) {
                return adapter.getPriorityHostBlockEntity(target);
            }
        }
        return null;
    }

    /**
     * {@link #getPriorityHostBlockEntity(Object)}で得たブロックエンティティを使って
     * {@code PriorityContainer}を開く直前に呼ぶ。対応するアダプタが無い場合は何もしない。
     */
    public static void prepareForPriorityEdit(Object target, TileEntity host) {
        for (PriorityAdapter adapter : ADAPTERS) {
            if (adapter.supports(target)) {
                adapter.prepareForPriorityEdit(target, host);
                return;
            }
        }
    }

    /**
     * 新しい種類のクラフトCPU/クラスタに対応するアダプタを登録する。
     *
     * <p>Mod初期化のできるだけ早い段階(推奨: コンストラクタ、またはFMLCommonSetupEvent)で
     * 呼び出すこと。同じオブジェクトを複数のアダプタが{@code supports}した場合、先に登録された
     * 方が優先される。</p>
     */
    public static void registerAdapter(PriorityAdapter adapter) {
        ADAPTERS.add(adapter);
    }
}
