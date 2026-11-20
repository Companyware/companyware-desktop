# Companyware
# Copyright (c) 2026 Companyware contributors
#
# SPDX-License-Identifier: MIT
# Licensed under the MIT License; see LICENSE in the project root.
# This file is part of Companyware.
param(
    [string]$BaseRef = $env:CW_POLICY_BASE_REF
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$eligibleExtensions = @('.java', '.ps1', '.cmd', '.js', '.ts', '.css')

Push-Location $projectRoot
try {
    if (-not (Test-Path -LiteralPath 'LICENSE' -PathType Leaf)) {
        throw 'The project-root LICENSE file is missing.'
    }

    if (-not $BaseRef) {
        git rev-parse --verify --quiet 'origin/develop' | Out-Null
        if ($LASTEXITCODE -eq 0) { $BaseRef = 'origin/develop' }
    }

    $candidates = New-Object 'System.Collections.Generic.HashSet[string]' ([StringComparer]::OrdinalIgnoreCase)
    if ($BaseRef) {
        git rev-parse --verify --quiet $BaseRef | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Unknown source-policy base reference: $BaseRef" }
        git diff --diff-filter=A --name-only "$BaseRef...HEAD" | ForEach-Object { [void]$candidates.Add($_) }
    } else {
        git diff-tree --no-commit-id --diff-filter=A --name-only -r HEAD | ForEach-Object { [void]$candidates.Add($_) }
    }
    git ls-files --others --exclude-standard | ForEach-Object { [void]$candidates.Add($_) }

    $violations = @()
    foreach ($relativePath in $candidates) {
        $extension = [IO.Path]::GetExtension($relativePath).ToLowerInvariant()
        if ($eligibleExtensions -notcontains $extension) { continue }
        if ($relativePath -match '(^|/)(target|vendor|generated)/') { continue }

        $path = Join-Path $projectRoot $relativePath
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { continue }
        $header = (Get-Content -LiteralPath $path -TotalCount 10) -join "`n"
        if ($header -notmatch 'SPDX-License-Identifier:\s*MIT' -or $header -notmatch 'LICENSE') {
            $violations += $relativePath
        }
    }

    if ($violations.Count -gt 0) {
        throw "Eligible new source files must start with the Companyware MIT/SPDX header:`n$($violations -join "`n")"
    }
    $baseDescription = if ($BaseRef) { $BaseRef } else { '<HEAD>' }
    Write-Output "Source policy verified for $($candidates.Count) added file(s); base=$baseDescription."
} finally {
    Pop-Location
}
