package de.blablubbabc.shopkeepers.bluemap.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Scheduler related utilities.
 */
public final class SchedulerUtils {

	private static void validatePluginTask(Plugin plugin, Runnable task) {
		if (plugin == null) {
			throw new IllegalArgumentException("plugin is null");
		}

		if (task == null) {
			throw new IllegalArgumentException("task is null");
		}
	}

	/**
	 * Checks if the current thread is the server's main thread.
	 * 
	 * @return <code>true</code> if currently running on the main thread
	 */
	public static boolean isMainThread() {
		return Bukkit.isPrimaryThread();
	}

	/**
	 * Schedules the given task to be run on the primary thread if required.
	 * <p>
	 * If the current thread is already the primary thread, the task will be run immediately.
	 * Otherwise, it attempts to schedule the task to run on the server's primary thread. However,
	 * if the plugin is disabled, the task won't be scheduled.
	 * 
	 * @param plugin
	 *            the plugin to use for scheduling, not <code>null</code>
	 * @param task
	 *            the task, not <code>null</code>
	 * @return <code>true</code> if the task was run or successfully scheduled to be run,
	 *         <code>false</code> otherwise
	 */
	public static boolean runOnMainThreadOrOmit(Plugin plugin, Runnable task) {
		validatePluginTask(plugin, task);
		if (isMainThread()) {
			task.run();
			return true;
		} else {
			return (runTaskOrOmit(plugin, task) != null);
		}
	}

	public static @Nullable BukkitTask runTaskOrOmit(Plugin plugin, Runnable task) {
		return runTaskLaterOrOmit(plugin, task, 0L);
	}

	public static @Nullable BukkitTask runTaskLaterOrOmit(
			Plugin plugin,
			Runnable task,
			long delay
	) {
		validatePluginTask(plugin, task);
		// Tasks can only be registered while enabled:
		if (plugin.isEnabled()) {
			try {
				return Bukkit.getScheduler().runTaskLater(plugin, task, delay);
			} catch (IllegalPluginAccessException e) {
				// Couldn't register task: The plugin got disabled just now.
			}
		}
		return null;
	}

	public static @Nullable BukkitTask runAsyncTaskOrOmit(Plugin plugin, Runnable task) {
		return runAsyncTaskLaterOrOmit(plugin, task, 0L);
	}

	public static @Nullable BukkitTask runAsyncTaskLaterOrOmit(
			Plugin plugin,
			Runnable task,
			long delay
	) {
		validatePluginTask(plugin, task);
		// Tasks can only be registered while enabled:
		if (plugin.isEnabled()) {
			try {
				return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
			} catch (IllegalPluginAccessException e) {
				// Couldn't register task: The plugin got disabled just now.
			}
		}
		return null;
	}

	private SchedulerUtils() {
	}
}
