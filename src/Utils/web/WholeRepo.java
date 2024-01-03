package Utils.web;

import Launcher.*;
import UIL.base.IImage;
import Utils.Version;
import Utils.json.Json;
import Utils.json.JsonDict;
import Utils.json.JsonElement;
import Utils.json.JsonList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * @since AdvancedAPICore 0.2.1
 */
public class WholeRepo extends Market {
    public final PluginContext context;
    public final WebClient client;
    public final String url;

    public WholeRepo(final PluginContext context, final WebClient client, final String url, final String id, final IImage icon) {
        super(id, icon);
        this.context = context;
        this.client = client;
        this.url = url.endsWith("/") ? url : url + '/';
    }

    public WholeRepo(final PluginContext context, final WebClient client, final String url, final String id, final Object name, IImage icon) {
        super(id, name, icon);
        this.context = context;
        this.client = client;
        this.url = url.endsWith("/") ? url : url + '/';
    }

    private final Object l = new Object();
    private Meta[] list = null;

    @Override
    public void checkForUpdates(final Meta... items) {
        synchronized (l) {
            if (list == null)
                try {
                    final ByteArrayOutputStream os = new ByteArrayOutputStream();
                    final WebResponse r = client.open("GET", new sURL(url + "packages.json"), os, true);
                    r.auto();
                    if (r.getResponseCode() == 200) {
                        final JsonList l = Json.parse(new ByteArrayInputStream(os.toByteArray()), true, "UTF-8").getAsList();
                        final ArrayList<Meta> ml = new ArrayList<>();
                        for (final JsonElement e : l) {
                            final JsonDict i = e.getAsDict();
                            final String t = i.getAsStringOrDefault("type", "plugin");
                            if (t.equals("plugin")) {
                                ml.add(new Meta(i.getAsString("id"), new Version(i.getAsString("version")), i.getAsString("author")) {
                                    final String
                                            n = i.getAsString("name"),
                                            sd = i.getAsStringOrDefault("shortDescription", null)
                                    ;

                                    @Override
                                    public IImage getIcon() {
                                        return null;
                                    }

                                    @Override
                                    public Object getName() {
                                        return n;
                                    }

                                    @Override
                                    public Object getShortDescription() {
                                        return sd;
                                    }

                                    @Override
                                    public TaskGroup install() {
                                        return new TaskGroupAutoProgress(1) {
                                            {
                                                addTask(new Task() {
                                                    @Override
                                                    public void run() throws Throwable {
                                                        final ByteArrayOutputStream os = new ByteArrayOutputStream();
                                                        final WebResponse r = client.open("GET",
                                                                new sURL(url + "packages/" + getID() + "/" + getVersion().toString() + ".jar"), os, true);
                                                        r.auto();
                                                        if (r.getResponseCode() == 200)
                                                            addTask(new InstallPluginTask(os.toByteArray()));
                                                        else
                                                            System.out.println("Unknown code: " + r.getResponseCode());
                                                    }
                                                });
                                            }
                                        };
                                    }
                                });
                            } else
                                System.out.println("Unknown meta type: " + t);
                        }
                        list = ml.toArray(new Meta[0]);
                    } else
                        throw new Exception("Unknown code: " + r.getResponseCode());
                } catch (final Exception ex) {
                    ex.printStackTrace();
                    list = new Meta[0];
                }
        }
        for (final Meta m : items) {
            for (final Meta t : list)
                if (t.getID().equals(m.getID())) {
                    if (t.getVersion().equals(m.getVersion()))
                        break;
                    final TaskGroup g = t.install();
                    if (FLCore.bindTaskGroup(t.getID(), g))
                        context.addTaskGroup(g);
                    break;
                }
        }
    }

    @Override
    public Meta[] find(String query) {
        if (query.isEmpty())
            return list;
        query = query.toLowerCase();
        final ArrayList<Meta> l = new ArrayList<>();
        for (final Meta m : list)
            if (m.getName().toString().toLowerCase().contains(query))
                l.add(m);
        return l.toArray(new Meta[0]);
    }
}