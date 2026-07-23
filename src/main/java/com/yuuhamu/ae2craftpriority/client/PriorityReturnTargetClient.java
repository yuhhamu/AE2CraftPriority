package com.yuuhamu.ae2craftpriority.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ContainerType;

import com.yuuhamu.ae2craftpriority.priority.PriorityReturnTarget;

/**
 * net.minecraft.client.Minecraft への参照を専用のクライアント側クラスへ隔離するための
 * ヘルパー。
 *
 * 2026-07-22発見のバグ: 以前は CraftingTileEntityMixin (共通/サーバーでも適用される
 * Mixin、ae2craftpriority.mixins.json の "mixins" 配列) の getContainerType() 内で
 * World#isRemote ガードの内側から直接 Minecraft.getInstance() を呼んでいたが、
 * Dedicated Server では net.minecraft.client.Minecraft クラス自体がクラスパス上に
 * 存在しない。Mixin (SpongePowered Mixin 0.8.2) は Mixin クラス自身のバイトコードを対象
 * クラスへマージする前処理 (preprocess) の際、そのメソッド本体が参照する型のクラス
 * メタデータを (実際にその分岐が実行されるかどうかに関わらず) 解決しようとするため、
 * org.spongepowered.asm.mixin.throwables.ClassMetadataNotFoundException:
 * net.minecraft.client.Minecraft が発生し、AE2本体のクラスロード (たまたま最初に
 * この Mixin 対象クラスの初期化を誘発した箇所) が MixinTransformerError としてクラッシュ
 * し、サーバーが起動できなくなっていた (README方法2の「戻り先」機能自体はクライアント
 * 専用の導線にしか関係しないにも関わらず、Mixin適用対象の共通クラスに影響が及んだ)。
 *
 * 修正方針: Minecraft への参照をこの独立した非Mixinクラスへ切り出す。このクラスは
 * 普通のJavaクラスとしてJVMの通常の (遅延) クラスロードに従うため、実際にクライアント
 * 側で呼び出されるまでロードされず、Mixinの前処理の対象にもならない (Mixinが処理する
 * のはMixinクラス自身のバイトコードのみであり、そこから呼び出す先のクラスの中身までは
 * 前処理時に検証しないため)。CraftingTileEntityMixin側からはこのクラスの静的メソッドを
 * 呼ぶだけにし、Minecraft型への参照を一切残さない。
 */
public final class PriorityReturnTargetClient {

    private PriorityReturnTargetClient() {
    }

    /**
     * ログイン中のクライアントプレイヤーに紐づく「戻り先」記録 (README方法3、端末の
     * Crafting Statusタブ経由で開いた場合の遷移先) があればそれを返す。無ければ null。
     * まだ消費 (削除) はしない (peek) —— 最終的な消費は PriorityContainerMixin の
     * サーバー側分岐が行う。
     */
    public static ContainerType<?> peekReturnContainerType() {
        final PlayerEntity localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            return null;
        }
        final PriorityReturnTarget.Target target = PriorityReturnTarget.peek(localPlayer.getUniqueID());
        return target != null ? target.getContainerType() : null;
    }
}
