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

final class WaterAbilities {
	private WaterAbilities() {
	}

	static List<Ability> create() {
		return List.of(
			new Ability("water_burst", "Water Burst",
				"A jet of water hits the target and hurls it back.",
				Items.PRISMARINE_SHARD, AbilitySettings.of(9, 1.5, 16, 0, 3, 0), WaterAbilities::burst),
			new Ability("water_heal", "Healing Spring",
				"Heals you instantly and gives you brief regeneration.",
				Items.GLISTERING_MELON_SLICE, AbilitySettings.of(35, 6, 0, 5, 0, 0), WaterAbilities::heal),
			new Ability("water_wave", "Tidal Wave",
				"A wave pushes back all nearby enemies. You swim faster afterwards.",
				Items.HEART_OF_THE_SEA, AbilitySettings.of(18, 1.4, 6, 8, 0, 0), WaterAbilities::wave)
		);
	}

	private static boolean burst(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		AbilityUtil.Ray ray = AbilityUtil.trace(player, level, s.radius, 0.8, ParticleTypes.SPLASH);
		if (ray.hit() != null) {
			LivingEntity target = ray.hit();
			AbilityUtil.damage(player, level, target, s.damage);
			Vec3 flat = AbilityUtil.flatLook(player);
			AbilityUtil.launch(target, new Vec3(flat.x * s.power, 0.35, flat.z * s.power));
		}
		AbilityUtil.particles(level, ParticleTypes.SPLASH, ray.end(), 40, 0.4, 0.2);
		AbilityUtil.sound(level, player.position(), SoundEvents.PLAYER_SPLASH, 1.2f, 1.2f);
		return true;
	}

	private static boolean heal(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		player.heal((float) s.power);
		AbilityUtil.effect(player, MobEffects.REGENERATION, s.durationSeconds, 0);
		player.clearFire();
		AbilityUtil.particles(level, ParticleTypes.HEART, player.position().add(0, 1.2, 0), 8, 0.4, 0.0);
		AbilityUtil.particles(level, ParticleTypes.SPLASH, player.position().add(0, 1, 0), 40, 0.5, 0.1);
		AbilityUtil.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.2f);
		return true;
	}

	private static boolean wave(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		Vec3 origin = player.position();
		for (LivingEntity target : AbilityUtil.enemiesAround(player, level, origin, s.radius)) {
			AbilityUtil.pushAway(target, origin, s.power, 0.3);
			AbilityUtil.damage(player, level, target, s.damage);
		}
		AbilityUtil.effect(player, MobEffects.DOLPHINS_GRACE, s.durationSeconds, 0);
		player.clearFire();
		AbilityUtil.particles(level, ParticleTypes.SPLASH, origin.add(0, 0.8, 0), 150, s.radius * 0.4, 0.2);
		AbilityUtil.particles(level, ParticleTypes.BUBBLE, origin.add(0, 0.8, 0), 40, s.radius * 0.3, 0.1);
		AbilityUtil.sound(level, origin, SoundEvents.GENERIC_SPLASH, 1.5f, 0.7f);
		return true;
	}
}
