package org.bourbon.compiler;

import static org.bourbon.compiler.literal.NumberLiteral.Magnitude.FractionalMagnitude.f;
import static org.bourbon.compiler.literal.NumberLiteral.Magnitude.FractionalMagnitude.m;
import static org.bourbon.compiler.literal.NumberLiteral.Magnitude.FractionalMagnitude.n;
import static org.bourbon.compiler.literal.NumberLiteral.Magnitude.FractionalMagnitude.p;
import static org.bourbon.compiler.literal.NumberLiteral.Magnitude.FractionalMagnitude.u;
import static org.bourbon.compiler.literal.NumberLiteral.Magnitude.IntegerMagnitude.G;
import static org.bourbon.compiler.literal.NumberLiteral.Magnitude.IntegerMagnitude.M;
import static org.bourbon.compiler.literal.NumberLiteral.Magnitude.IntegerMagnitude.P;
import static org.bourbon.compiler.literal.NumberLiteral.Magnitude.IntegerMagnitude.T;
import static org.bourbon.compiler.literal.NumberLiteral.Magnitude.IntegerMagnitude.k;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Gatherers;

import org.bourbon.compiler.literal.NumberLiteral;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

public class NumberLiteralTest {

    private static List<NumberLiteral> expand(String number, NumberLiteral.Magnitude magnitude) {
        var bigDecimal = new BigDecimal(number);
        return switch (magnitude) {
            case NumberLiteral.Magnitude.IntegerMagnitude m ->
                    List.of(NumberLiteral.of(bigDecimal), NumberLiteral.of(BigDecimal.ONE, magnitude), NumberLiteral.of(BigDecimal.ONE, m.asExponent()));

            case NumberLiteral.Magnitude.FractionalMagnitude f ->
                    List.of(NumberLiteral.of(bigDecimal), NumberLiteral.of(BigDecimal.ONE, magnitude), NumberLiteral.of(BigDecimal.ONE, f.asExponent()));

            default -> throw new IllegalArgumentException("Invalid magnitude");
        };
    }

    private static final List<Arguments.ArgumentSet> differentRepresentationsShouldEqual = Arrays.asList(
            Arguments.argumentSet("Thousands (10³, kilo): 1000 == 1k == 1.0e+3", expand("1000", k)),
            Arguments.argumentSet("Millions (10⁶, Mega): 1000000 == 1M == 1.0e+6", expand("1000000", M)),
            Arguments.argumentSet("Billions (10⁹, Giga): 1000000000 == 1G == 1.0e+9", expand("1000000000", G)),
            Arguments.argumentSet("Trillions (10¹², Tera): 1000000000000 == 1T == 1.0e+12", expand("1000000000000", T)),
            Arguments.argumentSet("Quadrillions (10¹⁵, Peta): 1000000000000000 == 1P == 1.0e+15", expand("1000000000000000", P)),
            Arguments.argumentSet("Thousandths (10⁻³, milli): 0.001 == 1m == 1.0e-3", expand("0.001", m)),
            Arguments.argumentSet("Millionths (10⁻⁶, micro): 0.000001 == 1u == 1.0e-6", expand("0.000001", u)),
            Arguments.argumentSet("Billionths (10⁻⁹, nano): 0.000000001 == 1n == 1.0e-9", expand("0.000000001", n)),
            Arguments.argumentSet("Trillionths (10⁻¹², pico): 0.000000000001 == 1p == 1.0e-12", expand("0.000000000001", p)),
            Arguments.argumentSet("Quadrillionths (10⁻¹⁵, femto): 0.000000000000001 == 1f == 1.0e-15",expand("0.000000000000001", f))
    );

    @FieldSource
    @ParameterizedTest
    void differentRepresentationsShouldEqual(List<NumberLiteral> values) {
        Assertions.assertAll("Expect all elements equal", values.stream()
                .gather(Gatherers.windowSliding(2))
                .map(window -> () -> Assertions.assertEquals(window.getFirst(), window.getLast())));
    }
}
