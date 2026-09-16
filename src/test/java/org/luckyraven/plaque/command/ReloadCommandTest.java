package org.luckyraven.plaque.command;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.luckyraven.keystone.persistence.FileManager;
import org.luckyraven.keystone.testkit.BukkitStatics;
import org.luckyraven.plaque.Plaque;
import org.luckyraven.plaque.board.BoardManager;
import org.luckyraven.plaque.bootstrap.PlaqueContext;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins {@code ReloadCommand.onExecute}'s sequencing (fix round 1, Important 2 — {@code exec/PLAQUE/G2-review.md}):
 * config must reload, then the {@code BeanLifecycle} pass, then boards rebuild — in that order, never any other.
 * Rebuilding boards before the file/bean reload would render them from stale config; reloading beans before the
 * files land would replay the old {@code scoreboard.yml}/{@code settings.yml} state.
 *
 * <p>{@code PlaqueContext} is {@code final}, mocked via Mockito's inline mock maker (the default mock maker on
 * Mockito 5.x — no separate {@code mockito-inline} artifact needed). {@code onExecute} is {@code protected}, so
 * this test lives in the same package to call it directly rather than going through Bukkit's command dispatcher.
 */
@DisplayName("ReloadCommand - /plaque reload sequences initializeAll() -> reloadBeans() -> rebuildAll()")
class ReloadCommandTest {

	@Test
	@DisplayName("onExecute() reloads files, then the bean lifecycle, then rebuilds every board, in that order")
	void onExecute_reloadsInOrder() {
		try (BukkitStatics bukkit = BukkitStatics.install()) {
			Plaque        plaque       = mock(Plaque.class);
			PlaqueContext context      = mock(PlaqueContext.class);
			FileManager   fileManager  = mock(FileManager.class);
			BoardManager  boardManager = mock(BoardManager.class);

			when(context.get(FileManager.class)).thenReturn(fileManager);
			when(context.get(BoardManager.class)).thenReturn(boardManager);

			ReloadCommand command = new ReloadCommand(plaque, context);
			CommandSender sender  = mock(CommandSender.class);

			command.onExecute(null, sender, new String[0]);

			InOrder order = inOrder(fileManager, context, boardManager);
			order.verify(fileManager).initializeAll();
			order.verify(context).reloadBeans();
			order.verify(boardManager).rebuildAll();
		}
	}

}
