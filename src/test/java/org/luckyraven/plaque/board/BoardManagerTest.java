package org.luckyraven.plaque.board;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.luckyraven.keystone.testkit.BukkitStatics;
import org.luckyraven.plaque.Plaque;
import org.luckyraven.plaque.board.configuration.BoardAddon;
import org.luckyraven.plaque.config.BoardSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins B5, the enable-time/reload board sweep, and (fix round 1, Minor 4) {@code rebuildAll()}'s
 * removeAll-then-sweep shape.
 *
 * <p>{@code createBoard}/{@code removeBoard} are overridden to record calls instead of building a real
 * {@code FastBoard} (which needs a live NMS server) — {@link BoardManager#sweepOnlinePlayers()} itself runs the
 * real production code, which is what the first test pins. {@link BoardAddon}/{@link BoardSettings}/
 * {@link Plaque} are Mockito mocks since none is touched once {@code createBoard} is overridden away.
 */
@DisplayName("BoardManager - the enable-time/reload sweep must reach every already-online player (B5)")
class BoardManagerTest {

	@Test
	@DisplayName("B5: sweepOnlinePlayers() builds a board for every already-online player")
	void sweepOnlinePlayers_reachesEveryOnlinePlayer() {
		try (BukkitStatics bukkit = BukkitStatics.install()) {
			Player alice = onlinePlayer();
			Player bob   = onlinePlayer();
			bukkit.statics().when(Bukkit::getOnlinePlayers).thenReturn(List.of(alice, bob));

			CountingBoardManager manager = countingManager();

			manager.sweepOnlinePlayers();

			assertEquals(List.of(alice, bob), manager.createBoardCalls);
		}
	}

	@Test
	@DisplayName("fix round 1, Minor 4: rebuildAll() calls removeAll() then sweepOnlinePlayers(), in that order")
	void rebuildAll_callsRemoveAllThenSweepOnlinePlayers() {
		List<String> callOrder = new ArrayList<>();

		BoardSettings settings = mock(BoardSettings.class); // isEnabled() defaults to false — sweep short-circuits

		BoardManager manager = new BoardManager(mock(Plaque.class), (player, raw) -> raw, mock(BoardAddon.class),
		                                        settings) {
			@Override
			public void removeAll() {
				callOrder.add("removeAll");
				super.removeAll();
			}

			@Override
			public void sweepOnlinePlayers() {
				callOrder.add("sweepOnlinePlayers");
				super.sweepOnlinePlayers();
			}
		};

		manager.rebuildAll();

		assertEquals(List.of("removeAll", "sweepOnlinePlayers"), callOrder);
	}

	private static CountingBoardManager countingManager() {
		BoardAddon    boardAddon = mock(BoardAddon.class);
		BoardSettings settings   = mock(BoardSettings.class);
		when(settings.isEnabled()).thenReturn(true);

		return new CountingBoardManager(mock(Plaque.class), boardAddon, settings);
	}

	private static Player onlinePlayer() {
		Player player = mock(Player.class);
		when(player.getUniqueId()).thenReturn(UUID.randomUUID());
		return player;
	}

	private static final class CountingBoardManager extends BoardManager {

		private final List<Player> createBoardCalls = new ArrayList<>();

		CountingBoardManager(Plaque plaque, BoardAddon boardAddon, BoardSettings settings) {
			super(plaque, (player, raw) -> raw, boardAddon, settings);
		}

		@Override
		public void createBoard(Player player) {
			createBoardCalls.add(player);
		}

	}

}
