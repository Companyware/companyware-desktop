# Companyware
# Copyright (c) 2026 Companyware contributors
#
# SPDX-License-Identifier: MIT
# Licensed under the MIT License; see LICENSE in the project root.
# This file is part of Companyware.

param(
    [Parameter(Mandatory = $true)][string]$Tag,
    [string]$ReleaseDirectory = 'target/release',
    [string]$PublishDirectory = 'target/release/publish'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

function Resolve-ProjectPath([string]$Path) {
    if ([IO.Path]::IsPathRooted($Path)) { return [IO.Path]::GetFullPath($Path) }
    return [IO.Path]::GetFullPath((Join-Path $projectRoot $Path))
}

function Get-Sha256([string]$Path) {
    return (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLowerInvariant()
}

[xml]$pom = Get-Content -LiteralPath (Join-Path $projectRoot 'pom.xml') -Raw
$version = [string]$pom.project.version
$expectedTag = "v$version"
if ($Tag -cne $expectedTag) {
    throw "Release tag '$Tag' does not match pom.xml version '$version'. Expected '$expectedTag'."
}

$releaseRoot = Resolve-ProjectPath $ReleaseDirectory
$publishRoot = Resolve-ProjectPath $PublishDirectory
$versionedArchive = Join-Path $releaseRoot "companyware-desktop-$version.zip"
$installer = Join-Path $publishRoot 'Companyware-Setup.exe'
$notes = Join-Path $projectRoot "docs\release-notes\$version.md"
foreach ($required in @($versionedArchive, $installer, $notes, (Join-Path $releaseRoot 'release-evidence.json'))) {
    if (-not (Test-Path -LiteralPath $required -PathType Leaf)) { throw "Required release file is missing: $required" }
}

$portable = Join-Path $publishRoot 'companyware-desktop.zip'
Copy-Item -LiteralPath $versionedArchive -Destination $portable -Force
Copy-Item -LiteralPath (Join-Path $releaseRoot 'release-evidence.json') -Destination (Join-Path $publishRoot 'release-evidence.json') -Force

$checksums = @(
    "$(Get-Sha256 $installer)  Companyware-Setup.exe",
    "$(Get-Sha256 $portable)  companyware-desktop.zip",
    "$(Get-Sha256 (Join-Path $publishRoot 'release-evidence.json'))  release-evidence.json"
)
$checksums | Set-Content -LiteralPath (Join-Path $publishRoot 'SHA256SUMS.txt') -Encoding Ascii

$manifest = [ordered]@{
    format = 'companyware-github-release-1'
    product = 'Companyware Desktop'
    version = $version
    tag = $Tag
    commit = (git -C $projectRoot rev-parse HEAD).Trim()
    prerelease = $version.Contains('-')
    assets = @(
        [ordered]@{ file = 'Companyware-Setup.exe'; sha256 = Get-Sha256 $installer; audience = 'users' },
        [ordered]@{ file = 'companyware-desktop.zip'; sha256 = Get-Sha256 $portable; audience = 'portable' },
        [ordered]@{ file = 'SHA256SUMS.txt'; audience = 'verification' }
    )
}
$manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $publishRoot 'release.json') -Encoding UTF8
Write-Output $publishRoot
