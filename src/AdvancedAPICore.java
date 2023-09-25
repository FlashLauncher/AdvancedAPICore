import Launcher.Plugin;
import Launcher.PluginContext;
import UIL.Lang;
import UIL.UI;
import UIL.base.IImage;
import Utils.FSFile;
import Utils.FSRoot;
import Utils.json.Json;
import Utils.json.JsonDict;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AdvancedAPICore extends Plugin {
    public AdvancedAPICore(final PluginContext context) { super(context); }

    @Override
    public void onPreEnableChild(final PluginContext context) {
        final FSRoot root = context.getPluginRoot();
        try {
            if (root.exists("markets"))
                for (final FSFile f : root.list("markets"))
                    try {
                        final JsonDict e = Json.parse(new String(root.readFully(f), StandardCharsets.UTF_8)).getAsDict();
                        final String
                                id = f.getNameWithoutExt(),
                                type = e.getAsString("type"),
                                repo = e.getAsString("repo"),
                                name = e.getAsString("name"),
                                path = e.getAsString("path"),
                                iconPath = e.getAsStringOrDefault("icon", null)
                        ;
                        if (type.equals("github-chunked")) {
                            IImage icon = null;
                            if (iconPath != null)
                                try {
                                    icon = UI.image(iconPath);
                                } catch (final IOException ex) {
                                    ex.printStackTrace();
                                }
                            context.addMarket(new GitHubChunkedRepo(context, id, name, icon, repo, "main", path) {
                                @Override
                                public String getName() {
                                    return Lang.get(name).toString();
                                }
                            });
                        }
                    } catch (final Exception ex) {
                        ex.printStackTrace();
                    }
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }
}