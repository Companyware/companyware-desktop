# Companyware
# Copyright (c) 2026 Companyware contributors
#
# SPDX-License-Identifier: MIT
# Licensed under the MIT License; see LICENSE in the project root.
# This file is part of Companyware.
param(
    [string]$BaseRef = $env:CW_POLICY_BASE_REF,
    [switch]$PolicyOnly,
    [switch]$AllowSnapshot
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
[xml]$pom = Get-Content -LiteralPath (Join-Path $projectRoot 'pom.xml') -Raw
$version = [string]$pom.project.version
if (-not $version) { throw 'The project version is missing from pom.xml.' }
$requiredFiles = @(
    'LICENSE', 'README.md', 'SECURITY.md', 'CONTRIBUTING.md', 'SUPPORT.md',
    'docs/README.md', 'docs/developer-guide.md',
    'docs/framework-release.md', 'docs/plugin-sdk-v1.md',
    'docs/release-process.md', 'docs/derby-backup.md',
    'docs/release-notes/UNRELEASED.md',
    "docs/release-notes/$version.md",
    "docs/security-review-$version.md",
    'docs/github-publication-checklist.md',
    'docs/github-repository-migration.md'
)

Push-Location $projectRoot
try {
    foreach ($relativePath in $requiredFiles) {
        if (-not (Test-Path -LiteralPath $relativePath -PathType Leaf)) {
            throw "Framework release prerequisite is missing: $relativePath"
        }
    }
    $applicationVersionLine = Get-Content -LiteralPath 'src/main/resources/application.properties' |
        Where-Object { $_ -match '^\s*companyware\.version\s*=' } | Select-Object -First 1
    if (-not $applicationVersionLine) { throw 'companyware.version is missing from application.properties.' }
    $applicationVersion = ($applicationVersionLine -split '=', 2)[1].Trim()
    if ($applicationVersion -ne $version) {
        throw "Runtime version $applicationVersion does not match Maven version $version."
    }
    & (Join-Path $PSScriptRoot 'verify-source-policy.ps1') -BaseRef $BaseRef
    if ($LASTEXITCODE -ne 0) { throw 'Source-policy verification failed.' }
    & (Join-Path $PSScriptRoot 'verify-publication-safety.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'Publication-safety verification failed.' }

    if ($PolicyOnly) {
        Write-Output 'Framework release policy and documentation prerequisites verified; build intentionally skipped.'
        return
    }

    if (-not $AllowSnapshot -and $version.EndsWith('-SNAPSHOT', [StringComparison]::OrdinalIgnoreCase)) {
        throw "Release verification refuses snapshot version $version. Select and document an explicit release version, or use -AllowSnapshot only for development CI."
    }

    & (Join-Path $PSScriptRoot 'build-release.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'Framework release build failed.' }
    $kind = if ($AllowSnapshot) { 'development candidate' } else { 'release' }
    Write-Output "Framework $kind verified at commit $(git rev-parse HEAD), version $version."
} finally {
    Pop-Location
}
