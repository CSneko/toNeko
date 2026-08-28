package dev.architectury.loom.forge.tool;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavaToolchainSpec;

public abstract class JavaExecutableFetcher {
	@Inject
	protected abstract JavaToolchainService getToolchainService();

	public static Provider<String> getJavaToolchainExecutable(Project project) {
		return project.provider(() -> {
			final JavaExecutableFetcher fetcher = project.getObjects().newInstance(JavaExecutableFetcher.class);
			final JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
			final JavaToolchainSpec toolchain = java.getToolchain();

			if (!toolchain.getLanguageVersion().isPresent()) {
				// Toolchain not configured, we'll use the runtime Java version.
				return null;
			}

			final JavaLauncher launcher = fetcher.getToolchainService().launcherFor(toolchain).get();
			return launcher.getExecutablePath().getAsFile().getAbsolutePath();
		});
	}
}
