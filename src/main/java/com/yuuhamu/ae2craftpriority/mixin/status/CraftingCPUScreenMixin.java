package com.yuuhamu.ae2craftpriority.mixin.status;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.yuuhamu.ae2craftpriority.client.PriorityWrenchButton;
import com.yuuhamu.ae2craftpriority.priority.CraftingStatusPriorityControl;
import com.yuuhamu.ae2craftpriority.priority.PriorityReturnTarget;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import appeng.container.implementations.PriorityContainer;
import appeng.container.me.crafting.CraftingCPUContainer;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.SwitchGuisPacket;

/**
 * README方法2(単体Crafting CPU画面)・方法3(端末のCrafting Statusタブ)の両方で使われる
 * {@code CraftingCPUScreen<T extends CraftingCPUContainer>}に、優先度編集画面を開くレンチ
 * アイコンボタンを追加する。{@code CraftingStatusContainer}は{@code CraftingCPUContainer}を
 * 継承しているため、この1つのScreenクラス・Mixinで両方をカバーできる(1.16.5実ソースで確認済み)。
 *
 * <p>{@code this.container}が{@link CraftingStatusPriorityControl}を実装している場合(=方法3、
 * 端末のCrafting Statusタブ)は、そちらが提供する専用アクション
 * ({@code ae2cp$openPrioritySettings()})経由で「現在追跡中のCPU」の優先度画面を開く。
 * そうでない場合(=方法2、Crafting CPUブロックを直接右クリック)は、バニラの
 * {@code SwitchGuisPacket(PriorityContainer.TYPE)}をそのまま送る(現在開いているContainerの
 * ロケータがそのままCraftingTileEntityを指しているため、これで正しく解決できる)。</p>
 */
@Mixin(value = CraftingCPUScreen.class, remap = false)
public abstract class CraftingCPUScreenMixin extends AEBaseScreen<CraftingCPUContainer> {

    // Mixinの都合上のダミーコンストラクタ。実際にインスタンス化されることはない。
    // 実クラスCraftingCPUScreenの実コンストラクタはpublicであり(javapで確認済み)、AE2自身の
    // ScreenRegistration側がラムダ(MethodHandle/invokedynamic経由)で外部からこのコンストラクタを
    // 呼び出すため、ここをprivateにするとMixin適用時にアクセス修飾子がそちらへ引き継がれてしまい、
    // 実機起動時に「IllegalAccessError: no such constructor ... newInvokeSpecial」でクラッシュする
    // (2026-07-22発見)。実クラスと同じpublicにしておく必要がある。
    public CraftingCPUScreenMixin(CraftingCPUContainer container, PlayerInventory playerInventory,
            ITextComponent title, ScreenStyle style) {
        super(container, playerInventory, title, style);
    }

    @Unique
    private IconButton ae2cp$priorityButton;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2cp$onInit(CraftingCPUContainer container, PlayerInventory playerInventory,
            ITextComponent title, ScreenStyle style, CallbackInfo ci) {
        // 匿名クラス(new IconButton(...) { ... })はMixinが対象クラスへマージする際の内部クラス
        // 再配置で実機起動時にNoClassDefFoundErrorを引き起こしたため、Mixin対象クラスに一切
        // マージされない独立したトップレベルクラス(PriorityWrenchButton)を使う。詳細は
        // Knowledge/mixin-anonymous-class-in-injected-method-breaks-classloading.md参照。
        this.ae2cp$priorityButton = new PriorityWrenchButton(btn -> this.ae2cp$openPriority());
        this.ae2cp$priorityButton.setMessage(GuiText.Priority.text());
        // 注意: ここでthis.addButton(...)しても、Screen#init(Minecraft, int, int)が
        // this.buttons/this.childrenをclear()してからAEBaseScreen#init()(WidgetContainer経由の
        // 再ポピュレート)を呼ぶため、画面が実際に開かれる際に消えてしまう(実機で「ボタンが
        // 見えない」原因、2026-07-22発見)。実際の追加はupdateBeforeRender()側で保証する。
    }

    @Unique
    private void ae2cp$openPriority() {
        if (this.container instanceof CraftingStatusPriorityControl) {
            ((CraftingStatusPriorityControl) this.container).ae2cp$openPrioritySettings();
            return;
        }

        // 方法2: 前回のCrafting Statusタブ(方法3)由来の戻り先記録が残っていれば破棄する。
        // ここでのアクセスは常に「直接右クリックで開いたCPU画面」であり、現在開いている
        // Container(CraftingCPUContainer)自身のロケータでPriorityContainerが正しく解決できる。
        if (this.minecraft != null && this.minecraft.player != null) {
            PriorityReturnTarget.clear(this.minecraft.player.getUniqueID());
        }
        NetworkHandler.instance().sendToServer(new SwitchGuisPacket(PriorityContainer.TYPE));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (this.ae2cp$priorityButton == null) {
            return;
        }
        // Screen#init(Minecraft, int, int)によってthis.buttons/this.childrenが
        // クリアされた後(画面を開いた直後、および画面リサイズ時)にも確実に再表示されるよう、
        // 毎フレームここで存在確認してから追加する(コンストラクタでのthis.addButton(...)だけでは
        // 消えてしまう問題への対処、2026-07-22発見)。
        if (!this.buttons.contains(this.ae2cp$priorityButton)) {
            this.addButton(this.ae2cp$priorityButton);
        }
        // 画面右上に配置する。2026-07-22(2回目)発見: 元々「xSize - 20」(右端から20px、AE2の
        // アイコンボタン1スロット分)の位置に置いていたところ、既存の別GUI要素(端末画面側の
        // タブボタン等)と重なってしまうとユーザーから報告があったため、ボタン1つ分(20px、
        // AE2純正のアイコンボタンのスロット幅、storage_bus.json等の"openPriority"配置と同じ
        // 間隔)さらに左へずらした。
        // 2026-07-22(3回目)発見: 上端に近すぎて別要素(CPU切替タブ等)と詰まって見えるとの
        // 報告を受け、Y座標も少し上へ(4→0)ずらしたが、今度は逆に近すぎたとの報告(4回目)を
        // 受け、その中間(0→2)へ再調整。
        // 2026-07-22(5回目)発見: X座標もさらに2pxほど左へとの報告を受け、-40→-42へ微調整。
        // 2026-07-23発見(6回目、感覚調整ではなく座標を実測): このボタンは端末の「Crafting
        // Status」タブ経由(README方法3)で開いた場合、AE2純正のCraftingStatusScreenが
        // コンストラクタで追加する純正「戻る」TabButton(AESubScreen#addBackButton、
        // assets/appliedenergistics2/screens/crafting_status.jsonの"back"定義: left=213,
        // top=-4、TabButton自体の実サイズは22x22)のすぐ左に並ぶ形になる。IconButtonの実サイズは
        // 16x16(IconButtonのコンストラクタで確認済み)なので、旧来の"xSize - 42"(右端から42px、
        // 右辺=xSize-42+16=xSize-26=212)だと純正「戻る」タブの左辺(左辺=213)との間に
        // 1pxの隙間ができてしまい、これが「絶妙なずれ」として見えていた。"xSize - 41"に
        // すると右辺がちょうど213に一致し、純正「戻る」タブと隙間なく隣接する
        // (TabButtonの"hideEdge"が隣接部の二重枠を避ける設計であることからも、本来は
        // 隙間ゼロで隣接させる意図と判断した)。Y座標(guiTop + 2)は「戻る」タブの下辺
        // (top=-4、高さ22 → 下辺=18)と本ボタンの下辺(2+16=18)が既に一致しており、
        // 変更不要と判断した。
        this.ae2cp$priorityButton.x = this.guiLeft + this.xSize - 41;
        this.ae2cp$priorityButton.y = this.guiTop + 2;
    }
}
