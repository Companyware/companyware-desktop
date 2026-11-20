# Companyware
# Copyright (c) 2026 Companyware contributors
#
# SPDX-License-Identifier: MIT
# Licensed under the MIT License; see LICENSE in the project root.
# This file is part of Companyware.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$forbidden = @(
    '(^|/)(target|data|logs|plugins-installed)/',
    '(^|/)(\.env($|\.)|derby\.log$)',
    '\.(jks|p12|pfx|key|pem|db|lck|log)$'
)

Push-Location $projectRoot
try {
    $violations = @()
    git ls-files | ForEach-Object {
        $path = $_.Replace('\', '/')
        foreach ($pattern in $forbidden) {
            if ($path -match $pattern) {
                $violations += $path
                break
            }
        }
    }
    if ($violations.Count -gt 0) {
        throw "Publication contains forbidden runtime, secret or generated paths:`n$($violations -join "`n")"
    }

    $workflow = '.github/workflows/verify.yml'
    if (-not (Test-Path -LiteralPath $workflow -PathType Leaf)) {
        throw 'The GitHub verification workflow is missing.'
    }
    $workflowText = Get-Content -LiteralPath $workflow -Raw
    if ($workflowText -notmatch '(?m)^permissions:\s*$' -or
        $workflowText -notmatch '(?m)^\s+contents:\s+read\s*$') {
        throw 'The GitHub workflow must declare read-only repository contents permission.'
    }

    Write-Output 'Publication safety verified: no tracked runtime/secret paths and read-only workflow permissions are present.'
} finally {
    Pop-Location
}
