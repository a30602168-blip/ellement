package com.example.ability;

import com.example.Elementsmp;
import com.example.config.AbilitySettings;
import com.example.util.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

final class NukeAbilities {
	private NukeAbilities() {
	}

	static List<Ability> create() {
		return List.of(
			new Ability("nuke_acid_rain", "Acid Rain",
				"Toxic rain falls over a huge area around you and slowly damages every enemy inside.",
				Items.SLIME_BALL, AbilitySettings.of(45, 0, 25, 10, 1.0, 0), NukeAbilities::acidRain),
			new Ability("nuke_shockwave", "Shockwave Aura",
				"For a few seconds, a shockwave around you constantly pushes every enemy away.",
				Items.FIREWORK_STAR, AbilitySettings.of(40, 0.9, 8, 3, 0, 0).kills(2), NukeAbilities::shockwave),
			new Ability("nuke_detonation", "Nuclear Detonation",
				"A gigantic explosion rages for 7 seconds and obliterates everything in a huge radius. You are immune.",
				Items.TNT, AbilitySettings.of(240, 0, 100, 7, 0, 0).kills(20), NukeAbilities::detonate)
		);
	}

	private static ServerPlayer findPlayer(UUID id) {
		return Elementsmp.server().getPlayerList().getPlayer(id);
	}

	private static boolean acidRain(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		UUID id = player.getUUID();
		Vec3 center = player.position();
		int total = (int) Math.round(s.durationSeconds * 20.0);
		AbilityUtil.sound(level, center, SoundEvents.WEATHER_RAIN, 4.0f, 0.6f);
		Elementsmp.scheduler().schedule(20, total, elapsed -> {
			ServerPlayer caster = findPlayer(id);
			if (caster == null || !caster.isAlive()) {
				return false;
			}
			AbilityUtil.particles(level, ParticleTypes.FALLING_SPORE_BLOSSOM, center.add(0, 8, 0), 200, 12, 0.0);
			for (LivingEntity target : AbilityUtil.enemiesAround(caster, level, center, s.radius)) {
				AbilityUtil.damage(caster, level, target, s.damage);
				AbilityUtil.particles(level, ParticleTypes.HAPPY_VILLAGER, target.position().add(0, 2.2, 0), 12, 0.4, 0.0);
				AbilityUtil.particles(level, ParticleTypes.FALLING_SPORE_BLOSSOM, target.position().add(0, 3, 0), 20, 0.6, 0.0);
			}
			return true;
		});
		return true;
	}

	private static boolean shockwave(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		UUID id = player.getUUID();
		int total = (int) Math.round(s.durationSeconds * 20.0);
		AbilityUtil.sound(level, player.position(), SoundEvents.ENDER_DRAGON_FLAP, 1.5f, 0.6f);
		Elementsmp.scheduler().schedule(2, total, elapsed -> {
			ServerPlayer caster = findPlayer(id);
			if (caster == null || !caster.isAlive()) {
				return false;
			}
			ServerLevel world = (ServerLevel) caster.level();
			Vec3 origin = caster.position();
			for (LivingEntity target : AbilityUtil.enemiesAround(caster, world, origin, s.radius)) {
				AbilityUtil.pushAway(target, origin, s.power, 0.15);
			}
			for (int i = 0; i < 24; i++) {
				double angle = Math.PI * 2 * i / 24 + elapsed * 0.15;
				double x = origin.x + Math.cos(angle) * s.radius * 0.5;
				double z = origin.z + Math.sin(angle) * s.radius * 0.5;
				world.sendParticles(ParticleTypes.CLOUD, x, origin.y + 0.3, z, 1, 0.0, 0.0, 0.0, 0.02);
			}
			if (elapsed % 10 == 0) {
				AbilityUtil.sound(world, origin, SoundEvents.ENDER_DRAGON_FLAP, 1.2f, 0.7f);
			}
			return true;
		});
		return true;
	}

	/**
	 * A shockwave expands from the activation point to the full radius over the configured duration.
	 * Everything inside the current radius is killed instantly, except the caster.
	 */
	private static boolean detonate(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		UUID id = player.getUUID();
		Vec3 center = player.position();
		double maxRadius = s.radius;
		int total = Math.max(1, (int) Math.round(s.durationSeconds * 20.0));
		Elementsmp.server().getPlayerList().broadcastSystemMessage(
			Text.bold("NUCLEAR DETONATION by " + player.getName().getString() + "!", ChatFormatting.DARK_RED), false);
		AbilityUtil.sound(level, center, SoundEvents.ENDER_DRAGON_GROWL, 30.0f, 0.5f);
		Elementsmp.scheduler().schedule(2, total, elapsed -> {
			double radius = Math.max(2.0, maxRadius * Math.min(1.0, elapsed / (double) total));
			double radiusSqr = radius * radius;
			List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class,
				new AABB(center, center).inflate(radius),
				e -> e.isAlive()
					&& !e.isSpectator()
					&& !e.getUUID().equals(id)
					&& !(e instanceof ServerPlayer other && other.isCreative())
					&& e.position().distanceToSqr(center) <= radiusSqr);
			for (LivingEntity victim : victims) {
				victim.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
			}
			int points = (int) Math.min(90, Math.max(16, radius * 1.5));
			for (int i = 0; i < points; i++) {
				double angle = Math.PI * 2 * i / points;
				double x = center.x + Math.cos(angle) * radius;
				double z = center.z + Math.sin(angle) * radius;
				level.sendParticles(ParticleTypes.FLAME, x, center.y + 1, z, 1, 0.5, 1.5, 0.5, 0.02);
				if (i % 3 == 0) {
					level.sendParticles(ParticleTypes.LARGE_SMOKE, x, center.y + 1, z, 1, 0.5, 1.5, 0.5, 0.02);
				}
			}
			if (elapsed % 4 == 0) {
				AbilityUtil.particles(level, ParticleTypes.EXPLOSION_EMITTER, center.add(0, 2, 0), 2, 4, 0.0);
			}
			if (elapsed % 10 == 0) {
				AbilityUtil.sound(level, center, SoundEvents.DRAGON_FIREBALL_EXPLODE, 12.0f, 0.5f);
			}
			return true;
		});
		return true;
	}
}
