package pl.bell.trade.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.bell.trade.BellTrade;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LangManager {

    private final BellTrade plugin;
    private FileConfiguration lang;

    public LangManager(BellTrade plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String language = plugin.getConfig().getString("language", "en");
        lang = loadAndMerge(language);
    }

    private FileConfiguration loadAndMerge(String langCode) {
        String fileName = "lang/" + langCode + ".yml";
        File diskFile = new File(plugin.getDataFolder(), fileName);

        FileConfiguration base = loadFromJar(fileName);
        if (base == null) {
            plugin.getLogger().warning("Lang file not found in jar: " + fileName + ", falling back to en");
            base = loadFromJar("lang/en.yml");
        }
        if (base == null) {
            plugin.getLogger().severe("No lang files in jar! Loading from disk.");
            return diskFile.exists()
                ? YamlConfiguration.loadConfiguration(diskFile)
                : new YamlConfiguration();
        }

        if (diskFile.exists()) {
            FileConfiguration disk = YamlConfiguration.loadConfiguration(diskFile);
            for (String key : disk.getKeys(true)) {
                if (!disk.isConfigurationSection(key) && base.contains(key)) {
                    base.set(key, disk.get(key));
                }
            }
        }

        try {
            diskFile.getParentFile().mkdirs();
            base.save(diskFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not save lang file: " + fileName, e);
        }

        return base;
    }

    private FileConfiguration loadFromJar(String fileName) {
        try (InputStream stream = plugin.getResource(fileName)) {
            if (stream == null) return null;
            return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading lang from jar: " + fileName, e);
            return null;
        }
    }

    public String get(String key, Object... args) {
        String prefix = lang.getString("prefix", "&8[&6BellTrade&8] &r");
        String msg = lang.getString(key, "&cMissing lang key: " + key);
        return applyPlaceholders(prefix + msg, args);
    }

    public String getRaw(String key, Object... args) {
        String msg = lang.getString(key, "&cMissing lang key: " + key);
        return applyPlaceholders(msg, args);
    }

    public List<String> getList(String key, Object... args) {
        return lang.getStringList(key).stream()
            .map(line -> applyPlaceholders(line, args))
            .collect(Collectors.toList());
    }

    public Component component(String key, Object... args) {
        return colorize(get(key, args));
    }

    public Component componentRaw(String key, Object... args) {
        return colorize(getRaw(key, args));
    }

    public Component colorize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public String formatAmount(double amount) {
        return plugin.getCurrencyConfig().formatAmount(amount);
    }

    public boolean has(String key) {
        return lang.contains(key);
    }

    public String categoryDisplayName(String categoryId, String yamlFallback) {
        String key = "shop.category." + categoryId;
        if (has(key)) return getRaw(key);
        return yamlFallback;
    }

    public String materialName(org.bukkit.Material material) {
        String key = "shop.material." + material.name();
        if (has(key)) return getRaw(key);
        return material.name().toLowerCase().replace('_', ' ');
    }

    private String applyPlaceholders(String msg, Object... args) {
        CurrencyConfig cc = plugin.getCurrencyConfig();
        msg = msg.replace("{currency}", cc.getCurrencyName())
                 .replace("{symbol}", cc.getCurrencySymbol());

        for (int i = 0; i + 1 < args.length; i += 2) {
            msg = msg.replace("{" + args[i] + "}", String.valueOf(args[i + 1]));
        }
        return msg;
    }
}
