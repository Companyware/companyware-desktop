param(
    [string]$OutputDirectory = 'target/release'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$outputRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot $OutputDirectory))
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

    # Spring Boot 2.1 preserves varying ZIP metadata. Normalize the executable JAR
    # before hashing so equal source commits produce byte-identical artifacts.
    Add-Type -AssemblyName System.IO.Compression
    $jarSource = [IO.Path]::GetFullPath('target/Companyware.jar')
    $jarNormalized = [IO.Path]::GetFullPath('target/Companyware.normalized.jar')
    $inputStream = [IO.File]::OpenRead($jarSource)
    try {
        $inputZip = [IO.Compression.ZipArchive]::new($inputStream, [IO.Compression.ZipArchiveMode]::Read, $false)
        try {
            $outputStream = [IO.File]::Open($jarNormalized, [IO.FileMode]::Create)
            try {
                $outputZip = [IO.Compression.ZipArchive]::new($outputStream, [IO.Compression.ZipArchiveMode]::Create, $false)
                try {
                    $inputZip.Entries | Sort-Object FullName | ForEach-Object {
                        $entry = $outputZip.CreateEntry($_.FullName, [IO.Compression.CompressionLevel]::NoCompression)
                        $entry.LastWriteTime = [DateTimeOffset]::Parse('2026-09-10T00:00:00Z')
                        if ($_.Name) {
                            $entryInput = $_.Open()
                            $entryOutput = $entry.Open()
                            try { $entryInput.CopyTo($entryOutput) } finally { $entryOutput.Dispose(); $entryInput.Dispose() }
                        }
                    }
                } finally { $outputZip.Dispose() }
            } finally { $outputStream.Dispose() }
        } finally { $inputZip.Dispose() }
    } finally { $inputStream.Dispose() }
    Move-Item -LiteralPath $jarNormalized -Destination $jarSource -Force

    & (Join-Path $PSScriptRoot 'verify-dependencies.ps1') -OutputDirectory $outputRoot
    if ($LASTEXITCODE -ne 0) { throw 'Dependency review failed.' }

    $stageName = "companyware-desktop-$version"
    $stage = Join-Path $outputRoot $stageName
    if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $stage | Out-Null
    Copy-Item -LiteralPath 'target/Companyware.jar' -Destination (Join-Path $stage 'Companyware.jar')
    Copy-Item -LiteralPath 'tools/start-companyware.cmd' -Destination (Join-Path $stage 'start-companyware.cmd')
    Copy-Item -LiteralPath 'README.md','CHANGELOG.md','LICENSE','SECURITY.md','SUPPORT.md' -Destination $stage
    Copy-Item -LiteralPath 'docs' -Destination $stage -Recurse
    Copy-Item -LiteralPath (Join-Path $outputRoot 'dependency-inventory.txt') -Destination $stage
    $commit = (git rev-parse HEAD).Trim()
    Set-Content -LiteralPath (Join-Path $stage 'BUILD-COMMIT') -Encoding Ascii -NoNewline -Value $commit

    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $stage 'Companyware.jar')).Hash.ToLowerInvariant()
    Set-Content -LiteralPath (Join-Path $stage 'Companyware.jar.sha256') -Encoding Ascii -NoNewline -Value "$hash  Companyware.jar"

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
    $archiveHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $archive).Hash.ToLowerInvariant()
    Set-Content -LiteralPath "$archive.sha256" -Encoding Ascii -NoNewline -Value "$archiveHash  $archiveName"
    $inventoryHash = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $outputRoot 'dependency-inventory.txt')).Hash.ToLowerInvariant()
    $evidence = [ordered]@{
        format = 'companyware-release-evidence-1'
        product = 'Companyware Desktop'
        version = $version
        commit = $commit
        artifacts = [ordered]@{
            jar = [ordered]@{ file = 'Companyware.jar'; sha256 = $hash }
            archive = [ordered]@{ file = $archiveName; sha256 = $archiveHash }
            dependencyInventory = [ordered]@{ file = 'dependency-inventory.txt'; sha256 = $inventoryHash }
        }
    }
    $evidence | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $outputRoot 'release-evidence.json') -Encoding UTF8
    Get-FileHash -Algorithm SHA256 -LiteralPath $archive
} finally {
    Pop-Location
}
