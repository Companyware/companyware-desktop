# Companyware
# Copyright (c) 2026 Companyware contributors
#
# SPDX-License-Identifier: MIT
# Licensed under the MIT License; see LICENSE in the project root.
# This file is part of Companyware.
param(
    [string]$OutputDirectory = 'target/release'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$outputRoot = if ([IO.Path]::IsPathRooted($OutputDirectory)) {
    [IO.Path]::GetFullPath($OutputDirectory)
} else {
    [IO.Path]::GetFullPath((Join-Path $projectRoot $OutputDirectory))
}
$inventory = Join-Path $outputRoot 'dependency-inventory.txt'
New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null

Push-Location $projectRoot
try {
    & .\mvnw.cmd -q dependency:tree "-DoutputFile=$inventory"
    if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve the Maven dependency inventory.' }
    $content = Get-Content -LiteralPath $inventory -Raw
    $prohibited = [ordered]@{
        'Log4j 1.x' = 'log4j:log4j:'
        'Bundled legacy Oracle JRE' = 'com.oracle.java:jre:'
        'SnakeYAML 1.x' = 'org.yaml:snakeyaml:jar:1.'
        'JUnit 4.12' = 'junit:junit:jar:4.12:'
        'Commons IO 2.1' = 'commons-io:commons-io:jar:2.1:'
    }
    foreach ($finding in $prohibited.GetEnumerator()) {
        if ($content.Contains($finding.Value)) {
            throw "Prohibited dependency remains: $($finding.Key) ($($finding.Value))"
        }
    }
    if (-not $content.Contains('org.springframework.boot:spring-boot-starter:jar:2.7.18:')) {
        throw 'The reviewed Java-8 Spring Boot baseline 2.7.18 is not resolved.'
    }
    if (-not $content.Contains('org.hibernate:hibernate-core:jar:5.6.15.Final:')) {
        throw 'The reviewed Hibernate baseline 5.6.15.Final is not resolved.'
    }
    if (-not $content.Contains('org.springframework:spring-core:jar:5.3.39:')) {
        throw 'The latest public Java-8 Spring Framework baseline 5.3.39 is not resolved.'
    }
    if (-not $content.Contains('org.springframework.security:spring-security-core:jar:5.8.16:')) {
        throw 'The latest public Java-8 Spring Security baseline 5.8.16 is not resolved.'
    }
    if (-not $content.Contains('org.apache.pdfbox:pdfbox:jar:2.0.37:')) {
        throw 'The reviewed PDFBox 2.x baseline 2.0.37 is not resolved.'
    }
    if (-not $content.Contains('org.yaml:snakeyaml:jar:2.7:')) {
        throw 'The reviewed Java-8 SnakeYAML baseline 2.7 is not resolved.'
    }
    Write-Output "Dependency inventory verified at $(git rev-parse HEAD): $inventory"
} finally {
    Pop-Location
}
