package org.luckyraven.plaque.bootstrap;

import lombok.CustomLog;
import lombok.Getter;
import org.bukkit.command.PluginCommand;
import org.luckyraven.keystone.bean.BeanFactory;
import org.luckyraven.keystone.bean.BeanLifecycle;
import org.luckyraven.keystone.bean.Phase;
import org.luckyraven.keystone.bean.SettingsLookup;
import org.luckyraven.keystone.bean.autowire.DependencyContainer;
import org.luckyraven.keystone.command.CommandManager;
import org.luckyraven.keystone.command.CommandTabCompleter;
import org.luckyraven.keystone.command.brigadier.BrigadierTabRegistrar;
import org.luckyraven.keystone.persistence.FileManager;
import org.luckyraven.plaque.Plaque;

/**
 * Single root for Plaque's wiring — the standalone-plugin twin of Gangland's {@code GanglandContext} with every
 * module concern removed (same shape as Bartizan's {@code BartizanContext}): no {@code ModuleLoader}, no module
 * scans, no {@code hostApi}, no repository-republish hook (this plugin keeps no database). Owns the only
 * {@link DependencyContainer} and {@link BeanFactory} that exist at runtime and runs the post-bootstrap listener
 * and command scans.
 *
 * <p>The bootstrap pipeline: {@code KernelConfig} (KERNEL) produces every bootstrap-critical singleton,
 * {@link #bootstrap()} scans {@code org.luckyraven.plaque.config} for {@code @Configuration} classes and drives
 * {@link BeanFactory#instantiate()} through KERNEL → FILE → CONFIG → LISTENER → COMMAND (Keystone's DATABASE
 * phase runs empty), then runs the listener and command scans. Unlike WS1's original plan draft, the COMMAND
 * phase is real here — {@code /plaque reload} is a keystone-command {@code Command}, not a plain
 * {@code CommandExecutor} (WS1-D2, user decision, orchestrator ruling W5).
 */
@CustomLog
public final class PlaqueContext {

	private static final String CONFIG_PACKAGE   = "org.luckyraven.plaque.config";
	private static final String LISTENER_PACKAGE = "org.luckyraven.plaque";
	private static final String COMMAND_PACKAGE  = "org.luckyraven.plaque.command";

	@Getter
	private final DependencyContainer container;
	@Getter
	private final BeanFactory         beanFactory;

	private final Plaque plaque;

	public PlaqueContext(Plaque plaque) {
		this.plaque = plaque;

		// Plaque ships no @ConditionalOnSetting-gated beans/listeners today, so a trivial always-false lookup is
		// enough (same minimal shape Bartizan/Oriel use) rather than a dedicated impl class.
		SettingsLookup settings = key -> false;

		this.container   = new DependencyContainer();
		this.beanFactory = new BeanFactory(container, plaque, settings);

		container.registerInstance(PlaqueContext.class, this);
		container.registerInstance(DependencyContainer.class, container);
		container.registerInstance(org.bukkit.plugin.java.JavaPlugin.class, plaque);
		container.registerInstance(Plaque.class, plaque);
		container.registerInstance(SettingsLookup.class, settings);
		container.registerInstance(BeanFactory.class, beanFactory);
	}

	/**
	 * Convenience accessor for code that needs a bean by raw type.
	 */
	public <T> T get(Class<T> type) {
		return container.getInstance(type);
	}

	/**
	 * Runs the reload lifecycle on all beans implementing {@link BeanLifecycle}.
	 */
	public void reloadBeans() {
		beanFactory.reloadLifecycleBeans();
	}

	/**
	 * Runs graceful shutdown on all beans implementing {@link BeanLifecycle} in reverse topological order.
	 */
	public void shutdownBeans() {
		beanFactory.shutdownLifecycleBeans();
	}

	/**
	 * Drive the phased bean instantiation, then run the listener and command scans. Must be called exactly once.
	 */
	public void bootstrap() {
		// FILE phase: after each file-initializer bean is registered, run FileManager.initializeAll() so the file
		// is loaded before the next FILE-phase bean's @Bean method runs. FileManager is produced by KernelConfig
		// in the KERNEL phase, so it is guaranteed to be in the container.
		beanFactory.setPhaseHook(Phase.FILE, beans -> {
			FileManager fm = container.getInstance(FileManager.class);
			if (fm != null) {
				fm.initializeAll();
			}
		});

		beanFactory.scan(CONFIG_PACKAGE);
		beanFactory.instantiate();

		runListenerPhase();
		runCommandPhase();
	}

	private void runListenerPhase() {
		DefaultListenerService listenerService = container.getInstance(DefaultListenerService.class);
		if (listenerService == null) {
			throw new IllegalStateException(
					this.getClass().getSimpleName() + ".bootstrap(): " + DefaultListenerService.class.getSimpleName() +
					" bean missing. Add a @Bean method that produces " + DefaultListenerService.class.getSimpleName() +
					" to a CONFIG-phase @Configuration class.");
		}
		listenerService.scanAndRegisterListeners(LISTENER_PACKAGE, plaque);
		listenerService.registerEvents();
		log.debug("Listener phase complete: {} listener(s) registered", listenerService.getListeners().size());
	}

	private void runCommandPhase() {
		CommandManager commandManager = container.getInstance(CommandManager.class);
		if (commandManager == null) {
			throw new IllegalStateException(
					this.getClass().getSimpleName() + ".bootstrap(): " + CommandManager.class.getSimpleName() +
					" bean missing. Add a @Bean method that produces " + CommandManager.class.getSimpleName() +
					" to a CONFIG-phase @Configuration class.");
		}
		PluginCommand command = plaque.getCommand(Plaque.FULL_PREFIX);
		if (command == null) {
			log.warn("Plugin command /{} not declared in plugin.yml — skipping command bind", Plaque.FULL_PREFIX);
			return;
		}
		command.setExecutor(commandManager);
		commandManager.scanAndRegisterCommands(COMMAND_PACKAGE, plaque.getClass().getClassLoader());

		CommandTabCompleter tabCompleter = new CommandTabCompleter(commandManager);
		command.setTabCompleter(tabCompleter);

		// Client-side Brigadier completion — safe on plain Spigot; on any failure it logs WARN and the
		// server-side tab completer above stays the only path.
		BrigadierTabRegistrar.registerIfSupported(plaque, command, commandManager);

		log.debug("Command phase complete: {} command(s) registered", commandManager.commandView().size());
	}

}
