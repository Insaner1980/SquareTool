#Requires -Version 5.1

[CmdletBinding()]
param(
    [switch]$PlanOnly,

    [switch]$AllowExternalUpload,

    [ValidateRange(1, 86400)]
    [int]$GradleTimeoutSeconds = 3600
)

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
$OutputEncoding = [Console]::OutputEncoding

function Get-SonarProjectProperties {
    param([string]$Path)

    $properties = @{}
    foreach ($line in Get-Content -LiteralPath $Path -Encoding utf8) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
            continue
        }

        $separator = $trimmed.IndexOf("=")
        if ($separator -lt 1) {
            continue
        }

        $key = $trimmed.Substring(0, $separator).Trim()
        $properties[$key] = $trimmed.Substring($separator + 1).Trim()
    }

    return $properties
}

function Set-SonarTokenFromCredentialManager {
    if (-not [string]::IsNullOrWhiteSpace($env:SONAR_TOKEN)) {
        return $true
    }

    if ($null -eq ("SonarCliCredentialReader" -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
using System.Text;

public static class SonarCliCredentialReader
{
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    private struct Credential
    {
        public uint Flags;
        public uint Type;
        public IntPtr TargetName;
        public IntPtr Comment;
        public System.Runtime.InteropServices.ComTypes.FILETIME LastWritten;
        public uint CredentialBlobSize;
        public IntPtr CredentialBlob;
        public uint Persist;
        public uint AttributeCount;
        public IntPtr Attributes;
        public IntPtr TargetAlias;
        public IntPtr UserName;
    }

    [DllImport("Advapi32.dll", EntryPoint = "CredReadW", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern bool CredRead(string target, uint type, uint flags, out IntPtr credential);

    [DllImport("Advapi32.dll", SetLastError = true)]
    private static extern void CredFree(IntPtr credential);

    public static string ReadGenericPassword(string target)
    {
        IntPtr pointer;
        if (!CredRead(target, 1, 0, out pointer))
        {
            return null;
        }

        try
        {
            Credential credential = (Credential)Marshal.PtrToStructure(pointer, typeof(Credential));
            if (credential.CredentialBlob == IntPtr.Zero || credential.CredentialBlobSize == 0)
            {
                return null;
            }

            byte[] bytes = new byte[credential.CredentialBlobSize];
            Marshal.Copy(credential.CredentialBlob, bytes, 0, bytes.Length);
            return Encoding.UTF8.GetString(bytes).TrimEnd('\0');
        }
        finally
        {
            CredFree(pointer);
        }
    }
}
'@
    }

    $token = [SonarCliCredentialReader]::ReadGenericPassword(
        "sonarqube-cli/sonarcloud.io:insaner1980"
    )
    if ([string]::IsNullOrWhiteSpace($token)) {
        return $false
    }

    $env:SONAR_TOKEN = $token
    return $true
}

function Test-SonarTokenConfigured {
    param([string]$RepoRoot)

    if (-not [string]::IsNullOrWhiteSpace($env:SONAR_TOKEN)) {
        return $true
    }

    foreach ($path in @(
        (Join-Path $env:USERPROFILE ".gradle\gradle.properties"),
        (Join-Path $RepoRoot "gradle.properties")
    )) {
        if (
            (Test-Path -LiteralPath $path -PathType Leaf) -and
            (Select-String -LiteralPath $path -Pattern "^\s*systemProp\.sonar\.token\s*=" -Quiet)
        ) {
            return $true
        }
    }

    return $false
}

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$propertiesPath = Join-Path $repoRoot "sonar-project.properties"
if (-not (Test-Path -LiteralPath $propertiesPath -PathType Leaf)) {
    throw "sonar-project.properties ei löytynyt: $propertiesPath"
}

$sonarProperties = Get-SonarProjectProperties -Path $propertiesPath
$projectKey = $sonarProperties["sonar.projectKey"]
$hostUrl = $sonarProperties["sonar.host.url"]
$sonarTokenConfigured = Set-SonarTokenFromCredentialManager
if ([string]::IsNullOrWhiteSpace($projectKey)) {
    throw "sonar.projectKey puuttuu sonar-project.properties-tiedostosta."
}
if ([string]::IsNullOrWhiteSpace($hostUrl)) {
    $hostUrl = "https://sonarcloud.io"
}

if ($PlanOnly) {
    Write-Output @(
        "sonar"
        "  - Gradle coverage, :app:assembleDebug and sonar: reports/sonar.txt"
        "  - requires SONAR_TOKEN or systemProp.sonar.token for the full scan"
        "  - token configured: $($sonarTokenConfigured.ToString().ToLowerInvariant())"
        "  - actual external upload requires -AllowExternalUpload"
        "  - project: $projectKey"
        "  - host: $hostUrl"
    )
    exit 0
}

$reportsDir = Join-Path $repoRoot "reports"
$scanReport = Join-Path $reportsDir "sonar.txt"
New-Item -ItemType Directory -Force -Path $reportsDir | Out-Null
Set-Content -LiteralPath $scanReport -Encoding utf8 -Value @(
    "sonar"
    "Root: $repoRoot"
    "Project: $projectKey"
    "Command: .\gradlew.bat sonar --console=plain --no-configuration-cache"
    "Started: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    ""
)

if (-not $AllowExternalUpload) {
    Add-Content -LiteralPath $scanReport -Encoding utf8 -Value @(
        "ERROR: EXTERNAL_UPLOAD_APPROVAL_REQUIRED"
        "Sonar-analyysi voi lähettää lähdekoodia ja analyysimetatietoa ulkoiseen palveluun."
        "Tarkista PlanOnly-tuloste ja käytä -AllowExternalUpload vain nimenomaisella luvalla."
    )
    Get-Content -LiteralPath $scanReport
    exit 2
}

if (-not $sonarTokenConfigured -and -not (Test-SonarTokenConfigured -RepoRoot $repoRoot)) {
    Add-Content -LiteralPath $scanReport -Encoding utf8 -Value @(
        "ERROR: SONAR_TOKEN_MISSING"
        "Aseta SONAR_TOKEN-ympäristömuuttuja tai systemProp.sonar.token Gradle-asetuksiin."
    )
    Get-Content -LiteralPath $scanReport
    exit 2
}

Push-Location -LiteralPath $repoRoot
try {
    $env:SONAR_HOST_URL = if ($env:SONAR_HOST_URL) { $env:SONAR_HOST_URL } else { $hostUrl }
    Import-Module "C:\Dev\Android-check\tools\CheckRuntime.psm1" -Force -ErrorAction Stop
    $scanResult = Invoke-ManagedProcess `
        -Executable (Join-Path $repoRoot "gradlew.bat") `
        -Arguments @("sonar", "--console=plain", "--no-configuration-cache") `
        -WorkingDirectory $repoRoot `
        -TimeoutSeconds $GradleTimeoutSeconds

    foreach ($streamText in @($scanResult.StandardOutput, $scanResult.StandardError)) {
        if (-not [string]::IsNullOrWhiteSpace($streamText)) {
            Add-Content -LiteralPath $scanReport -Encoding utf8 -Value $streamText
            Write-Output $streamText
        }
    }

    if ($scanResult.TimedOut) {
        Add-Content -LiteralPath $scanReport -Encoding utf8 -Value "ERROR: SONAR_ANALYSIS_TIMEOUT ($GradleTimeoutSeconds s)"
        exit 2
    }
    if ($scanResult.ExitCode -ne 0) {
        Add-Content -LiteralPath $scanReport -Encoding utf8 -Value "ERROR: SONAR_ANALYSIS_FAILED (exit $($scanResult.ExitCode))"
        exit 2
    }

    exit 0
}
catch {
    Add-Content -LiteralPath $scanReport -Encoding utf8 -Value "ERROR: SONAR_ANALYSIS_PROCESS_ERROR: $($_.Exception.Message)"
    Write-Error $_
    exit 2
}
finally {
    Pop-Location
}
