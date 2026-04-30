$javaHome = "C:\Program Files\Java\jdk-21.0.10"

if (!(Test-Path "$javaHome\bin\java.exe")) {
    Write-Host "Java 21 not found at: $javaHome"
    exit 1
}

$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"

Write-Host "JAVA_HOME=$env:JAVA_HOME"
& "$env:JAVA_HOME\bin\java.exe" -version

Set-Location "$PSScriptRoot\.."
Set-Location "brewdeck-api"

Write-Host "Running Spotless check..."
.\mvnw.cmd spotless:check

if ($LASTEXITCODE -ne 0) {
    Write-Host "Spotless failed. Run: .\mvnw.cmd spotless:apply"
    exit 1
}

Write-Host "Running tests..."
.\mvnw.cmd test

if ($LASTEXITCODE -ne 0) {
    Write-Host "Tests failed."
    exit 1
}

Write-Host "Pre-commit checks passed."