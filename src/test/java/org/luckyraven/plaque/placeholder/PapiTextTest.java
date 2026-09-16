package org.luckyraven.plaque.placeholder;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.luckyraven.keystone.placeholder.PlaceholderProvider;
import org.luckyraven.keystone.testkit.BukkitStatics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the board's placeholder chain (WS1-D5/A5): PlaceholderAPI first, then an optional Keystone
 * {@link PlaceholderProvider} another plugin (e.g. Gangland) may publish on the {@code ServicesManager}, then raw
 * text. All three fallbacks are exercised here.
 *
 * <p>The "PAPI present" case mirrors {@code PlaceholderAPIProviderTest} in keystone-hooks: percent-free text
 * short-circuits before {@link org.luckyraven.keystone.papi.PlaceholderAPIProvider} ever touches PlaceholderAPI's
 * static bridge, so this stays safe without a live PAPI plugin on the test classpath.
 */
@DisplayName("PapiText - PAPI, then ServicesManager PlaceholderProvider, then raw text (WS1-D5)")
class PapiTextTest {

	private static final OfflinePlayer PLAYER = mock(OfflinePlayer.class);

	@Test
	@DisplayName("PlaceholderAPI absent, no ServicesManager registration: raw text passes through unchanged")
	void papiAbsent_noFallback_returnsRawText() {
		try (BukkitStatics bukkit = BukkitStatics.install()) {
			stubPapiPresence(bukkit.pluginManager(), false);
			stubServiceRegistration(bukkit.servicesManager(), null);

			PapiText papiText = new PapiText();

			assertEquals("%gangland_user_balance%", papiText.resolve(PLAYER, "%gangland_user_balance%"));
		}
	}

	@Test
	@DisplayName("PlaceholderAPI present, percent-free text: delegates to PlaceholderAPIProvider's short-circuit")
	void papiPresent_percentFreeText_returnsUnchanged() {
		try (BukkitStatics bukkit = BukkitStatics.install()) {
			stubPapiPresence(bukkit.pluginManager(), true);
			stubServiceRegistration(bukkit.servicesManager(), null);

			PapiText papiText = new PapiText();

			assertEquals("plain text, no tokens", papiText.resolve(PLAYER, "plain text, no tokens"));
		}
	}

	@Test
	@DisplayName("D5: PlaceholderAPI absent, a ServicesManager PlaceholderProvider is registered: fallback fires")
	void papiAbsent_serviceFallbackRegistered_fallbackResolves() {
		try (BukkitStatics bukkit = BukkitStatics.install()) {
			stubPapiPresence(bukkit.pluginManager(), false);

			PlaceholderProvider fallback = (player, raw) -> raw.replace("%gangland_user_balance%", "500");
			stubServiceRegistration(bukkit.servicesManager(), fallback);

			PapiText papiText = new PapiText();

			assertEquals("Purse: 500", papiText.resolve(PLAYER, "Purse: %gangland_user_balance%"));
		}
	}

	private static void stubPapiPresence(PluginManager pluginManager, boolean present) {
		when(pluginManager.getPlugin(eq("PlaceholderAPI"))).thenReturn(present ? mock(Plugin.class) : null);
	}

	@SuppressWarnings("unchecked")
	private static void stubServiceRegistration(ServicesManager servicesManager, PlaceholderProvider provider) {
		if (provider == null) {
			when(servicesManager.getRegistration(PlaceholderProvider.class)).thenReturn(null);
			return;
		}

		RegisteredServiceProvider<PlaceholderProvider> registration = mock(RegisteredServiceProvider.class);
		when(registration.getProvider()).thenReturn(provider);
		when(servicesManager.getRegistration(PlaceholderProvider.class)).thenReturn(registration);
	}

}
