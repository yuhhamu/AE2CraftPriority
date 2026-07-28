$out = "Z:\Claude\Projects\MinecraftMods\AE2CraftPriority-1.21.1\diag\av-products.txt"
"=== AntivirusProduct (SecurityCenter2) ===" | Out-File -FilePath $out -Encoding utf8
try {
    Get-CimInstance -Namespace root/SecurityCenter2 -ClassName AntivirusProduct |
        Select-Object displayName, productState, pathToSignedProductExe |
        Format-List | Out-File -FilePath $out -Encoding utf8 -Append
} catch {
    "ERROR: $($_.Exception.Message)" | Out-File -FilePath $out -Encoding utf8 -Append
}
"=== Get-Service (security-related) ===" | Out-File -FilePath "Z:\Claude\Projects\MinecraftMods\AE2CraftPriority-1.21.1\diag\av-products.txt" -Encoding utf8 -Append
Get-Service | Where-Object {
    $_.DisplayName -match "Defender|Antivirus|Security|Kaspersky|Norton|McAfee|Avast|AVG|ESET|Bitdefender|Sophos|Trend|Malwarebytes|CrowdStrike|SentinelOne|Cortex|Carbon|Webroot|F-Secure|VPN|Firewall|Zscaler|Netskope|WFP"
} | Select-Object Name, DisplayName, Status |
    Format-Table -AutoSize | Out-File -FilePath "Z:\Claude\Projects\MinecraftMods\AE2CraftPriority-1.21.1\diag\av-products.txt" -Encoding utf8 -Append

"=== Firewall rules referencing java.exe ===" | Out-File -FilePath "Z:\Claude\Projects\MinecraftMods\AE2CraftPriority-1.21.1\diag\av-products.txt" -Encoding utf8 -Append
Get-NetFirewallApplicationFilter | Where-Object { $_.Program -like "*java.exe*" } |
    ForEach-Object {
        $rule = $_ | Get-NetFirewallRule
        [PSCustomObject]@{ Program = $_.Program; RuleName = $rule.DisplayName; Enabled = $rule.Enabled; Action = $rule.Action; Direction = $rule.Direction }
    } | Format-Table -AutoSize | Out-File -FilePath "Z:\Claude\Projects\MinecraftMods\AE2CraftPriority-1.21.1\diag\av-products.txt" -Encoding utf8 -Append

"=== Installed WFP callout drivers (non-Microsoft, filtered) ===" | Out-File -FilePath "Z:\Claude\Projects\MinecraftMods\AE2CraftPriority-1.21.1\diag\av-products.txt" -Encoding utf8 -Append
Get-CimInstance -ClassName Win32_SystemDriver | Where-Object { $_.State -eq "Running" -and $_.PathName -notmatch "system32" } |
    Select-Object Name, DisplayName, PathName | Format-Table -AutoSize |
    Out-File -FilePath "Z:\Claude\Projects\MinecraftMods\AE2CraftPriority-1.21.1\diag\av-products.txt" -Encoding utf8 -Append
