# Companyware
# Copyright (c) 2026 Companyware contributors
#
# SPDX-License-Identifier: MIT
# Licensed under the MIT License; see LICENSE in the project root.
# This file is part of Companyware.

param(
    [string]$ReleaseDirectory = 'target/release',
    [string]$JavaRuntimeDirectory,
    [string]$InnoSetupCompiler,
    [string]$OutputDirectory = 'target/release/publish'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

function Resolve-ProjectPath([string]$Path) {
    if ([IO.Path]::IsPathRooted($Path)) { return [IO.Path]::GetFullPath($Path) }
    return [IO.Path]::GetFullPath((Join-Path $projectRoot $Path))
}

function Get-PomVersion {
    [xml]$pom = Get-Content -LiteralPath (Join-Path $projectRoot 'pom.xml') -Raw
    return [string]$pom.project.version
}

$version = Get-PomVersion
if (-not $version -or $version.EndsWith('-SNAPSHOT', [StringComparison]::OrdinalIgnoreCase)) {
    throw 'The Windows installer requires an explicit non-SNAPSHOT project version.'
}

$releaseRoot = Resolve-ProjectPath $ReleaseDirectory
$stage = Join-Path $releaseRoot "companyware-desktop-$version"
if (-not (Test-Path -LiteralPath (Join-Path $stage 'Companyware.jar') -PathType Leaf)) {
    throw "Release stage is missing Companyware.jar: $stage"
}

if (-not $JavaRuntimeDirectory) {
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'jre\bin\javaw.exe'))) {
        $JavaRuntimeDirectory = Join-Path $env:JAVA_HOME 'jre'
    } else {
        throw 'Pass -JavaRuntimeDirectory or use a Java 8 JDK whose JAVA_HOME contains jre\bin\javaw.exe.'
    }
}
$runtime = [IO.Path]::GetFullPath($JavaRuntimeDirectory)
if (-not (Test-Path -LiteralPath (Join-Path $runtime 'bin\javaw.exe') -PathType Leaf)) {
    throw "The private Java runtime does not contain bin\javaw.exe: $runtime"
}
$runtimeLicense = Join-Path (Split-Path -Parent $runtime) 'LICENSE'
if (-not (Test-Path -LiteralPath $runtimeLicense -PathType Leaf)) {
    throw "The private Java runtime must come from a JDK distribution with its LICENSE beside the jre directory: $runtimeLicense"
}

if (-not $InnoSetupCompiler) {
    $candidates = @(
        (Join-Path ${env:ProgramFiles(x86)} 'Inno Setup 6\ISCC.exe'),
        (Join-Path $env:ProgramFiles 'Inno Setup 6\ISCC.exe')
    ) | Where-Object { $_ -and (Test-Path -LiteralPath $_ -PathType Leaf) }
    $InnoSetupCompiler = $candidates | Select-Object -First 1
}
if (-not $InnoSetupCompiler -or -not (Test-Path -LiteralPath $InnoSetupCompiler -PathType Leaf)) {
    throw 'Inno Setup 6 compiler (ISCC.exe) was not found. Pass -InnoSetupCompiler explicitly.'
}

$output = Resolve-ProjectPath $OutputDirectory
New-Item -ItemType Directory -Force -Path $output | Out-Null
$script = Join-Path $projectRoot 'installer\companyware-desktop.iss'

& $InnoSetupCompiler "/DMyAppVersion=$version" "/DSourceDir=$stage" "/DRuntimeDir=$runtime" "/DInstallerOutputDir=$output" $script
if ($LASTEXITCODE -ne 0) { throw "Inno Setup failed with exit code $LASTEXITCODE." }

$installer = Join-Path $output 'Companyware-Setup.exe'
if (-not (Test-Path -LiteralPath $installer -PathType Leaf)) {
    throw "Inno Setup completed without producing $installer"
}
Write-Output $installer
