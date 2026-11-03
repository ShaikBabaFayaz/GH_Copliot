$ErrorActionPreference = "Stop"

try {
    # Read hook input from VS Code.
    $inputJson = [Console]::In.ReadToEnd()
    $data = $inputJson | ConvertFrom-Json

    $toolName = $data.tool_name
    $toolInput = $data.tool_input

    # Process only file-editing and file-creation tools.
    if ($toolName -notin @("Edit", "Write", "edit", "create")) {
        Write-Output '{}'
        exit 0
    }

    # Identify the affected file.
    $filePath = $toolInput.filePath

    if (-not $filePath) {
        $filePath = $toolInput.path
    }

    if (-not $filePath) {
        Write-Output '{}'
        exit 0
    }

    # Normalize Windows paths.
    $normalizedPath = $filePath.Replace('\', '/')

    # Continue only when a Java application source file changes.
    if ($normalizedPath -match '(^|/)src/main/java/.*\.java$') {

        $message = @"
A Java source file was modified: $filePath

Analyze the code change and identify which SDLC documents
actually need review.

Consider these documents:
- docs/requirements.md
- docs/architecture.md
- docs/design-review.md
- docs/implementation-plan.md
- docs/verification.md
- docs/pr-preparation.md
- docs/CHANGELOG.md

Return only the documents that may be affected, with a brief
reason for each.

Do not list every document automatically.
Do not modify documents unless they genuinely need updating.
Do not modify approved requirements without authorization.
"@

        @{
            additionalContext = $message
        } | ConvertTo-Json -Compress

        exit 0
    }

    # No relevant source change detected.
    Write-Output '{}'
    exit 0
}
catch {
    [Console]::Error.WriteLine(
        "Documentation check hook failed: $($_.Exception.Message)"
    )
    exit 1
}
