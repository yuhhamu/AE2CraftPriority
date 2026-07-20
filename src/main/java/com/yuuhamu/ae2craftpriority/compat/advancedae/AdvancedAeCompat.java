package com.yuuhamu.ae2craftpriority.compat.advancedae;

import com.yuuhamu.ae2craftpriority.api.CraftPriorityApi;

/**
 * AdvancedAE (modId: {@code advanced_ae}) 対応の初期化。
 *
 * <p>{@code AE2CraftPriorityMod} のコンストラクタから、{@code ModList.get().isLoaded("advanced_ae")}
 * が {@code true} のときだけ呼び出すこと。このクラス自体がAdvancedAEのクラス
 * ({@code net.pedroksl.advanced_ae.*})を参照するため、呼び出し元でガードしていない状態で
 * クラスロードすると、AdvancedAE未導入環境で {@code NoClassDefFoundError} になる。</p>
 *
 * <p>Mixin設定({@code ae2craftpriority-advancedae.mixins.json})はここで動的登録しない
 * (詳細は build.gradle の {@code mixin{}} ブロックのコメント参照)。この設定ファイルは
 * build.gradleで静的・最早期登録し、{@code "required": false} によりAdvancedAE未導入環境では
 * 対象クラスが無くても安全にスキップされるようにしている。</p>
 */
public final class AdvancedAeCompat {

    public static final String MOD_ID = "advanced_ae";

    private AdvancedAeCompat() {
    }

    public static void init() {
        CraftPriorityApi.registerAdapter(new AdvancedAeCpuAdapter());
    }
}
