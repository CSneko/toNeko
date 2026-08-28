package dev.architectury.loom.forge.tool;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.output.NullOutputStream;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.configuration.ShowStacktrace;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;
import org.jetbrains.annotations.Nullable;

/**
 * Contains helpers for executing Forge's command line tools
 * with suppressed output streams to prevent annoying log spam.
 */
public abstract class ForgeToolExecutor {
	@Inject
	protected abstract ExecOperations getExecOperations();

	public static boolean shouldShowVerboseStdout(Project project) {
		// if running with INFO or DEBUG logging
		return project.getGradle().getStartParameter().getLogLevel().compareTo(LogLevel.LIFECYCLE) < 0;
	}

	public static boolean shouldShowVerboseStderr(Project project) {
		// if stdout is shown or stacktraces are visible so that errors printed to stderr show up
		return shouldShowVerboseStdout(project) || project.getGradle().getStartParameter().getShowStacktrace() != ShowStacktrace.INTERNAL_EXCEPTIONS;
	}

	public static Settings getDefaultSettings(Project project) {
		final Settings settings = project.getObjects().newInstance(Settings.class);
		settings.getExecutable().set(JavaExecutableFetcher.getJavaToolchainExecutable(project));
		settings.getShowVerboseStdout().set(shouldShowVerboseStdout(project));
		settings.getShowVerboseStderr().set(shouldShowVerboseStderr(project));
		return settings;
	}

	/**
	 * Executes an external Java process.
	 *
	 * <p>This method cannot be used during configuration.
	 * Use {@link ForgeToolValueSource#exec(Project, Action)} for those cases.
	 *
	 * @param project      the project
	 * @param configurator the {@code javaexec} configuration action
	 * @return the execution result
	 */
	public static ExecResult exec(Project project, Action<? super Settings> configurator) {
		final Settings settings = getDefaultSettings(project);
		configurator.execute(settings);
		return project.getObjects().newInstance(ForgeToolExecutor.class).exec(settings);
	}

	private ExecResult exec(Settings settings) {
		return exec(getExecOperations(), settings);
	}

	public static ExecResult exec(ExecOperations execOperations, Settings settings) {
		return execOperations.javaexec(spec -> {
			final @Nullable String executable = settings.getExecutable().getOrNull();
			if (executable != null) spec.setExecutable(executable);
			spec.getMainClass().set(settings.getMainClass());
			spec.setArgs(settings.getProgramArgs().get());
			spec.setJvmArgs(settings.getJvmArgs().get());
			spec.setClasspath(settings.getExecClasspath());

			if (settings.getShowVerboseStdout().get()) {
				spec.setStandardOutput(System.out);
			} else {
				spec.setStandardOutput(NullOutputStream.NULL_OUTPUT_STREAM);
			}

			if (settings.getShowVerboseStderr().get()) {
				spec.setErrorOutput(System.err);
			} else {
				spec.setErrorOutput(NullOutputStream.NULL_OUTPUT_STREAM);
			}
		});
	}

	public interface Settings {
		@Input
		Property<String> getExecutable();

		@Input
		ListProperty<String> getProgramArgs();

		@Input
		ListProperty<String> getJvmArgs();

		@Input
		Property<String> getMainClass();

		@Classpath
		ConfigurableFileCollection getExecClasspath();

		@Input
		Property<Boolean> getShowVerboseStdout();

		@Input
		Property<Boolean> getShowVerboseStderr();

		default void classpath(Object... paths) {
			getExecClasspath().from(paths);
		}

		default void setClasspath(Object... paths) {
			getExecClasspath().setFrom(paths);
		}

		default void args(String... args) {
			getProgramArgs().addAll(args);
		}

		default void args(Collection<String> args) {
			getProgramArgs().addAll(args);
		}

		default void setArgs(List<String> args) {
			getProgramArgs().set(args);
		}

		default void jvmArgs(String... args) {
			getJvmArgs().addAll(args);
		}

		default void jvmArgs(Collection<String> args) {
			getJvmArgs().addAll(args);
		}
	}
}
