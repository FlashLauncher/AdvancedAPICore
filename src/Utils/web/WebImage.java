package Utils.web;

import UIL.UI;
import UIL.base.LoadingImage;

import java.io.ByteArrayOutputStream;
import java.net.URI;

public class WebImage extends LoadingImage {
    public final URI uri;

    public WebImage(final WebClient client, final URI uri) {
        this.uri = uri;
        if (uri.getScheme().equals("https") || uri.getScheme().equals("http")) {
            new Thread(() -> {
                try {
                    final ByteArrayOutputStream os = new ByteArrayOutputStream();
                    final WebResponse r = client.open("GET", uri, os, true);
                    r.auto();
                    if (r.responseCode == WebResponse.OK)
                        setImage(UI.image(os.toByteArray()));
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
            return;
        }
        setImage(null);
    }
}