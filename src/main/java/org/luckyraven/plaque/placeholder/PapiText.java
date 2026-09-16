package org.luckyraven.plaque.placeholder;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.luckyraven.keystone.papi.PlaceholderAPIProvider;
import org.luckyraven.keystone.placeholder.CompositePlaceholderProvider;
import org.luckyraven.keystone.placeholder.PlaceholderProvider;

/**
 * The board's placeholder chain (WS1-D5/A5): PlaceholderAPI first, then an optional Keystone
 * {@link PlaceholderProvider} another plugin (e.g. Gangland, via {@code GanglandPlaceholder.asProvider()}) may
 * publish on Bukkit's {@code ServicesManager} — the same seam {@code ItemVocabulary} already uses — then raw
 * text. PAPI presence is checked once, at construction: {@link PlaceholderAPIProvider} touches PAPI's static
 * bridge, which is unsafe to call when the plugin is not installed, mirroring the soft-dependency gate used
 * everywhere else in this studio. The {@code ServicesManager} lookup is resolved fresh on every call, never
 * cached — the publishing plugin may enable after this one, or not be installed at all.
 */
public final class PapiText implements PlaceholderProvider {

	private final PlaceholderProvider chain;

	public PapiText() {
		this(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null);
	}

	/** Test seam — bypasses the live Bukkit plugin-manager lookup. */
	PapiText(boolean papiPresent) {
		PlaceholderProvider papi = papiPresent ? new PlaceholderAPIProvider() : (player, raw) -> raw;

		PlaceholderProvider serviceFallback = (player, raw) -> {
			var registration = Bukkit.getServicesManager().getRegistration(PlaceholderProvider.class);
			return registration == null ? raw : registration.getProvider().resolve(player, raw);
		};

		this.chain = CompositePlaceholderProvider.of(papi, serviceFallback);
	}

	@Override
	public String resolve(OfflinePlayer player, String raw) {
		return chain.resolve(player, raw);
	}

}
