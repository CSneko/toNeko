package org.cneko.gal.common.util.pack;

import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.RepositorySource;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ExternalPackSource implements RepositorySource {
    public static final ExternalPackSource INSTANCE = new ExternalPackSource();
    @Override
    public void loadPacks(@NotNull Consumer<Pack> consumer) {
        // 创建并注册ExternalPack
        Pack pack = ExternalPack.createResourcePack();
        consumer.accept(pack);
    }
}
