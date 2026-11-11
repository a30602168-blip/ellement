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

final class EarthAbilities {
	private EarthAbilities() {
	}

	static List<Ability> create() {
		return List.of(
			new Ability("earth_wall", "Earth Wall",
				"Raises a massive stone wall out of the ground in front of you.",
				Items.COBBLESTONE, AbilitySettings.of(18, 3, 2, 8, 0, 0), EarthAbilities::wall),
			new Ability("earth_slam", "Rockfall",
				"A hail of stones hits the targeted area, damages and lifts enemies.",
				Items.FLINT, AbilitySettings.of(14, 0.6, 3, 2, 5, 0), EarthAbilities::slam),
			new Ability("earth_skin", "Stone Skin",
				"Strong armor for a short time, but you are slower.",
				Items.IRON_NUGGET, AbilitySettings.of(30, 1, 0, 6, 0, 0), EarthAbilities::skin)
		);
	}

	private static BlockParticleOption stoneParticle() {
		return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
	}

	private static boolean wall(ServerPlayer player, ServerLevel level, AbilitySettings s) {
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
				if (Elementsmp.tempBlocks().place(level, pos, Blocks.STONE, duration, now)) {
					placed++;
					AbilityUtil.particles(level, stoneParticle(), Vec3.atCenterOf(pos), 6, 0.3, 0.05);
				}
			}
		}
		if (placed == 0) {
			return false;
		}
		AbilityUtil.sound(level, player.position(), SoundEvents.STONE_BREAK, 1.5f, 0.6f);
		return true;
	}

	private static boolean slam(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		AbilityUtil.Ray ray = AbilityUtil.trace(player, level, 14, 0.8, null);
		Vec3 center = ray.hit() != null ? ray.hit().position() : ray.end();
		for (LivingEntity target : AbilityUtil.enemiesAround(player, level, center, s.radius)) {
			AbilityUtil.damage(player, level, target, s.damage);
			AbilityUtil.launch(target, new Vec3(0, s.power, 0));
			AbilityUtil.effect(target, MobEffects.SLOWNESS, s.durationSeconds, 1);
		}
		AbilityUtil.particles(level, stoneParticle(), center.add(0, 1, 0), 120, s.radius * 0.4, 0.2);
		AbilityUtil.particles(level, ParticleTypes.POOF, center.add(0, 0.5, 0), 25, s.radius * 0.3, 0.05);
		AbilityUtil.sound(level, center, SoundEvents.ANVIL_LAND, 1.0f, 0.6f);
		AbilityUtil.sound(level, center, SoundEvents.STONE_BREAK, 1.5f, 0.5f);
		return true;
	}

	private static boolean skin(ServerPlayer player, ServerLevel level, AbilitySettings s) {
		AbilityUtil.effect(player, MobEffects.RESISTANCE, s.durationSeconds, 1);
		AbilityUtil.effect(player, MobEffects.SLOWNESS, s.durationSeconds, 0);
		AbilityUtil.particles(level, stoneParticle(), player.position().add(0, 1, 0), 60, 0.5, 0.1);
		AbilityUtil.sound(level, player.position(), SoundEvents.STONE_BREAK, 1.2f, 0.7f);
		return true;
	}
}
