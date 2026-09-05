#MISE description="Configure .bazelproject.user with the local mise Java path for IntelliJ"

$javaPath = mise where java

@"
ide_java_home_override: $javaPath
java_language_level: 25
"@ | Set-Content -Path .bazelproject.user -Encoding utf8

Write-Host "Configured .bazelproject.user with ide_java_home_override: $javaPath"
