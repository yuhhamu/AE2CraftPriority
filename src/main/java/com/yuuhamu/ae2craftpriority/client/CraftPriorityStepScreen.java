package com.yuuhamu.ae2craftpriority.client;

import java.util.OptionalInt;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import com.yuuhamu.ae2craftpriority.menu.CraftPriorityStepContainer;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.implementations.NumberEntryWidget;

/**
 * README「方法1」(個数を確定すると優先度画面が開く)の優先度入力画面。
 *
 * <p>{@link CraftPriorityStepContainer}は独自のクライアントアクション({@code ae2cp$confirmPriority}/
 * {@code ae2cp$back})を持つため、AE2純正の{@code AESubScreen}の汎用「戻る」機構は使わず、
 * このScreen自身が「次へ」「戻る」ボタンを直接{@code container}へ委譲する(1.18.2版の
 * {@code CraftPriorityStepScreen}と同じ構成)。</p>
 */
public class CraftPriorityStepScreen extends AEBaseScreen<CraftPriorityStepContainer> {

    private final NumberEntryWidget priority;
    private final Button nextButton;
    private final Button backButton;

    public CraftPriorityStepScreen(CraftPriorityStepContainer container, PlayerInventory playerInventory,
            ITextComponent title, ScreenStyle style) {
        super(container, playerInventory, title, style);

        this.priority = new NumberEntryWidget(NumberEntryType.PRIORITY);
        // 2026-07-22(2回目)修正: NumberEntryWidget#populateScreen()をCFRで再デコンパイルして
        // 確認したところ、setTextFieldBoundsのx/yは「ウィジェット自身のスタイル解決済み
        // left/top(このcraft_priority_step.jsonでは20,30 — AE2純正priority.jsonの"priority"
        // ウィジェットと全く同じ値)からの相対オフセット」であり、かつテキストフィールド自体は
        // コンストラクタでfunc_146185_a(false)(setBordered(false)相当)により枠を描画しない
        // ことも確認した。つまり画面に見えている入力欄の「箱」はConfirmableTextField自身では
        // なく、背景テクスチャ(guis/priority.png、AE2純正のpriority.pngをそのまま流用)に
        // 焼き込まれた装飾であり、その箱の位置はAE2純正PriorityScreenの
        // setTextFieldBounds(62, 57, 50)を前提に描かれている。以前の(20, 57, 100)は
        // Y座標(+側行と-側行の間の隙間)は合っていたが、X座標と幅が純正と異なっていたため、
        // 実際のテキスト("0_")が背景の箱の位置とずれて表示されていた
        // (Knowledge/ae2-numberentrywidget-textfield-y-position-convention.md参照・追記予定)。
        // ウィジェットのスタイル(left/top/width/height)がAE2純正priority.jsonの"priority"と
        // 完全に一致しているため、setTextFieldBoundsの値も純正と完全に一致させることで
        // 背景の箱と正しく重なるようにする。
        this.priority.setTextFieldBounds(62, 57, 50);
        this.priority.setMinValue(Integer.MIN_VALUE);
        this.priority.setValue(container.getPriorityValue());
        this.priority.setOnConfirm(this::confirm);
        widgets.add("priority", this.priority);

        this.nextButton = new Button(0, 0, 50, 20, new TranslationTextComponent("gui.ae2craftpriority.next"),
                btn -> this.confirm());
        this.backButton = new Button(0, 0, 50, 20, new TranslationTextComponent("gui.ae2craftpriority.back"),
                btn -> this.container.goBack());
    }

    @Override
    protected void init() {
        super.init();
        this.addButton(this.nextButton);
        this.addButton(this.backButton);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.nextButton.x = this.guiLeft + this.xSize - 4 - this.nextButton.getWidth();
        this.nextButton.y = this.guiTop + this.ySize - 4 - 20;
        this.backButton.x = this.guiLeft + 4;
        this.backButton.y = this.guiTop + this.ySize - 4 - 20;
    }

    @Override
    public void drawBG(MatrixStack matrixStack, final int offsetX, final int offsetY, final int mouseX,
            final int mouseY, float partialTicks) {
        super.drawBG(matrixStack, offsetX, offsetY, mouseX, mouseY, partialTicks);
        this.priority.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void confirm() {
        final OptionalInt value = this.priority.getIntValue();
        if (value.isPresent()) {
            this.container.confirmPriority(value.getAsInt());
        }
    }
}
