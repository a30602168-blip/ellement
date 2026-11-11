package com.example;

import com.example.ability.Ability;
import com.example.ability.AbilityRegistry;
import com.example.element.Element;
import com.example.element.ElementManager;
import com.example.gui.ElementGui;
import com.example.util.CooldownManager;
import com.example.util.Text;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Registers all server events of the mod.
 */
final class ElementEvents {
	private ElementEvents() {
	}

	static void register() {
		ServerLifecycleEvents.SERVER_STARTING.register(Elementsmp::setServer);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			Elementsmp.tempBlocks().clearAll();
			Elementsmp.scheduler().clearAll();
			Elementsmp.store().save();
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			ElementManager.assignIfAbsent(player);
			ElementManager.ensureFocus(player);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
			ElementManager.forget(handler.getPlayer().getUUID()));

		// Only kills of other players count for the Nuke element.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer victim
				&& source.getEntity() instanceof ServerPlayer killer
				&& killer != victim
				&& Elementsmp.store().get(killer.getUUID()) == Element.NUKE) {
				int kills = Elementsmp.store().addKill(killer.getUUID());
				killer.displayClientMessage(Text.bold("+1 kill (" + kills + ")", ChatFormatting.RED), true);
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
			ElementManager.ensureFocus(newPlayer));

		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (!(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.PASS;
			}
			ItemStack stack = serverPlayer.getItemInHand(hand);
			if (!ElementManager.isFocus(stack)) {
				return InteractionResult.PASS;
			}
			if (serverPlayer.isShiftKeyDown()) {
				ElementManager.cycle(serverPlayer);
			} else {
				ElementManager.castSelected(serverPlayer);
			}
			return InteractionResult.SUCCESS;
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int tick = server.getTickCount();
			Elementsmp.tempBlocks().tick(tick);
			Elementsmp.scheduler().tick(tick);
			if (tick % 5 == 0) {
				actionBar(server.getPlayerList().getPlayers());
			}
			if (tick % 10 == 0) {
				ElementGui.refreshOpen(server);
			}
		});
	}

	/** Shows the selected ability with its cooldown while the focus item is held. */
	private static void actionBar(List<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			if (!ElementManager.isFocus(player.getMainHandItem()) && !ElementManager.isFocus(player.getOffhandItem())) {
				continue;
			}
			Element element = Elementsmp.store().get(player.getUUID());
			if (element == null) {
				continue;
			}
			List<Ability> abilities = AbilityRegistry.get(element);
			if (abilities.isEmpty()) {
				continue;
			}
			Ability ability = abilities.get(ElementManager.selectedIndex(player));
			int remaining = ElementManager.remainingTicks(player, ability);
			String suffix = element == Element.NUKE ? " | Kills: " + Elementsmp.store().getKills(player.getUUID()) : "";
			if (remaining > 0) {
				player.displayClientMessage(Text.plain(ability.name() + " | " + CooldownManager.format(remaining) + suffix, ChatFormatting.RED), true);
			} else {
				player.displayClientMessage(Text.plain(ability.name() + " | ready" + suffix, element.color()), true);
			}
		}
	}
}
