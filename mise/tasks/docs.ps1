#MISE description="Build Antora documentation site using Bazel"
#USAGE flag "-o --open" help="Open docs in default browser after build"
#USAGE flag "-s --serve" help="Serve docs on a local HTTP server"

bazel build //docs:site

New-Item -ItemType Directory -Force -Path build/site | Out-Null
tar -xzf bazel-bin/docs/site_archive.tar.gz -C build/site

$SiteIndex = Join-Path $PWD "build/site/index.html"
Write-Host ""
Write-Host "Documentation built successfully!"
Write-Host "Browse locally: file://$SiteIndex"
Write-Host ""

if ($env:usage_open -eq "true") {
    Start-Process $SiteIndex
} elseif ($env:usage_serve -eq "true") {
    Write-Host "Serving docs via jwebserver at http://localhost:8080 (Ctrl+C to stop)..."
    jwebserver -d build/site -p 8080
}