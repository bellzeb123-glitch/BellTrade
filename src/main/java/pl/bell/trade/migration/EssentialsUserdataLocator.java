package pl.bell.trade.migration;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class EssentialsUserdataLocator {

    private EssentialsUserdataLocator() {}

    public record LocateResult(File folder, List<String> triedPaths) {}

    /**
     * Szuka folderu userdata Essentials/EssentialsX względem katalogu serwera (nie CWD JVM).
     */
    public static LocateResult locate(JavaPlugin plugin, String configPath, String commandPath) {
        Set<File> candidates = new LinkedHashSet<>();
        List<String> tried = new ArrayList<>();

        if (commandPath != null && !commandPath.isBlank()) {
            candidates.add(resolve(plugin, commandPath));
        }
        if (configPath != null && !configPath.isBlank()) {
            candidates.add(resolve(plugin, configPath));
        }

        File pluginsDir = plugin.getDataFolder().getParentFile();
        for (String pluginFolder : List.of("Essentials", "EssentialsX", "Essentials2")) {
            candidates.add(new File(pluginsDir, pluginFolder + File.separator + "userdata"));
        }

        for (File dir : candidates) {
            tried.add(dir.getAbsolutePath());
            if (dir.isDirectory()) {
                File[] yml = dir.listFiles((d, name) -> name.endsWith(".yml"));
                if (yml != null && yml.length > 0) {
                    return new LocateResult(dir, tried);
                }
            }
        }

        for (File dir : candidates) {
            if (dir.isDirectory()) {
                return new LocateResult(dir, tried);
            }
        }

        return new LocateResult(null, tried);
    }

    public static String formatNotFound(LocateResult result) {
        StringBuilder sb = new StringBuilder("Nie znaleziono folderu userdata Essentials.");
        sb.append(" Sprawdzono: ");
        for (int i = 0; i < result.triedPaths().size(); i++) {
            if (i > 0) sb.append(" | ");
            sb.append(result.triedPaths().get(i));
        }
        sb.append(". Wgraj backup plugins/Essentials/userdata lub podaj sciezke: ");
        sb.append("/btrade import essentials <replace|add|max> <sciezka>");
        return sb.toString();
    }

    public static File resolve(JavaPlugin plugin, String path) {
        File direct = new File(path);
        if (direct.isAbsolute()) {
            return direct;
        }

        File serverRoot = plugin.getDataFolder().getParentFile().getParentFile();
        File fromServerRoot = new File(serverRoot, path);
        if (fromServerRoot.exists()) {
            return fromServerRoot;
        }

        String trimmed = path;
        if (trimmed.startsWith("plugins/") || trimmed.startsWith("plugins\\")) {
            trimmed = trimmed.substring("plugins/".length());
            File fromPlugins = new File(plugin.getDataFolder().getParentFile(), trimmed);
            if (fromPlugins.exists()) {
                return fromPlugins;
            }
            return fromPlugins;
        }

        return fromServerRoot;
    }

    public static Optional<File> locateFolder(JavaPlugin plugin, String configPath, String commandPath) {
        LocateResult r = locate(plugin, configPath, commandPath);
        return Optional.ofNullable(r.folder());
    }
}
