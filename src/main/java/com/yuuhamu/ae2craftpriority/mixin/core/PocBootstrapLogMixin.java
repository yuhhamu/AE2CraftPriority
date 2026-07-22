package com.yuuhamu.ae2craftpriority.mixin.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.core.AppEng;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * design/PORT-DESIGN-1.12.2.md 8章「推奨実装順序」手順1のPoC専用Mixin。
 *
 * 目的: Forge1.12.2 + MixinBooterの組み合わせでMixinが正しくロード・適用されるかを、
 * 優先度ロジックとは無関係な「1行ログを出すだけ」の最小構成で検証する。
 * このMixinが動作確認できたら、design 11-3節の9個の本実装Mixinへ順次置き換えていく。
 *
 * 対象: {@code appeng.core.AppEng}(AE2本体のメインMODクラス、必ず早期にロードされる)
 * の{@code preInit(FMLPreInitializationEvent)}(135〜136行目、private)の末尾。
 * AE2側クラス(appeng.*パッケージ)が対象のため remap = false
 * (design/PORT-DESIGN-1.12.2.md 6.3節の使い分けルール参照)。
 */
@Mixin(value = AppEng.class, remap = false)
public abstract class PocBootstrapLogMixin {

    private static final Logger AE2CP_POC_LOGGER = LogManager.getLogger("AE2CraftPriority-PoC");

    @Inject(method = "preInit", at = @At("TAIL"))
    private void ae2cp$onAppEngPreInit(FMLPreInitializationEvent event, CallbackInfo ci) {
        AE2CP_POC_LOGGER.info(
                "AE2CraftPriority(1.12.2): Mixin PoC OK - AppEng#preInit()末尾に到達しました。"
                        + "MixinBooter経由のMixin適用が機能しています。");
    }
}
