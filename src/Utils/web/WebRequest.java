package Utils.web;

import java.io.IOException;
import java.io.InputStream;

public class WebRequest {
    private final InputStream is;

    public final String
            method,
            path,
            protocol
    ;

    public WebRequest(final InputStream inputStream) throws IOException {
        is = inputStream;
        method = readTo(' ');
        path = readTo(' ');
        protocol = readLine();


    }

    private char readChar() throws IOException {
        final int i = is.read();
        if (i == -1)
            throw new IOException("End");
        return (char) i;
    }

    private String readTo(final char ch) throws IOException {
        final StringBuilder b = new StringBuilder();
        while (true) {
            final char c = readChar();
            if (c == ch)
                break;
            b.append(c);
        }
        return b.toString();
    }

    private String readTo(final char... chars) throws IOException {
        final StringBuilder b = new StringBuilder();
        m:
        while (true) {
            final char c = readChar();
            for (final char ch : chars)
                if (ch == c)
                    break m;
            b.append(c);
        }
        return b.toString();
    }

    private String readLine() throws IOException {
        final StringBuilder b = new StringBuilder();
        while (true) {
            final char c = readChar();
            if (c == '\r')
                continue;
            if (c == '\n')
                break;
            b.append(c);
        }
        return b.toString();
    }
}