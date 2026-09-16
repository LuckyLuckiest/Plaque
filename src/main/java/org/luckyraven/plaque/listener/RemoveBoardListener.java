package org.luckyraven.plaque.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.luckyraven.keystone.bean.listener.ListenerHandler;
import org.luckyraven.plaque.board.BoardManager;

/**
 * Was the scoreboard branch of Gangland's {@code RemoveAccountListener}/{@code UserManager.onPreClear()}. Ends
 * and forgets a player's board on quit; the remaining half of that upstream cleanup (every board still active at
 * shutdown) is {@link BoardManager#removeAll()}, called from {@code Plaque#onDisable()}.
 */
@ListenerHandler
public class RemoveBoardListener implements Listener {

	private final BoardManager boardManager;

	public RemoveBoardListener(BoardManager boardManager) {
		this.boardManager = boardManager;
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		boardManager.removeBoard(player);
	}

}
