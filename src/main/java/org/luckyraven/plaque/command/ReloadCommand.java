package org.luckyraven.plaque.command;

import lombok.CustomLog;
import org.bukkit.command.CommandSender;
import org.luckyraven.keystone.bean.command.CommandHandler;
import org.luckyraven.keystone.command.Command;
import org.luckyraven.keystone.command.argument.Argument;
import org.luckyraven.keystone.persistence.FileManager;
import org.luckyraven.keystone.util.ChatUtil;
import org.luckyraven.plaque.Plaque;
import org.luckyraven.plaque.board.BoardManager;
import org.luckyraven.plaque.bootstrap.PlaqueContext;

/**
 * {@code /plaque reload} — reloads {@code settings.yml}/{@code scoreboard.yml} through
 * {@link FileManager#initializeAll()}, runs the {@code BeanLifecycle} reload pass, then rebuilds every online
 * player's board ({@link BoardManager#rebuildAll()}, reusing B5's enable-time sweep). Mirrors Bartizan's own
 * {@code ReloadCommand} shape (a keystone-command {@code Command}, WS1-D2/orchestrator ruling W5) — this plugin
 * has exactly one command, so no {@code InformationManager}/{@code commands.json} help-listing layer is built
 * for it (ponytail: that machinery earns its keep once there is more than one command to list, not before).
 */
@CustomLog
@CommandHandler
public final class ReloadCommand extends Command {

	private final PlaqueContext context;

	public ReloadCommand(Plaque plaque, PlaqueContext context) {
		super(plaque, Plaque.FULL_PREFIX, "reload", false, "rl");

		this.context = context;
	}

	@Override
	protected void onExecute(Argument argument, CommandSender commandSender, String[] arguments) {
		commandSender.sendMessage(ChatUtil.color("&bReloading&7 Plaque..."));

		try {
			FileManager fileManager = context.get(FileManager.class);
			if (fileManager != null) {
				fileManager.initializeAll();
			}

			context.reloadBeans();

			BoardManager boardManager = context.get(BoardManager.class);
			if (boardManager != null) {
				boardManager.rebuildAll();
			}

			commandSender.sendMessage(ChatUtil.color("&aReload complete."));
		} catch (Throwable throwable) {
			commandSender.sendMessage(ChatUtil.color("&cThere was a problem reloading Plaque!"));
			log.error(throwable.getMessage(), throwable);
		}
	}

	@Override
	protected void initializeArguments() {
		// No sub-arguments — a bare /plaque reload is the whole surface.
	}

	@Override
	protected void help(CommandSender sender, int page) {
		sender.sendMessage(ChatUtil.color("&b/plaque reload &7- reloads config and rebuilds every online player's board."));
	}

}
