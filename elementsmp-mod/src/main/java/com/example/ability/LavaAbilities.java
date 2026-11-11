package com.example.ability;

import com.example.config.AbilitySettings;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class LavaAbilities {
	private LavaAbilities() {
	}

	static List<Ability> create() {
		return List.of(
			new Ability("lava_fireball", "Lava Shot",
				"Fires a glowing projectile that burns enemies.",
				Items.FIRE_CHARGE, AbilitySettings.of(9, 24, 0, 5, 5, 0), LavaAbilities::fireball),
			new Ability("lava_heatwave", "Heatwave",
				"A shockwave of heat burns and pushes away all nearby enemies.",
				Items.MAGMA_CREAM, AbilitySettings.of(20, 0.9, 5, 4, 4, 0), LavaAbilities::heatwave),
			new Ability("lava_shield", "Magma Skin",
				"You become briefly immune to fire and lava.",
				Items.MAGMA_BLOCK, AbilitySettings.of(40, 1, 0, 14, 0, 0), LavaAbilities::shield)
		);
	}

	private static boolean fireball(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		AbilityUtil.Ray ray = AbilityUtil.trace(player, level, s.power, 0.9, ParticleTypes.FLAME);
		if (ray.hit() != null) {
			LivingEntity target = ray.hit();
			AbilityUtil.damage(player, level, target, s.damage);
			target.igniteForSeconds((float) s.durationSeconds);
		}
		AbilityUtil.particles(level, ParticleTypes.LAVA, ray.end(), 20, 0.4, 0.1);
		AbilityUtil.particles(level, ParticleTypes.LARGE_SMOKE, ray.end(), 8, 0.3, 0.02);
		AbilityUtil.sound(level, player.position(), SoundEvents.BLAZE_SHOOT, 1.2f, 0.9f);
		AbilityUtil.sound(level, ray.end(), SoundEvents.LAVA_POP, 1.0f, 1.0f);
		return true;
	}

	private static boolean heatwave(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		Vec3 origin = player.position();
		for (LivingEntity target : AbilityUtil.enemiesAround(player, level, origin, s.radius)) {
			AbilityUtil.damage(player, level, target, s.damage);
			target.igniteForSeconds((float) s.durationSeconds);
			AbilityUtil.pushAway(target, origin, s.power, 0.3);
		}
		AbilityUtil.particles(level, ParticleTypes.FLAME, origin.add(0, 1, 0), 120, s.radius * 0.35, 0.12);
		AbilityUtil.particles(level, ParticleTypes.LAVA, origin.add(0, 1, 0), 30, s.radius * 0.3, 0.0);
		AbilityUtil.sound(level, origin, SoundEvents.DRAGON_FIREBALL_EXPLODE, 1.0f, 1.0f);
		return true;
	}

	private static boolean shield(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		AbilityUtil.effect(player, MobEffects.FIRE_RESISTANCE, s.durationSeconds, 0);
		player.clearFire();
		AbilityUtil.particles(level, ParticleTypes.FLAME, player.position().add(0, 1, 0), 40, 0.5, 0.04);
		AbilityUtil.sound(level, player.position(), SoundEvents.FIRECHARGE_USE, 1.0f, 0.8f);
		return true;
	}
}
