package com.yuuhamu.ae2craftpriority.client;

import java.io.FileNotFoundException;

import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.ScreenManager.IScreenFactory;

import com.yuuhamu.ae2craftpriority.menu.CraftPriorityStepContainer;

import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.StyleManager;

/**
 * クライアント側専用の初期化処理。{@code FMLClientSetupEvent}から一度だけ呼ばれる想定。
 *
 * <p>本アドオンが追加する新しいContainer種別は{@link CraftPriorityStepContainer#TYPE}のみ
 * (他は全てAE2純正Containerに対するMixinで、Screenの登録もAE2純正のまま変わらない)。
 * {@code StyleManager.loadStyleDoc(path)}は常に{@code appliedenergistics2}名前空間の
 * リソースを解決する(1.16.5実ソースで確認済み)ため、対応するスタイルJSONは
 * {@code src/main/resources/assets/appliedenergistics2/screens/craft_priority_step.json}
 * (本アドオン自身のリソースだが、AE2側の名前空間に配置)に置いている。</p>
 */
public final class ClientSetup {

    private static final String CRAFT_PRIORITY_STEP_STYLE = "/screens/craft_priority_step.json";

    private ClientSetup() {
    }

    public static void init() {
        final IScreenFactory<CraftPriorityStepContainer, CraftPriorityStepScreen> factory = (container, playerInv,
                title) -> {
            final ScreenStyle style;
            try {
                style = StyleManager.loadStyleDoc(CRAFT_PRIORITY_STEP_STYLE);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(
                        "Failed to read Screen JSON file: " + CRAFT_PRIORITY_STEP_STYLE + ": " + e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException("Failed to read Screen JSON file: " + CRAFT_PRIORITY_STEP_STYLE, e);
            }
            return new CraftPriorityStepScreen(container, playerInv, title, style);
        };
        ScreenManager.registerFactory(CraftPriorityStepContainer.TYPE, factory);
    }
}
