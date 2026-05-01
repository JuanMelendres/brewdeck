Write-Host "Running full verification before push..."
Set-Location brewdeck-api
.\mvnw.cmd clean verify pmd:check

$javaHome = "C:\Program Files\Java\jdk-21.0.10"

if (!(Test-Path "$javaHome\bin\java.exe")) {
    Write-Host "❌ Java 21 not found at $javaHome"
    exit 1
}

$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"

Write-Host "✅ Using JAVA_HOME=$env:JAVA_HOME"
& "$env:JAVA_HOME\bin\java.exe" -version

if ($LASTEXITCODE -ne 0) {
    Write-Host "Pre-push validation failed."
    exit 1
}

Set-Location ..
Write-Host "Pre-push checks passed."