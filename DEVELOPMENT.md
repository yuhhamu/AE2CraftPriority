# AE2 Crafting Priority (Fabric 1.20.1) — 開発者向け情報

> 本MODの開発にはAnthropicのAIアシスタント「Claude」を活用しています。

プレイヤー向けの使い方は [README.md](README.md) を参照してください。

## パッケージ構成

`src/main/java/com/yuuhamu/ae2craftpriority/` 配下は、Forge/1.20.1版と同じ役割分担で構成されている。

- `AE2CraftPriorityMod.java` — `ModInitializer`(Fabricの共通側エントリポイント)
- `client/` — `AE2CraftPriorityClientMod.java`(`ClientModInitializer`、クライアント側エントリポイント)、`ClientSetup.java`、優先度設定用の独自画面(`CraftPriorityStepScreen.java`)、`PriorityBackIconOverride.java`
- `menu/` — 優先度設定用の独自メニュー(`CraftPriorityStepMenu.java`)
- `api/` — 他Modから優先度を読み書きするための公開API(`CraftPriorityApi`/`PriorityAdapter`)。Forge版と同じクラス構成
- `priority/` — 優先度そのもののデータモデル(`PriorityHolder`/`PriorityOrderedHashSet`/`PendingCraftPriority`等)。Forge版と共通の設計
- `mixin/` — Vanilla AE2本体を対象にしたMixin。Forge版と同じ4分類で構成:
  - `mixin/core/` — 優先度の保持・スケジューリング本体(`CraftingBlockEntityMixin`/`CraftingCPUClusterMixin`/`CraftingCPUMenuMixin`/`CraftingCpuLogicMixin`/`CraftingServiceMixin`)
  - `mixin/craft/` — クラフト開始フロー(`CraftAmountMenuMixin`/`CraftConfirmMenuMixin`)
  - `mixin/status/` — AE2純正「Crafting Status」画面・優先度画面へのUI統合(`CraftingStatusMenuMixin`/`CraftingCPUScreenMixin`/`PriorityScreenMixin`)
  - `mixin/accessor/` — AE2 GUI内部のprivateフィールドを読み書きする汎用Accessor(`TabButtonAccessor`/`WidgetContainerAccessor`)

`compat/`(サードパーティAddon対応)ディレクトリは**Fabric版には存在しない**。AdvancedAEは2026年8月時点でFabric版が存在しないため、Forge版にあった`compat/advancedae/`一式はこのソースツリーには含まれていない。

## Fabric版固有の技術的な違い(Forge/1.20.1版との差分)

Mixinの対象クラス・技術的な設計思想そのものはForge/1.20.1版と共通(同じMinecraftバージョン向けのAE2内部実装 `appeng.*` を対象としている)。各Mixinの詳細な対象メンバー・技術的根拠はForge/1.20.1版の[DEVELOPMENT.md](../../Forge/1.20.1/DEVELOPMENT.md)を参照。以下はFabric版特有の差分。

- **ビルドシステム**: Architectury Loom(`dev.architectury.loom` バージョン `1.10.455`)。
- **マッピング**: 通常のFabric ModはYarnマッピングを使うが、本MODは`build.gradle`で
  `mappings loom.layered() { officialMojangMappings() }`としてOfficial Mojangマッピングを採用している。
  AE2のFabric版(GitHub tag `fabric/v15.4.10`)自身もOfficial Mojangマッピングでビルドされており、
  かつForge版と共通のMixinソース資産(対象クラス名・メソッド名がForge/1.20.1のMCP/公式マッピングと一致)を
  そのまま流用できるようにするための意図的な選択。
- **エントリポイント**: `fabric.mod.json`の`entrypoints`で`main`(`AE2CraftPriorityMod`)と
  `client`(`client.AE2CraftPriorityClientMod`)を分離登録している(Forge版の`@Mod`+
  `FMLJavaModLoadingContext`イベントバス方式とは異なる、Fabric標準の初期化方式)。
- **Mixin設定**: `ae2craftpriority.mixins.json`で`package: com.yuuhamu.ae2craftpriority.mixin`配下を
  一括登録し、共通側(`mixins`キー、8件)とクライアント専用側(`client`キー、4件)に分離している。
- **依存関係の解決**:
  - `fabric-api`: `0.88.1+1.20.1`に固定(下記「重要な制約」参照)
  - `teamreborn:energy`: AE2のエネルギー相互運用に必須。`modImplementation`のため最終jarには
    `include`されない → 配布物として利用者側で別途Team Reborn Energy APIのjarを`mods`フォルダに
    導入してもらう必要がある(README.mdの「必要なもの」に記載済み)
  - `maven.modrinth:ae2:${ae2_fabric_version_id}`: Modrinth上のバージョン番号表記(`15.4.10`)が
    Forge版・Fabric版で共用されているため、バージョン番号ではなく個別のバージョンID
    (`kA3rm9EP` = 「AE2 15.4.10 [FABRIC]」)を直接指定している

## 重要な制約: Fabric APIバージョンの固定

`gradle.properties`に以下のコメント付きで明記されている通り、Fabric APIは **0.88.1+1.20.1 に固定** している。

```properties
fabric_api_version=0.88.1+1.20.1
```

AE2 15.4.10 [FABRIC]が実際にビルドで使用しているFabric APIバージョン(AE2自身のリポジトリの
`gradle.properties`の`fabric_version`値、GitHub `AppliedEnergistics/Applied-Energistics-2` タグ
`fabric/v15.4.10` 参照)に合わせている。これより新しいFabric APIを使うと、AEItems初期化時に
`NoSuchMethodError`(FabricItemSettings関連)でサーバー起動がクラッシュすることを実機確認済み。

このバージョンを上げる場合は、AE2側が要求するFabric APIバージョンが変わっていないか必ず確認すること。
実際にこのプロジェクトの開発中にも、JEI/JourneyMapの互換バージョン選定でこの制約に抵触しかけたが、
Fabric APIを上げる代わりにJEI `15.0.0.12` / JourneyMap `1.20.1-5.10.3-fabric`という組み合わせに
落ち着かせることで、Fabric APIのバージョンを一切変えずに解決した実績がある。

## 開発者向けAPI(`api` パッケージ)

Forge版と同じ構成(`CraftPriorityApi`ファサード + `PriorityAdapter`拡張ポイント)の公開APIを
提供している。パッケージ名・クラス名・メソッドシグネチャは共通のため、詳細な利用方法は
Forge/1.20.1版DEVELOPMENT.mdの「開発者向けAPI」節を参照。

## サードパーティAddon対応

Fabric版では対応アドオンMOD向けの専用コード(`compat/`)は存在しない。

- **AdvancedAE**: 2026年8月時点でFabric版が存在しないため対象外。
- **Mega Cells / ExtendedAE / Applied Mekanistics**: 標準のPattern Provider拡張のため、追加コード
  なしで動作する想定(Forge版と同じ設計思想)。個別の動作確認は未実施。

## ビルド方法

- JDK 17
- `./gradlew build`(Fabric Loom)
- `./gradlew runClient` / `./gradlew runServer` で動作確認。`run/`(クライアント)・`run/server/`
  (サーバー)に実行ディレクトリを分離しており、Forge/NeoForge版と同じ構成になっている。

## 対応バージョン範囲の根拠

Forge/1.20.1版DEVELOPMENT.mdの「対応バージョン範囲の根拠」節を参照。AE2は同一Minecraftバージョン
(1.20.1)向けであればForge/Fabric間で内部実装(`appeng.*`パッケージ)が共通のため、同じ根拠がFabric版にも
当てはまる。
