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

    $releaseWorkflow = '.github/workflows/release.yml'
    if (Test-Path -LiteralPath $releaseWorkflow -PathType Leaf) {
        $releaseWorkflowText = Get-Content -LiteralPath $releaseWorkflow -Raw
        if ($releaseWorkflowText -notmatch '(?m)^\s+tags:\s*$' -or
            $releaseWorkflowText -notmatch '(?m)^\s+-\s+["'']v\*["'']\s*$') {
            throw 'The release workflow must be limited to explicit v* tag pushes.'
        }
        if ($releaseWorkflowText -match '(?m)^\s*pull_request\s*:') {
            throw 'The write-enabled release workflow must never run for pull requests.'
        }
        if ($releaseWorkflowText -notmatch '(?m)^permissions:\s*$' -or
            $releaseWorkflowText -notmatch '(?m)^\s+contents:\s+write\s*$') {
            throw 'The tag-only release workflow must declare its contents: write permission explicitly.'
        }
    }

    Write-Output 'Publication safety verified: runtime/secret paths are absent; verification is read-only and release writes are tag-only.'
} finally {
    Pop-Location
}
