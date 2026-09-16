package org.luckyraven.plaque.board;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.luckyraven.keystone.placeholder.PlaceholderProvider;
import org.luckyraven.plaque.Plaque;
import org.luckyraven.plaque.board.configuration.BoardAddon;
import org.luckyraven.plaque.board.driver.DriverHandler;
import org.luckyraven.plaque.board.driver.version.DriverV3;
import org.luckyraven.plaque.board.part.Line;
import org.luckyraven.plaque.config.BoardSettings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Was Gangland's {@code ScoreboardManager} (gangland-impl). Two differences from the upstream class: (1) this
 * plugin has no User/UserManager to hold a per-player board reference, so {@code BoardManager} owns that
 * registry itself; (2) under WS1 decision D1(b) only {@link DriverV3} exists — the reflection-based
 * {@code getDrivers()} scan and the {@code DriverV1}/{@code DriverV2} switch branches are gone, replaced with a
 * hardcoded {@code DriverV3} (the unrecognised-{@code Driver:} warning lives in {@link BoardSettings#initialize()}
 * — fix round 1, Minor 3 — not here, so it fires once per load/reload instead of once per board built).
 *
 * <p>Takes {@link Plaque} (not a bare {@code JavaPlugin}) specifically so {@link #getDriverHandler(Player)} can
 * read {@code plaque.getViaAPI()} lazily, every call, instead of holding a {@code ViaAPI}-typed field itself —
 * fix round 1, Important 1: see {@link Plaque#getViaAPI()}'s javadoc for why a soft-dependency type may not
 * appear in a bean's own method descriptor.
 */
public class BoardManager {

	private final Plaque              plaque;
	private final PlaceholderProvider placeholder;
	private final BoardAddon          boardAddon;
	private final BoardSettings       settings;

	private final Map<UUID, Board> boards = new HashMap<>();

	public BoardManager(Plaque plaque, PlaceholderProvider placeholder, BoardAddon boardAddon,
	                    BoardSettings settings) {
		this.plaque      = plaque;
		this.placeholder = placeholder;
		this.boardAddon  = boardAddon;
		this.settings    = settings;
	}

	public DriverHandler getDriverHandler(Player player) {
		// UI-01: every board gets its own deep copy of the configured lines (Line#update advances a rotation
		// cursor; sharing the configured instances made animated content cycle once per online player per tick).
		List<Line> lines = Line.copyAll(boardAddon.getLines());
		Line       title = boardAddon.getTitle().copy();

		return new DriverV3(placeholder, plaque.getViaAPI(), player, title, lines);
	}

	/** Builds and starts a board for one player, if the feature is enabled and they don't already have one. */
	public void createBoard(Player player) {
		if (!settings.isEnabled()) return;
		if (boards.containsKey(player.getUniqueId())) return;

		Board board = new Board(plaque, getDriverHandler(player));
		boards.put(player.getUniqueId(), board);
		board.start();
	}

	/** Ends and forgets one player's board, if they have one. */
	public void removeBoard(Player player) {
		Board board = boards.remove(player.getUniqueId());
		if (board != null) board.end();
	}

	/**
	 * B5: builds and starts a board for every already-online player that doesn't have one yet. Called at the end
	 * of {@code Plaque#onEnable()} and again by {@code /plaque reload} — covers a {@code /reload}, a
	 * {@code /plugman enable}, or any enable with players already on, which a join/quit-listener-only port would
	 * silently miss (WS1 plan risk table, row 6).
	 */
	public void sweepOnlinePlayers() {
		if (!settings.isEnabled()) return;

		Bukkit.getOnlinePlayers().forEach(this::createBoard);
	}

	/**
	 * Kills every tracked board and rebuilds it from the current configuration. Used by {@code /plaque reload}.
	 * Exhaustive over the board map (fix round 1, Minor 4) rather than iterating
	 * {@code Bukkit.getOnlinePlayers()} — a player who went offline between joining and the reload still gets
	 * cleaned up, not just re-swept.
	 */
	public void rebuildAll() {
		removeAll();
		sweepOnlinePlayers();
	}

	/** Kills every active board without rebuilding. Used by {@code Plaque#onDisable()}. */
	public void removeAll() {
		boards.values().forEach(Board::end);
		boards.clear();
	}

}
