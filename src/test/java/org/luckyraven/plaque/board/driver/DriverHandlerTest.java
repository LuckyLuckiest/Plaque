package org.luckyraven.plaque.board.driver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.luckyraven.keystone.placeholder.PlaceholderProvider;
import org.luckyraven.plaque.board.part.Line;
import org.luckyraven.plaque.board.part.StaticLine;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * gi=78: pins the one-time initial board build against the regression where the deleted
 * {@code shouldUpdateLine()} gate excluded static/{@code Interval:0} lines from ever being resolved, leaving a
 * static title blank and those rows showing their raw, unresolved placeholder text forever.
 *
 * <p>{@link DriverHandler} itself can't be constructed here — its constructor builds a real {@code FastBoard},
 * whose static initializer reflects into {@code CraftChatMessage} and throws {@code ExceptionInInitializerError}
 * off a server-less test JVM (empirically verified in the gi=78 commit) — so this exercises
 * {@link DriverHandler#resolveAll(PlaceholderProvider, org.bukkit.entity.Player, List)}, the static, side-effect-free
 * step {@code updateBoard()} now routes every line through unconditionally. Placeholder seam is a one-line lambda
 * (documentation/TESTING.md §6), as in {@code LineTest}, so nothing here needs a Bukkit server either.
 */
@DisplayName("DriverHandler.resolveAll() - the one-time initial build resolves every line (gi=78)")
class DriverHandlerTest {

	private static final PlaceholderProvider UPPERCASE = (player, text) -> text.toUpperCase();

	@Test
	@DisplayName("gi=78: static and Interval:0 lines are placeholder-resolved, not left raw or blank")
	void resolveAll_resolvesStaticAndIntervalZeroLines() {
		StaticLine staticLine = new StaticLine();
		staticLine.addContent("static-text");

		Line intervalZero = new Line(0L, 1);
		intervalZero.addContent("zero-text");

		Line periodic = new Line(20L, 2);
		periodic.addContent("periodic-text");

		List<Line> lines = List.of(staticLine, intervalZero, periodic);

		Map<Line, String> resolved = DriverHandler.resolveAll(UPPERCASE, null, lines);

		assertEquals("STATIC-TEXT", resolved.get(staticLine), "a static line must be resolved, not raw");
		assertEquals("ZERO-TEXT", resolved.get(intervalZero), "an Interval:0 line must be resolved, not raw");
		assertEquals("PERIODIC-TEXT", resolved.get(periodic));
	}

}
