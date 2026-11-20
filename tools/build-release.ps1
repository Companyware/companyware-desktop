param(
    [string]$OutputDirectory = 'target/release'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$outputRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot $OutputDirectory))

function Get-Sha256([string]$Path) {
    $algorithm = [Security.Cryptography.SHA256]::Create()
    $stream = [IO.File]::OpenRead($Path)
    try {
        return ([BitConverter]::ToString($algorithm.ComputeHash($stream))).Replace('-', '').ToLowerInvariant()
    } finally {
        $stream.Dispose()
        $algorithm.Dispose()
    }
}

$javaHome = $env:JAVA_HOME
if (-not $javaHome -or -not (Test-Path -LiteralPath (Join-Path $javaHome 'bin/javac.exe'))) {
    throw 'JAVA_HOME must point to a JDK (Java 8 or newer).'
}

Push-Location $projectRoot
try {
    [xml]$pom = Get-Content -LiteralPath 'pom.xml' -Raw
    $version = [string]$pom.project.version
    if (-not $version -or $version.EndsWith('-SNAPSHOT', [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Release artifacts require an explicit non-SNAPSHOT version.'
    }
    & .\mvnw.cmd clean verify
    if ($LASTEXITCODE -ne 0) { throw "Maven verification failed with exit code $LASTEXITCODE" }

    # Keep the Spring Boot JAR exactly as produced. Repacking it with generic ZIP
    # APIs can compress nested dependency JARs and make the application unbootable.
    Add-Type -AssemblyName System.IO.Compression

    & .\mvnw.cmd -Pwindows-exe launch4j:launch4j
    if ($LASTEXITCODE -ne 0) { throw "Windows EXE build failed with exit code $LASTEXITCODE" }
    if (-not (Test-Path -LiteralPath 'target/Companyware.exe' -PathType Leaf)) {
        throw 'Launch4j completed without creating target/Companyware.exe.'
    }

    & (Join-Path $PSScriptRoot 'verify-dependencies.ps1') -OutputDirectory $outputRoot
    if ($LASTEXITCODE -ne 0) { throw 'Dependency review failed.' }

    $stageName = "companyware-desktop-$version"
    $stage = Join-Path $outputRoot $stageName
    if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $stage | Out-Null
    Copy-Item -LiteralPath 'target/Companyware.jar' -Destination (Join-Path $stage 'Companyware.jar')
    Copy-Item -LiteralPath 'target/Companyware.exe' -Destination (Join-Path $stage 'Companyware.exe')
    Copy-Item -LiteralPath 'tools/start-companyware.cmd' -Destination (Join-Path $stage 'start-companyware.cmd')
    Copy-Item -LiteralPath 'README.md','CHANGELOG.md','LICENSE','SECURITY.md','SUPPORT.md' -Destination $stage
    Copy-Item -LiteralPath 'docs' -Destination $stage -Recurse
    Copy-Item -LiteralPath (Join-Path $outputRoot 'dependency-inventory.txt') -Destination $stage
    $commit = (git rev-parse HEAD).Trim()
    Set-Content -LiteralPath (Join-Path $stage 'BUILD-COMMIT') -Encoding Ascii -NoNewline -Value $commit

    $hash = Get-Sha256 (Join-Path $stage 'Companyware.jar')
    Set-Content -LiteralPath (Join-Path $stage 'Companyware.jar.sha256') -Encoding Ascii -NoNewline -Value "$hash  Companyware.jar"
    $exeHash = Get-Sha256 (Join-Path $stage 'Companyware.exe')
    Set-Content -LiteralPath (Join-Path $stage 'Companyware.exe.sha256') -Encoding Ascii -NoNewline -Value "$exeHash  Companyware.exe"

    $archiveName = "$stageName.zip"
    $archive = Join-Path $outputRoot $archiveName
    if (Test-Path -LiteralPath $archive) { Remove-Item -LiteralPath $archive }
    $stream = [IO.File]::Open($archive, [IO.FileMode]::CreateNew)
    try {
        $zip = [IO.Compression.ZipArchive]::new($stream, [IO.Compression.ZipArchiveMode]::Create, $false)
        try {
            Get-ChildItem -LiteralPath $stage -Recurse -File | Sort-Object FullName | ForEach-Object {
                $relative = $stageName + '/' + $_.FullName.Substring($stage.Length + 1).Replace('\', '/')
                $entry = $zip.CreateEntry($relative, [IO.Compression.CompressionLevel]::NoCompression)
                $entry.LastWriteTime = [DateTimeOffset]::Parse('2026-09-10T00:00:00Z')
                $input = [IO.File]::OpenRead($_.FullName)
                $output = $entry.Open()
                try { $input.CopyTo($output) } finally { $output.Dispose(); $input.Dispose() }
            }
        } finally { $zip.Dispose() }
    } finally { $stream.Dispose() }
    $archiveHash = Get-Sha256 $archive
    Set-Content -LiteralPath "$archive.sha256" -Encoding Ascii -NoNewline -Value "$archiveHash  $archiveName"
    $inventoryHash = Get-Sha256 (Join-Path $outputRoot 'dependency-inventory.txt')
    $evidence = [ordered]@{
        format = 'companyware-release-evidence-1'
        product = 'Companyware Desktop'
        version = $version
        commit = $commit
        artifacts = [ordered]@{
            jar = [ordered]@{ file = 'Companyware.jar'; sha256 = $hash }
            exe = [ordered]@{ file = 'Companyware.exe'; sha256 = $exeHash }
            archive = [ordered]@{ file = $archiveName; sha256 = $archiveHash }
            dependencyInventory = [ordered]@{ file = 'dependency-inventory.txt'; sha256 = $inventoryHash }
        }
    }
    $evidence | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $outputRoot 'release-evidence.json') -Encoding UTF8
    Write-Output "SHA256 $archiveHash $archive"
} finally {
    Pop-Location
}
