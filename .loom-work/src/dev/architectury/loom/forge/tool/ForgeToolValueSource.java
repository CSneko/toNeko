package dev.architectury.loom.forge.tool;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.gradle.api.tasks.Nested;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;

public abstract class ForgeToolValueSource implements ValueSource<ExecResult, ForgeToolValueSource.Parameters> {
	@Inject
	protected abstract ExecOperations getExecOperations();

	public static Provider<ExecResult> create(Project project, Action<? super ForgeToolExecutor.Settings> configurator) {
		return project.getProviders().of(ForgeToolValueSource.class, spec -> {
			ForgeToolExecutor.Settings settings = ForgeToolExecutor.getDefaultSettings(project);
			configurator.execute(settings);
			spec.getParameters().getSettings().set(settings);
		});
	}

	/**
	 * Executes an external Java process during project configuration.
	 *
	 * @param project      the project
	 * @param configurator an action that configures the exec settings
	 */
	public static void exec(Project project, Action<? super ForgeToolExecutor.Settings> configurator) {
		create(project, configurator).get().rethrowFailure().assertNormalExitValue();
	}

	@Override
	public ExecResult obtain() {
		return ForgeToolExecutor.exec(getExecOperations(), getParameters().getSettings().get());
	}

	public interface Parameters extends ValueSourceParameters {
		@Nested
		Property<ForgeToolExecutor.Settings> getSettings();
	}
}
