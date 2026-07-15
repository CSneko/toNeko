package org.cneko.toneko.bukkit.items;

import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.cneko.toneko.bukkit.ToNeko;

import java.io.*;
import java.nio.file.*;
import java.util.logging.Level;

/**
 * Integrates CraftEngine for custom item model rendering on vanilla clients.
 * Auto-copies toneko item YAML configs to CraftEngine's pack directory on startup.
 */
public class CraftEngineIntegration {
    private static boolean enabled = false;
    private static ItemManager itemManager;

    public static void init() {
        if (Bukkit.getPluginManager().getPlugin("CraftEngine") != null) {
            try {
                copyPackFiles();
                itemManager = CraftEngine.instance().itemManager();
                enabled = true;
                ToNeko.INSTANCE.getLogger().log(Level.INFO, "CraftEngine detected, custom item models enabled");
            } catch (Exception e) {
                ToNeko.INSTANCE.getLogger().log(Level.WARNING, "CraftEngine found but failed to init: " + e.getMessage());
            }
        }
    }

    /** Copy bundled pack YAML files to CraftEngine's packs directory */
    private static void copyPackFiles() {
        Path targetDir = Paths.get("plugins/CraftEngine/packs/toneko");
        try {
            Files.createDirectories(targetDir);
            copyDir("craftengine-packs/toneko", targetDir);
        } catch (Exception e) {
            ToNeko.INSTANCE.getLogger().log(Level.WARNING, "Failed to copy CraftEngine pack files: " + e.getMessage());
        }
    }

    private static void copyDir(String resourcePath, Path target) throws Exception {
        var classLoader = CraftEngineIntegration.class.getClassLoader();
        var base = Paths.get(resourcePath);
        // Walk the resource files using the classloader
        copyResources(classLoader, resourcePath, target);
    }

    private static void copyResources(ClassLoader cl, String path, Path targetDir) throws Exception {
        var url = cl.getResource(path);
        if (url == null) return;
        if (url.getProtocol().equals("jar")) {
            // Read from JAR
            var conn = url.openConnection();
            try (var in = conn.getInputStream();
                 var reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.endsWith("/")) continue;
                    String resPath = path + "/" + line;
                    Path targetFile = targetDir.resolve(line);
                    Files.createDirectories(targetFile.getParent());
                    try (var is = cl.getResourceAsStream(resPath)) {
                        if (is != null) Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } else {
            // Local file system (dev)
            Path source = Paths.get(url.toURI());
            if (Files.isDirectory(source)) {
                Files.walk(source).forEach(f -> {
                    try {
                        Path rel = source.relativize(f);
                        Path tf = targetDir.resolve(rel.toString());
                        if (Files.isDirectory(f)) Files.createDirectories(tf);
                        else Files.copy(f, tf, StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception ignored) {}
                });
            }
        }
    }

    public static boolean isEnabled() { return enabled; }

    /** Get or create a custom-wrapped item for the given player context */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static ItemStack createItem(String id, ItemStack fallback, Player player) {
        if (!enabled || itemManager == null) return fallback;
        try {
            Key key = Key.of("toneko", id);
            Object item = itemManager.createCustomWrappedItem(key, null);
            if (item instanceof net.momirealms.craftengine.core.item.Item ceItem) {
                return (ItemStack) ceItem.platformItem();
            }
        } catch (Exception ignored) {}
        return fallback;
    }
}
