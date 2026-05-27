<#
.SYNOPSIS
    Installs JDK, Android SDK, VSCode extensions, and configures build script for LegalMotion APK.
.DESCRIPTION
    One‑time setup to build Android APKs from VSCode (no Android Studio required).
#>

Write-Host "=== VSCode Android APK Builder Setup ===" -ForegroundColor Cyan

# -------------------------------
# 1. Install Java JDK 17 if not present
# -------------------------------
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
$installedJdk = $null
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    $installedJdk = $env:JAVA_HOME
} else {
    $installedJdk = Get-ChildItem "C:\Program Files\Microsoft" -Filter "jdk-17*" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
    if (-not $installedJdk) {
        $installedJdk = Get-ChildItem "$env:LOCALAPPDATA\Programs\Java" -Filter "jdk-17*" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
    }
}
$javaPath = if ($javaCmd) { $javaCmd.Source } elseif ($installedJdk) { Join-Path $installedJdk "bin\java.exe" } else { $null }
if ($installedJdk) {
    $env:JAVA_HOME = $installedJdk
    $env:Path = "$installedJdk\bin;" + $env:Path
}
if (-not $javaPath -or -not (& $javaPath -version 2>&1 | Select-String "17")) {
    Write-Host "Installing Java JDK 17 (ZIP, no admin required)..." -ForegroundColor Yellow
    $jdkUrl = "https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip"
    $jdkZip  = "$env:TEMP\jdk17.zip"
    $jdkDest = "$env:LOCALAPPDATA\Programs\Java\jdk-17"
    Invoke-WebRequest -Uri $jdkUrl -OutFile $jdkZip
    if (Test-Path $jdkDest) { Remove-Item $jdkDest -Recurse -Force }
    Expand-Archive -Path $jdkZip -DestinationPath "$env:LOCALAPPDATA\Programs\Java" -Force
    # Rename the extracted folder (Microsoft JDK zip contains a versioned subfolder)
    $extracted = Get-ChildItem "$env:LOCALAPPDATA\Programs\Java" -Directory | Where-Object { $_.Name -like "jdk-17*" } | Select-Object -First 1
    if ($extracted -and $extracted.FullName -ne $jdkDest) {
        Rename-Item -Path $extracted.FullName -NewName "jdk-17"
    }
    Remove-Item $jdkZip -ErrorAction SilentlyContinue
    # Set JAVA_HOME
    # Find the extracted JDK folder (winget installs to C:\Program Files\Microsoft\jdk-17*)
    $msJdk = Get-ChildItem "C:\Program Files\Microsoft" -Filter "jdk-17*" -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
    if (-not $msJdk) { $msJdk = $jdkDest }
    [System.Environment]::SetEnvironmentVariable("JAVA_HOME", $msJdk, "User")
    $env:JAVA_HOME = $msJdk
    $env:Path += ";$env:JAVA_HOME\bin"
    Write-Host "Java JDK 17 installed." -ForegroundColor Green
} else {
    Write-Host "Java JDK 17 already present." -ForegroundColor Green
}

# -------------------------------
# 2. Install Android SDK Command-line Tools
# -------------------------------
$androidHome = "$env:USERPROFILE\AppData\Local\Android\Sdk"
if (-not (Test-Path "$androidHome\cmdline-tools\latest\bin\sdkmanager.bat")) {
    Write-Host "Downloading Android SDK command-line tools..." -ForegroundColor Yellow
    $sdkUrl = "https://dl.google.com/android/repository/commandlinetools-win-14742923_latest.zip"
    $zipPath = "$env:TEMP\cmdline-tools.zip"
    Invoke-WebRequest -Uri $sdkUrl -OutFile $zipPath
    Expand-Archive -Path $zipPath -DestinationPath "$env:TEMP\cmdline-tools"
    New-Item -ItemType Directory -Force -Path "$androidHome\cmdline-tools" | Out-Null
    Move-Item -Path "$env:TEMP\cmdline-tools\cmdline-tools" -Destination "$androidHome\cmdline-tools\latest" -Force
    Remove-Item $zipPath
} else {
    Write-Host "Android SDK already present." -ForegroundColor Green
}

# Set environment variables
[System.Environment]::SetEnvironmentVariable("ANDROID_HOME", $androidHome, "User")
[System.Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $androidHome, "User")
$env:ANDROID_HOME = $androidHome
$env:ANDROID_SDK_ROOT = $androidHome
$env:Path += ";$androidHome\platform-tools;$androidHome\cmdline-tools\latest\bin"

# Accept licenses and ensure required SDK packages are present on every run
$sdkmanager = "$androidHome\cmdline-tools\latest\bin\sdkmanager.bat"
& $sdkmanager --licenses --sdk_root=$androidHome
& $sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" --sdk_root=$androidHome
Write-Host "Android SDK packages verified." -ForegroundColor Green

# -------------------------------
# 3. Install VSCode extensions (if VSCode is installed)
# -------------------------------
$vscode = Get-Command code -ErrorAction SilentlyContinue
if ($vscode) {
    Write-Host "Installing VSCode extensions..." -ForegroundColor Yellow
    $extensions = @(
        "mathiasfrohlich.Kotlin",
        "fwcd.kotlin",
        "naco-siren.gradle-language",
        "vscjava.vscode-gradle",
        "richardwillis.vscode-gradle-extension-pack"
    )
    foreach ($ext in $extensions) {
        code --install-extension $ext --force
    }
    Write-Host "VSCode extensions installed." -ForegroundColor Green
} else {
    Write-Host "VSCode not found. Please install VSCode first from https://code.visualstudio.com/" -ForegroundColor Red
}

# -------------------------------
# 4. Create build script inside LegalMotion project
# -------------------------------
$legalMotionDir = $PSScriptRoot
if (-not $legalMotionDir) { $legalMotionDir = $PWD }

$buildScript = @'
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

# Clean previous builds
.\gradlew clean

# Build debug APK (unsigned, but installable)
.\gradlew assembleDebug

if (Test-Path "app\build\outputs\apk\debug\app-debug.apk") {
    Write-Host "APK created at: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Green
    # Optionally install on connected device
    $choice = Read-Host "Install on connected device? (y/n)"
    if ($choice -eq 'y') {
        adb install -r app\build\outputs\apk\debug\app-debug.apk
    }
} else {
    Write-Host "Build failed!" -ForegroundColor Red
}
'@

$buildScriptPath = Join-Path $legalMotionDir "build_apk.ps1"
$buildScript | Out-File -FilePath $buildScriptPath -Encoding utf8
Write-Host "Build script created at $buildScriptPath" -ForegroundColor Green

Write-Host "`n=== Setup Complete ===" -ForegroundColor Cyan
Write-Host "Next steps:"
Write-Host "1. Open the LegalMotion folder in VSCode."
Write-Host "2. Open Terminal (Ctrl+`) and run: .\build_apk.ps1"
Write-Host "3. The APK will be in app\build\outputs\apk\debug\"
Write-Host "   You can also install it directly on an Android device via USB debugging."