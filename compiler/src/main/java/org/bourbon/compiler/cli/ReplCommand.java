package org.bourbon.compiler.cli;

import static org.bourbon.compiler.cli.AnsiAdvisoryConsole.Style.NerdFont;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.bourbon.compiler.Scanner;
import org.bourbon.compiler.Source;
import org.bourbon.compiler.advisory.Advisory;
import org.bourbon.compiler.advisory.AdvisoryConsole;
import org.bourbon.compiler.advisory.AdvisoryLayout;
import org.bourbon.compiler.diagnostic.Consultant;
import org.bourbon.compiler.diagnostic.Diagnostic;
import org.bourbon.compiler.diagnostic.DiagnosticLayer;
import org.bourbon.compiler.diagnostic.DiagnosticPipeline;
import org.bourbon.compiler.diagnostic.code.Catalog;
import org.bourbon.compiler.diagnostic.code.DiagnosticCode;
import org.bourbon.compiler.effects.Effects;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.TerminalBuilder;

import picocli.CommandLine.Command;

@Command(
    name = "repl",
    description = "Opens a new Bourbon language REPL session.",
    mixinStandardHelpOptions = true
)
public final class ReplCommand implements Callable<Integer> {
    private static final String VANILLA_POD = Character.toString(0xf366);
    final String prompt1 = "\u0001\u001B[30;44m\u0002 " + VANILLA_POD + " bourbon\u0001\u001B[0;34m\u0002\uE0B0\u0001\u001B[0m\u0002 ";
    final String prompt2 = "\u0001\u001B[30;44m\u0002 " + VANILLA_POD + "     ...\u0001\u001B[0;34m\u0002\uE0B0\u0001\u001B[0m\u0002 ";

    private final Map<String, Source> sources = new HashMap<>();

    @Override
    public Integer call() {
        if (System.console() == null || !System.console().isTerminal()) {
            System.err.println("Error: The Bourbon REPL requires an interactive TTY environment and cannot be run in a non-interactive console.");
            System.err.println("Please run the REPL from a native terminal.");
            return Sysexits.EX_UNAVAILABLE;
        }

        try (var terminal = TerminalBuilder.builder()
                .system(true)
                .build()) {

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            terminal.writer().println(welcomeBanner());
            terminal.flush();

            while (true) {
                String line;
                try {
                    line = reader.readLine(prompt1);
                } catch (UserInterruptException e) {
                    // Ctrl+C - ignore and clear the line
                    continue;
                } catch (EndOfFileException e) {
                    // Ctrl+D
                    break;
                }

                if (line == null) {
                    break;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                var source = Source.of(line);

                var tokens = Effects.handle(() -> Scanner.scanTokens(source))
                        .with(Catalog.Handler.class, Catalog.builder()
                                .add(DiagnosticCode::standardErrorCodes)
                                .build())
                        .with(Diagnostic.Handler.class, DiagnosticPipeline.to(new Consultant())
                                .layer(DiagnosticLayer.validating())
                                .build())
                        .with(Advisory.Handler.class, new AdvisoryLayout(name ->
                                // FIXME: Make source content dependent on source name
                                Source.named(name).of(source.content())))
                        .with(AdvisoryConsole.Handler.class, new AnsiAdvisoryConsole(NerdFont))
                        .with(Terminal.Handler.class, Terminal.of(terminal))
                        .get();

                // Echo back for now
                for (var token : tokens) {
                    terminal.writer().println(token);
                }

                terminal.flush();
            }

        } catch (IOException e) {
            System.err.println("Error initializing REPL terminal: " + e.getMessage());
            return Sysexits.EX_IOERR;
        }

        return Sysexits.EX_OK;
    }

    private String welcomeBanner() {
        StringBuilder out = new StringBuilder("Welcome to Bourbon language REPL session\n");
        for (var versionLine : new BourbonCommand().getVersion()) {
            out.append(versionLine).append("\n");
        }
        return out.append("Start typing and witness the magic!\n").toString();
    }
}
