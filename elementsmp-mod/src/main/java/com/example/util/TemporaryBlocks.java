package com.example.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Places blocks (e.g. walls) that automatically disappear after some time.
 */
public final class TemporaryBlocks {
	private record Entry(ServerLevel level, BlockPos pos, Block block, int expireTick) { }

	private final List<Entry> entries = new ArrayList<>();

	/** Only places the block in air. */
	public boolean place(ServerLevel level, BlockPos pos, Block block, int durationTicks, int nowTick) {
		if (!level.getBlockState(pos).isAir()) {
			return false;
		}
		level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
		entries.add(new Entry(level, pos.immutable(), block, nowTick + durationTicks));
		return true;
	}

	public void tick(int nowTick) {
		if (entries.isEmpty()) {
			return;
		}
		Iterator<Entry> it = entries.iterator();
		while (it.hasNext()) {
			Entry entry = it.next();
			if (nowTick >= entry.expireTick()) {
				remove(entry);
				it.remove();
			}
		}
	}

	/** Removes all temporary blocks immediately (e.g. on server stop). */
	public void clearAll() {
		for (Entry entry : entries) {
			remove(entry);
		}
		entries.clear();
	}

	private void remove(Entry entry) {
		if (entry.level().getBlockState(entry.pos()).is(entry.block())) {
			entry.level().setBlock(entry.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
	}
}
