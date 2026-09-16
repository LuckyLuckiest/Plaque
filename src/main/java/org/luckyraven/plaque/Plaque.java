package org.luckyraven.plaque;

import lombok.CustomLog;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plaque: a standalone Keystone-powered scoreboard plugin, split out of Gangland Warfare's
 * {@code gangland-ui/scoreboard-api} module (decoupling wave WS1, 2026-09-16).
 *
 * <p>This is a G0/G1 stub. Bootstrap wiring (a {@code BeanFactory}-driven context, config loading, the
 * PAPI/{@code PlaceholderProvider} placeholder chain and the enable-time board sweep) lands in G2 — see
 * {@code brainstorming/decoupling-wave-2026-09-14/plans/WS1-scoreboard.md} in the Gangland repo.
 */
@CustomLog
public final class Plaque extends JavaPlugin {

	@Override
	public void onEnable() {
		log.info("Plaque {} enabling (G0/G1 stub — bootstrap lands in G2)", getDescription().getVersion());
	}

	@Override
	public void onDisable() {
		log.info("Plaque disabled");
	}

}
