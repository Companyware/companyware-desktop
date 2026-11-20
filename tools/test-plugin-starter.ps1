# Companyware
# Copyright (c) 2026 Companyware contributors
#
# SPDX-License-Identifier: MIT
# Licensed under the MIT License; see LICENSE in the project root.
# This file is part of Companyware.
$ErrorActionPreference = 'Stop'
$sourceRoot = Split-Path -Parent $PSScriptRoot
$testRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("companyware-plugin-starter-test-" + [guid]::NewGuid())

function Expect-Failure([scriptblock]$Action, [string]$ExpectedMessage, [string]$Label) {
    try {
        & $Action
        throw "Expected failure did not occur: $Label"
    } catch {
        if ($_.Exception.Message -like 'Expected failure did not occur:*') { throw }
        if ($_.Exception.Message -notlike "*$ExpectedMessage*") {
            throw "Unexpected failure for ${Label}: $($_.Exception.Message)"
        }
    }
}

function With-Override([hashtable]$Values, [string]$Key, [string]$Value) {
    $copy = @{}
    foreach ($name in $Values.Keys) { $copy[$name] = $Values[$name] }
    $copy[$Key] = $Value
    return $copy
}

function Project-Hashes([string]$Root) {
    $prefix = $Root.TrimEnd('\') + '\'
    return Get-ChildItem -LiteralPath $Root -Recurse -File | Sort-Object FullName | ForEach-Object {
        $relative = $_.FullName.Substring($prefix.Length).Replace('\', '/')
        "$relative=$((Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash)"
    }
}

[System.IO.Directory]::CreateDirectory($testRoot) | Out-Null
try {
    $firstParent = Join-Path $testRoot 'first'
    $secondParent = Join-Path $testRoot 'second'
    [System.IO.Directory]::CreateDirectory($firstParent) | Out-Null
    [System.IO.Directory]::CreateDirectory($secondParent) | Out-Null
    $display = 'Inventory & "Quotes": O''Brien'
    $author = 'A & B: "Team" O''Brien'
    $arguments = @{
        PluginId = 'example.inventory'
        DisplayName = $display
        JavaPackage = 'example.companyware.inventory'
        EntryClass = 'InventoryPlugin'
        Author = $author
    }
    $first = & (Join-Path $sourceRoot 'tools\new-plugin-project.ps1') -ParentDirectory $firstParent @arguments
    $second = & (Join-Path $sourceRoot 'tools\new-plugin-project.ps1') -ParentDirectory $secondParent @arguments

    [xml](Get-Content -LiteralPath (Join-Path $first 'pom.xml') -Raw) | Out-Null
    $yaml = Get-Content -LiteralPath (Join-Path $first 'plugin.yaml') -Raw
    $expectedName = "name: 'Inventory & `"Quotes`": O''Brien'"
    $expectedAuthor = "author: 'A & B: `"Team`" O''Brien'"
    if ($yaml -notmatch [regex]::Escape($expectedName)) { throw 'YAML display name was not safely quoted.' }
    if ($yaml -notmatch [regex]::Escape($expectedAuthor)) { throw 'YAML author was not safely quoted.' }
    $java = Get-Content -LiteralPath (Join-Path $first 'src\main\java\example\companyware\inventory\InventoryPlugin.java') -Raw
    if ($java -notmatch [regex]::Escape('Inventory & \"Quotes\": O''Brien')) { throw 'Java string was not safely escaped.' }
    if (-not (Test-Path -LiteralPath (Join-Path $first 'LICENSE') -PathType Leaf)) { throw 'Generated LICENSE is missing.' }
    if ($java -match 'import (core|desktop|pluginmanager)\.') { throw 'Generated source imports Desktop internals.' }

    $firstHashes = Project-Hashes $first
    $secondHashes = Project-Hashes $second
    if (Compare-Object $firstHashes $secondHashes) { throw 'Generated project content is not deterministic.' }

    Expect-Failure { & (Join-Path $sourceRoot 'tools\new-plugin-project.ps1') -ParentDirectory $firstParent @arguments } 'Refusing to overwrite existing path' 'existing target'
    $invalidId = With-Override $arguments 'PluginId' '../escape'
    Expect-Failure { & (Join-Path $sourceRoot 'tools\new-plugin-project.ps1') -ParentDirectory $secondParent @invalidId } 'PluginId must be lowercase' 'traversal ID'
    $keywordPackage = With-Override $arguments 'JavaPackage' 'example.class.plugin'
    Expect-Failure { & (Join-Path $sourceRoot 'tools\new-plugin-project.ps1') -ParentDirectory $secondParent @keywordPackage } 'reserved segment: class' 'Java keyword'
    $reservedId = With-Override $arguments 'PluginId' 'con'
    Expect-Failure { & (Join-Path $sourceRoot 'tools\new-plugin-project.ps1') -ParentDirectory $secondParent @reservedId } 'reserved filesystem name: con' 'reserved filesystem name'
    $multilineName = With-Override $arguments 'DisplayName' "line1`nline2"
    Expect-Failure { & (Join-Path $sourceRoot 'tools\new-plugin-project.ps1') -ParentDirectory $secondParent @multilineName } 'must each be a single line' 'multiline display name'
    $multilineAuthor = With-Override $arguments 'Author' "line1`nline2"
    Expect-Failure { & (Join-Path $sourceRoot 'tools\new-plugin-project.ps1') -ParentDirectory $secondParent @multilineAuthor } 'must each be a single line' 'multiline author'

    $junction = Join-Path $testRoot 'junction-parent'
    try {
        New-Item -ItemType Junction -Path $junction -Target $secondParent -ErrorAction Stop | Out-Null
        $junctionArguments = With-Override $arguments 'PluginId' 'example.junction'
        Expect-Failure { & (Join-Path $sourceRoot 'tools\new-plugin-project.ps1') -ParentDirectory $junction @junctionArguments } 'must not be a symbolic link or junction' 'junction parent'
    } catch {
        Write-Output "Junction runtime test skipped: $($_.Exception.Message)"
    }
    Write-Output 'Plugin starter generation tests passed.'
} finally {
    $resolved = [System.IO.Path]::GetFullPath($testRoot)
    $tempPrefix = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath()).TrimEnd('\') + '\'
    if ($resolved.StartsWith($tempPrefix, [System.StringComparison]::OrdinalIgnoreCase) -and
        (Split-Path -Leaf $resolved) -like 'companyware-plugin-starter-test-*' -and
        (Test-Path -LiteralPath $resolved)) {
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
}
