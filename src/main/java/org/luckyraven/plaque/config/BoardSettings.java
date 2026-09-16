package org.luckyraven.plaque.config;

import lombok.CustomLog;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.luckyraven.keystone.exception.PluginException;
import org.luckyraven.keystone.persistence.FileHandler;
import org.luckyraven.keystone.persistence.FileInitializer;
import org.luckyraven.keystone.persistence.FileManager;

import java.io.IOException;
import java.util.Objects;

/**
 * Reads {@code Enable}/{@code Driver} off {@code settings.yml}. Small enough that this repo has no
 * {@code Settings} god-class to hold it (WS1 plan C7) — Bukkit's own typed {@link FileConfiguration} getters
 * replace Gangland's {@code NodeReader} validation layer, which is Gangland application code, not part of
 * Keystone.
 */
@CustomLog
public class BoardSettings implements FileInitializer {

	private final FileHandler fileHandler;

	private @Getter boolean enabled;
	private @Getter String  driver;

	public BoardSettings(FileManager fileManager) {
		try {
			fileManager.checkFileLoaded("settings");
			this.fileHandler = Objects.requireNonNull(fileManager.getFile("settings"));
		} catch (IOException exception) {
			throw new PluginException(exception);
		}
	}

	@Override
	public FileHandler getFileHandler() {
		return fileHandler;
	}

	/**
	 * Runs once per file load/reload — deliberately not in {@code BoardManager.getDriverHandler()}, which is
	 * called once per player (fix round 1, Minor 3): the unrecognised-{@code Driver:} warning below must fire
	 * once per load, not once per board built.
	 */
	@Override
	public void initialize() {
		FileConfiguration settings = fileHandler.getFileConfiguration();

		this.enabled = settings.getBoolean("Enable", true);
		this.driver  = settings.getString("Driver", "Driver_V3");

		if (driver == null || !driver.equalsIgnoreCase("driver_v3")) {
			log.warn("settings.yml Driver: '{}' is not recognised — this plugin ships only Driver_V3 " +
			         "(Driver_V1/Driver_V2 were retired when this plugin split from Gangland Warfare). Boards " +
			         "will use Driver_V3 instead of silently reproducing a deleted driver's behaviour.", driver);
		}
	}

}
