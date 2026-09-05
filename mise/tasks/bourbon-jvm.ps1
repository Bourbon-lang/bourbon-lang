#MISE description="Build and run the JVM-based compiler"
#MISE depends=["ide-setup"]
bazel build //compiler:compiler_jvm
if ($args.Count -eq 0) {
  & bazel-bin/compiler/compiler_jvm.cmd repl
} else {
  & bazel-bin/compiler/compiler_jvm.cmd $args
}
