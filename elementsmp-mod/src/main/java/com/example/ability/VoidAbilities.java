package com.example.ability;

import com.example.config.AbilitySettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

final class VoidAbilities {
	private VoidAbilities() {
	}

	static List<Ability> create() {
		return List.of(
			new Ability("void_blink", "Void Blink",
				"Teleports you a short distance in your look direction.",
				Items.ENDER_PEARL, AbilitySettings.of(12, 1, 9, 0, 0, 0), VoidAbilities::blink),
			new Ability("void_blast", "Dark Explosion",
				"An explosion of dark energy damages enemies, darkens their vision and pushes them away. Costs you health.",
				Items.DRAGON_BREATH, AbilitySettings.of(25, 1.2, 5, 4, 6, 2), VoidAbilities::blast),
			new Ability("void_repel", "Void Repel",
				"A focused beam of void energy hurls enemies far back and weakens them.",
				Items.ECHO_SHARD, AbilitySettings.of(16, 2.2, 8, 5, 0, 0), VoidAbilities::repel)
		);
	}

	private static boolean passable(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	private static boolean blink(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		Vec3 start = player.position();
		Vec3 dir = player.getLookAngle();
		Vec3 best = null;
		for (double d = 1.0; d <= s.radius; d += 0.5) {
			Vec3 feet = start.add(dir.scale(d));
			BlockPos feetPos = BlockPos.containing(feet);
			if (!passable(level, feetPos) || !passable(level, feetPos.above())) {
				break;
			}
			best = feet;
		}
		if (best == null) {
			return false;
		}
		AbilityUtil.particles(level, ParticleTypes.PORTAL, start.add(0, 1, 0), 60, 0.4, 0.5);
		AbilityUtil.sound(level, start, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);
		player.teleportTo(level, best.x, best.y, best.z, Set.<Relative>of(), player.getYRot(), player.getXRot(), true);
		player.resetFallDistance();
		AbilityUtil.particles(level, ParticleTypes.REVERSE_PORTAL, best.add(0, 1, 0), 60, 0.4, 0.2);
		AbilityUtil.sound(level, best, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.3f);
		return true;
	}

	private static boolean blast(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		Vec3 origin = player.position();
		for (LivingEntity target : AbilityUtil.enemiesAround(player, level, origin, s.radius)) {
			AbilityUtil.damage(player, level, target, s.damage);
			AbilityUtil.effect(target, MobEffects.DARKNESS, s.durationSeconds, 0);
			AbilityUtil.pushAway(target, origin, s.power, 0.4);
		}
		AbilityUtil.particles(level, ParticleTypes.SQUID_INK, origin.add(0, 1, 0), 80, s.radius * 0.3, 0.15);
		AbilityUtil.particles(level, ParticleTypes.REVERSE_PORTAL, origin.add(0, 1, 0), 100, s.radius * 0.3, 0.3);
		AbilityUtil.particles(level, ParticleTypes.EXPLOSION, origin.add(0, 1, 0), 3, 0.5, 0.0);
		AbilityUtil.sound(level, origin, SoundEvents.DRAGON_FIREBALL_EXPLODE, 1.2f, 0.6f);
		if (s.selfDamage > 0) {
			player.hurtServer(level, level.damageSources().magic(), (float) s.selfDamage);
		}
		return true;
	}

	private static boolean repel(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		Vec3 look = player.getLookAngle();
		Vec3 flat = AbilityUtil.flatLook(player);
		Vec3 origin = player.position();
		for (LivingEntity target : AbilityUtil.enemiesAround(player, level, origin, s.radius)) {
			Vec3 toTarget = target.position().subtract(origin);
			if (toTarget.lengthSqr() > 1.0E-4 && toTarget.normalize().dot(look) > 0.7) {
				AbilityUtil.launch(target, new Vec3(flat.x * s.power, 0.4, flat.z * s.power));
				AbilityUtil.effect(target, MobEffects.WEAKNESS, s.durationSeconds, 0);
				AbilityUtil.damage(player, level, target, s.damage);
			}
		}
		Vec3 eye = player.getEyePosition();
		for (int i = 1; i <= (int) s.radius; i++) {
			AbilityUtil.particles(level, ParticleTypes.PORTAL, eye.add(look.scale(i)), 8, 0.2, 0.3);
		}
		AbilityUtil.sound(level, origin, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.5f);
		return true;
	}
}
