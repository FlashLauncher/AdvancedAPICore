package Utils.web;

import Launcher.*;
import UIL.base.IImage;
import Utils.*;
import Utils.json.Json;
import Utils.json.JsonDict;
import Utils.web.WebClient;
import Utils.web.WebResponse;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class GitHubChunkedRepo extends Market {
    private static final WebClient client = new WebClient() {{ allowRedirect = true; timeout = 5000; }};
    public final String repo, branch, path;
    private final ConcurrentLinkedQueue<Meta> metas = new ConcurrentLinkedQueue<>();
    private final Object locker = new Object();
    private boolean isNotFinished = true;
    public final PluginContext context;

    public GitHubChunkedRepo(final PluginContext context, final String id, final Object name, final IImage icon, final String repo, final String branch, final String path) {
        super(id, name, icon);
        this.repo = repo;
        this.branch = branch;
        this.path = path;
        this.context = context;

        context.addTaskGroup(new TaskGroupAutoProgress(1) {{
            addTask(new Task() {
                @Override
                public void run() {
                    try {
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        client.open("GET", new URI("https://raw.githubusercontent.com/" + repo + "/" + branch + "/" + path), os, true).auto();
                        IniGroup g = new IniGroup(new String(os.toByteArray(), StandardCharsets.UTF_8), false).getAsGroup(FlashLauncher.VERSION.toString());
                        final File c = new File(context.getPluginCache(), id);
                        final ArrayList<String> allowed = new ArrayList<>();
                        if (g != null) {
                            if (!c.exists())
                                c.mkdirs();
                            for (final Map.Entry<String, Object> e : g.entrySet()) {
                                final File f = new File(c, e.getKey() + ".json");
                                if (f.exists()) {
                                    allowed.add(f.getName());
                                    continue;
                                }
                                os = new ByteArrayOutputStream();
                                client.open("GET", new URI("https://raw.githubusercontent.com/" + repo + "/" + branch + "/" + e.getValue()), os, true).auto();
                                final byte[] d = os.toByteArray();
                                if (Core.hashToHex("SHA-256", d).equals(e.getKey()))
                                    try (final FileOutputStream fos = new FileOutputStream(f)) {
                                        fos.write(Json.parse(new ByteArrayInputStream(d), true, StandardCharsets.UTF_8).getAsList().toString().getBytes(StandardCharsets.UTF_8));
                                        allowed.add(f.getName());
                                    }
                            }
                        }
                        if (c.exists()) {
                            final File[] l = c.listFiles();
                            if (l != null)
                                for (final File f : l)
                                    if (!allowed.contains(f.getName()))
                                        f.delete();
                        }
                    } catch (final Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            });

            addTask(new Task() {
                @Override
                public void run() {
                    try {
                        metas.clear();
                        final File[] l = new File(context.getPluginCache(), id).listFiles();
                        if (l != null)
                            for (final File f : l)
                                for (final JsonDict d : Json.parse(Files.newInputStream(f.toPath()), true, StandardCharsets.UTF_8).getAsList().toArray(new JsonDict[0])) {
                                    URI uri = null;
                                    if (d.has("icon"))
                                        try {
                                            uri = URI.create(d.getAsString("icon"));
                                        } catch (final IllegalArgumentException ex) {
                                            ex.printStackTrace();
                                        }
                                    final WebImage icon = uri == null ? null : new WebImage(client, uri);
                                    metas.add(new Meta(d.getAsString("id"), new Version(d.getAsString("version")), d.getAsString("author")) {
                                        private final String
                                                n = d.getAsString("name"),
                                                sd = d.getAsStringOrDefault("shortDescription", null),
                                                asset = d.getAsString("asset")
                                        ;

                                        @Override public IImage getIcon() { return icon; }
                                        @Override public Object getName() { return n; }
                                        @Override public Object getShortDescription() { return sd; }

                                        @Override
                                        public TaskGroup install() {
                                            return new TaskGroupAutoProgress(1) {{
                                                addTask(new Task() {
                                                    @Override
                                                    public void run() throws Throwable {
                                                        while (true) {
                                                            ByteArrayOutputStream os = new ByteArrayOutputStream();
                                                            final WebResponse r = client.open("GET", new URI(asset), os, true);
                                                            r.auto();
                                                            if (r.getResponseCode() == WebResponse.OK) {
                                                                if (FlashLauncher.VERSION.toString().equals("0.2") || FlashLauncher.VERSION.toString().equals("0.2.1"))
                                                                    try {
                                                                        System.out.println("Fix InstallPluginTask.java");
                                                                        final ByteArrayOutputStream os2 = new ByteArrayOutputStream();
                                                                        try (
                                                                                final ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(os.toByteArray()));
                                                                                final ZipOutputStream zos = new ZipOutputStream(os2)
                                                                                ) {
                                                                            for (ZipEntry e = zis.getNextEntry(); e != null; e = zis.getNextEntry()) {
                                                                                zos.putNextEntry(new ZipEntry(e.getName()));
                                                                                if (!e.getName().endsWith("/")) {
                                                                                    final byte[] d = IO.readFully(zis, false);
                                                                                    zos.write(d, 0, d.length);
                                                                                    if (e.getName().equals("fl-info.ini")) {
                                                                                        zos.closeEntry();
                                                                                        zos.putNextEntry(new ZipEntry("fl-plugin.ini"));
                                                                                        zos.write(d, 0, d.length);
                                                                                    }
                                                                                }
                                                                                zos.closeEntry();
                                                                            }
                                                                        }
                                                                        os = os2;
                                                                    } catch (final Exception ex) {
                                                                        ex.printStackTrace();
                                                                    }
                                                                addTask(new InstallPluginTask(os.toByteArray()));
                                                                break;
                                                            } else {
                                                                final long s = Long.parseLong(r.headers.get("X-RateLimit-Reset")) * 1000 - System.currentTimeMillis();
                                                                if (s <= 0)
                                                                    continue;
                                                                System.out.println("Rate Limit, wait " + (s / 1000) + "s");
                                                                Thread.sleep(s);
                                                            }
                                                        }
                                                    }
                                                });
                                            }};
                                        }
                                    });
                                }
                    } catch (final Throwable ex) {
                        ex.printStackTrace();
                    }
                    synchronized (locker) {
                        isNotFinished = false;
                        locker.notifyAll();
                    }
                }
            });
        }});
    }

    @Override public void checkForUpdates(final Meta... items) {
        try {
            synchronized (locker) {
                if (isNotFinished)
                    locker.wait();
            }
            for (final Meta m : items) {
                for (final Meta m1 : metas)
                    if (m1.getID().equals(m.getID())) {
                        if (!m1.getVersion().equals(m.getVersion())) {
                            final TaskGroup g = m1.install();
                            if (FLCore.bindTaskGroup(m1.getID(), g))
                                context.addTaskGroup(g);
                        }
                        break;
                    }
            }
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Meta[] find(String search) {
        search = search.toLowerCase();
        final ArrayList<Meta> l = new ArrayList<>();
        for (final Meta m : metas)
            if (m.getName().toString().toLowerCase().contains(search))
                l.add(m);
        return l.toArray(new Meta[0]);
    }
}
