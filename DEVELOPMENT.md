# AE2 Crafting Priority — 1.16.5版 開発メモ

このファイルは、1.20.1版(`main`ブランチ)の[DEVELOPMENT.md](https://github.com/yuhhamu/AE2CraftPriority/blob/main/DEVELOPMENT.md)に相当する、1.16.5版向けの開発者向け技術メモです。詳しい開発履歴・調査記録はObsidian側の
プロジェクトノートで管理しています。ここではREADME.mdから参照される「開発者向けAPI」の内容のみ
まとめます。

## 開発者向けAPI(`api` パッケージ)

他Modから優先度を読み書きできる公開APIを提供している。基本設計は1.20.1版と共通(詳細は
1.20.1版DEVELOPMENT.mdを参照)。1.16.5版では、端末経由の優先度画面遷移(README方法3)に
必要な2メソッドが追加されている。

- `api.CraftPriorityApi`(公開ファサード): `static int getPriority(Object)` /
  `static void setPriority(Object, int)` / `static boolean isSupported(Object)` /
  `static void registerAdapter(PriorityAdapter)` に加え、1.16.5版では
  `static TileEntity getPriorityHostBlockEntity(Object)` /
  `static void prepareForPriorityEdit(Object, TileEntity)` の2メソッドを持つ。
  まず内部の`PriorityHolder`(Mixinで追加した既存フィールド)を優先してチェックし、
  該当しなければ`CopyOnWriteArrayList<PriorityAdapter>`として保持しているアダプタ登録を
  順に照会する。どちらにも該当しない場合は`PriorityHolder.DEFAULT_PRIORITY`(0)を返す。
- `api.PriorityAdapter`(拡張ポイント): `supports(Object)` / `getPriority(Object)` /
  `setPriority(Object, int)`を実装したクラスを`CraftPriorityApi.registerAdapter(...)`で
  登録すると、AE2純正の`CraftingCPUCluster`以外の第三者Mod製CPU/クラスタ型にも本MODの
  優先度システムを適用できる。

## 関連

- ビルド・実機起動テストの手順は`mod-test-runner`プロジェクトを参照。
- 詳細な開発履歴・不具合調査記録はObsidian Vaultの
  `Projects/AE2-CraftingPriority-Addon/1.16.5.md`および関連Knowledgeノートを参照。
