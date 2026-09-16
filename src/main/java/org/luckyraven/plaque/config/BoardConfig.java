package org.luckyraven.plaque.config;

import org.luckyraven.keystone.bean.Bean;
import org.luckyraven.keystone.bean.Configuration;
import org.luckyraven.keystone.bean.SettingsLookup;
import org.luckyraven.keystone.bean.autowire.DependencyContainer;
import org.luckyraven.keystone.command.CommandManager;
import org.luckyraven.keystone.placeholder.PlaceholderProvider;
import org.luckyraven.plaque.Plaque;
import org.luckyraven.plaque.board.BoardManager;
import org.luckyraven.plaque.board.configuration.BoardAddon;
import org.luckyraven.plaque.bootstrap.DefaultListenerService;
import org.luckyraven.plaque.placeholder.PapiText;

/**
 * CONFIG-phase wiring — the seams Plaque itself needs a bean for: the placeholder chain, the board factory/
 * registry, and the {@code ListenerService}/{@code CommandManager} instances the LISTENER/COMMAND phase scans
 * consume. Same shape as Bartizan's own {@code WiringConfig}.
 */
@Configuration
public final class BoardConfig {

	private final Plaque plaque;

	public BoardConfig(Plaque plaque) {
		this.plaque = plaque;
	}

	@Bean
	public PlaceholderProvider placeholderProvider() {
		return new PapiText();
	}

	@Bean
	public BoardManager boardManager(PlaceholderProvider placeholderProvider, BoardAddon boardAddon,
	                                 BoardSettings settings) {
		return new BoardManager(plaque, placeholderProvider, boardAddon, settings);
	}

	@Bean
	public DefaultListenerService listenerService(DependencyContainer container, SettingsLookup settings) {
		return new DefaultListenerService(plaque, container, settings);
	}

	@Bean
	public CommandManager commandManager(DependencyContainer container, SettingsLookup settings) {
		return new CommandManager(plaque, container, settings, Plaque.FULL_PREFIX, Plaque.FULL_PREFIX);
	}

}
