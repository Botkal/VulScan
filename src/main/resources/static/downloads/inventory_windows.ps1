$reportPath = "C:\SoftwareInventory.csv"
$hostname = $env:COMPUTERNAME

[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
chcp 65001 > $null

function To-CPE($vendor, $product, $version) {
    $v = ($vendor -replace '[^a-zA-Z0-9]', '_').ToLower()
    $p = ($product -replace '[^a-zA-Z0-9]', '_').ToLower()
    $ver = ($version -replace '[^a-zA-Z0-9\.]', '_').ToLower()

    return "cpe:2.3:a:${v}:${p}:${ver}:*:*:*:*:*:*:*"
}

function Normalize-InstallDate($raw) {
    if (-not $raw) { return $null }
    if ($raw -match '^\d{8}$') {
        return "{0}-{1}-{2}" -f $raw.Substring(0,4), $raw.Substring(4,2), $raw.Substring(6,2)
    }
    return $raw
}

function Limit-Text($value, $maxLen = 255) {
    if ($null -eq $value) { return $null }
    $text = [string]$value
    if ($text.Length -le $maxLen) { return $text }
    return $text.Substring(0, $maxLen)
}

$paths = @(
  "HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*",
  "HKLM:\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*"
)

$win32_raw = Get-ItemProperty $paths | Where-Object { $_.DisplayName }

$win32 = $win32_raw | ForEach-Object {
    $vendor  = $_.Publisher
    $product = $_.DisplayName
    $version = Limit-Text $_.DisplayVersion 100

    $v0   = if ($vendor)  { $vendor }  else { "unknown" }
    $p0   = if ($product) { $product } else { "unknown" }
    $ver0 = if ($version) { $version } else { "unknown" }

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

$store_raw = Get-AppxPackage

$store = $store_raw | ForEach-Object {
    $vendor  = $_.Publisher
    $product = $_.Name
    $version = Limit-Text $_.Version 100

    $v0   = if ($vendor)  { $vendor }  else { "unknown" }
    $p0   = if ($product) { $product } else { "unknown" }
    $ver0 = if ($version) { $version } else { "unknown" }

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

$all = $win32 + $store

# UTF-8 BOM nélkül
$csv = $all | ConvertTo-Csv -NoTypeInformation
if ($csv.Count -gt 0) {
    $csv[0] = $csv[0] -replace "^`uFEFF", ''
}
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllLines($reportPath, $csv, $utf8NoBom)

Write-Host "Software inventory CSV created: $reportPath"

$apiUrl = $env:API_URL
if (-not $apiUrl) { $apiUrl = 'http://localhost:8080/api/inventory/import' }

$authUrl = $env:AUTH_URL
if (-not $authUrl) {
    if ($apiUrl -match '/api/inventory/import$') {
        $authUrl = ($apiUrl -replace '/api/inventory/import$', '/api/auth/login')
    } else {
        $authUrl = 'http://localhost:8080/api/auth/login'
    }
}

$apiToken = $null
$apiUser = 'superadmin@vulscan.local'
$apiPass = 'SuperAdmin123!'

Write-Host "Upload target: $apiUrl"

if (-not $apiToken -and $apiUser -and $apiPass) {
    try {
        $loginPayload = @{ email = $apiUser; password = $apiPass } | ConvertTo-Json
        $loginResp = Invoke-RestMethod -Uri $authUrl -Method Post -Body $loginPayload -ContentType 'application/json'
        if ($loginResp -and $loginResp.accessToken) {
            $apiToken = $loginResp.accessToken
            Write-Host "JWT token acquired: $authUrl"
        }
    }
    catch {
        Write-Warning "Login failed: $($_.Exception.Message)"
    }
}

if (-not $apiToken) {
    Write-Warning "No API token. Check credentials and auth endpoint settings."
}

try {
    $boundary = [System.Guid]::NewGuid().ToString()
    $LF = "`r`n"

    $fileBytes = [System.IO.File]::ReadAllBytes($reportPath)
    $fileContent = [System.Text.Encoding]::UTF8.GetString($fileBytes)

    $bodyLines = (
        "--$boundary",
        "Content-Disposition: form-data; name=`"file`"; filename=`"SoftwareInventory.csv`"",
        "Content-Type: text/csv$LF",
        $fileContent,
        "--$boundary--$LF"
    ) -join $LF

    $headers = @{}
    if ($apiToken) {
        $headers['Authorization'] = "Bearer $apiToken"
    }

    Invoke-RestMethod -Uri $apiUrl -Method Post -Body $bodyLines -ContentType "multipart/form-data; boundary=$boundary" -Headers $headers
    Write-Host "Upload successful."
}
catch {
    $statusCode = $null
    $responseBody = $null
    if ($_.Exception.Response) {
        try {
            $statusCode = $_.Exception.Response.StatusCode.value__
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            $reader.Dispose()
        } catch {}
    }

    if ($statusCode -eq 401 -or $statusCode -eq 403) {
        Write-Warning "Upload failed ($statusCode): missing permission or invalid token."
        if ($responseBody) { Write-Warning "Server response: $responseBody" }
    }
    else {
        if ($responseBody) {
            Write-Warning "Upload failed: $responseBody"
        } else {
            Write-Warning "Upload failed: $($_.Exception.Message)"
        }
    }
}
