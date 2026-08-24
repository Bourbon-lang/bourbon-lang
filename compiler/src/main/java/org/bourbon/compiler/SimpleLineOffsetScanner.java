package org.bourbon.compiler;

import java.util.Arrays;

/// Simple line input scanner that scans the entire character sequence and returns an array of line offsets.
public final class SimpleLineOffsetScanner implements LineOffsetScanner {

    @Override public int[] scan(CharSequence input) {
        int len = input.length();
        int[] buf = new int[Math.max(8, len / 32)];
        int count = 0;
        buf[count++] = 0; // Line 1 starts at 0
        int i = 0;
        int limit = len - 3; // Stop when fewer than 4 chars remain

        while (i < limit) {
            char c0 = input.charAt(i);
            char c1 = input.charAt(i + 1);
            char c2 = input.charAt(i + 2);
            char c3 = input.charAt(i + 3);

            if (c0 == '\n' || c1 == '\n' || c2 == '\n' || c3 == '\n') {
                if (c0 == '\n') { if (count == buf.length) buf = grow(buf); buf[count++] = i + 1; }
                if (c1 == '\n') { if (count == buf.length) buf = grow(buf); buf[count++] = i + 2; }
                if (c2 == '\n') { if (count == buf.length) buf = grow(buf); buf[count++] = i + 3; }
                if (c3 == '\n') { if (count == buf.length) buf = grow(buf); buf[count++] = i + 4; }
            }
            i += 4;
        }

        // Scalar tail loop for remaining (0 to 3) characters
        while (i < len) {
            if (input.charAt(i) == '\n') { if (count == buf.length) buf = grow(buf); buf[count++] = i + 1; }
            i++;
        }

        // Always add length of the whole string as last row
        if (i > 0 && input.charAt(i - 1) != '\n') {
            if (count == buf.length) buf = grow(buf); buf[count++] = input.length();
        }

        return (count == buf.length) ? buf : Arrays.copyOf(buf, count);
    }

    private static int[] grow(int[] buf) {
        return Arrays.copyOf(buf, buf.length * 2);
    }
}
