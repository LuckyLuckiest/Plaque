package org.luckyraven.plaque.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.luckyraven.keystone.bean.listener.ListenerHandler;
import org.luckyraven.plaque.board.BoardManager;

/**
 * Was Gangland's {@code PlayerScoreboardListener}. Listens to plain Bukkit {@link PlayerJoinEvent} instead of
 * Gangland's {@code UserDataInitEvent} — this plugin has no user-data bootstrap sequence to hook (WS1 plan §2/§3).
 */
@ListenerHandler
public class PlayerBoardListener implements Listener {

	private final BoardManager boardManager;

	public PlayerBoardListener(BoardManager boardManager) {
		this.boardManager = boardManager;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		boardManager.createBoard(player);
	}

}
