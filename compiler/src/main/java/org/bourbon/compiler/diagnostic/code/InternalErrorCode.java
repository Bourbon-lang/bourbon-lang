package org.bourbon.compiler.diagnostic.code;

/// Internal compiler error codes.
public enum InternalErrorCode implements DiagnosticCode {

    /// Generic compiler error diagnostic code that is usually used
    /// when the compiler encounters an unexpected condition that it
    /// cannot cleanly handle itself.
    ///
    /// Report as a bug to Bourbon language maintainers.
    CompilerBug("INT0000", "Compiler bug"),

    /// Generic IO error occurred.
    ///
    /// An unexpected IO exception occurred while trying to access IO-bound resources.
    /// This is a compiler internal error, signifying inability to complete the compilation process due to unexpected IO issues.
    IoError("INT0001", "I/O Error"),

    /// File not found.
    ///
    /// Returned by whenever the compiler tries to access a file and fails to open it at an expected location.
    /// This is a compiler internal error, signifying inability to complete the compilation process due to a missing or unreachable file.
    IoFileNotFound("INT0002", "File not found");

    private final String id;
    private final String title;

    InternalErrorCode(String id, String title) {
        this.id = id;
        this.title = title;
    }

    @Override public String id() {
        return id;
    }

    @Override public String title() {
        return title;
    }
}
