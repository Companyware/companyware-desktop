param(
    [Parameter(Mandatory = $true)][string]$PluginDirectory,
    [Parameter(Mandatory = $true)][string]$Descriptor,
    [Parameter(Mandatory = $true)][string]$Output,
    [string]$ClassesRoot = 'target/classes'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$projectRoot = Split-Path -Parent $PSScriptRoot
$classesRoot = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $ClassesRoot))
$pluginRoot = Join-Path $classesRoot $PluginDirectory
$descriptorPath = Join-Path $projectRoot $Descriptor
if (-not (Test-Path -LiteralPath $pluginRoot -PathType Container)) { throw "Compile the project first; missing $pluginRoot" }
if (-not (Test-Path -LiteralPath $descriptorPath -PathType Leaf)) { throw "Missing descriptor: $descriptorPath" }

$temporary = Join-Path ([System.IO.Path]::GetTempPath()) ("companyware-plugin-" + [guid]::NewGuid())
$jarPath = Join-Path $temporary 'plugin.jar'
$packageRoot = Join-Path $temporary 'package'
New-Item -ItemType Directory -Path $packageRoot | Out-Null
try {
    $jar = [System.IO.Compression.ZipFile]::Open($jarPath, 'Create')
    try {
        Get-ChildItem -LiteralPath $pluginRoot -Recurse -File | ForEach-Object {
            $relative = $_.FullName.Substring($classesRoot.Length + 1).Replace('\', '/')
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($jar, $_.FullName, $relative) | Out-Null
        }
    } finally { $jar.Dispose() }
    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $packageRoot 'plugin.jar')
    Copy-Item -LiteralPath $descriptorPath -Destination (Join-Path $packageRoot 'plugin.yaml')
    $outputPath = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $Output))
    $outputDirectory = Split-Path -Parent $outputPath
    New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
    if (Test-Path -LiteralPath $outputPath) { Remove-Item -LiteralPath $outputPath }
    [System.IO.Compression.ZipFile]::CreateFromDirectory($packageRoot, $outputPath)
    Write-Output $outputPath
} finally {
    if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary -Recurse -Force }
}
