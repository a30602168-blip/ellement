package com.example.ability;

import com.example.Elementsmp;
import com.example.config.AbilitySettings;
import com.example.util.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Orbital Strike Cannon and Stab Shot.
 * Settings: Strike - range = max targeting distance, durationSeconds = charge time, count = number of blasts,
 * power = blast size, radius = crater radius. Stab Shot - count = number of blasts, power = blast size,
 * radius = distance between blasts.
 */
final class OrbitalAbilities {
	private OrbitalAbilities() {
	}

	static List<Ability> create() {
		return List.of(
			new Ability("orbital_strike_cannon", "Orbital Strike Cannon",
				"Marks the ground you look at. After a short charge, a barrage of massive blasts rains down from orbit and carves a huge crater.",
				Items.BEACON, AbilitySettings.of(90, 6, 8, 2, 0, 0).range(150).count(30), OrbitalAbilities::orbitalStrike),
			new Ability("orbital_stab_shot", "Stab Shot",
				"Fires a piercing line of blasts straight ahead that drills a deep, narrow tunnel.",
				Items.TRIDENT, AbilitySettings.of(60, 3.5, 1.5, 0, 0, 0).count(40), OrbitalAbilities::stabShot)
		);
	}

	private static ServerPlayer findPlayer(UUID id) {
		return Elementsmp.server().getPlayerList().getPlayer(id);
	}

	private static void blast(ServerLevel level, Entity source, double x, double y, double z, float power) {
		Level.ExplosionInteraction mode = Elementsmp.config().orbitalBlockDamage
			? Level.ExplosionInteraction.TNT
			: Level.ExplosionInteraction.NONE;
		level.explode(source, x, y, z, power, mode);
	}

	/** First solid block along the look direction, or null if there is none in range. */
	private static Vec3 groundTarget(ServerPlayer player, ServerLevel level, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getLookAngle();
		for (double d = 1.0; d <= range; d += 1.0) {
			Vec3 point = eye.add(dir.scale(d));
			BlockPos pos = BlockPos.containing(point);
			if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
				return point;
			}
		}
		return null;
	}

	private static boolean orbitalStrike(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		Vec3 target = groundTarget(player, level, s.range);
		if (target == null) {
			player.sendSystemMessage(Text.chat("No target in range. Aim at the ground.", ChatFormatting.RED));
			return false;
		}
		double minDistance = s.radius + s.power * 2.0 + 2.0;
		if (player.position().distanceTo(target) < minDistance) {
			player.sendSystemMessage(Text.chat("Target too close! Aim at least "
				+ (int) Math.ceil(minDistance) + " blocks away.", ChatFormatting.RED));
			return false;
		}
		UUID id = player.getUUID();
		int chargeTicks = (int) Math.round(s.durationSeconds * 20.0);
		AbilityUtil.sound(level, target, SoundEvents.BEACON_ACTIVATE, 8.0f, 1.0f);
		Elementsmp.scheduler().schedule(2, chargeTicks, elapsed -> {
			for (double y = 0; y < 80; y += 2) {
				level.sendParticles(ParticleTypes.END_ROD, target.x, target.y + y, target.z, 1, 0.1, 0.0, 0.1, 0.0);
			}
			for (int i = 0; i < 32; i++) {
				double angle = Math.PI * 2 * i / 32;
				level.sendParticles(ParticleTypes.END_ROD, target.x + Math.cos(angle) * s.radius, target.y + 0.3,
					target.z + Math.sin(angle) * s.radius, 1, 0.0, 0.0, 0.0, 0.0);
			}
			return true;
		}, () -> startBarrage(id, level, target, s));
		return true;
	}

	private static void startBarrage(UUID id, ServerLevel level, Vec3 target, AbilitySettings s) {
		int blasts = Math.max(1, s.count);
		double depth = s.radius * 0.6;
		Elementsmp.scheduler().schedule(2, (blasts - 1) * 2, elapsed -> {
			double progress = (elapsed / 2) / (double) blasts;
			double spread = s.radius * (1.0 - 0.7 * progress);
			ThreadLocalRandom random = ThreadLocalRandom.current();
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = Math.sqrt(random.nextDouble()) * spread;
			double x = target.x + Math.cos(angle) * distance;
			double z = target.z + Math.sin(angle) * distance;
			double y = target.y - depth * progress;
			for (double h = 0; h < 60; h += 3) {
				level.sendParticles(ParticleTypes.END_ROD, x, y + h, z, 1, 0.1, 0.0, 0.1, 0.0);
			}
			blast(level, findPlayer(id), x, y, z, (float) s.power);
			return true;
		});
	}

	private static boolean stabShot(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		UUID id = player.getUUID();
		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getLookAngle().normalize();
		double start = s.power * 2.0 + 1.0;
		double spacing = Math.max(0.5, s.radius);
		int blasts = Math.max(1, s.count);
		AbilityUtil.sound(level, eye, SoundEvents.BLAZE_SHOOT, 2.0f, 0.5f);
		Elementsmp.scheduler().schedule(1, blasts - 1, elapsed -> {
			Vec3 point = eye.add(dir.scale(start + elapsed * spacing));
			level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 6, 0.2, 0.2, 0.2, 0.02);
			blast(level, findPlayer(id), point.x, point.y, point.z, (float) s.power);
			return true;
		});
		return true;
	}
}
