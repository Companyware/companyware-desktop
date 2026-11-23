# Companyware
# Copyright (c) 2026 Companyware contributors
#
# SPDX-License-Identifier: MIT
# Licensed under the MIT License; see LICENSE in the project root.
# This file is part of Companyware.
param(
    [Parameter(Mandatory = $true)][string]$ParentDirectory,
    [Parameter(Mandatory = $true)][string]$PluginId,
    [Parameter(Mandatory = $true)][string]$DisplayName,
    [Parameter(Mandatory = $true)][string]$JavaPackage,
    [Parameter(Mandatory = $true)][string]$EntryClass,
    [string]$Author = 'Companyware plugin developer',
    [string]$Version = '1.0.0'
)

$ErrorActionPreference = 'Stop'
$sourceRoot = Split-Path -Parent $PSScriptRoot
$templateRoot = Join-Path $PSScriptRoot 'plugin-starter'

if ($PluginId -notmatch '^[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*$') {
    throw 'PluginId must be lowercase and use dot or dash separated alphanumeric segments.'
}
if ($JavaPackage -notmatch '^[a-z][a-z0-9]*(?:\.[a-z][a-z0-9]*)+$') {
    throw 'JavaPackage must be a lowercase dotted Java package.'
}
if ($EntryClass -notmatch '^[A-Z][A-Za-z0-9]*$') {
    throw 'EntryClass must be a Java class name beginning with an uppercase letter.'
}
if ($Version -notmatch '^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$') {
    throw 'Version must be a numeric semantic version (major.minor.patch).'
}
if ([string]::IsNullOrWhiteSpace($DisplayName) -or [string]::IsNullOrWhiteSpace($Author)) {
    throw 'DisplayName and Author must not be empty.'
}
if ($DisplayName -match '[\r\n]' -or $Author -match '[\r\n]') {
    throw 'DisplayName and Author must each be a single line.'
}

$javaKeywords = @('abstract','assert','boolean','break','byte','case','catch','char','class','const',
    'continue','default','do','double','else','enum','extends','final','finally','float','for','goto',
    'if','implements','import','instanceof','int','interface','long','native','new','package','private',
    'protected','public','return','short','static','strictfp','super','switch','synchronized','this',
    'throw','throws','transient','try','void','volatile','while','true','false','null')
$reservedWindowsNames = @('con','prn','aux','nul','clock$','com1','com2','com3','com4','com5','com6',
    'com7','com8','com9','lpt1','lpt2','lpt3','lpt4','lpt5','lpt6','lpt7','lpt8','lpt9')
foreach ($segment in $JavaPackage.Split('.')) {
    if ($javaKeywords -contains $segment -or $reservedWindowsNames -contains $segment) {
        throw "JavaPackage contains a reserved segment: $segment"
    }
}

$parentPath = [System.IO.Path]::GetFullPath($ParentDirectory)
if (-not (Test-Path -LiteralPath $parentPath -PathType Container)) {
    throw "Parent directory does not exist: $parentPath"
}
$parentInfo = Get-Item -LiteralPath $parentPath -Force
if (($parentInfo.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
    throw "Parent directory must not be a symbolic link or junction: $parentPath"
}
$folderName = $PluginId.Replace('.', '-').Replace('_', '-')
if ($reservedWindowsNames -contains $folderName) {
    throw "PluginId resolves to a reserved filesystem name: $folderName"
}
$projectPath = [System.IO.Path]::GetFullPath((Join-Path $parentPath $folderName))
if (-not $projectPath.StartsWith($parentPath.TrimEnd('\') + '\', [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'Generated project path escapes the selected parent directory.'
}
if (Test-Path -LiteralPath $projectPath) {
    throw "Refusing to overwrite existing path: $projectPath"
}

function ConvertTo-XmlText([string]$Value) {
    return [System.Security.SecurityElement]::Escape($Value)
}
function ConvertTo-YamlSingleQuoted([string]$Value) {
    return $Value.Replace("'", "''")
}
function ConvertTo-JavaString([string]$Value) {
    $builder = [System.Text.StringBuilder]::new()
    foreach ($character in $Value.ToCharArray()) {
        switch ([int]$character) {
            8 { [void]$builder.Append('\b') }
            9 { [void]$builder.Append('\t') }
            12 { [void]$builder.Append('\f') }
            34 { [void]$builder.Append('\"') }
            92 { [void]$builder.Append('\\') }
            default {
                if ([int]$character -lt 32) {
                    [void]$builder.Append(('\u{0:x4}' -f [int]$character))
                } else {
                    [void]$builder.Append($character)
                }
            }
        }
    }
    return $builder.ToString()
}

$tokens = @{
    '__PLUGIN_ID__' = $PluginId
    '__DISPLAY_NAME_XML__' = ConvertTo-XmlText $DisplayName
    '__DISPLAY_NAME_YAML__' = ConvertTo-YamlSingleQuoted $DisplayName
    '__DISPLAY_NAME_JAVA__' = ConvertTo-JavaString $DisplayName
    '__DISPLAY_NAME_MARKDOWN__' = $DisplayName
    '__JAVA_PACKAGE__' = $JavaPackage
    '__ENTRY_CLASS__' = $EntryClass
    '__AUTHOR_YAML__' = ConvertTo-YamlSingleQuoted $Author
    '__PLUGIN_VERSION__' = $Version
    '__SDK_VERSION__' = '0.29.0'
    '__SDK_API_VERSION__' = '1.0.0'
}

function Expand-Template([string]$Template, [string]$Destination) {
    $text = Get-Content -LiteralPath $Template -Raw
    foreach ($token in $tokens.Keys) { $text = $text.Replace($token, $tokens[$token]) }
    $directory = Split-Path -Parent $Destination
    [System.IO.Directory]::CreateDirectory($directory) | Out-Null
    [System.IO.File]::WriteAllText($Destination, $text, [System.Text.UTF8Encoding]::new($false))
}

[System.IO.Directory]::CreateDirectory($projectPath) | Out-Null
try {
    $packagePath = $JavaPackage.Replace('.', [System.IO.Path]::DirectorySeparatorChar)
    Expand-Template (Join-Path $templateRoot 'pom.xml.template') (Join-Path $projectPath 'pom.xml')
    Expand-Template (Join-Path $templateRoot 'plugin.yaml.template') (Join-Path $projectPath 'plugin.yaml')
    Expand-Template (Join-Path $templateRoot 'README.md.template') (Join-Path $projectPath 'README.md')
    Expand-Template (Join-Path $templateRoot 'Plugin.java.template') (Join-Path $projectPath "src\main\java\$packagePath\$EntryClass.java")
    Expand-Template (Join-Path $templateRoot 'PluginTest.java.template') (Join-Path $projectPath "src\test\java\$packagePath\${EntryClass}Test.java")
    [System.IO.Directory]::CreateDirectory((Join-Path $projectPath 'src\assembly')) | Out-Null
    Copy-Item -LiteralPath (Join-Path $templateRoot 'plugin-assembly.xml') -Destination (Join-Path $projectPath 'src\assembly\plugin.xml')
    Copy-Item -LiteralPath (Join-Path $templateRoot 'gitignore.template') -Destination (Join-Path $projectPath '.gitignore')
    Copy-Item -LiteralPath (Join-Path $sourceRoot 'LICENSE') -Destination (Join-Path $projectPath 'LICENSE')
    Copy-Item -LiteralPath (Join-Path $sourceRoot 'mvnw') -Destination (Join-Path $projectPath 'mvnw')
    Copy-Item -LiteralPath (Join-Path $sourceRoot 'mvnw.cmd') -Destination (Join-Path $projectPath 'mvnw.cmd')
    $wrapperDestination = Join-Path $projectPath '.mvn\wrapper'
    [System.IO.Directory]::CreateDirectory($wrapperDestination) | Out-Null
    Copy-Item -LiteralPath (Join-Path $sourceRoot '.mvn\wrapper\maven-wrapper.properties') -Destination $wrapperDestination
    Write-Output $projectPath
} catch {
    if (Test-Path -LiteralPath $projectPath) { Remove-Item -LiteralPath $projectPath -Recurse -Force }
    throw
}
