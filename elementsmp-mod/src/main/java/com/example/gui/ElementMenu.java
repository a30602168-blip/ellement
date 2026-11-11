package com.example.gui;

import com.example.Elementsmp;
import com.example.ability.Ability;
import com.example.ability.AbilityRegistry;
import com.example.config.AbilitySettings;
import com.example.element.Element;
import com.example.element.ElementManager;
import com.example.util.CooldownManager;
import com.example.util.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Purely server-side chest GUI. Nothing in it can be taken out.
 */
public final class ElementMenu extends ChestMenu {
	private static final int[] ABILITY_SLOTS = {11, 13, 15};

	private final ServerPlayer owner;
	private final SimpleContainer content;

	public ElementMenu(int containerId, Inventory inventory, SimpleContainer content, ServerPlayer owner) {
		super(MenuType.GENERIC_9x3, containerId, inventory, content, 3);
		this.owner = owner;
		this.content = content;
		refresh();
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		// Block all clicks and resync the client.
		this.broadcastFullState();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	/** Rebuilds the content (also for cooldown displays). */
	public void refresh() {
		ItemStack pane = Text.item(Items.GRAY_STAINED_GLASS_PANE, Component.literal(" "), List.of(), false);
		for (int i = 0; i < content.getContainerSize(); i++) {
			content.setItem(i, pane.copy());
		}
		Element element = Elementsmp.store().get(owner.getUUID());
		if (element == null) {
			content.setItem(13, Text.item(Items.BARRIER, Text.bold("No element yet", ChatFormatting.RED), List.of(), false));
			return;
		}
		List<Ability> abilities = AbilityRegistry.get(element);
		int selected = ElementManager.selectedIndex(owner);

		List<Component> headLore = new ArrayList<>();
		headLore.add(Text.plain(element.description(), ChatFormatting.GRAY));
		headLore.add(Component.empty());
		headLore.add(Text.plain("Abilities: " + abilities.size(), ChatFormatting.WHITE));
		if (element == Element.NUKE) {
			headLore.add(Text.plain("Player kills: " + Elementsmp.store().getKills(owner.getUUID()), ChatFormatting.RED));
		}
		if (!abilities.isEmpty()) {
			headLore.add(Text.plain("Active: " + abilities.get(selected).name(), element.color()));
		}
		content.setItem(4, Text.item(element.icon(), Text.bold("Element: " + element.displayName(), element.color()), headLore, true));

		for (int i = 0; i < abilities.size() && i < ABILITY_SLOTS.length; i++) {
			Ability ability = abilities.get(i);
			AbilitySettings settings = ElementManager.settings(ability);
			int remaining = ElementManager.remainingTicks(owner, ability);

			List<Component> lore = new ArrayList<>();
			lore.add(Text.plain(ability.description(), ChatFormatting.GRAY));
			lore.add(Component.empty());
			lore.add(Text.plain("Cooldown: " + trim(settings.cooldownSeconds) + "s", ChatFormatting.WHITE));
			int required = settings.killsRequired;
			int kills = Elementsmp.store().getKills(owner.getUUID());
			if (required > 0) {
				lore.add(Text.plain("Kills required: " + required + " (you have " + kills + ")",
					kills >= required ? ChatFormatting.GREEN : ChatFormatting.RED));
			}
			if (remaining > 0) {
				lore.add(Text.plain("Remaining " + CooldownManager.format(remaining), ChatFormatting.RED));
			} else if (required > 0 && kills < required) {
				lore.add(Text.plain("Not enough kills", ChatFormatting.RED));
			} else {
				lore.add(Text.plain("Ready", ChatFormatting.GREEN));
			}
			if (i == selected) {
				lore.add(Component.empty());
				lore.add(Text.plain("Currently selected", ChatFormatting.YELLOW));
			}
			content.setItem(ABILITY_SLOTS[i], Text.item(ability.icon(),
				Text.bold((i + 1) + ". " + ability.name(), element.color()), lore, i == selected));
		}

		List<Component> help = new ArrayList<>();
		help.add(Text.plain("Right-click with the focus item: cast", ChatFormatting.YELLOW));
		help.add(Text.plain("Shift + right-click: switch ability", ChatFormatting.YELLOW));
		help.add(Text.plain("/element cast <1-3>: cast directly", ChatFormatting.YELLOW));
		help.add(Text.plain("/element focus: get a new focus item", ChatFormatting.YELLOW));
		content.setItem(22, Text.item(Items.BOOK, Text.bold("Controls", ChatFormatting.WHITE), help, false));
	}

	private static String trim(double value) {
		return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
	}
}
