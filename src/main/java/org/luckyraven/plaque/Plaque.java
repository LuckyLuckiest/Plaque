package org.luckyraven.plaque;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import lombok.CustomLog;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.luckyraven.plaque.board.BoardManager;
import org.luckyraven.plaque.bootstrap.PlaqueContext;

/**
 * Plaque: a standalone Keystone-powered scoreboard plugin, split out of Gangland Warfare's
 * {@code gangland-ui/scoreboard-api} module (decoupling wave WS1, 2026-09-16). See
 * {@code brainstorming/decoupling-wave-2026-09-14/plans/WS1-scoreboard.md} in the Gangland repo for the full
 * plan.
 */
@Getter
@CustomLog
public final class Plaque extends JavaPlugin {

	public static final String FULL_PREFIX = "plaque";

	private PlaqueContext context;

	/**
	 * Resolved once, after bootstrap, by {@link #dependencyHandler()}; stays {@code null} when ViaVersion is
	 * absent. Deliberately kept here rather than on {@code BoardManager} (fix round 1, Important 1 — see
	 * {@code exec/PLAQUE/G2-review.md}): {@code Plaque} is {@code registerInstance}d directly into the
	 * container, never scanned reflectively, so a soft-dependency type in one of its method descriptors is safe.
	 * A registered <em>bean</em> (like {@code BoardManager}) is not — {@code BeanFactory.runPostConstruct} calls
	 * {@code getDeclaredMethods()} on every bean, which throws {@code NoClassDefFoundError} for the whole class
	 * the moment any method signature names a type that is not on the classpath (ViaVersion absent), silently
	 * skipping that bean's post-construct wiring via {@code ReflectionGuard.orSkip}.
	 */
	private ViaAPI<?> viaAPI;

	@Override
	public void onEnable() {
		try {
			this.context = new PlaqueContext(this);
			context.bootstrap();

			dependencyHandler();

			// B5: cover a /reload, a /plugman enable, or any enable with players already on — without this, a
			// join/quit-listener-only port leaves already-online players boardless until their next join.
			context.get(BoardManager.class).sweepOnlinePlayers();
		} catch (Throwable t) {
			log.error("Plaque failed to enable", t);
			getServer().getPluginManager().disablePlugin(this);
		}
	}

	@Override
	public void onDisable() {
		if (context != null) {
			BoardManager boardManager = context.get(BoardManager.class);
			if (boardManager != null) {
				boardManager.removeAll();
			}

			try {
				context.shutdownBeans();
			} catch (Throwable t) {
				log.error("Plaque bean shutdown failed; clearing the container anyway", t);
			} finally {
				context.getContainer().clear();
			}
		}

		log.info("Plaque disabled");
	}

	/**
	 * Checks soft dependencies (WS1-D3): ViaVersion (max-line-length awareness inside
	 * {@code DriverHandler.FastBoardImpl}) and PlaceholderAPI (dynamic placeholders — see {@code PapiText}).
	 * Neither is required to boot; PlaceholderAPI's absence gets a loud boot warning because a board without it
	 * silently renders literal {@code %gangland_*%} tokens instead of gang/user data (WS1 plan Risk 1/D3).
	 */
	private void dependencyHandler() {
		if (Bukkit.getPluginManager().getPlugin("ViaVersion") != null) {
			log.info("Found ViaVersion, linking...");
			this.viaAPI = Via.getAPI();
			log.info("Linked ViaVersion");
		}

		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			log.info("Found PlaceholderAPI, linking...");
		} else {
			log.warn("PlaceholderAPI not found: dynamic %gangland_*%-style placeholders will render literally " +
			         "unless another plugin publishes a Keystone PlaceholderProvider on the ServicesManager. " +
			         "Static board lines still work fine without it.");
		}
	}

}
