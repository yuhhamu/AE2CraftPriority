# AE2CraftPriority 1.21.1 (NeoForge) 移植 — 開発記録

対象: Minecraft 1.21.1 + NeoForge 21.1.169 + AE2 + AdvancedAE(任意依存)
設計書: `Z:\Claude\Projects\MinecraftMods\AE2CraftPriority\design\PORT-DESIGN-1.21.1.md`
GitHub登録は未実施(ユーザー指示待ち)。ローカルのみ、`main`ブランチ。

## 2026-07-22: 準備フェーズ

### 実施内容

1. `AE2CraftPriority-1.21.1\` を新規作成し、1.20.1版の `src/`, `build.gradle`, `gradle.properties`,
   `settings.gradle`, `.gitignore` をコピー(`.git`は含めず)。初回コミット済み。
2. 設計書 `PORT-DESIGN-1.21.1.md` 全文(0〜10章)を精読。
3. 1.20.1版の実ソース(12 Mixin全部、`priority/`, `menu/CraftPriorityStepMenu.java`,
   `AE2CraftPriorityMod.java`, `client/ClientSetup.java`, `compat/advancedae/`一式、
   `build.gradle`/`gradle.properties`/`settings.gradle`/`mods.toml`/両mixins.json)を全読し、
   設計書の記述と突き合わせ。**差分なし、設計書の記述はすべて正確と確認。**
4. AE2公式リポジトリ`1.21.1`ブランチを`/tmp/refs/ae2-1211`にshallow clone(参考実装用、
   プロジェクトには含めない)し、以下を実ソースで裏取り:
   - `build.gradle`/`settings.gradle`/`src/main/neoforge.mods.toml`の構造 → 設計書5章のテンプレートと整合確認
   - Mod本体クラス(`AppEngClient`/`AppEngServer`、`AppEngBase`継承)のコンストラクタが
     `(IEventBus modEventBus, ModContainer container)`を受け取るパターンを確認。設計書4.1節の
     `(IEventBus modEventBus)`単体パターンもNeoForgeが正式サポートするコンストラクタ形状の一つ
     (公式ドキュメント既知)であり、本MODでは単純な方で問題ない。
   - `CraftingCPUCluster.writeToNBT/readFromNBT`が`(CompoundTag, HolderLookup.Provider)`に
     なっていることを実ソース1行単位で確認(設計書2-1節、信頼度「高」の裏付け)。
   - `MenuHostLocator`クラスの実在、`MenuOpener.open(...)`/`CraftAmountMenu.open(...)`の
     第2/3引数が`MenuHostLocator`型であることを確認(設計書2-4/2-5節の裏付け)。
   - `CraftingService.craftingCPUClusters`(`private final Set<...> = new HashSet<>()`)、
     コンストラクタ引数`(IGrid, IStorageService, IEnergyService)`が1.20.1と完全一致することを確認。
   - `CraftingCpuLogic.cluster`フィールド、`trySubmitJob`/`getUnitBlock`/`CraftingCPUMenu`の
     コンストラクタ・`getGrid()`/`CraftingStatusMenu`の`setCPU`/`getOrAssignCpuSerial`/
     `createCpuList`/`TabButton`の`icon`・`item`フィールド/`WidgetContainer.widgets`
     (`Map<String, AbstractWidget>`)—**設計書が「変更なし」とした10 Mixin全項目を実ソースで個別確認、
     すべて一致**。
5. AdvancedAE `1.6.11-1.21.1-neoforge`タグを`/tmp/refs/advancedae`にclone(参考実装用)し、
   compat対象5クラス(`net.pedroksl.advanced_ae.*`)を実ソースで確認:
   - `AdvCraftingCPUCluster.getActiveCPUs()`、`AdvCraftingCPULogic.cpu`フィールド/`trySubmitJob`、
     `AdvCraftingBlockEntity.getUnitBlock()`、`QuantumComputerMenu`(`CraftingCPUMenu`継承、
     `setCPU`/`createCpuList`/`getOrAssignCpuSerial`)、`AdvCraftingCPU.cluster`フィールド名
     (リフレクション読み取り対象、`AdvancedAeCpuAdapter`参照)— **いずれも1.20.1版Mixinが
     前提とするメンバーがそのまま存在。**
   - **【設計書に無い新規発見】`AdvCraftingCPU.writeToNBT`/`readFromNBT`も、AE2本体の
     `CraftingCPUCluster`と同様に`(CompoundTag, HolderLookup.Provider)`2引数化されている。**
     設計書7.2節はクラス名一致のみ確認・シグネチャレベルは「未検証」としていたが、これは
     Minecraft本体の1.20.5前後のNBT registry化がAdvancedAE側にも及んでいるためと判断できる。
     → ステップ9(AdvancedAE互換レイヤー移植)で`AdvCraftingCPUMixin`の`@Inject`シグネチャに
     `HolderLookup.Provider registries`引数を追加すること(`CraftingCPUClusterMixin`と同じ対応)。
6. Modrinth APIで実際の配布バージョンを確認:
   - AE2: `19.2.17`(release, loaders=neoforge, game_versions=1.21.1)
   - AdvancedAE: `1.6.11-1.21.1`(release, loaders=neoforge, game_versions=1.21.1)。
     依存先はAE2本体とGeckoLib(project id `8BmcQJ2H`)。AdvancedAEは`compileOnly`のみで
     ランタイム同梱しないため通常は影響しないはずだが、**コンパイル時にGeckoLibのクラスが
     解決できずエラーになった場合はGeckoLibも`compileOnly`で追加すること**(ビルド時の
     既知リスクとして記録)。
   - AdvancedAE `1.6.11-1.21.1`自体のビルド時`neoforge_version=21.1.209`(range `[21.1.169,)`)を
     確認、設計書が採用する`neoforge_version=21.1.169`と互換(範囲内)。
7. JDK 21が`Z:\Claude\Tools\Java\`配下に未配置だったため、Eclipse Temurin 21(21.0.11+10)を
   `Z:\Claude\Tools\Java\jdk-21\`へ配置し、`Z:\Claude\.env`に`JAVA_HOME_21`を追加(ユーザー報告済み)。
   Gradle 8.14.2(既存)はModDevGradle 2.0.74と組み合わせて動作する想定(要ビルド確認)。
8. `AE2CraftPriority-1.21.1`で`git init`(ブランチ名`main`)、1.20.1ソースそのままの状態で初回コミット。
   GitHubへの登録は行っていない。

### 確定したgradle.properties値(実装時に反映)

```
java_version=21
minecraft_version=1.21.1
neoforge_version=21.1.169
neoForge.parchment.minecraftVersion=1.21
neoForge.parchment.mappingsVersion=2024.07.07
ae2_version=19.2.17
advancedae_version=1.6.11-1.21.1
mixin_version=0.8.5 (MixinGradleプラグイン自体は不要)
```

### 既知のリスク・要フォロー事項

- ModDevGradle 2.0.74のMixin内蔵サポートが実際に`./gradlew build`まで通るかは未検証(設計書10章
  リスク表、ステップ1で最初に検証)。
- AdvancedAE compileOnly時のGeckoLib解決要否(上記6参照)。
- `AdvCraftingCPUMixin`のNBTシグネチャ変更(上記5参照、設計書に無い新規発見)。

### 次の作業

ステップ1(ツールチェイン構築)から設計書8章の順序通りに着手する。

## 2026-07-22: ステップ1〜3(ツールチェイン構築・Mod本体登録)実施記録

### 実施内容と発見した実際の不具合

1. `build.gradle`/`gradle.properties`/`settings.gradle`/`neoforge.mods.toml`(`mods.toml`から改名)を
   設計書5章のテンプレートに従って作成。`ae2craftpriority.mixins.json`/
   `ae2craftpriority-advancedae.mixins.json`の`compatibilityLevel`を`JAVA_17`→`JAVA_21`に変更。
2. `AE2CraftPriorityMod.java`を設計書4.1節通りにNeoForge向けへ書き換え(`IEventBus modEventBus`を
   直接受け取るコンストラクタ)。`ClientSetup.java`は無変更(`appeng.init.client.InitScreens`のAPI形状は
   AE2実ソースで確認済み・変更なし)。
3. 設計書2-1節(`CraftingCPUClusterMixin`のNBT引数追加)・2-4/2-5/2-12節(`MenuLocator`→
   `MenuHostLocator`リネーム、`CraftAmountMenuMixin`/`CraftConfirmMenuMixin`/
   `PriorityReturnTarget`/`CraftPriorityStepMenu`)を実装。grep で`MenuLocator`(Host以外)の
   残存が無いことを確認済み。
4. **gradle-mcp-server経由で実際に`compileJava`を実行し、設計書のテンプレートだけでは気づけない
   実際のビルド不具合を2件発見・修正した**(設計書10章が「4.3節の構成で./gradlew buildが通ることを
   最初の疎通確認ステップで必ず検証する」と警告していた通りの事態):
   - **不具合1**: `settings.gradle`の`plugins { id 'net.neoforged.moddev.repositories' version '2.0.74' }`
     と`build.gradle`の`plugins { id 'net.neoforged.moddev' version '2.0.74' }`のように両方でバージョンを
     指定すると、`Error resolving plugin ... plugin is already on the classpath with an unknown version`
     で失敗する。AE2自身の実ソースを確認したところ、**バージョンは`settings.gradle`の
     `pluginManagement { plugins { ... } }`ブロックでのみ宣言し、`settings.gradle`本体の`plugins{}`と
     `build.gradle`の`plugins{}`はバージョン指定なしで適用する**のが正しい構成だった。両ファイルを修正。
   - **不具合2**: `build.gradle`に`repositories { mavenCentral(); maven {modrinth} }`を直接書くと、
     Gradleのデフォルト解決モード(`PREFER_PROJECT`)により**`settings.gradle`側の
     `dependencyResolutionManagement`の宣言が丸ごと無視される**
     (`net.neoforged:neoform-runtime:1.0.13`のようなNeoForge本体が要求する成果物がMaven Centralにしか
     探しに行かず解決不能になり、`BUILD FAILED`)。設計書5.3節のテンプレートはAE2の実際のパターンから
     この点で乖離していた(AE2の`build.gradle`には`repositories{}`ブロックが存在しない)。
     `build.gradle`から`repositories{}`ブロックを削除し、`settings.gradle`の
     `dependencyResolutionManagement.repositories`に`https://maven.neoforged.net/releases`・
     `mavenCentral()`・Modrinth mavenを一元化することで解決。
5. 上記2件の修正後、`compileJava`が`createMinecraftArtifacts`タスク(NeoFormRuntimeによる
   Minecraft本体の復元処理)まで到達することを確認。**依存解決・Mixin設定・プラグイン構成としては
   正常に機能していることを確認できた。**

### 現在のブロッカー(2026-07-22時点、要ユーザー対応)

`createMinecraftArtifacts`タスクが、フォークした`Z:\Claude\Tools\Java\jdk-21\bin\java.exe`
(NeoFormRuntimeツール本体)の実行中に次のエラーで失敗する:

```
java.io.UncheckedIOException: java.io.IOException: Unable to establish loopback connection
Caused by: java.net.SocketException: Unrecognized Windows Sockets error: 10106: socket
    at sun.nio.ch.Net.socket0
    at sun.nio.ch.WEPollSelectorImpl.<init>
    at java.net.http.HttpClientImpl$SelectorManager.<init>
```

NeoFormRuntimeがMinecraft資材ダウンロード用に`java.net.http.HttpClient`を初期化する際、内部的に
NIO `Selector`のwakeup機構としてループバック(localhost)TCP接続を1本張る実装になっているが、
そのソケット生成がWindows側で失敗している。Windows Sockets エラー10106は一般に
`WSAEPROVIDERFAILEDINIT`(Winsockサービスプロバイダの初期化失敗)に対応し、Winsockカタログの
破損、またはアンチウイルス/VPN等がインストールするLSP(Layered Service Provider)の不整合、
あるいは新規に配置した`jdk-21\bin\java.exe`に対するファイアウォール/セキュリティソフトの
ブロックが典型的な原因。

Gradleデーモン自体(JDK 17、Maven Central等への通信は正常)は問題なく動作しているため、
**Windows全体のネットワークスタックというよりは、新規配置したJDK 21のjava.exeに固有の問題である
可能性が高い**(ダウンロード破損、またはこの実行ファイルパスに対するセキュリティソフトの挙動)。

**ユーザーに確認・対応を依頼したい事項**(管理者権限が必要な操作を含むため、Z:\Claude\CLAUDE.md
9章に従いここで作業を止めています):

1. `Z:\Claude\Tools\Java\jdk-21\bin\java.exe`に対して、Windows Defenderやアンチウイルスソフトの
   通知・ブロック履歴が無いか確認(あれば例外設定)
2. 上記で解決しない場合、`jdk-21`フォルダを一度削除し、Eclipse Temurin 21を手動で再ダウンロード・
   再配置(ダウンロード破損の可能性の切り分け)
3. それでも解決しない場合、管理者権限で`netsh winsock reset`を実行し、PC再起動
   (Winsockカタログ自体の破損が疑われる場合の一般的な対処)

解決後、`compileJava`(および`runClient`)を再実行して先へ進める。


### 2026-07-22: Windows Sockets error 10106 原因調査(ユーザーからの「原因の特定方法を教えて」への回答として実施)

上記ブロッカーについて、`gradle-mcp-server`経由でWindows実機上に一時診断タスク(`build.gradle`に
`diagWinsockCatalog`/`diagSecurityProducts`/`diagCrypto`として追加、調査完了後に削除予定)を追加し、
実際にコマンドを実行して原因を切り分けた。手順と結果は以下の通り。

**手順1: JDK21固有の問題かどうかの切り分け**
`DiagSocketTest.java`(失敗している`HttpClient.newHttpClient()`呼び出しを再現する最小コード)を
`jdk-17\bin\java.exe`と`jdk-21\bin\java.exe`の両方で個別に実行。
→ **両方のJDKで同一のエラー(Windows Sockets error 10106)が発生**。これにより「JDK21固有
(ダウンロード破損やこの実行ファイルパスへのセキュリティソフトの誤検知)」という当初の仮説は
棄却された。問題はJavaのバージョンやインストール個体に依存しない、より基盤側(OS/Winsock)の
問題である。

**手順2: Winsockカタログの破損・不正なLSPの有無を確認**
`netsh winsock show catalog`(管理者権限不要)の出力をファイルへリダイレクトして取得
(出力はCP932/Shift-JISエンコードのため`iconv -f CP932 -t UTF-8`で変換して確認)。
全40エントリを機械的に検査:
- 全プロバイダーDLLパスが`%SystemRoot%\system32\`配下の標準Microsoft製DLL
  (mswsock.dll, wshqos.dll, napinsp.dll, nlasvc.dll, wshtcpip.dll, winrnr.dll)のみ
- プロバイダーIDも全て既知の標準GUID(MSAFD TCP/IP、標準QoS、AF_UNIX、Hyper-V RAW等)
- サードパーティ(アンチウイルス/VPN等が追加する)LSPエントリは**1件も存在しない**
→ **Winsockカタログ自体は健全**。当初の仮説「アンチウイルス/VPNのLSPがカタログを汚染している」
は棄却された。

**手順3: PowerShellスクリプトによるセキュリティソフト検出(失敗)**
`Get-CimInstance -Namespace root/SecurityCenter2`等でインストール済みアンチウイルス製品を
直接照会しようとしたが、**この試み自体が失敗**。原因調査の過程で偶然、重大な事実が判明した
(手順4)。

**手順4: 【重要な発見】Windows PowerShell(5.1)自体が起動時エラーで失敗している**
`powershell.exe -NoProfile -Command "$PSVersionTable"`を実行したところ、PowerShellスクリプトが
一切実行されず、以下のエラーのみが出力された(標準出力はUTF-16LEエンコードだったため
`iconv -f UTF-16LE -t UTF-8`で変換して判読):

```
Windows PowerShell 内部エラー。マネージ Windows PowerShell の読み込みがエラー 8009001d で失敗しました。
```

`0x8009001D`は`NTE_PROV_TYPE_NOT_DEF`(暗号化サービスプロバイダーの種類が正しく定義されていない)
を示すHRESULTで、CryptoAPI(CAPI)の初期化に関するエラー。PowerShell 5.1は起動時に.NET Framework
の管理エンジンをロードする過程でコード署名検証等のためCryptoAPIを呼び出すため、この基盤が
壊れているとPowerShell自体が起動できなくなる。

**手順5: 関連する暗号化サブシステムの状態を追加確認**
- `HKLM\System\CurrentControlSet\Control\Lsa\FipsAlgorithmPolicy`の`Enabled`値 → `0`
  (FIPSモードは無効。FIPSポリシーが原因ではない)
- `certutil -csplist`(CSP一覧) → Microsoft標準の暗号化サービスプロバイダー群は全て正常に
  列挙される(Pluton等、非搭載ハードウェア向けの「実装されていません」は正常な応答)。
  ただし一覧の末尾、Smart Card Key Storage Provider付近で`CertUtil: -csplist コマンド
  エラーです: 0x80004001 (E_NOTIMPL)`が発生 — スマートカードリーダー非搭載環境では
  一般的に見られる挙動であり、これ単体が決定的な異常とは言い切れない。
- .NET Framework(2.0/3.0/3.5/4.8)のインストール状態はレジストリ上正常に見える。
- PowerShell 7(`pwsh`)は`where`コマンドで検出されず、**このマシンにはWindows PowerShell 5.1
  しか存在せず、それが起動不能な状態**であることを確認。

### 現時点での結論(2026-07-22)

一連の切り分けにより、当初報告した3つの仮説(JDK21固有の問題／Winsockカタログ破損／
サードパーティLSP)は**いずれも証拠と矛盾する**ことが判明した。一方で、**Java(2バージョンとも)
のループバックソケット生成失敗(WSAEPROVIDERFAILEDINIT, error 10106)と、Windows PowerShell自体
の起動失敗(CryptoAPIエラー 8009001D)が同時に発生している**という事実は、両者が独立した偶然の
不具合ではなく、**OS基盤側(Winsockサービスプロバイダーの初期化 および/または CryptoAPIの
初期化に関わる共通の下位コンポーネント)に何らかの障害が生じている**ことを強く示唆している。

典型的な原因としては次のようなものが考えられる(いずれも確定はしていない):
- `C:\ProgramData\Microsoft\Crypto\`配下の鍵コンテナのアクセス権限破損
  (セキュリティソフトやDLP製品によるACL変更、プロファイル破損等)
- Windows Update の不完全な適用による システムファイル/カタログの破損
- ネットワーク層・暗号層の両方にフックするタイプのセキュリティ製品(アンチウイルスの
  「HTTPS/SSL検査」機能等)による干渉。この種の製品はWinsock LSPとしてではなく、より低レベルの
  WFP(Windows Filtering Platform)コールアウトドライバやカーネルフィルタとして動作するため、
  `netsh winsock show catalog`には**そもそも表れない**(手順2で「LSPなし」だったことと矛盾しない)

**推奨する次の対応(いずれも管理者権限が必要、またはユーザー環境の変更を伴うため実行を保留し、
ユーザーの判断・実行を仰ぐ)**:

1. (低リスク・即効性は不明) `sfc /scannow` および
   `DISM /Online /Cleanup-Image /RestoreHealth` を管理者権限で実行し、Windowsシステムファイル・
   コンポーネントストアの破損を修復。CryptoAPI関連DLL/カタログの破損が原因であれば、これで
   PowerShellが正常起動するようになるはずで、Java側の症状改善も期待できる。
2. (中リスク) 管理者権限で`netsh winsock reset`を実行後、再起動。Winsockプロバイダーチェーンを
   初期状態に戻す標準的な対処で、カタログのLSP一覧には表れないタイプの不整合もリセットされる
   可能性がある。
3. (確認のみ・低リスク) タスクマネージャーまたは「Windows セキュリティ」アプリで、現在
   有効になっているウイルス対策/セキュリティ製品を確認していただきたい。特に「HTTPS/SSL
   スキャン」「ネットワーク保護」等の機能を持つサードパーティ製品(Windows Defender以外)が
   有効な場合、それを一時的に無効化した状態で`compileJava`が通るか再テストすることで、
   セキュリティ製品由来かどうかを直接切り分けられる。
4. (参考) 上記1〜3のいずれか実施後、`Z:\Claude\Projects\MinecraftMods\AE2CraftPriority-1.21.1`で
   `gradlew compileJava`(またはgradle-mcp-server経由)を再実行し、`createMinecraftArtifacts`が
   成功するか確認する。

**現状の一時診断ファイルについて**: `build.gradle`末尾の`diagWinsockCatalog`/
`diagSecurityProducts`/`diagCrypto`タスク、および`diag/`フォルダ一式は、上記ブロッカーが
解決するまで**意図的に残置**している(解決後の再検証で再度使う可能性があるため)。
ブロッカー解決後、正常にビルド・起動確認が取れた時点でこれらを削除し、`build.gradle`を
モジュール本体のみのクリーンな状態に戻す。
