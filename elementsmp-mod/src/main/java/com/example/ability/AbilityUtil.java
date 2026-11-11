package com.example.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Shared helper functions for all abilities.
 */
public final class AbilityUtil {
	private AbilityUtil() {
	}

	public record Ray(Vec3 end, LivingEntity hit) { }

	/** All attackable living entities in the radius (excluding the caster, spectators and creative players). */
	public static List<LivingEntity> enemiesAround(ServerPlayer caster, ServerLevel level, Vec3 center, double radius) {
		AABB box = new AABB(center, center).inflate(radius);
		double radiusSqr = radius * radius;
		return level.getEntitiesOfClass(LivingEntity.class, box, e ->
			e != caster
				&& e.isAlive()
				&& !e.isSpectator()
				&& !(e instanceof ServerPlayer other && other.isCreative())
				&& e.position().distanceToSqr(center) <= radiusSqr);
	}

	/** Sets the motion of an entity and syncs it to the client. */
	public static void launch(Entity entity, Vec3 velocity) {
		entity.setDeltaMovement(velocity);
		entity.hurtMarked = true;
	}

	/** Pushes an entity horizontally away from the origin. */
	public static void pushAway(Entity entity, Vec3 origin, double strength, double lift) {
		Vec3 diff = entity.position().subtract(origin);
		Vec3 flat = new Vec3(diff.x, 0, diff.z);
		if (flat.lengthSqr() < 1.0E-4) {
			flat = new Vec3(0, 0, 1);
		}
		flat = flat.normalize().scale(strength);
		launch(entity, new Vec3(flat.x, lift, flat.z));
	}

	/** Horizontal, normalized look direction. */
	public static Vec3 flatLook(ServerPlayer player) {
		Vec3 look = player.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		if (flat.lengthSqr() < 1.0E-4) {
			return new Vec3(0, 0, 1);
		}
		return flat.normalize();
	}

	public static void sound(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
	}

	public static void particles(ServerLevel level, ParticleOptions type, Vec3 pos, int count, double spread, double speed) {
		level.sendParticles(type, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
	}

	public static void effect(LivingEntity target, Holder<MobEffect> effect, double seconds, int amplifier) {
		int ticks = Math.max(1, (int) Math.round(seconds * 20.0));
		target.addEffect(new MobEffectInstance(effect, ticks, amplifier));
	}

	/** Magic damage with the caster as the source (respects the PvP setting). */
	public static void damage(ServerPlayer caster, ServerLevel level, LivingEntity target, double amount) {
		if (amount > 0) {
			target.hurtServer(level, level.damageSources().indirectMagic(caster, caster), (float) amount);
		}
	}

	/** Shoots a ray in the look direction and returns the first entity hit or the end point. */
	public static Ray trace(ServerPlayer player, ServerLevel level, double range, double width, ParticleOptions trail) {
		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getLookAngle();
		Vec3 end = eye;
		for (double d = 0.5; d <= range; d += 0.5) {
			Vec3 point = eye.add(dir.scale(d));
			BlockPos blockPos = BlockPos.containing(point);
			if (!level.getBlockState(blockPos).getCollisionShape(level, blockPos).isEmpty()) {
				break;
			}
			end = point;
			if (trail != null && d >= 1.0) {
				level.sendParticles(trail, point.x, point.y, point.z, 2, 0.05, 0.05, 0.05, 0.0);
			}
			List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class,
				new AABB(point, point).inflate(width),
				e -> e != player && e.isAlive() && !e.isSpectator()
					&& !(e instanceof ServerPlayer other && other.isCreative()));
			if (!hits.isEmpty()) {
				return new Ray(point, hits.get(0));
			}
		}
		return new Ray(end, null);
	}
}
