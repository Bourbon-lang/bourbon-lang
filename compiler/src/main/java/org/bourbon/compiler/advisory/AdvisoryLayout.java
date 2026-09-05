package org.bourbon.compiler.advisory;

import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Code;
import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Gutter;
import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Location;
import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Message;
import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Pointer;
import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Severity;
import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Snippet;
import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Text;
import static org.bourbon.compiler.advisory.AdvisoryConsole.Part.Underline;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import org.bourbon.compiler.Source;

/// An {@link Advisory.Handler} sink that formats advisories onto character-grid media via {@link AdvisoryConsole}.
///
/// `AdvisoryLayout` orchestrates the visual geometry of an advisory:
/// - Resolves source files referenced by diagnostic label spans using the provided {@code sources} function.
/// - Calculates line number gutter widths based on the span line numbers.
/// - Enforces 1:1 label pairing, ensuring every annotation is preceded directly by its corresponding source line.
public class AdvisoryLayout implements Advisory.Handler {
    private final Function<String, Source> sources;

    /// Creates a layout handler that resolves source files by name using {@code sources}.
    public AdvisoryLayout(Function<String, Source> sources) {
        this.sources = sources;
    }

    @Override
    public void submit(Advisory advisory) {
        layout(advisory);
    }

    /// Coordinates the grid layout for {@code advisory} and renders it via {@link AdvisoryConsole}.
    private void layout(Advisory advisory) {
        Advisory.Severity severity = advisory.severity();
        AdvisoryConsole.println(Severity(severity), Code(advisory.code()), Message(advisory.message()));

        var groupedByName = advisory.annotations().stream()
                .collect(Collectors.groupingBy(Advisory.SourceAnnotation::sourceName));

        groupedByName.entrySet().stream()
                .sorted(Comparator.comparing(this::hasPrimary, Comparator.reverseOrder()).thenComparing(Map.Entry::getKey))
                .forEach(entry -> {
                    var sourceName = entry.getKey();
                    int gutterWidth = 3 + (int)Math.floor(Math.log10(maxLineNumber(entry.getValue())));
                    var annotations = sortedAnnotations(entry.getValue());
                    var primary = annotations.stream()
                            .filter(Advisory.SourceAnnotation::isPrimary)
                            .findFirst()
                            .orElseGet(annotations::getFirst);

                    var source = sources.apply(sourceName);
                    AdvisoryConsole.println(Pointer(gutterWidth), Location(sourceName, primary.line(), primary.column()));
                    AdvisoryConsole.println(Gutter(gutterWidth));

                    for (Advisory.SourceAnnotation annotation : annotations) {
                        AdvisoryConsole.println(Gutter(gutterWidth, annotation.line()), Snippet(source.line(annotation.line())));
                        int offset = annotation.column() - 1;
                        int width = annotation.span().length();
                        if (annotation.isPrimary()) {
                            AdvisoryConsole.println(Gutter(gutterWidth), Underline(offset, width, severity), Text(annotation.message(), severity));
                        } else {
                            AdvisoryConsole.println(Gutter(gutterWidth), Underline(offset, width), Text(annotation.message()));
                        }
                        AdvisoryConsole.println(Gutter(gutterWidth));
                    }
                });
        AdvisoryConsole.printNewLine();
    }

    private List<Advisory.SourceAnnotation> sortedAnnotations(List<Advisory.SourceAnnotation> annotations) {
        return annotations.stream()
                .sorted(comparing(Advisory.SourceAnnotation::line, Advisory.SourceAnnotation::column))
                .toList();
    }

    private static int maxLineNumber(List<Advisory.SourceAnnotation> annotations) {
        return annotations.stream()
                .mapToInt(Advisory.SourceAnnotation::line).max()
                .orElse(0);
    }

    private boolean hasPrimary(Map.Entry<String, List<Advisory.SourceAnnotation>> entry) {
        return entry.getValue().stream().anyMatch(Advisory.SourceAnnotation::isPrimary);
    }

    private <T> Comparator<T> comparing(ToIntFunction<T> first, ToIntFunction<T> second) {
        return Comparator.comparingInt(first).thenComparingInt(second);
    }

    public static class AdvisoryLayoutBuilder {
        public AdvisoryLayoutBuilder() {
        }
    }
}
