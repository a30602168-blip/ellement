package com.example.ability;

import com.example.Elementsmp;
import com.example.config.AbilitySettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class SandAbilities {
	private SandAbilities() {
	}

	static List<Ability> create() {
		return List.of(
			new Ability("sand_storm", "Sandstorm",
				"Engulfs nearby enemies in sand and blinds them.",
				Items.GLOWSTONE_DUST, AbilitySettings.of(22, 1, 7, 4, 0, 0), SandAbilities::storm),
			new Ability("sand_barrier", "Sand Barrier",
				"Summons a sandstone wall in front of you that crumbles after a short time.",
				Items.SANDSTONE, AbilitySettings.of(18, 3, 2, 7, 0, 0), SandAbilities::barrier),
			new Ability("sand_trap", "Quicksand",
				"Heavily slows all nearby enemies.",
				Items.CLAY_BALL, AbilitySettings.of(16, 2, 6, 5, 0, 0), SandAbilities::trap)
		);
	}

	private static boolean storm(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		Vec3 origin = player.position();
		for (LivingEntity target : AbilityUtil.enemiesAround(player, level, origin, s.radius)) {
			AbilityUtil.effect(target, MobEffects.BLINDNESS, s.durationSeconds, 0);
			AbilityUtil.damage(player, level, target, s.damage);
			AbilityUtil.particles(level, ParticleTypes.POOF, target.position().add(0, 1, 0), 15, 0.4, 0.05);
		}
		AbilityUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
			origin.add(0, 1, 0), 120, s.radius * 0.4, 0.1);
		AbilityUtil.sound(level, origin, SoundEvents.SAND_BREAK, 1.5f, 0.6f);
		return true;
	}

	private static boolean barrier(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		Direction facing = player.getDirection();
		Direction side = facing.getClockWise();
		BlockPos base = player.blockPosition().relative(facing, 3);
		int halfWidth = Math.max(1, (int) s.radius);
		int height = Math.max(1, (int) s.power);
		int duration = (int) Math.round(s.durationSeconds * 20.0);
		int now = Elementsmp.server().getTickCount();
		int placed = 0;
		for (int w = -halfWidth; w <= halfWidth; w++) {
			for (int h = 0; h < height; h++) {
				BlockPos pos = base.relative(side, w).above(h);
				if (Elementsmp.tempBlocks().place(level, pos, Blocks.SANDSTONE, duration, now)) {
					placed++;
					AbilityUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
						Vec3.atCenterOf(pos), 6, 0.3, 0.05);
				}
			}
		}
		if (placed == 0) {
			return false;
		}
		AbilityUtil.sound(level, player.position(), SoundEvents.SAND_BREAK, 1.2f, 0.8f);
		return true;
	}

	private static boolean trap(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		Vec3 origin = player.position();
		int amplifier = Math.max(0, (int) s.power);
		for (LivingEntity target : AbilityUtil.enemiesAround(player, level, origin, s.radius)) {
			AbilityUtil.effect(target, MobEffects.SLOWNESS, s.durationSeconds, amplifier);
			AbilityUtil.damage(player, level, target, s.damage);
			AbilityUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
				target.position().add(0, 0.2, 0), 20, 0.3, 0.05);
		}
		AbilityUtil.particles(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
			origin.add(0, 0.2, 0), 80, s.radius * 0.4, 0.05);
		AbilityUtil.sound(level, origin, SoundEvents.SAND_BREAK, 1.2f, 0.5f);
		return true;
	}
}
