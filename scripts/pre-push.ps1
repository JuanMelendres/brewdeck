Write-Host "Running full verification before push..."
Set-Location brewdeck-api
.\mvnw.cmd clean verify pmd:check

$env:JAVA_HOME="C:\Program Files\Java\jdk-21.0.10"
$env:Path="$env:JAVA_HOME\bin;$env:Path"

if ($LASTEXITCODE -ne 0) {
    Write-Host "Pre-push validation failed."
    exit 1
}

Set-Location ..
Write-Host "Pre-push checks passed."