package Utils.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class WebResponse {
    public static final int
            NOT_FOUND = 404,
            MOVED_PERMANENTLY = 301,
            OK = 200
    ;

    public final OutputStream os;

    public WebResponse(final OutputStream outputStream) { os = outputStream; }

    int responseCode = -1;
    String response = null;

    public final ConcurrentHashMap<String, String> headers = new ConcurrentHashMap<>();

    public abstract void connect(final Map<String, String> headers, final byte[] data) throws IOException, InterruptedException;
    public void connect(final byte[] data) throws IOException, InterruptedException { connect(null, data); }
    public void connect() throws IOException, InterruptedException { connect(null, new byte[0]); }

    public abstract void readAll() throws IOException, InterruptedException;
    public abstract void close() throws IOException, InterruptedException;

    public void auto(final Map<String, String> map, final byte[] data) throws IOException, InterruptedException {
        connect(map, data);
        readAll();
        close();
    }

    public void auto(final byte[] data) throws IOException, InterruptedException {
        connect(null, data);
        readAll();
        close();
    }

    public void auto() throws IOException, InterruptedException {
        connect(null, new byte[0]);
        readAll();
        close();
    }

    public final int getResponseCode() { return responseCode; }
}
