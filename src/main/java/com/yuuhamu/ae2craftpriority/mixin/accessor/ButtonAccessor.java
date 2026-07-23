package com.yuuhamu.ae2craftpriority.mixin.accessor;

import net.minecraft.client.gui.widget.button.Button;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * バニラ{@code net.minecraft.client.gui.widget.button.Button}の{@code protected final}
 * フィールド{@code onPress}(押下時コールバック、javap -pで直接宣言を確認済み)への
 * 書き込みアクセサ。
 *
 * <p>2026-07-22発見: {@code PriorityScreenMixin}で「戻る」ボタンを丸ごと新しい
 * {@code TabButton}インスタンスへ置き換える方式(コンストラクタの{@code <init> TAIL}へ
 * Injectして{@code WidgetContainer}の内部マップのエントリを差し替える)を実装したが、
 * 実機で検証したところこのInjectメソッド自体が一度も呼ばれていないことが判明した
 * (原因未特定。同一プロジェクトの{@code CraftingCPUScreenMixin}では同種の
 * {@code <init> TAIL}Injectが正常に動作しているため、Mixin側の一般的な制約ではなく
 * {@code PriorityScreen}固有の何らかの事情によるものと推測される)。
 *
 * <p>そのため、ウィジェットの参照を丸ごと差し替える方式(populateScreen()より前に
 * マップを書き換える必要があり、タイミングがシビア)ではなく、AE2純正の
 * {@code AESubScreen#addBackButton}が生成し、既に画面に正しく配置・登録済みの
 * 「戻る」{@code TabButton}インスタンスをそのまま再利用し、そのフィールドを直接
 * 書き換える(アイコン/ラベルは既存の{@link TabButtonAccessor}、押下時の遷移先は
 * このアクセサ)方式に切り替えた。この方式は{@code updateBeforeRender()}
 * (実際にオーバーライドされるバーチャルメソッド呼び出しであり、コンストラクタInjectより
 * 動作が安定していることが本プロジェクトの{@code CraftingCPUScreenMixin}で実証済み)
 * から呼び出すため、タイミングの問題も無い。
 *
 * <p>本プロジェクトでは、AE2側のクラス({@code remap = false})だけでなく、初めて
 * バニラ({@code net.minecraft}側、{@code remap = true}がデフォルト)のクラスへの
 * アクセサを追加する。{@code final}フィールドだが、SpongePowered Mixinの
 * {@code @Accessor}はセッターとして使う際に対象フィールドの{@code final}修飾子を
 * 自動的に除去して書き込み可能にする。
 */
@Mixin(Button.class)
public interface ButtonAccessor {

    @Accessor("onPress")
    void ae2cp$setOnPress(Button.IPressable onPress);
}
