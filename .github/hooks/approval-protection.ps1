$ErrorActionPreference = "Stop"

try {
    $inputJson = [Console]::In.ReadToEnd()
    $data = $inputJson | ConvertFrom-Json

    $toolName = $data.tool_name
    $toolArgs = $data.tool_input

    # Only check file editing and creation tools.
    if ($toolName -notin @("edit", "create")) {
        Write-Output '{}'
        exit 0
    }

    # Get the target file path.
    $filePath = $toolArgs.filePath
    if (-not $filePath) {
        $filePath = $toolArgs.path
    }

    if (-not $filePath) {
        Write-Output '{}'
        exit 0
    }

    $normalizedPath = $filePath.Replace('\', '/')

    $protectedFiles = @(
        "docs/requirements.md",
        "docs/architecture.md"
    )

    foreach ($file in $protectedFiles) {
        if ($normalizedPath -match "(^|/)$([regex]::Escape($file))$") {
            @{
                permissionDecision = "deny"
                permissionDecisionReason = "This SDLC document is protected. Obtain authorization before modifying it."
            } | ConvertTo-Json -Compress
            exit 0
        }
    }

    Write-Output '{}'
    exit 0
}
catch {
    [Console]::Error.WriteLine(
        "Approval protection hook failed: $($_.Exception.Message)"
    )
    exit 1
}
