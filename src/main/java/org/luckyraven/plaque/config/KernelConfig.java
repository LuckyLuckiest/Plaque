package org.luckyraven.plaque.config;

import org.luckyraven.keystone.bean.Bean;
import org.luckyraven.keystone.bean.Configuration;
import org.luckyraven.keystone.bean.Phase;
import org.luckyraven.keystone.diagnostics.Diagnostics;
import org.luckyraven.keystone.diagnostics.LoggingSink;
import org.luckyraven.keystone.diagnostics.RecentFaultsSink;
import org.luckyraven.keystone.persistence.FileHandler;
import org.luckyraven.keystone.persistence.FileManager;
import org.luckyraven.plaque.Plaque;

/**
 * KERNEL-phase configuration that produces every bootstrap-critical singleton Plaque needs before the FILE phase
 * begins — the standalone-plugin twin of Gangland's {@code KernelConfig}, same shape as Bartizan's own
 * {@code KernelConfig} (bartizan.md §2 B10): no {@code ModuleLoader}, no per-module {@code commands.json} merge,
 * no database beans (this plugin keeps no database — §6 of the WS1 plan).
 */
@Configuration(phase = Phase.KERNEL)
public class KernelConfig {

	private final Plaque plaque;

	public KernelConfig(Plaque plaque) {
		this.plaque = plaque;
	}

	/**
	 * The plugin's fault hub (Keystone diagnostics). Command dispatch errors funnel here (keystone-command.md
	 * "Failure handling") so they are classified and kept in a recent-faults ring instead of only logged.
	 */
	@Bean
	public Diagnostics diagnostics() {
		Diagnostics hub = Diagnostics.withDefaults()
		                             .addSink(new LoggingSink())
		                             .addSink(new RecentFaultsSink());
		Diagnostics.install(hub);
		return hub;
	}

	/**
	 * Registers the two YAML files this plugin owns. {@code scoreboard.yml}'s schema is byte-identical to
	 * Gangland's own (WS1 plan §5) — an owner copies the file across verbatim.
	 */
	@Bean
	public FileManager fileManager() {
		FileManager fm = new FileManager(plaque);
		fm.addFile(new FileHandler(plaque, "settings", ".yml"), true);
		fm.addFile(new FileHandler(plaque, "scoreboard", ".yml"), true);

		return fm;
	}

}
