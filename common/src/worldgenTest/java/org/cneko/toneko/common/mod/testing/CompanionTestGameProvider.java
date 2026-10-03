package org.cneko.toneko.common.mod.testing;

import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.Arguments;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** Headless game provider for applying an isolated production mixin to the actual Minecraft classes. */
public final class CompanionTestGameProvider implements GameProvider {
    private final GameTransformer transformer = new GameTransformer();
    private List<Path> paths;
    public String getGameId() { return "companion_regression"; }
    public String getGameName() { return "Mushroom companion regression"; }
    public String getRawGameVersion() { return "26.1.2"; }
    public String getNormalizedGameVersion() { return "26.1.2"; }
    public Collection<BuiltinMod> getBuiltinMods() { return List.of(); }
    public String getEntrypoint() { return CompanionMixinLauncher.class.getName(); }
    public Path getLaunchDirectory() { return Path.of("."); }
    public boolean requiresUrlClassLoader() { return false; }
    public Set<BuiltinTransform> getBuiltinTransforms(String name) { return Set.of(); }
    public boolean isEnabled() { return true; }
    public boolean locateGame(FabricLauncher launcher, String[] args) { paths = launcher.getClassPath(); return true; }
    public void initialize(FabricLauncher launcher) {
        launcher.setValidParentClassPath(paths);
        transformer.locateEntrypoints(launcher, paths);
    }
    public GameTransformer getEntrypointTransformer() { return transformer; }
    public void unlockClassPath(FabricLauncher launcher) {
        // Loader, Mixin and ASM must keep a single identity in the parent loader.
        paths.stream().filter(path -> {
            String name = path.getFileName().toString();
            return !name.startsWith("fabric-loader-") && !name.startsWith("sponge-mixin-") && !name.startsWith("asm-");
        }).forEach(path -> launcher.addToClassPath(path));
    }
    public void launch(ClassLoader loader) { throw new UnsupportedOperationException("Fixtures only; no game launch"); }
    public Arguments getArguments() { return new Arguments(); }
    public String[] getLaunchArguments(boolean sanitize) { return new String[0]; }
    public boolean canOpenErrorGui() { return false; }
}
