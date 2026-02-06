$reportPath = "C:\SoftwareInventory.csv"
$hostname = $env:COMPUTERNAME

function To-CPE($vendor, $product, $version) {
    # egyszerű tisztítás
    $v = ($vendor -replace '[^a-zA-Z0-9]', '_').ToLower()
    $p = ($product -replace '[^a-zA-Z0-9]', '_').ToLower()
    $ver = ($version -replace '[^a-zA-Z0-9\.]', '_').ToLower()

    return "cpe:2.3:a:$v:$p:$ver:*:*:*:*:*:*:*"
}

function Normalize-InstallDate($raw) {
    if (-not $raw) { return $null }

    # YYYYMMDD → YYYY-MM-DD
    if ($raw -match '^\d{8}$') {
        return "{0}-{1}-{2}" -f $raw.Substring(0,4), $raw.Substring(4,2), $raw.Substring(6,2)
    }

    # egyéb formátum → hagyjuk érintetlenül
    return $raw
}

# Win32 programok
$paths = @(
  "HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*",
  "HKLM:\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*"
)

$win32_raw = Get-ItemProperty $paths | Where-Object { $_.DisplayName }

$win32 = $win32_raw | ForEach-Object {
    $vendor  = $_.Publisher
    $product = $_.DisplayName
    $version = $_.DisplayVersion

    # null-safe értékek CPE-hez
    $v0   = $vendor  ?? "unknown"
    $p0   = $product ?? "unknown"
    $ver0 = $version ?? "unknown"

    [PSCustomObject]@{
        hostname    = $hostname
        product     = $product
        vendor      = $vendor
        version     = $version
        installedOn = Normalize-InstallDate $_.InstallDate
        source      = "Win32"
        cpe         = To-CPE $v0 $p0 $ver0
    }
}

# Store appok
$store_raw = Get-AppxPackage

$store = $store_raw | ForEach-Object {
    $vendor  = $_.Publisher
    $product = $_.Name
    $version = $_.Version

    # null-safe értékek CPE-hez
    $v0   = $vendor  ?? "unknown"
    $p0   = $product ?? "unknown"
    $ver0 = $version ?? "unknown"

    [PSCustomObject]@{
        hostname    = $hostname
        product     = $product
        vendor      = $vendor
        version     = $version
        installedOn = $null
        source      = "Store"
        cpe         = To-CPE $v0 $p0 $ver0
    }
}

# Egyesített lista
$all = $win32 + $store

# Export
$all | Export-Csv -Path $reportPath -NoTypeInformation -Encoding UTF8

Write-Host "Szoftverleltár elkészült: $reportPath"

# Upload to API if requested (defaults to localhost)
$apiUrl = $env:API_URL
if (-not $apiUrl) { $apiUrl = 'http://localhost:8080/api/inventory/import' }

Write-Host "Feltöltés: $apiUrl"

try {
    Write-Host "Uploading $reportPath to $apiUrl using Invoke-RestMethod..."
    $form = @{ file = Get-Item -Path $reportPath }
    $resp = Invoke-RestMethod -Uri $apiUrl -Method Post -Form $form -ErrorAction Stop
    Write-Host "Upload successful. Response:`n$($resp | Out-String)"
} catch {
    Write-Warning "Invoke-RestMethod failed: $($_.Exception.Message)"
    Write-Host "Falling back to curl.exe if available..."
    try {
        $curlPath = (Get-Command curl.exe -ErrorAction SilentlyContinue).Source
        if ($null -ne $curlPath) {
            & $curlPath -sS -F "file=@$reportPath" $apiUrl
            Write-Host "Upload via curl.exe finished."
        } else {
            Write-Error "curl.exe not found and Invoke-RestMethod failed. Upload aborted."
            exit 1
        }
    } catch {
        Write-Error "Fallback upload failed: $($_.Exception.Message)"
        exit 1
    }
}
