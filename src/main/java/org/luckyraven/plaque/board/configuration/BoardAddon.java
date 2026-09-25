package org.luckyraven.plaque.board.configuration;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.luckyraven.keystone.exception.PluginException;
import org.luckyraven.keystone.persistence.FileHandler;
import org.luckyraven.keystone.persistence.FileInitializer;
import org.luckyraven.keystone.persistence.FileManager;
import org.luckyraven.plaque.board.part.Line;
import org.luckyraven.plaque.board.part.StaticLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BoardAddon implements FileInitializer {

	/** Vanilla clients only render this many sidebar lines; rows beyond it are configured but never shown. */
	private static final int MAX_ROWS = 15;

	private final FileHandler fileHandler;

	private final @Getter List<Line> lines;
	private @Getter       Line       title;

	public BoardAddon(FileManager fileManager) {
		this.lines = new ArrayList<>();

		try {
			fileManager.checkFileLoaded("scoreboard");
			this.fileHandler = Objects.requireNonNull(fileManager.getFile("scoreboard"));
		} catch (IOException exception) {
			throw new PluginException(exception);
		}
	}

	@Override
	public FileHandler getFileHandler() {
		return fileHandler;
	}

	@Override
	public void initialize() {
		FileConfiguration scoreboard = fileHandler.getFileConfiguration();

		// build the new title/rows in locals first; a bad config (e.g. a renamed Board.Title key) throws here,
		// before this.title/this.lines are touched, so a failed reload leaves the previous good board in place
		// instead of half-clearing it (gi=77).
		List<String> titleLines = getLines(scoreboard, "Title");
		long         interval   = scoreboard.getLong("Board.Title.Interval");

		Line newTitle = titleLines.size() == 1 ? new StaticLine() : new Line(interval);
		newTitle.addAllContents(titleLines);

		List<Line> newLines = initializeRows(scoreboard);

		this.title = newTitle;
		this.lines.clear();
		this.lines.addAll(newLines);
	}

	private List<String> getLines(FileConfiguration scoreboard, String section) {
		return Objects.requireNonNull(scoreboard.getConfigurationSection("Board." + section)).getStringList("Lines");
	}

	private List<Line> initializeRows(FileConfiguration scoreboard) {
		List<Line> rows  = new ArrayList<>();
		int        index = 0;

		// gi=79: scan every row up to the vanilla sidebar cap instead of stopping at the first missing one, so a
		// gap (e.g. row 5 missing) doesn't silently drop every row after it.
		for (int row = 1; row <= MAX_ROWS; row++) {
			ConfigurationSection section = scoreboard.getConfigurationSection("Board.Rows." + row);
			if (section == null) continue;

			List<String> lines    = getLines(scoreboard, "Rows." + row);
			long         rowInterval = section.getLong("Interval");

			Line line = rowInterval == 0L ? new StaticLine(index++) : new Line(rowInterval, index++);
			line.addAllContents(lines);

			rows.add(line);
		}

		return rows;
	}

}
