package com.example.util;

import com.example.Elementsmp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Runs repeating tasks on the server thread (used for ongoing ability effects).
 */
public final class TaskScheduler {
	private static final class Task {
		final int start;
		final int period;
		final int total;
		final IntPredicate step;
		final Runnable onEnd;
		int next;

		Task(int start, int period, int total, IntPredicate step, Runnable onEnd) {
			this.start = start;
			this.period = period;
			this.total = total;
			this.step = step;
			this.onEnd = onEnd;
			this.next = start;
		}
	}

	private final List<Task> tasks = new ArrayList<>();
	private final List<Task> pending = new ArrayList<>();

	/**
	 * Runs {@code step} every {@code periodTicks} ticks for {@code totalTicks} ticks.
	 * The step receives the elapsed ticks and returns false to cancel the task.
	 */
	public void schedule(int periodTicks, int totalTicks, IntPredicate step) {
		schedule(periodTicks, totalTicks, step, null);
	}

	/** Same as above, but {@code onEnd} runs when the task finishes or is cancelled. */
	public void schedule(int periodTicks, int totalTicks, IntPredicate step, Runnable onEnd) {
		int now = Elementsmp.server().getTickCount();
		pending.add(new Task(now, Math.max(1, periodTicks), Math.max(0, totalTicks), step, onEnd));
	}

	public void tick(int nowTick) {
		if (!pending.isEmpty()) {
			tasks.addAll(pending);
			pending.clear();
		}
		Iterator<Task> it = tasks.iterator();
		while (it.hasNext()) {
			Task task = it.next();
			if (nowTick < task.next) {
				continue;
			}
			int elapsed = task.next - task.start;
			boolean keepGoing;
			try {
				keepGoing = task.step.test(elapsed);
			} catch (RuntimeException ex) {
				Elementsmp.LOGGER.error("Scheduled task failed", ex);
				keepGoing = false;
			}
			task.next += task.period;
			if (!keepGoing || elapsed >= task.total) {
				if (task.onEnd != null) {
					task.onEnd.run();
				}
				it.remove();
			}
		}
	}

	public void clearAll() {
		tasks.clear();
		pending.clear();
	}
}
