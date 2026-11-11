package com.example.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages cooldowns per player and ability based on the server tick.
 */
public final class CooldownManager {
	private final Map<UUID, Map<String, Integer>> expiry = new HashMap<>();

	public int remainingTicks(UUID player, String abilityId, int nowTick) {
		Map<String, Integer> map = expiry.get(player);
		if (map == null) {
			return 0;
		}
		Integer until = map.get(abilityId);
		if (until == null) {
			return 0;
		}
		int remaining = until - nowTick;
		if (remaining <= 0) {
			map.remove(abilityId);
			return 0;
		}
		return remaining;
	}

	public void start(UUID player, String abilityId, int nowTick, double seconds) {
		int ticks = (int) Math.round(seconds * 20.0);
		if (ticks <= 0) {
			return;
		}
		expiry.computeIfAbsent(player, k -> new HashMap<>()).put(abilityId, nowTick + ticks);
	}

	public void clear(UUID player) {
		expiry.remove(player);
	}

	public static String format(int ticks) {
		return String.format(java.util.Locale.ROOT, "%.1fs", ticks / 20.0);
	}
}
