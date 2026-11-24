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

    $windowsIcon = 'src/main/resources/images/companyware.ico'
    if (-not (Test-Path -LiteralPath $windowsIcon -PathType Leaf)) {
        throw 'The branded Windows icon is missing.'
    }
    $iconBytes = [IO.File]::ReadAllBytes((Resolve-Path -LiteralPath $windowsIcon))
    if ($iconBytes.Length -lt 22 -or
        [BitConverter]::ToUInt16($iconBytes, 0) -ne 0 -or
        [BitConverter]::ToUInt16($iconBytes, 2) -ne 1 -or
        [BitConverter]::ToUInt16($iconBytes, 4) -lt 1) {
        throw 'The branded Windows icon has an invalid ICO header.'
    }
    $pomText = Get-Content -LiteralPath 'pom.xml' -Raw
    if ($pomText -notmatch '<icon>src/main/resources/images/companyware\.ico</icon>') {
        throw 'Launch4j must embed the branded Windows icon.'
    }
    $installerText = Get-Content -LiteralPath 'installer/companyware-desktop.iss' -Raw
    if ($installerText -notmatch 'SetupIconFile=\{#SourceDir\}\\Companyware\.ico' -or
        $installerText -notmatch 'IconFilename: "\{app\}\\Companyware\.ico"') {
        throw 'The installer and installed shortcuts must use the branded Windows icon.'
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
        if ($releaseWorkflowText -match '(?m)^\s+workflow_dispatch:\s*$') {
            if ($releaseWorkflowText -notmatch '(?m)^\s+confirmation:\s*$' -or
                $releaseWorkflowText -notmatch "(?m)-cne 'RELEASE'") {
                throw 'A manually dispatched release must require the exact RELEASE confirmation.'
            }
        }
        if ($releaseWorkflowText -notmatch '(?m)^permissions:\s*$' -or
            $releaseWorkflowText -notmatch '(?m)^\s+contents:\s+write\s*$') {
            throw 'The tag-only release workflow must declare its contents: write permission explicitly.'
        }
    }

    Write-Output 'Publication safety verified: runtime/secret paths are absent; verification is read-only and release writes require an explicit tag push or confirmed manual dispatch.'
} finally {
    Pop-Location
}
