module bourbon.compiler {

    requires org.jspecify;
    requires info.picocli;
    requires org.jline.terminal;
    requires org.jline.reader;
    requires jdk.jshell;

    exports org.bourbon.compiler;
    exports org.bourbon.compiler.cli;

    exports org.bourbon.compiler.effects;
    exports org.bourbon.compiler.literal;
    exports org.bourbon.compiler.diagnostic;
    exports org.bourbon.compiler.diagnostic.code;
    exports org.bourbon.compiler.advisory;

    opens org.bourbon.compiler.cli to info.picocli;
}
