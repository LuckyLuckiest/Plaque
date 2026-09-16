package org.luckyraven.plaque.config;

import org.luckyraven.keystone.bean.Bean;
import org.luckyraven.keystone.bean.Configuration;
import org.luckyraven.keystone.bean.Phase;
import org.luckyraven.keystone.persistence.FileManager;
import org.luckyraven.plaque.board.configuration.BoardAddon;

/**
 * FILE-phase configuration: registers this plugin's two {@code FileInitializer}s with the {@link FileManager}
 * bean {@code KernelConfig} produced. {@code PlaqueContext}'s FILE-phase hook calls
 * {@code FileManager#initializeAll()} right after this phase finishes, so both beans' typed fields are populated
 * before any CONFIG-phase bean (e.g. {@code BoardManager}) is constructed — same ordering Gangland's own
 * {@code FileConfig} relies on.
 */
@Configuration(phase = Phase.FILE)
public class FileConfig {

	@Bean
	public BoardSettings boardSettings(FileManager fileManager) {
		BoardSettings settings = new BoardSettings(fileManager);
		fileManager.registerInitializer(settings);
		return settings;
	}

	@Bean
	public BoardAddon boardAddon(FileManager fileManager) {
		BoardAddon addon = new BoardAddon(fileManager);
		fileManager.registerInitializer(addon);
		return addon;
	}

}
