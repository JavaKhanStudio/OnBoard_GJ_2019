package jks.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * A scenario driven on the game's own clock: each step runs on a frame once the one before it
 * has had its time, so a fade or a load it started has finished whatever the machine's speed.
 * The first step waits for the game to come up. A step that throws ends the scenario, and
 * what it threw is kept in error for the test to report.
 */
final class Steps
{
	interface Step { void run() throws Exception; }

	private static final class Entry
	{
		final double wait;
		final Step step;
		final BooleanSupplier ready;
		final double timeout;

		Entry(double wait, Step step, BooleanSupplier ready, double timeout)
		{
			this.wait = wait;
			this.step = step;
			this.ready = ready;
			this.timeout = timeout;
		}
	}

	private final double first;
	private final double gap;
	private final List<Entry> entries = new ArrayList<>();

	private int next;
	private double due;

	/** What a step threw. Null means every step that ran, ran clean. */
	volatile Throwable error;

	/** The first step runs once first seconds have passed, each next one gap seconds after. */
	Steps(double first, double gap)
	{
		this.first = first;
		this.gap = gap;
		this.due = first;
	}

	Steps add(Step step)
	{
		return add(gap, step);
	}

	/** A step, and how long the game's clock runs before the next one. */
	Steps add(double wait, Step step)
	{
		entries.add(new Entry(wait, step, null, 0));
		return this;
	}

	/** A step that runs once ready holds, or timeout seconds after it came due if it never does. */
	Steps addWhen(BooleanSupplier ready, double timeout, double wait, Step step)
	{
		entries.add(new Entry(wait, step, ready, timeout));
		return this;
	}

	int size()
	{
		return entries.size();
	}

	/** Game seconds from the start to the last step's wait running out. */
	double seconds()
	{
		double total = first;
		for (Entry entry : entries) total += entry.wait;
		return total;
	}

	/** Runs the steps from the harness's frame hook. */
	void drive(GameHarness harness)
	{
		harness.frameHook = frame ->
		{
			if (error != null || next >= entries.size() || harness.gameSeconds < due) return;
			Entry entry = entries.get(next);
			try
			{
				if (entry.ready != null && !entry.ready.getAsBoolean() && harness.gameSeconds < due + entry.timeout) return;
				next++;
				entry.step.run();
			}
			catch (Throwable t)
			{
				error = t;
			}
			due = harness.gameSeconds + entry.wait;
		};
	}
}
