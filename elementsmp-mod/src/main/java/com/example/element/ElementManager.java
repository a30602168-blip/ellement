package com.example.element;

import com.example.Elementsmp;
import com.example.ability.Ability;
import com.example.ability.AbilityRegistry;
import com.example.config.AbilitySettings;
import com.example.config.ElementConfig;
import com.example.util.CooldownManager;
import com.example.util.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Core logic: element assignment, focus item, casting abilities.
 */
public final class ElementManager {
	private static final String FOCUS_TAG = "elementsmp_focus";
	private static final Map<UUID, Integer> SELECTED = new HashMap<>();

	private ElementManager() {
	}

	// ---------------------------------------------------------------- Assignment

	/** Rolls an element based on configured probabilities and already assigned elements. */
	public static Element roll() {
		ElementConfig config = Elementsmp.config();
		Map<Element, Integer> counts = Elementsmp.store().counts();
		Element[] all = Element.values();
		double[] weights = new double[all.length];
		double total = 0;
		for (int i = 0; i < all.length; i++) {
			double base = config.probabilityOf(all[i]);
			int assigned = counts.getOrDefault(all[i], 0);
			weights[i] = base / (1.0 + Math.max(0.0, config.balanceStrength) * assigned);
			total += weights[i];
		}
		ThreadLocalRandom random = ThreadLocalRandom.current();
		if (total <= 0) {
			return all[random.nextInt(all.length)];
		}
		double r = random.nextDouble() * total;
		for (int i = 0; i < all.length; i++) {
			r -= weights[i];
			if (r < 0) {
				return all[i];
			}
		}
		return all[all.length - 1];
	}

	/** Assigns an element to a player on first join. Existing assignments stay unchanged. */
	public static synchronized Element assignIfAbsent(ServerPlayer player) {
		Element existing = Elementsmp.store().get(player.getUUID());
		if (existing != null) {
			return existing;
		}
		Element rolled = roll();
		Elementsmp.store().set(player.getUUID(), rolled);
		if (rolled.legendary()) {
			announceLegendary(player, rolled);
		}
		player.sendSystemMessage(Text.chat("You have been given the element ", ChatFormatting.GRAY)
			.append(Text.chat(rolled.displayName(), rolled.color()).withStyle(ChatFormatting.BOLD))
			.append(Text.chat(". Use /element or your focus item.", ChatFormatting.GRAY)));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
			SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
		return rolled;
	}

	/** Sets an element and updates the focus item. */
	public static void setElement(ServerPlayer player, Element element) {
		Elementsmp.store().set(player.getUUID(), element);
		SELECTED.remove(player.getUUID());
		Elementsmp.cooldowns().clear(player.getUUID());
		replaceFocus(player);
	}

	/** Removes the assignment and rolls again immediately (player is online). */
	public static Element reset(ServerPlayer player) {
		Elementsmp.store().remove(player.getUUID());
		SELECTED.remove(player.getUUID());
		Elementsmp.cooldowns().clear(player.getUUID());
		Element rolled = assignIfAbsent(player);
		replaceFocus(player);
		return rolled;
	}

	public static void forget(UUID uuid) {
		SELECTED.remove(uuid);
		Elementsmp.cooldowns().clear(uuid);
	}

	// ---------------------------------------------------------------- Focus item

	public static boolean isFocus(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.copyTag().contains(FOCUS_TAG);
	}

	private static ItemStack createFocus(Element element) {
		List<Component> lore = new ArrayList<>();
		lore.add(Text.plain(element.description(), ChatFormatting.GRAY));
		lore.add(Component.empty());
		lore.add(Text.plain("Right-click: cast ability", ChatFormatting.YELLOW));
		lore.add(Text.plain("Shift + right-click: switch ability", ChatFormatting.YELLOW));
		ItemStack stack = Text.item(element.icon(),
			Text.bold("Focus: " + element.displayName(), element.color()), lore, true);
		CompoundTag tag = new CompoundTag();
		tag.putBoolean(FOCUS_TAG, true);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return stack;
	}

	private static boolean hasFocus(ServerPlayer player) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (isFocus(inv.getItem(i))) {
				return true;
			}
		}
		return false;
	}

	private static void removeFocus(ServerPlayer player) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (isFocus(inv.getItem(i))) {
				inv.setItem(i, ItemStack.EMPTY);
			}
		}
	}

	private static void giveFocus(ServerPlayer player, Element element) {
		ItemStack stack = createFocus(element);
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}

	/** Gives the focus item if the player has none. */
	public static void ensureFocus(ServerPlayer player) {
		Element element = Elementsmp.store().get(player.getUUID());
		if (element == null || !Elementsmp.config().giveFocusItem || hasFocus(player)) {
			return;
		}
		giveFocus(player, element);
	}

	/** Replaces existing focus items with one of the current element. */
	public static void replaceFocus(ServerPlayer player) {
		removeFocus(player);
		Element element = Elementsmp.store().get(player.getUUID());
		if (element != null && Elementsmp.config().giveFocusItem) {
			giveFocus(player, element);
		}
	}

	/** Always gives a (new) focus item, e.g. via /element focus. */
	public static void forceFocus(ServerPlayer player) {
		Element element = Elementsmp.store().get(player.getUUID());
		if (element != null) {
			removeFocus(player);
			giveFocus(player, element);
		}
	}

	// ---------------------------------------------------------------- Abilities

	public static int selectedIndex(ServerPlayer player) {
		Element element = Elementsmp.store().get(player.getUUID());
		if (element == null) {
			return 0;
		}
		int size = AbilityRegistry.get(element).size();
		int index = SELECTED.getOrDefault(player.getUUID(), 0);
		return size == 0 ? 0 : Math.floorMod(index, size);
	}

	public static void cycle(ServerPlayer player) {
		Element element = Elementsmp.store().get(player.getUUID());
		if (element == null) {
			return;
		}
		List<Ability> abilities = AbilityRegistry.get(element);
		if (abilities.isEmpty()) {
			return;
		}
		int next = (selectedIndex(player) + 1) % abilities.size();
		SELECTED.put(player.getUUID(), next);
		Ability ability = abilities.get(next);
		player.displayClientMessage(Text.plain("Selected: ", ChatFormatting.GRAY)
			.append(Text.bold(ability.name(), element.color())), true);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
			SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.4f);
	}

	public static AbilitySettings settings(Ability ability) {
		return Elementsmp.config().settingsFor(ability);
	}

	public static int remainingTicks(ServerPlayer player, Ability ability) {
		return Elementsmp.cooldowns().remainingTicks(player.getUUID(), ability.id(), Elementsmp.server().getTickCount());
	}

	/** Casts the ability with the given index (0-based). */
	public static void cast(ServerPlayer player, int index) {
		Element element = Elementsmp.store().get(player.getUUID());
		if (element == null) {
			player.sendSystemMessage(Text.chat("You do not have an element yet.", ChatFormatting.RED));
			return;
		}
		List<Ability> abilities = AbilityRegistry.get(element);
		if (index < 0 || index >= abilities.size()) {
			return;
		}
		Ability ability = abilities.get(index);
		int now = Elementsmp.server().getTickCount();
		int remaining = Elementsmp.cooldowns().remainingTicks(player.getUUID(), ability.id(), now);
		if (remaining > 0) {
			player.displayClientMessage(Text.plain(ability.name() + ": " + CooldownManager.format(remaining), ChatFormatting.RED), true);
			return;
		}
		AbilitySettings settings = settings(ability);
		int required = settings.killsRequired;
		int kills = Elementsmp.store().getKills(player.getUUID());
		if (required > 0 && kills < required) {
			player.displayClientMessage(Text.plain(ability.name() + " needs " + required + " kills (you have " + kills + ")", ChatFormatting.RED), true);
			return;
		}
		boolean success;
		try {
			success = ability.action().cast(player, (ServerLevel) player.level(), settings);
		} catch (RuntimeException ex) {
			Elementsmp.LOGGER.error("Error in ability {}", ability.id(), ex);
			success = false;
		}
		if (success) {
			Elementsmp.cooldowns().start(player.getUUID(), ability.id(), now, settings.cooldownSeconds);
			if (required > 0 && Elementsmp.config().consumeKills) {
				Elementsmp.store().spendKills(player.getUUID(), required);
			}
			player.displayClientMessage(Text.bold(ability.name(), element.color()), true);
		} else {
			player.displayClientMessage(Text.plain(ability.name() + " could not be cast.", ChatFormatting.RED), true);
		}
	}

	/** Shows the "LEGENDARY PULL" title on the player's screen and announces it to the server. */
	private static void announceLegendary(ServerPlayer player, Element element) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 30));
		player.connection.send(new ClientboundSetSubtitleTextPacket(
			Text.plain("You pulled the element " + element.displayName() + "!", element.color())));
		player.connection.send(new ClientboundSetTitleTextPacket(Text.bold("LEGENDARY PULL", ChatFormatting.GOLD)));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
			SoundEvents.ENDER_DRAGON_GROWL, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
		Elementsmp.server().getPlayerList().broadcastSystemMessage(
			Text.chat(player.getName().getString() + " pulled the ", ChatFormatting.GRAY)
				.append(Text.chat("LEGENDARY", ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD))
				.append(Text.chat(" element ", ChatFormatting.GRAY))
				.append(Text.chat(element.displayName(), element.color()).withStyle(ChatFormatting.BOLD))
				.append(Text.chat("!", ChatFormatting.GRAY)), false);
	}

	public static void castSelected(ServerPlayer player) {
		cast(player, selectedIndex(player));
	}
}
