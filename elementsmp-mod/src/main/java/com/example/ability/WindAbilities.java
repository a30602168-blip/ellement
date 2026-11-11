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

final class WindAbilities {
	private WindAbilities() {
	}

	static List<Ability> create() {
		return List.of(
			new Ability("wind_gust", "Gust",
				"Hurls all enemies in front of you back with a powerful gust of wind.",
				Items.WIND_CHARGE, AbilitySettings.of(10, 1.7, 7, 0, 1, 0), WindAbilities::gust),
			new Ability("wind_dash", "Storm Dash",
				"Catapults you forward and gives you a short speed boost.",
				Items.SUGAR, AbilitySettings.of(8, 1.6, 0, 4, 0, 0), WindAbilities::dash),
			new Ability("wind_leap", "Air Leap",
				"A high leap into the air. You float gently back down.",
				Items.PHANTOM_MEMBRANE, AbilitySettings.of(12, 1.1, 0, 6, 0, 0), WindAbilities::leap)
		);
	}

	private static boolean gust(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		Vec3 look = player.getLookAngle();
		Vec3 flat = AbilityUtil.flatLook(player);
		Vec3 origin = player.position();
		for (LivingEntity target : AbilityUtil.enemiesAround(player, level, origin, s.radius)) {
			Vec3 toTarget = target.position().subtract(origin);
			if (toTarget.lengthSqr() > 1.0E-4 && toTarget.normalize().dot(look) > 0.2) {
				AbilityUtil.launch(target, new Vec3(flat.x * s.power, 0.45, flat.z * s.power));
				AbilityUtil.damage(player, level, target, s.damage);
			}
		}
		Vec3 eye = player.getEyePosition();
		for (int i = 1; i <= (int) s.radius; i++) {
			AbilityUtil.particles(level, ParticleTypes.CLOUD, eye.add(look.scale(i)), 6, 0.3, 0.02);
		}
		AbilityUtil.sound(level, origin, SoundEvents.ENDER_DRAGON_FLAP, 1.2f, 1.4f);
		return true;
	}

	private static boolean dash(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		Vec3 flat = AbilityUtil.flatLook(player);
		AbilityUtil.launch(player, new Vec3(flat.x * s.power, 0.25, flat.z * s.power));
		AbilityUtil.effect(player, MobEffects.SPEED, s.durationSeconds, 1);
		AbilityUtil.particles(level, ParticleTypes.CLOUD, player.position().add(0, 0.5, 0), 25, 0.4, 0.05);
		AbilityUtil.sound(level, player.position(), SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0f, 1.2f);
		return true;
	}

	private static boolean leap(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		Vec3 flat = AbilityUtil.flatLook(player);
		AbilityUtil.launch(player, new Vec3(flat.x * 0.3, s.power, flat.z * 0.3));
		player.resetFallDistance();
		AbilityUtil.effect(player, MobEffects.SLOW_FALLING, s.durationSeconds, 0);
		AbilityUtil.particles(level, ParticleTypes.CLOUD, player.position(), 30, 0.5, 0.05);
		AbilityUtil.sound(level, player.position(), SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.8f);
		return true;
	}
}
