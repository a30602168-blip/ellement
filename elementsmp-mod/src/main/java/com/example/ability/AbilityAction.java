package com.example.ability;

import com.example.config.AbilitySettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface AbilityAction {
	/**
	 * Executes the ability.
	 *
	 * @return true if the ability was triggered (cooldown starts), false on failure
	 */
	boolean cast(ServerPlayer player, ServerLevel level, AbilitySettings settings);
}
