package dev.architectury.loom.forge;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.fabricmc.loom.configuration.providers.forge.ForgeRunTemplate;
import net.fabricmc.loom.util.IOFunction;

public record UserdevConfig(
		String mcp,
		String universal,
		String sources,
		String patches,
		Optional<String> patchesOriginalPrefix,
		Optional<String> patchesModifiedPrefix,
		String binpatches,
		BinaryPatcherConfig binpatcher,
		List<String> libraries,
		Map<String, ForgeRunTemplate> runs,
		List<String> sass,
		AccessTransformerLocation ats
) {
	public static final Codec<UserdevConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("mcp").forGetter(UserdevConfig::mcp),
			Codec.STRING.fieldOf("universal").forGetter(UserdevConfig::universal),
			Codec.STRING.fieldOf("sources").forGetter(UserdevConfig::sources),
			Codec.STRING.fieldOf("patches").forGetter(UserdevConfig::patches),
			Codec.STRING.optionalFieldOf("patchesOriginalPrefix").forGetter(UserdevConfig::patchesOriginalPrefix),
			Codec.STRING.optionalFieldOf("patchesModifiedPrefix").forGetter(UserdevConfig::patchesModifiedPrefix),
			Codec.STRING.fieldOf("binpatches").forGetter(UserdevConfig::binpatches),
			BinaryPatcherConfig.CODEC.fieldOf("binpatcher").forGetter(UserdevConfig::binpatcher),
			Codec.STRING.listOf().fieldOf("libraries").forGetter(UserdevConfig::libraries),
			ForgeRunTemplate.MAP_CODEC.fieldOf("runs").forGetter(UserdevConfig::runs),
			Codec.STRING.listOf().optionalFieldOf("sass", List.of()).forGetter(UserdevConfig::sass),
			AccessTransformerLocation.CODEC.fieldOf("ats").forGetter(UserdevConfig::ats)
	).apply(instance, UserdevConfig::new));

	public record BinaryPatcherConfig(String dependency, List<String> args) {
		public static final Codec<BinaryPatcherConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("version").forGetter(BinaryPatcherConfig::dependency),
				Codec.STRING.listOf().fieldOf("args").forGetter(BinaryPatcherConfig::args)
		).apply(instance, BinaryPatcherConfig::new));
	}

	public sealed interface AccessTransformerLocation {
		Codec<AccessTransformerLocation> CODEC = Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(
				either -> either.map(Directory::new, FileList::new),
				location -> location.visit(Either::left, Either::right)
		);

		<T> T visit(Function<String, T> ifDirectory, Function<List<String>, T> ifFileList);
		<T> T visitIo(IOFunction<String, T> ifDirectory, IOFunction<List<String>, T> ifFileList) throws IOException;

		record Directory(String path) implements AccessTransformerLocation {
			@Override
			public <T> T visit(Function<String, T> ifDirectory, Function<List<String>, T> ifFileList) {
				return ifDirectory.apply(path);
			}

			@Override
			public <T> T visitIo(IOFunction<String, T> ifDirectory, IOFunction<List<String>, T> ifFileList) throws IOException {
				return ifDirectory.apply(path);
			}
		}

		record FileList(List<String> paths) implements AccessTransformerLocation {
			@Override
			public <T> T visit(Function<String, T> ifDirectory, Function<List<String>, T> ifFileList) {
				return ifFileList.apply(paths);
			}

			@Override
			public <T> T visitIo(IOFunction<String, T> ifDirectory, IOFunction<List<String>, T> ifFileList) throws IOException {
				return ifFileList.apply(paths);
			}
		}
	}
}
