package org.bourbon.compiler.literal;

import static org.bourbon.compiler.literal.NumberLiteralFormatError.NumberBase.BINARY;
import static org.bourbon.compiler.literal.NumberLiteralFormatError.NumberBase.DECIMAL;
import static org.bourbon.compiler.literal.NumberLiteralFormatError.NumberBase.HEXADECIMAL;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.jspecify.annotations.Nullable;

public record NumberLiteral(BigDecimal value, Magnitude magnitude) {

    public static final NumberLiteral ZERO = new NumberLiteral(BigDecimal.ZERO, Magnitude.One.ONE);
    public static final NumberLiteral ONE = new NumberLiteral(BigDecimal.ONE, Magnitude.One.ONE);

    public static NumberLiteral of(BigDecimal value, Magnitude magnitude) {
        return new NumberLiteral(value, magnitude);
    }

    public static NumberLiteral of(BigDecimal value) {
        return new NumberLiteral(value, Magnitude.ONE);
    }

    @Override
    public String toString() {
        return value.toString() + magnitude;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NumberLiteral other) {
            return this.toBigDecimal().compareTo(other.toBigDecimal()) == 0;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.toBigDecimal().hashCode();
    }


    //<editor-fold desc="Converter functions">

    public BigDecimal toBigDecimal() {
        return magnitude.valueOf(value);
    }

    public BigInteger toBigInteger() {
        return magnitude.valueOf(value).toBigInteger();
    }

    public long toLong() {
        return magnitude.valueOf(value).longValue();
    }

    public int toInt() {
        return magnitude.valueOf(value).intValue();
    }

    public double toDouble() {
        return magnitude.valueOf(value).doubleValue();
    }

    public float toFloat() {
        return magnitude.valueOf(value).floatValue();
    }

    //</editor-fold>

    public sealed interface Magnitude {
        Magnitude ONE = One.ONE;

        BigDecimal valueOf(BigDecimal value);

        static boolean isMagnitudeSuffix(char c) {
            return switch (c) {
                // Integer magnitudes
                case 'k', 'M', 'G', 'T', 'P' -> true;
                // Fractional magnitudes
                case 'm', 'u', 'n', 'p', 'f' -> true;
                default -> false;
            };
        }

        static Magnitude valueOf(String input) {
            return switch (input) {
                // Integer magnitudes
                case "k", "M", "G", "T", "P" -> IntegerMagnitude.valueOf(input);
                // Fractional magnitudes
                case "m", "u", "n", "p", "f" -> FractionalMagnitude.valueOf(input);
                default -> Exponent.valueOf(input);
            };
        }

        enum One implements Magnitude {
            ONE {
                @Override
                public BigDecimal valueOf(BigDecimal value) {
                    return value;
                }

                @Override public String toString() {
                    return "";
                }
            }
        }

        enum IntegerMagnitude implements Magnitude {
            k(3), M(6), G(9), T(12), P(15);

            private final int magnitude;

            IntegerMagnitude(int magnitude) {
                this.magnitude = magnitude;
            }

            public Exponent asExponent() {
                return Exponent.valueOf("e+" + magnitude);
            }

            @Override
            public BigDecimal valueOf(BigDecimal value) {
                return value.movePointRight(magnitude);
            }

            public static IntegerMagnitude valueOf(char c) {
                return switch (c) {
                    case 'k' -> IntegerMagnitude.k;
                    case 'M' -> IntegerMagnitude.M;
                    case 'G' -> IntegerMagnitude.G;
                    case 'T' -> IntegerMagnitude.T;
                    case 'P' -> IntegerMagnitude.P;
                    default -> throw new IllegalArgumentException("Invalid magnitude: " + c);
                };
            }

            public static boolean isMagnitudeSuffix(char c) {
                return switch (c) {
                    case 'k', 'M', 'G', 'T', 'P' -> true;
                    default -> false;
                };
            }

        }

        enum FractionalMagnitude implements Magnitude {
            m(3), u(6), n(9), p(12), f(15);

            private final int magnitude;

            FractionalMagnitude(int magnitude) {
                this.magnitude = magnitude;
            }

            public BigDecimal valueOf(BigDecimal value) {
                return value.movePointLeft(magnitude);
            }

            public static Magnitude valueOf(char c) {
                return switch (c) {
                    case 'm' -> FractionalMagnitude.m;
                    case 'u' -> FractionalMagnitude.u;
                    case 'n' -> FractionalMagnitude.n;
                    case 'p' -> FractionalMagnitude.p;
                    case 'f' -> FractionalMagnitude.f;
                    default -> throw new IllegalArgumentException("Invalid fractional magnitude: " + c);
                };
            }

            public static boolean isMagnitudeSuffix(char c) {
                return switch (c) {
                    case 'm', 'u', 'n', 'p', 'f' -> true;
                    default -> false;
                };
            }
            public Exponent asExponent() {
                return Exponent.valueOf("e-" + magnitude);
            }
        }

        record Exponent(int magnitude) implements Magnitude {
            public static Exponent of(String exponent) {
                return new Exponent(Integer.parseInt(exponent));
            }

            @Override
            public BigDecimal valueOf(BigDecimal value) {
                // movePointRight(-3) automatically shifts left 3
                return value.movePointRight(magnitude);
            }

            @Override
            public String toString() {
                return "e" + (magnitude > 0 ? "+" : "-") + Math.abs(magnitude);
            }

            public static Exponent valueOf(String input) {
                if (input.length() < 3)
                    throw new IllegalArgumentException("Exponent must be at least 3 characters long");

                char c0 = input.charAt(0);
                if (Character.toLowerCase(c0) != 'e')
                    throw new IllegalArgumentException("Exponent must start with 'e' or 'E'");

                char c1 = input.charAt(1);
                if (c1 != '+' && c1 != '-')
                    throw new IllegalArgumentException("Exponent must have a sign after '" + c0 + "'");

                var exponent = Integer.parseInt(input.substring(1));
                return new Exponent(exponent);
            }

        }
    }


    public static @Nullable NumberLiteral parse(String input) throws NumberLiteralFormatError {
        if (input.isBlank()) {
            return null;
        }

        return switch (input.charAt(0)) {
            case '$' -> parseBinaryValue(input);
            case '#' -> parseHexadecimalValue(input);
            default -> parseDecimalValue(input);
        };
    }

    @SuppressWarnings("DuplicatedCode")
    private static NumberLiteral parseBinaryValue(String input) {
        if (input.length() < 2)
            throw NumberLiteralFormatError.missingDigitsAfterPrefix(BINARY, input.charAt(0), 1);

        int i = 1;
        var digits = new StringBuilder();
        while (i < input.length()) {
            int offset = i;
            var c = input.charAt(i++);
            if (c == '_') {
                if (offset == 1)
                    throw NumberLiteralFormatError.invalidLeadingUnderscore(BINARY, offset);

                if (i == input.length())
                    throw NumberLiteralFormatError.invalidTrailingUnderscore(BINARY, offset);

                if (input.charAt(i) == '_')
                    throw NumberLiteralFormatError.doubleUnderscore(BINARY, i);

                continue;
            }

            if (c != '0' && c != '1')
                throw NumberLiteralFormatError.invalidDigit(BINARY, c, offset);

            digits.append(c);
        }

        if (digits.isEmpty())
            throw NumberLiteralFormatError.missingDigitsAfterPrefix(BINARY, input.charAt(0), 1);

        var bigInteger = new BigInteger(digits.toString(), 2);
        return new NumberLiteral(new BigDecimal(bigInteger), Magnitude.ONE);
    }

    @SuppressWarnings("DuplicatedCode")
    private static NumberLiteral parseHexadecimalValue(String input) {
        if (input.length() < 2)
            throw NumberLiteralFormatError.missingDigitsAfterPrefix(HEXADECIMAL, input.charAt(0), 1);

        int i = 1;
        var digits = new StringBuilder();
        while (i < input.length()) {
            int offset = i;
            var c = input.charAt(i++);
            if (c == '_') {
                if (offset == 1)
                    throw NumberLiteralFormatError.invalidLeadingUnderscore(HEXADECIMAL, offset);

                if (i == input.length())
                    throw NumberLiteralFormatError.invalidTrailingUnderscore(HEXADECIMAL, offset);

                if (input.charAt(i) == '_')
                    throw NumberLiteralFormatError.doubleUnderscore(HEXADECIMAL, i);

                continue;
            }

            if ((c < '0' || c > '9') && (c < 'A' || c > 'F') && (c < 'a' || c > 'f'))
                throw NumberLiteralFormatError.invalidDigit(HEXADECIMAL, c, offset);

            digits.append(c);
        }

        if (digits.isEmpty())
            throw NumberLiteralFormatError.missingDigitsAfterPrefix(HEXADECIMAL, input.charAt(0), 1);

        var bigInteger = new BigInteger(digits.toString(), 16);
        return new NumberLiteral(new BigDecimal(bigInteger), Magnitude.ONE);
    }

    @SuppressWarnings("DuplicatedCode")
    private static NumberLiteral parseDecimalValue(String input) {
        int decimalSeparator = -1;

        var decimal = BigDecimal.ZERO;
        var magnitude = Magnitude.ONE;

        var decimalDigits = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            int offset = i;
            char c = input.charAt(i++);
            if (c == '_') {
                if (offset == 0)
                    throw NumberLiteralFormatError.invalidLeadingUnderscore(DECIMAL, offset);

                if (i == input.length())
                    throw NumberLiteralFormatError.invalidTrailingUnderscore(DECIMAL, offset);

                if (input.charAt(i) == '_')
                    throw NumberLiteralFormatError.doubleUnderscore(DECIMAL, i);

                if (input.charAt(i) == '.')
                    throw NumberLiteralFormatError.underscoreBeforeDecimal(offset);

                continue;
            }

            if (c == '.') {
                if (decimalDigits.isEmpty())
                    throw NumberLiteralFormatError.missingDecimalDigits(offset);

                if (decimalSeparator != -1)
                    throw NumberLiteralFormatError.multipleDecimalPoints(offset);

                if (i == input.length())
                    throw NumberLiteralFormatError.missingDigitsAfterDecimalPoint(i);

                if (input.charAt(i) == '_')
                    throw NumberLiteralFormatError.underscoreAfterDecimal(i);

                decimalSeparator = offset;
                decimalDigits.append(c);
                continue;
            }

            if (c == 'e' || c == 'E') {
                if (i >= input.length() || (input.charAt(i) != '+' && input.charAt(i) != '-'))
                    throw NumberLiteralFormatError.missingExponentSign(input.substring(offset), offset);

                i++;
                if (i >= input.length())
                    throw NumberLiteralFormatError.missingExponentDigits(input.substring(offset), offset);

                while (i < input.length()) {
                    c = input.charAt(i++);
                    if (c == 'e' || c == 'E')
                        throw NumberLiteralFormatError.multipleExponents(input.substring(offset), offset, c, i-1);

                    if (c < '0' || c > '9')
                        throw NumberLiteralFormatError.invalidExponentDigits(input.substring(offset), offset, c, i - 1);
                }

                magnitude = Magnitude.Exponent.valueOf(input.substring(offset));
                break;
            }

            if (Magnitude.isMagnitudeSuffix(c)) {
                if (decimalDigits.isEmpty())
                    throw NumberLiteralFormatError.missingDecimalDigits(offset);

                if (i < input.length())
                    throw NumberLiteralFormatError.invalidDigit(DECIMAL, c, offset);

                magnitude = Magnitude.valueOf(input.substring(offset));
                break;
            }

            if (c < '0' || c > '9')
                throw NumberLiteralFormatError.invalidDigit(DECIMAL, c, i - 1);

            decimalDigits.append(c);
        }

        if (decimalDigits.isEmpty())
            throw NumberLiteralFormatError.missingDecimalDigits(0);

        decimal = new BigDecimal(decimalDigits.toString());

        return new NumberLiteral(decimal, magnitude);
    }

}
