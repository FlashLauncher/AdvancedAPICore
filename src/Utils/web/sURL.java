package Utils.web;

import java.net.URL;

/**
 * @since AdvancedAPICore 0.2.2
 */
public class sURL {
    public final String
            scheme,
            domain,
            file
    ;

    public final int port;

    /**
     * @since AdvancedAPICore 0.2.2
     */
    public sURL(final String scheme, final String domain, final int port, final String file) {
        this.scheme = scheme;
        this.domain = domain;
        this.port = port;
        this.file = file;
    }

    /**
     * @since AdvancedAPICore 0.2.2
     */
    public sURL(final URL url) {
        this.scheme = url.getProtocol();
        this.domain = url.getHost();
        this.port = url.getPort();
        this.file = url.getFile();
    }

    /**
     * @since AdvancedAPICore 0.2.2
     */
    public sURL(String url) {
        int i = url.indexOf("://");
        if (i == -1)
            scheme = "";
        else {
            scheme = url.substring(0, i);
            url = url.substring(i + 3);
        }
        i = url.indexOf('/');
        if (i == -1) {
            i = url.indexOf('@');
            if (i != -1) {
                System.err.println("Utils/sURL.java does not have logins and passwords support");
                url = url.substring(i + 1);
            }
            i = url.indexOf(':');
            if (i == -1) {
                domain = url;
                port = -1;
            } else {
                domain = url.substring(0, i);
                url = url.substring(i + 1);
                port = url.isEmpty() ? -1 : Integer.parseInt(url);
            }
            file = "";
            return;
        }
        file = url.substring(i);
        url = url.substring(0, i);

        i = url.indexOf('@');
        if (i != -1) {
            System.err.println("Utils/sURL.java does not have logins and passwords support");
            url = url.substring(i + 1);
        }
        i = url.indexOf(':');
        if (i == -1) {
            domain = url;
            port = -1;
        } else {
            domain = url.substring(0, i);
            url = url.substring(i + 1);
            port = url.isEmpty() ? -1 : Integer.parseInt(url);
        }
    }
}