package org.luckyraven.plaque.board;

import org.bukkit.plugin.java.JavaPlugin;
import org.luckyraven.keystone.placeholder.effect.FlashPlaceholderWrapper;
import org.luckyraven.keystone.timer.RepeatingTimer;
import org.luckyraven.plaque.board.driver.DriverHandler;

public class Board {

	private final DriverHandler  driver;
	private final RepeatingTimer timer;

	public Board(JavaPlugin plugin, DriverHandler driver) {
		this.driver = driver;

		// repeating the task each tick
		this.timer = new RepeatingTimer(plugin, 0L, 1L, time -> {
			FlashPlaceholderWrapper.setCurrentTick(time.getTickCount());

			this.driver.update();
		});
	}

	/**
	 * Starts the per-tick board refresh on the <b>main thread</b>. UI-02: this used to run asynchronously, and the
	 * task body resolves placeholders (gang/user/bank lookups, PlaceholderAPI expansions) and pushes FastBoard
	 * packets — none of which are thread-safe. Per {@code feedback_repeating_timer_async}, only flag flips and
	 * cancels may run with {@code start(true)}.
	 */
	public void start() {
		if (timer == null) return;

		timer.start(false);
	}

	public void end() {
		if (timer == null) return;

		timer.stop();
		driver.getFastBoard().delete();
	}

	@Override
	public String toString() {
		return String.format("Board{driver=%s,timer=%s}", driver, timer);
	}

}
