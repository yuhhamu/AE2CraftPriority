

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
