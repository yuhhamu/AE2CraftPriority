package com.yuuhamu.ae2craftpriority.mixin.status;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.yuuhamu.ae2craftpriority.mixin.accessor.ButtonAccessor;
import com.yuuhamu.ae2craftpriority.mixin.accessor.TabButtonAccessor;
import com.yuuhamu.ae2craftpriority.mixin.accessor.WidgetContainerAccessor;
import com.yuuhamu.ae2craftpriority.priority.CraftingPriorityHostMarker;
import com.yuuhamu.ae2craftpriority.priority.PriorityBackNavigationControl;
import com.yuuhamu.ae2craftpriority.priority.PriorityReturnTarget;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.PriorityScreen;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.TabButton;
import appeng.container.implementations.PriorityContainer;
import appeng.container.me.crafting.CraftingStatusContainer;
import appeng.core.localization.GuiText;

/**
 * バニラの優先度編集画面({@code PriorityScreen})が、Crafting CPU(README方法2・方法3)の
 * 優先度編集のために開かれたときの「戻る」ボタンの遷移先とアイコン/ラベルを調整する。
 *
 * <p>バニラの{@code AESubScreen#goBack()}(既存の「戻る」ボタンのデフォルト動作)は常に
 * {@code SwitchGuisPacket(previousContainerType)}のみを送る設計で、サーバー側は「現在開いている
 * Container自身のContainerLocator」をそのまま再利用してしまう({@code SwitchGuisPacket}を
 * 実機jarからCFRで再デコンパイルして確認済み: {@code serverPacketData}は
 * {@code player.openContainer.getLocator()}をそのまま{@code ContainerOpener.openContainer}へ渡す)。
 * 方法3(端末のCrafting Statusタブ経由)の場合、このロケータは端末ではなく代表
 * {@code CraftingTileEntity}を指しているため、{@code CraftingStatusContainer}が要求する
 * {@code ITerminalHost}として解決できず、{@code ContainerOpener.openContainer}が{@code false}を
 * 返して何も起きない(2026-07-22発見: 「戻れない」というユーザー報告の根本原因)。
 *
 * <p>2026-07-22発見: 当初は「戻る」ボタンを丸ごと新しい{@code TabButton}に差し替える方式を
 * {@code <init> TAIL}のInjectで実装したが、実機でこのInjectメソッド自体が一度も呼ばれない
 * ことが判明した(原因不明。プロジェクト内の他の{@code <init> TAIL}Inject(例:
 * {@code CraftingCPUScreenMixin})は正常動作しているため、{@code PriorityScreen}固有の事情と
 * 推測されるが特定できていない)。そのため、実際に動作することが実証済みの
 * {@code updateBeforeRender()}(バーチャルメソッドのオーバーライド)から、AE2純正の
 * {@code AESubScreen#addBackButton}が生成し既に画面に登録済みの「戻る」{@code TabButton}
 * インスタンスをそのまま再利用し、そのフィールド(アイコン・ラベル・押下時コールバック)を
 * 直接書き換える方式に変更した。</p>
 *
 * <p>2026-07-22発見(2): 判定用に{@link PriorityReturnTarget#take}(消費・削除)を使うと、
 * シングルプレイヤーではクライアントとサーバーが同一静的マップを共有しているため、
 * ここでの判定処理がサーバー側の戻り先記録を先に消費してしまい、実際に「戻る」ボタンが
 * 押された時にはサーバー側({@code PriorityContainerMixin})で何も残っていない状態になる。
 * 判定用途には非破壊の{@link PriorityReturnTarget#peek}を使い、実際の消費(削除)は
 * {@code PriorityContainerMixin}側の1箇所のみで行う。実機検証済み。</p>
 *
 * <p>クライアント側のみのMixin({@code ae2craftpriority.mixins.json}の{@code client}リストに
 * 属する)であるため、対応Mod(このアドオン)を導入していない環境の心配は無い。ただし
 * {@code this.container}(=バニラの{@code PriorityContainer})のホストが本アドオンの
 * {@link CraftingPriorityHostMarker}を実装しているかどうかは毎回instanceofで判定し、
 * ストレージバス等バニラ本来の優先度画面には一切影響しない。</p>
 */
@Mixin(value = PriorityScreen.class, remap = false)
public abstract class PriorityScreenMixin extends AEBaseScreen<PriorityContainer> {

    // Mixinの都合上のダミーコンストラクタ。実際にインスタンス化されることはない。
    public PriorityScreenMixin(PriorityContainer container, PlayerInventory playerInventory, ITextComponent title,
            ScreenStyle style) {
        super(container, playerInventory, title, style);
    }

    @Unique
    private static final ResourceLocation AE2CP$CRAFT_HAMMER_TEXTURE = new ResourceLocation("ae2craftpriority",
            "textures/gui/craft_hammer.png");

    @Unique
    private boolean ae2cp$backButtonHandled;

    @Unique
    private TabButton ae2cp$backButtonRef;

    @Unique
    private boolean ae2cp$useCraftHammerIcon;

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (this.ae2cp$backButtonHandled) {
            return;
        }
        this.ae2cp$backButtonHandled = true;
        this.ae2cp$rewireBackButton();
    }

    /**
     * 2026-07-22発見(3): AE2の{@code Icon}列挙型は自前のアイコンアトラス({@code guis/states.png},
     * 256x256)にしか対応しておらず、1.16.5向けにビルドされたAE2にはハンマー用のアイコンが
     * 存在しない(実機jarを{@code Icon.class}までCFRで再デコンパイルし、"hammer"を大小文字
     * 区別なしで全文検索して確認済み: 該当なし)。一方、1.20.1/1.21.1向けの本アドオン移植版は
     * AE2 15.4.10以降に追加された{@code Icon.CRAFT_HAMMER}をそのまま使用している。
     *
     * <p>そこで、AE2本体のGitHub({@code forge/v15.4.10}タグ、modid{@code ae2}の
     * {@code assets/ae2/textures/guis/states.png})から実物のハンマーアイコン(座標
     * {@code (48,144)}、16x16)を取得し、本アドオン自身のテクスチャ({@code
     * assets/ae2craftpriority/textures/gui/craft_hammer.png})として同梱した。AE2純正の
     * {@code Icon}列挙型はAE2自身のアトラス以外を参照できないため、{@code TabButton}の
     * icon/itemフィールドは両方ともnull/EMPTYのままにしておき(標準の描画は何もしない)、
     * 代わりに{@code AEBaseScreen#drawFG}を本Mixinでオーバーライドし、AE2純正の
     * {@code Blitter}({@code appeng.client.gui.style.Blitter}、任意の
     * {@code ResourceLocation}を受け付けられることを実機jarのデコンパイルで確認済み)を
     * 使って独自テクスチャを直接blitしている。
     *
     * <p>2026-07-23発見: 当初はバニラ{@code Screen#render}のAE2側オーバーライド
     * ({@code func_230430_a_}、コンパイル時に実機jarから確認したSRG名)を直接
     * オーバーライドする方式を試みたが、実際にビルドすると
     * 「method does not override or implement a method from a supertype」で
     * コンパイルエラーになった。本プロジェクトのMixinソースはSRG名直書き
     * (remap=false)で書く運用だが、これは実行時のMixin変換(バイトコード上の
     * ターゲット解決)にのみ適用され、Mixinクラス自体の{@code extends}やプレーンな
     * メソッドオーバーライドはMixin変換を経ない**通常のjavacコンパイル**であるため、
     * コンパイル時クラスパス(fg.deobfで用意されたAE2依存jar)上での実際のメソッド名が
     * 一致している必要がある。今回のケースでは一致しなかった(理由未特定)。
     * 一方{@code drawFG}はAE2が独自に用意した空実装のフック({@code
     * appeng.client.gui.AEBaseScreen}に{@code public void drawFG(MatrixStack, int,
     * int, int, int) {}"}として直接定義されている、バニラのオーバーライドではない
     * AE2自身のメソッド)であるため、SRG/official名の食い違いを気にする必要が無く、
     * 確実にコンパイルできる。呼び出しタイミングもバニラ{@code
     * ContainerScreen#render}のフロー上、ボタン本体の描画(drawGuiContainerForegroundLayer
     * 相当が呼ばれる直前、既にウィジェット/ボタン群は描画済み)の後であることを
     * {@code AEBaseScreen}のデコンパイル結果({@code func_230451_b_}が
     * {@code this.drawFG(...)}を呼ぶ)で確認済みなので、ボタン本体の描画に
     * 上書きされる心配は無い。</p>
     */
    @Override
    public void drawFG(MatrixStack matrixStack, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(matrixStack, offsetX, offsetY, mouseX, mouseY);
        if (!this.ae2cp$useCraftHammerIcon || this.ae2cp$backButtonRef == null) {
            return;
        }
        // 2026-07-23発見: drawFG()が呼ばれる時点でMatrixStackは既に(guiLeft, guiTop)分
        // 平行移動された「GUIパネル相対」座標系になっている(AEBaseScreen#func_230451_b_が
        // super.func_230430_a_内で(guiLeft,guiTop)平行移動した状態でdrawGuiContainerForegroundLayer
        // 相当を呼び、そこからdrawFGが呼ばれる)。一方TabButton#getTooltipAreaX/Yが返すのは
        // ボタン自身のx/yで、これは(バニラのボタン一覧描画が平行移動の外側=絶対スクリーン座標系で
        // 行われるため)既にguiLeft/guiTopを含んだ「絶対スクリーン座標」。そのままdrawFG内で使うと
        // guiLeft/guiTopが二重に加算され、実機で「戻る」ボタンから大きく右下にずれた位置(画面端の
        // 外側)にアイコンが描画される不具合が発生した(ユーザー報告により発覚)。drawFGが引数として
        // 受け取るoffsetX/offsetY(=guiLeft/guiTop)を差し引き、GUIパネル相対座標に変換して修正。
        final int btnOffsetX = this.ae2cp$backButtonRef.getHideEdge() ? 1 : 0;
        final int x = this.ae2cp$backButtonRef.getTooltipAreaX() - offsetX + btnOffsetX + 3;
        final int y = this.ae2cp$backButtonRef.getTooltipAreaY() - offsetY + 3;
        Blitter.texture(AE2CP$CRAFT_HAMMER_TEXTURE, 16, 16)
                .src(0, 0, 16, 16)
                .dest(x, y)
                .blit(matrixStack, 0);
    }

    private void ae2cp$rewireBackButton() {
        if (!(this.container.getPriorityHost() instanceof CraftingPriorityHostMarker)) {
            return;
        }
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        // 判定用途のため非破壊のpeek()を使う(take()は使わない)。実際の消費は
        // PriorityContainerMixin側のサーバー分岐でのみ行う。
        final PriorityReturnTarget.Target target = PriorityReturnTarget.peek(this.minecraft.player.getUniqueID());
        if (target == null || target.getContainerType() != CraftingStatusContainer.TYPE) {
            return;
        }

        final Widget backWidget = ((WidgetContainerAccessor) this.widgets).ae2cp$getWidgets().get("back");
        if (!(backWidget instanceof TabButton)) {
            return;
        }
        final TabButton backButton = (TabButton) backWidget;

        // アイコン/ラベルを差し替える(既存インスタンスをそのまま流用するため、位置・
        // 登録状態には一切手を加えない)。AE2純正のIcon列挙型・itemスタックによる描画は
        // どちらも使わず(両方null/EMPTYのままにして標準描画を止め)、
        // func_230430_a_のオーバーライド側でAE2本家から移植したハンマーアイコンを
        // 独自にblitする(詳細はクラスJavadocおよびfunc_230430_a_のJavadoc参照)。
        ((TabButtonAccessor) backButton).ae2cp$setIcon(null);
        ((TabButtonAccessor) backButton).ae2cp$setItem(ItemStack.EMPTY);
        backButton.setMessage(GuiText.CraftingStatus.text());

        this.ae2cp$backButtonRef = backButton;
        this.ae2cp$useCraftHammerIcon = true;

        // 押下時の遷移先を、専用ロジック(PriorityBackNavigationControl経由、
        // PriorityReturnTargetに記録済みの端末自身のロケータを使って直接開き直す)に差し替える。
        ((ButtonAccessor) (Button) backButton).ae2cp$setOnPress(
                btn -> ((PriorityBackNavigationControl) this.container).ae2cp$returnToCraftingStatus());
    }
}
