#MISE description="Run compiler tests"
#MISE depends=["ide-setup"]
bazel test //:test
