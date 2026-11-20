# Companyware
# Copyright (c) 2026 Companyware contributors
#
# SPDX-License-Identifier: MIT
# Licensed under the MIT License; see LICENSE in the project root.
# This file is part of Companyware.
param(
    [string]$Repository = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$source = [IO.Path]::GetFullPath($Repository)
if ((git -C $source status --porcelain).Count -ne 0) {
    throw 'Clean-clone verification must start from a clean source checkout.'
}
$temporaryRoot = Join-Path ([IO.Path]::GetTempPath()) ('companyware-release-' + [guid]::NewGuid())
$cloneA = Join-Path $temporaryRoot 'a'
$cloneB = Join-Path $temporaryRoot 'b'
try {
    New-Item -ItemType Directory -Force -Path $temporaryRoot | Out-Null
    git clone --quiet --no-hardlinks $source $cloneA
    if ($LASTEXITCODE -ne 0) { throw 'Unable to create first verification clone.' }
    git clone --quiet --no-hardlinks $source $cloneB
    if ($LASTEXITCODE -ne 0) { throw 'Unable to create second verification clone.' }
    & (Join-Path $cloneA 'tools/build-release.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'Release verification failed in the first clean clone.' }
    & (Join-Path $cloneB 'tools/build-release.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'Release verification failed in the second clean clone.' }

    [xml]$pom = Get-Content -LiteralPath (Join-Path $cloneA 'pom.xml') -Raw
    $version = [string]$pom.project.version
    $stageName = "companyware-desktop-$version"
    $files = @(
        "target/release/$stageName/Companyware.jar",
        "target/release/$stageName.zip",
        'target/release/dependency-inventory.txt',
        'target/release/release-evidence.json'
    )
    foreach ($relativePath in $files) {
        $hashA = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $cloneA $relativePath)).Hash
        $hashB = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $cloneB $relativePath)).Hash
        if ($hashA -ne $hashB) { throw "Clean-clone outputs differ for $relativePath." }
        Write-Output "$relativePath SHA-256 $($hashA.ToLowerInvariant())"
    }
    Write-Output "Two-build clean-clone release verified at commit $(git -C $cloneA rev-parse HEAD)"
} finally {
    if (Test-Path -LiteralPath $temporaryRoot) {
        $resolved = (Resolve-Path -LiteralPath $temporaryRoot).Path
        $tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
        if (-not $resolved.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove path outside the temporary directory: $resolved"
        }
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
