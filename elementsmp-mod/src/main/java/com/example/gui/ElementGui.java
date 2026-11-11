package com.example.gui;

import com.example.util.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;

public final class ElementGui {
	private ElementGui() {
	}

	public static void open(ServerPlayer player) {
		SimpleContainer container = new SimpleContainer(27);
		Component title = Text.bold("Elements", ChatFormatting.DARK_GRAY);
		player.openMenu(new SimpleMenuProvider(
			(id, inventory, p) -> new ElementMenu(id, inventory, container, player), title));
	}

	/** Updates all open element menus (cooldown display). */
	public static void refreshOpen(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.containerMenu instanceof ElementMenu menu) {
				menu.refresh();
			}
		}
	}
}
