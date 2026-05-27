param(
    [Parameter(Mandatory=$true)]
    [string]$Source
)

Add-Type -AssemblyName System.Drawing

if (-not (Test-Path $Source)) {
    Write-Error "Source file not found: $Source"
    exit 1
}

$resDir = Join-Path $PSScriptRoot "..\app\src\main\res"

$densities = [ordered]@{
    "mipmap-mdpi"    = 48
    "mipmap-hdpi"    = 72
    "mipmap-xhdpi"   = 96
    "mipmap-xxhdpi"  = 144
    "mipmap-xxxhdpi" = 192
}

$adaptiveSizePx = 432

function Resize-Image {
    param([string]$In, [string]$Out, [int]$W, [int]$H)
    $src = [System.Drawing.Image]::FromFile($In)
    $bmp = New-Object System.Drawing.Bitmap($W, $H)
    $g   = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode  = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode      = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode    = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.DrawImage($src, 0, 0, $W, $H)
    $bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bmp.Dispose(); $src.Dispose()
}

foreach ($entry in $densities.GetEnumerator()) {
    $dir = Join-Path $resDir $entry.Key
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }

    $size = $entry.Value

    $outFile = Join-Path $dir "ic_launcher.png"
    Resize-Image -In $Source -Out $outFile -W $size -H $size
    Write-Host "  OK $($entry.Key)/ic_launcher.png ($size x $size px)"

    $outRound = Join-Path $dir "ic_launcher_round.png"
    Resize-Image -In $Source -Out $outRound -W $size -H $size
    Write-Host "  OK $($entry.Key)/ic_launcher_round.png ($size x $size px)"
}

$fgDir = Join-Path $resDir "mipmap-xxxhdpi"
if (-not (Test-Path $fgDir)) { New-Item -ItemType Directory -Path $fgDir | Out-Null }

$src    = [System.Drawing.Image]::FromFile($Source)
$canvas = New-Object System.Drawing.Bitmap($adaptiveSizePx, $adaptiveSizePx)
$g      = [System.Drawing.Graphics]::FromImage($canvas)
$g.InterpolationMode  = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.SmoothingMode      = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.PixelOffsetMode    = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$g.Clear([System.Drawing.Color]::Transparent)
$g.DrawImage($src, 0, 0, $adaptiveSizePx, $adaptiveSizePx)

$fgPath = Join-Path $fgDir "ic_launcher_foreground.png"
$canvas.Save($fgPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $canvas.Dispose(); $src.Dispose()
Write-Host "  OK mipmap-xxxhdpi/ic_launcher_foreground.png ($adaptiveSizePx x $adaptiveSizePx px)"

Write-Host ""
Write-Host "All icons generated successfully!" -ForegroundColor Green
