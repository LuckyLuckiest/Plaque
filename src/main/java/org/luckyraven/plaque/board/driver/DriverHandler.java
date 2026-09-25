package org.luckyraven.plaque.board.driver;

import com.viaversion.viaversion.api.ViaAPI;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import fr.mrmicky.fastboard.FastBoard;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.luckyraven.keystone.placeholder.PlaceholderProvider;
import org.luckyraven.plaque.board.part.Line;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public abstract class DriverHandler {

	private final PlaceholderProvider placeholder;
	private final FastBoard           fastBoard;
	private final List<Line>          lines;
	private final Line                title;
	private final Map<Line, Long>     lineUpdateCounts;

	private long globalTickCount;

	public DriverHandler(PlaceholderProvider placeholder, ViaAPI<?> viaAPI, Player player, Line title,
	                     List<Line> lines) {
		this.placeholder      = placeholder;
		this.fastBoard        = new FastBoardImpl(player, viaAPI);
		this.title            = title;
		// UI-01: own the list. The caller used to hand in the live configuration list, and appending the title to
		// it added one more entry per board created — once per join and per reload, without bound.
		this.lines            = new ArrayList<>(lines);
		this.lineUpdateCounts = new HashMap<>();
		this.globalTickCount  = 0L;

		this.lines.add(title);

		// initialize line update counts
		this.lines.forEach(line -> lineUpdateCounts.put(line, 0L));

		// need to update all the values so when initialized, the user doesn't see the placeholders
		updateBoard();
	}

	public abstract void update();

	@Override
	public String toString() {
		return String.format("DriverHandler{title=%s,lines=%s}", fastBoard, lines);
	}

	public long getLineTickCount(Line line) {
		return lineUpdateCounts.getOrDefault(line, 0L);
	}

	protected String updateLine(Line line) {
		return line.update(placeholder, fastBoard.getPlayer());
	}

	protected void incrementTick() {
		globalTickCount++;
	}

	private void updateBoard() {
		// gi=78: this is the one-time initial resolve (called only from the constructor, see its comment), so every
		// line must be resolved here regardless of interval/static-ness — the old shouldUpdateLine() gate that used
		// to guard this call also excluded static/Interval:0 lines from ever running, leaving titles blank and
		// those rows showing their raw, unresolved placeholder text forever. resolveAll() is the unconditional,
		// side-effect-free step that used to be gated; it is extracted (and static) so a future regression is
		// pinned by DriverHandlerTest without needing a live FastBoard/NMS.
		lines.forEach(line -> lineUpdateCounts.merge(line, 1L, Long::sum));

		Map<Line, String> resolved = resolveAll(placeholder, fastBoard.getPlayer(), lines);

		fastBoard.updateTitle(resolved.get(title));

		List<String> updateLines = lines.stream().filter(line -> line != title).map(resolved::get).toList();

		fastBoard.updateLines(updateLines);
	}

	/**
	 * gi=78: resolves every line's content (title included), unconditionally — regardless of interval/static-ness.
	 * Static and package-private so it's directly unit-testable without constructing a live FastBoard: FastBoard's
	 * static initializer reflects into {@code CraftChatMessage} and throws off a server-less test JVM.
	 */
	static Map<Line, String> resolveAll(PlaceholderProvider placeholder, Player player, List<Line> lines) {
		Map<Line, String> resolved = new HashMap<>();

		for (Line line : lines) {
			resolved.put(line, line.update(placeholder, player));
		}

		return resolved;
	}

	private static class FastBoardImpl extends FastBoard {

		private final ViaAPI<?> viaAPI;

		public FastBoardImpl(Player player, ViaAPI<?> viaAPI) {
			super(player);

			this.viaAPI = viaAPI;
		}

		@Override
		protected boolean hasLinesMaxLength() {
			// change the max line length according to the player version using ViaVersion
			if (viaAPI != null) {
				int playerVersion = viaAPI.getPlayerVersion(getPlayer().getUniqueId());
				int version       = ProtocolVersion.v1_13.getVersion();

				return playerVersion < version;
			}

			// assuming the players are all >1.13
			return false;
		}
	}

}
