package Utils.web;

import Utils.Core;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebClient {
    public SSLSocketFactory sslSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();

    public ConcurrentHashMap<String, String> headers = new ConcurrentHashMap<String, String>()
    {{ put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36 Edg/113.0.1774.50"); }};

    public int timeout = 5000;
    public boolean allowRedirect = false;

    public WebResponse open(final String method, final URI uri, final OutputStream outputStream, final boolean autoCloseStream) throws IOException { return open(method, uri.toURL(), outputStream, autoCloseStream); }

    public WebResponse open(final String method, final URL urlAddr, final OutputStream outputStream, final boolean autoCloseStream) throws IOException {
        switch (urlAddr.getProtocol()) {
            case "https": case "http":
                return new WebResponse(outputStream) {
                    private URL url = urlAddr;
                    private Socket s;
                    private OutputStream os;
                    private InputStream is;

                    @Override
                    public void connect(final Map<String, String> headers, final byte[] data) throws IOException {
                        final byte[] send;
                        {
                            final StringBuilder b = new StringBuilder(method).append(" ").append(
                                    url.getFile().isEmpty() ? "/" : Core.encodeURI(url.getFile())
                            ).append(" HTTP/1.1\nHost: ").append(url.getHost()).append(url.getPort() == -1 ? "" : ":" + url.getPort()).append("\n");

                            if (headers == null || headers.isEmpty())
                                for (Map.Entry<String, String> e : WebClient.this.headers.entrySet())
                                    b.append(e.getKey()).append(": ").append(e.getValue()).append('\n');
                            else {
                                for (Map.Entry<String, String> e : WebClient.this.headers.entrySet())
                                    if (!headers.containsKey(e.getKey()))
                                        b.append(e.getKey()).append(": ").append(e.getValue()).append('\n');
                                for (final Map.Entry<String, String> e : headers.entrySet())
                                    b.append(e.getKey()).append(": ").append(e.getValue()).append('\n');
                            }

                            if (data != null)
                                b.append("Content-Length: ").append(data.length).append('\n');
                            send = b.append('\n').toString().getBytes(StandardCharsets.UTF_8);
                        }
                        s = url.getProtocol().equals("https") ? sslSocketFactory.createSocket() : new Socket();
                        try {
                            s.setSoTimeout(timeout);
                            s.connect(new InetSocketAddress(url.getHost(), url.getPort() == -1 ? url.getDefaultPort() : url.getPort()));

                            os = s.getOutputStream();
                            os.write(send);
                            if (data != null)
                                os.write(data);
                            os.flush();

                            is = s.getInputStream();
                            StringBuilder line = new StringBuilder();
                            int b, pb = 0, i = 0, rc = -1;
                            while (true) {
                                b = is.read();
                                if (b == -1) {
                                    try {
                                        s.close();
                                    } catch (final IOException ignored) {}
                                    return;
                                }
                                final char ch = (char) b;
                                if (ch == '\n') {
                                    if (pb == b) break;
                                    pb = b;
                                    String l = line.toString();
                                    if (i == 0) {
                                        final int pe = l.indexOf(' ');
                                        if (pe == -1) {
                                            try {
                                                s.close();
                                            } catch (final IOException ignored) {
                                            }
                                            return;
                                        }
                                        l = l.substring(pe + 1);
                                        final int ce = l.indexOf(' ');
                                        if (ce == -1) {
                                            try {
                                                s.close();
                                            } catch (final IOException ignored) {
                                            }
                                            return;
                                        }
                                        rc = Integer.parseInt(l.substring(0, ce));
                                        response = l.substring(ce + 1);
                                    } else {
                                        final int eq = line.indexOf(":");
                                        if (eq == -1) {
                                            try {
                                                s.close();
                                            } catch (final IOException ignored) {
                                            }
                                            return;
                                        }
                                        //System.out.println(line);
                                        this.headers.put(line.substring(0, eq), line.substring(eq + 2));
                                    }
                                    line = new StringBuilder();
                                    i++;
                                    continue;
                                } else if (ch == '\r') continue;
                                line.append(ch);
                                pb = b;
                            }
                            if ((rc >= 301 && rc <= 303 || rc == 307) && this.headers.containsKey("Location") && allowRedirect) {
                                try {
                                    url = new URI(this.headers.get("Location")).toURL();
                                    try {
                                        s.close();
                                    } catch (final IOException ignored) {}
                                    connect(headers, data);
                                    return;
                                } catch (final URISyntaxException ex) {
                                    ex.printStackTrace();
                                }
                            }
                            responseCode = rc;
                        } catch (final SSLHandshakeException ex) {
                            try { s.close(); } catch (final IOException ignored) {}
                            responseCode = -2;
                        } catch (final IOException ex) {
                            try { s.close(); } catch (final IOException ignored) {}
                            throw ex;
                        }
                    }

                    @Override public void readAll() throws IOException {
                        final byte[] d = new byte[1024 * 1024];
                        if (headers.containsKey("Transfer-Encoding"))
                            switch (headers.get("Transfer-Encoding")) {
                                case "chunked":
                                    StringBuilder len = new StringBuilder();
                                    while (true) {
                                        int b = is.read();
                                        if (b == -1) break;
                                        final char ch = (char) b;
                                        if (ch == '\r') continue;
                                        if (ch == '\n') {
                                            if (len.length() == 0) continue;
                                            int size = Core.fromHexInt(len.toString()), r;
                                            len = new StringBuilder();
                                            if (size == 0) break;
                                            while (size > 0) {
                                                r = is.read(d, 0, Math.min(size, d.length));
                                                if (r == -1) break;
                                                size -= r;
                                                outputStream.write(d, 0, r);
                                                outputStream.flush();
                                            }
                                            continue;
                                        }
                                        len.append(ch);
                                    }
                                    break;
                            }
                        else if (headers.containsKey("Content-Length")) {
                            long size = Long.parseUnsignedLong(headers.get("Content-Length"));
                            while (size > 0) {
                                int l = is.read(d, 0, (int) Math.min(size, d.length));
                                if (l == -1) return;
                                size -= l;
                                outputStream.write(d, 0, l);
                                outputStream.flush();
                            }
                            try { s.close(); } catch (final IOException ignored) {}
                        }
                    }

                    @Override public void close() throws IOException { s.close(); outputStream.flush(); if (autoCloseStream) outputStream.close(); }
                };
            default: return new WebResponse(outputStream) {
                @Override public void connect(final Map<String, String> headers, final byte[] data) {}
                @Override public void readAll() {}
                @Override public void close() {}
            };
        }
    }

    public static void postFormData(final Map<String, Object> map, OutputStream os) throws IOException {
        boolean first = true;
        for (final Map.Entry<String, Object> e : map.entrySet()) {
            if (first)
                first = false;
            else
                os.write('&');
            os.write((URLEncoder.encode(e.getKey(), "UTF-8") + '=').getBytes(StandardCharsets.UTF_8));
            if (e.getValue() instanceof String)
                os.write(URLEncoder.encode((String) e.getValue(), "UTF-8").getBytes(StandardCharsets.UTF_8));
            else if (e.getValue() instanceof char[]) {
                final ByteBuffer bb = StandardCharsets.UTF_8.encode(CharBuffer.wrap((char[]) e.getValue()));
                final byte[] d = new byte[bb.remaining()];
                bb.get(d);
                os.write(d);
            } else
                os.write(URLEncoder.encode(e.getValue().toString(), "UTF-8").getBytes(StandardCharsets.UTF_8));
        }
    }

    public static byte[] postFormData(final Map<String, Object> map) throws IOException {
        try (
                final ByteArrayOutputStream os = new ByteArrayOutputStream()
        ) {
            postFormData(map, os);
            return os.toByteArray();
        }
    }
}
