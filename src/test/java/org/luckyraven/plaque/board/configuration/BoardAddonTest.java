package org.luckyraven.plaque.board.configuration;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.luckyraven.keystone.persistence.FileHandler;
import org.luckyraven.keystone.persistence.FileManager;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins two {@link BoardAddon#initialize()} defects surfaced by the testserver wave 1 sweep.
 */
@DisplayName("BoardAddon - initialize() must not half-clear state on a bad reload, and row parsing must not stop at a gap")
class BoardAddonTest {

	@Test
	@DisplayName("gi=77: a reload with a missing Board.Title section leaves the previous good title/lines in place")
	void initialize_badTitleSection_keepsPreviousGoodState() throws IOException {
		FileManager manager = mock(FileManager.class);
		FileHandler handler = mock(FileHandler.class);
		when(manager.getFile("scoreboard")).thenReturn(handler);

		when(handler.getFileConfiguration()).thenReturn(goodConfig());
		BoardAddon addon = new BoardAddon(manager);
		addon.initialize();

		assertNotNull(addon.getTitle());
		assertEquals(1, addon.getLines().size());

		when(handler.getFileConfiguration()).thenReturn(badTitleConfig());
		assertThrows(NullPointerException.class, addon::initialize);

		assertNotNull(addon.getTitle(), "a failed reload must not null out the previously good title");
		assertEquals(1, addon.getLines().size(), "a failed reload must not clear the previously good lines");
	}

	@Test
	@DisplayName("gi=79: row parsing continues past a gap instead of stopping at the first missing row")
	void initializeRows_skipsGapInsteadOfStopping() throws IOException {
		FileManager manager = mock(FileManager.class);
		FileHandler handler = mock(FileHandler.class);
		when(manager.getFile("scoreboard")).thenReturn(handler);
		when(handler.getFileConfiguration()).thenReturn(rowsConfig(1, 2, 3, 4, 6));

		BoardAddon addon = new BoardAddon(manager);
		addon.initialize();

		assertEquals(5, addon.getLines().size(), "row 6 must not be dropped just because row 5 is missing");
	}

	@Test
	@DisplayName("gi=79: row parsing caps at 15 rows (the vanilla sidebar limit) instead of sending them all")
	void initializeRows_capsAtVanillaSidebarLimit() throws IOException {
		FileManager manager = mock(FileManager.class);
		FileHandler handler = mock(FileHandler.class);
		when(manager.getFile("scoreboard")).thenReturn(handler);
		int[] rows = new int[17];
		for (int i = 0; i < 17; i++) rows[i] = i + 1;
		when(handler.getFileConfiguration()).thenReturn(rowsConfig(rows));

		BoardAddon addon = new BoardAddon(manager);
		addon.initialize();

		assertEquals(15, addon.getLines().size(), "vanilla clients only render 15 sidebar lines");
	}

	private static FileConfiguration goodConfig() {
		YamlConfiguration config = new YamlConfiguration();
		config.set("Board.Title.Lines", List.of("&6Title"));
		config.set("Board.Title.Interval", 0);
		config.set("Board.Rows.1.Lines", List.of("Row 1"));
		config.set("Board.Rows.1.Interval", 0);
		return config;
	}

	private static FileConfiguration badTitleConfig() {
		YamlConfiguration config = new YamlConfiguration();
		// 'Board.Title' renamed -> missing, matching the repro (e.g. to 'Titel')
		config.set("Board.Titel.Lines", List.of("&6Title"));
		config.set("Board.Rows.1.Lines", List.of("Row 1"));
		config.set("Board.Rows.1.Interval", 0);
		return config;
	}

	private static FileConfiguration rowsConfig(int... rowNumbers) {
		YamlConfiguration config = new YamlConfiguration();
		config.set("Board.Title.Lines", List.of("&6Title"));
		config.set("Board.Title.Interval", 0);
		for (int row : rowNumbers) {
			config.set("Board.Rows." + row + ".Lines", List.of("Row " + row));
			config.set("Board.Rows." + row + ".Interval", 0);
		}
		return config;
	}

}
