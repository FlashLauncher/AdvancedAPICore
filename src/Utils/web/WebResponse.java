package Utils.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;

public abstract class WebResponse {
    public static final int
            NOT_FOUND = 404,
            MOVED_PERMANENTLY = 301,
            OK = 200
    ;

    public final OutputStream os;

    public WebResponse(final OutputStream outputStream) {
        os = outputStream;
    }

    int responseCode = -1;
    String response = null;

    public final ConcurrentHashMap<String, String> headers = new ConcurrentHashMap<>();
    public abstract void connect() throws IOException, InterruptedException;
    public abstract void readAll() throws IOException, InterruptedException;
    public abstract void close() throws IOException, InterruptedException;
    public void auto() throws IOException, InterruptedException {
        connect();
        readAll();
        close();
    }

    public final int getResponseCode() { return responseCode; }
}
