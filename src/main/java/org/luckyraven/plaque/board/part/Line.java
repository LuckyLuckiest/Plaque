package org.luckyraven.plaque.board.part;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.luckyraven.keystone.placeholder.PlaceholderProvider;
import org.luckyraven.keystone.util.ChatUtil;

import java.util.ArrayList;
import java.util.List;

public class Line {

	private final @Getter long         interval;
	private final         List<String> contents;
	private final @Getter int          usedIndex;

	private int index;

	public Line(long interval) {
		this(interval, 0);
	}

	public Line(long interval, int index) {
		this.interval  = interval;
		this.index     = 0;
		this.contents  = new ArrayList<>();
		this.usedIndex = index;
	}

	/**
	 * Copy constructor. Clones the interval, board slot and rotation cursor, and takes a private copy of the
	 * already-coloured contents — {@link #addContent(String)} is deliberately bypassed so
	 * {@link ChatUtil#color(String)} is not applied a second time.
	 */
	protected Line(Line source) {
		this.interval  = source.interval;
		this.usedIndex = source.usedIndex;
		this.index     = source.index;
		this.contents  = new ArrayList<>(source.contents);
	}

	/**
	 * A board-private clone of this line. {@link #update(PlaceholderProvider, Player)} advances the rotation
	 * cursor, so every player's board must own its own {@code Line} instances — sharing the configured ones makes
	 * animated content cycle once per online player per tick instead of once per tick (UI-01).
	 *
	 * @return an independent copy holding the same contents
	 */
	public Line copy() {
		return new Line(this);
	}

	/**
	 * Copies a whole board's worth of lines through {@link #copy()}, preserving order.
	 *
	 * @param lines the configured lines
	 *
	 * @return a new, independently mutable list of independent lines
	 */
	public static List<Line> copyAll(List<Line> lines) {
		List<Line> copies = new ArrayList<>(lines.size());

		for (Line line : lines) {
			copies.add(line.copy());
		}

		return copies;
	}

	public void addContent(String content) {
		contents.add(ChatUtil.color(content));
	}

	public void addAllContents(List<String> contents) {
		contents.forEach(this::addContent);
	}

	/**
	 * UI-17: an empty {@code Lines:} list (a misconfigured board) used to throw
	 * {@code IndexOutOfBoundsException} here on every tick; return an empty string instead.
	 */
	public String getCurrentContent() {
		if (contents.isEmpty()) return "";

		return contents.get(index);
	}

	public String update(PlaceholderProvider placeholder, Player player) {
		if (contents.isEmpty()) return "";

		String data       = getCurrentContent();
		String newContent = placeholder.resolve(player, data);

		if (newContent.isEmpty()) newContent = data;

		index = (index + 1) % contents.size();

		return newContent;
	}

	public boolean isStatic() {
		return this instanceof StaticLine || interval == 0L;
	}

	@Override
	public int hashCode() {
		return (int) interval;
	}

	@Override
	public String toString() {
		return String.format("Line{interval=%d,contents=%s}", interval, contents);
	}

}
