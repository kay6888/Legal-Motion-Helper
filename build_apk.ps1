# build_apk.ps1
# Builds a signed/unsigned APK from the LegalMotion project
Write-Host "Building LegalMotion APK..." -ForegroundColor Cyan

# Ensure JAVA_HOME and ANDROID_HOME are set
$jdkPath = Get-ChildItem "C:\Program Files\Microsoft" -Filter "jdk-17*" -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
if (-not $jdkPath) {
    $jdkPath = "$env:LOCALAPPDATA\Programs\Java\jdk-17"
}
if ($jdkPath -and (Test-Path $jdkPath)) {
    $env:JAVA_HOME = $jdkPath
    $env:Path = "$jdkPath\bin;" + $env:Path
}
$env:ANDROID_HOME = "$env:USERPROFILE\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$adbPath = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
if (Test-Path $adbPath) {
    $env:Path = "$(Split-Path $adbPath);" + $env:Path
}

# Clean previous builds
.\gradlew clean

# Build debug APK (unsigned, but installable)
.\gradlew assembleDebug

if (Test-Path "app\build\outputs\apk\debug\app-debug.apk") {
    Write-Host "APK created at: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Green
    # Optionally install on connected device
    $choice = Read-Host "Install on connected device? (y/n)"
    if ($choice -eq 'y') {
        if (-not (Test-Path $adbPath)) {
            Write-Host "adb not found at $adbPath" -ForegroundColor Red
            exit 1
        }

        $deviceLines = & $adbPath devices | Select-Object -Skip 1 | Where-Object { $_.Trim() }
        $authorizedDevice = $deviceLines | Where-Object { $_ -match "\sdevice$" } | Select-Object -First 1
        $unauthorizedDevice = $deviceLines | Where-Object { $_ -match "\sunauthorized$" } | Select-Object -First 1
        $offlineDevice = $deviceLines | Where-Object { $_ -match "\soffline$" } | Select-Object -First 1

        if ($authorizedDevice) {
            & $adbPath install -r "app\build\outputs\apk\debug\app-debug.apk"
        } elseif ($unauthorizedDevice) {
            Write-Host "Connected device is unauthorized. Unlock the phone, accept the USB debugging prompt, then run the script again." -ForegroundColor Yellow
            Write-Host "Current adb device: $unauthorizedDevice" -ForegroundColor Yellow
            exit 1
        } elseif ($offlineDevice) {
            Write-Host "Connected device is offline. Reconnect USB, toggle USB debugging, then run the script again." -ForegroundColor Yellow
            Write-Host "Current adb device: $offlineDevice" -ForegroundColor Yellow
            exit 1
        } else {
            Write-Host "No authorized Android device detected. Connect a device with USB debugging enabled, then run the script again." -ForegroundColor Yellow
            exit 1
        }
    }
} else {
    Write-Host "Build failed!" -ForegroundColor Red
}
