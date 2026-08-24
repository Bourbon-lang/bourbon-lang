package org.bourbon.compiler.literal;

import java.util.Locale;

public class NumberLiteralFormatError extends NumberFormatException {
    public final Detail detail;

    public Detail getDetail() {
        return detail;
    }

    public enum NumberBase {
        BINARY("0 or 1"),
        HEXADECIMAL("0-9, a-f, A-F"),
        DECIMAL("0-9");

        private final String validDigits;

        NumberBase(String validDigits) {
            this.validDigits = validDigits;
        }

        public String validDigits() { return validDigits; }

        public String capitalised() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }

        public String toLowerCase() {
            return name().toLowerCase();
        }
    }

    public sealed interface Detail {
        int offset();
        default int length() { return 1; }

        record InvalidDigit(NumberBase base, char character, int offset) implements Detail {}
        record MissingDigitsAfterPrefix(NumberBase base, char prefix, int offset) implements Detail {}
        record MissingDecimalDigits(int offset) implements Detail {}
        record MultipleDecimalPoints(int offset) implements Detail {}
        record MissingDigitsAfterDecimalPoint(int offset) implements Detail {}
        record MissingExponentSign(String rawExponent, int exponentOffset) implements Detail {
            @Override public int offset() {
                return exponentOffset + 1;
            }

            public int exponentLength() {
                return rawExponent.length();
            }
        }

        record MissingExponentDigits(String rawExponent, int exponentOffset) implements Detail {
            @Override public int offset() {
                return exponentOffset + rawExponent.length();
            }

            public int exponentLength() {
                return rawExponent.length();
            }
        }

        record MultipleExponents(String rawExponent, int exponentOffset, char secondExponentChar, int offset) implements Detail {
            public int exponentLength() {
                return rawExponent.length();
            }
        }

        record InvalidExponentDigits(String rawExponent, int exponentOffset, char invalidDigit, int offset) implements Detail {
            public int exponentLength() {
                return rawExponent.length();
            }
        }
        record MisplacedUnderscore(NumberBase base, Placement placement, int offset) implements Detail {
            public enum Placement {
                LEADING, TRAILING, DOUBLE, BEFORE_DECIMAL_POINT, AFTER_DECIMAL_POINT;
                public String toLowerCase() {
                    return name().toLowerCase(Locale.ROOT).replace('_', ' ');
                }
            }
        }
    }

    protected NumberLiteralFormatError(String message, Detail detail) {
        super(message);
        this.detail = detail;
    }

    public static NumberLiteralFormatError missingDigitsAfterPrefix(NumberBase base, char prefix, int offset) {
        return new NumberLiteralFormatError(
                "Missing digits in " + base.toLowerCase() + " literal",
                new Detail.MissingDigitsAfterPrefix(base, prefix, offset));
    }

    public static NumberLiteralFormatError missingDecimalDigits(int offset) {
        return new NumberLiteralFormatError(
                "Missing digits in decimal literal",
                new Detail.MissingDecimalDigits(offset));
    }

    public static NumberLiteralFormatError invalidDigit(NumberBase base, char character, int offset) {
        return new NumberLiteralFormatError(
                "Invalid " + base.toLowerCase() + " digit '" + character + "'",
                new Detail.InvalidDigit(base, character, offset));
    }

    public static NumberLiteralFormatError invalidLeadingUnderscore(NumberBase base, int offset) {
        return new NumberLiteralFormatError(
                "Underscore is not allowed at the beginning of a " + base.toLowerCase() + " literal",
                new Detail.MisplacedUnderscore(base, Detail.MisplacedUnderscore.Placement.LEADING, offset));
    }

    public static NumberLiteralFormatError invalidTrailingUnderscore(NumberBase base, int offset) {
        return new NumberLiteralFormatError(
                "Underscore is not allowed at the end of a " + base.toLowerCase() + " literal",
                new Detail.MisplacedUnderscore(base, Detail.MisplacedUnderscore.Placement.TRAILING, offset));
    }

    public static NumberLiteralFormatError doubleUnderscore(NumberBase base, int offset) {
        return new NumberLiteralFormatError(
                "Double underscore is not allowed in a " + base.toLowerCase() + " literal",
                new Detail.MisplacedUnderscore(base, Detail.MisplacedUnderscore.Placement.DOUBLE, offset));
    }

    public static NumberLiteralFormatError underscoreBeforeDecimal(int offset) {
        return new NumberLiteralFormatError(
                "Underscore is not allowed immediately before decimal point '.'",
                new Detail.MisplacedUnderscore(NumberBase.DECIMAL, Detail.MisplacedUnderscore.Placement.BEFORE_DECIMAL_POINT, offset));
    }

    public static NumberLiteralFormatError underscoreAfterDecimal(int offset) {
        return new NumberLiteralFormatError(
                "Underscore is not allowed immediately after decimal point '.'",
                new Detail.MisplacedUnderscore(NumberBase.DECIMAL, Detail.MisplacedUnderscore.Placement.AFTER_DECIMAL_POINT, offset));
    }

    public static NumberLiteralFormatError multipleDecimalPoints(int offset) {
        return new NumberLiteralFormatError("Multiple decimal points in number literal",
                new Detail.MultipleDecimalPoints(offset));
    }

    public static NumberLiteralFormatError missingDigitsAfterDecimalPoint(int offset) {
        return new NumberLiteralFormatError("Missing digits after decimal point",
                new Detail.MissingDigitsAfterDecimalPoint(offset));
    }

    public static NumberLiteralFormatError missingExponentDigits(String rawExponent, int exponentOffset) {
        return new NumberLiteralFormatError(
                "Missing digits in decimal exponent",
                new Detail.MissingExponentDigits(rawExponent, exponentOffset));
    }

    public static NumberLiteralFormatError missingExponentSign(String rawExponent, int exponentOffset) {
        return new NumberLiteralFormatError(
                "Missing exponent sign in decimal exponent",
                new Detail.MissingExponentSign(rawExponent, exponentOffset));
    }

    public static NumberLiteralFormatError multipleExponents(String rawExponent, int offset, char secondExponentChar, int secondExponentOffset) {
        return new NumberLiteralFormatError(
                "Multiple exponents in decimal exponent",
                new Detail.MultipleExponents(rawExponent, offset, secondExponentChar, secondExponentOffset));
    }

    public static NumberLiteralFormatError invalidExponentDigits(String rawExponent, int offset, char invalidChar, int invalidCharOffset) {
        return new NumberLiteralFormatError("Invalid exponent digits in decimal exponent",
                new Detail.InvalidExponentDigits(rawExponent, offset, invalidChar, invalidCharOffset));
    }

}
