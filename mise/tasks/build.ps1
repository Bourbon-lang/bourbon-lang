#MISE description="Build all targets, including the native compiler"
#MISE depends=["ide-setup"]
bazel build //:build
