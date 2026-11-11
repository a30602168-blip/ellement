package com.example.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/**
 * Helper functions for text and item display.
 */
public final class Text {
	private Text() {
	}

	/** Text without the default italics of item names and lore. */
	public static MutableComponent plain(String text, ChatFormatting color) {
		return Component.literal(text).withStyle(Style.EMPTY.withItalic(false).applyFormat(color));
	}

	public static MutableComponent bold(String text, ChatFormatting color) {
		return Component.literal(text).withStyle(Style.EMPTY.withItalic(false).withBold(true).applyFormat(color));
	}

	public static ItemStack item(Item item, Component name, List<Component> lore, boolean glint) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.CUSTOM_NAME, name);
		if (!lore.isEmpty()) {
			stack.set(DataComponents.LORE, new ItemLore(lore));
		}
		if (glint) {
			stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
		return stack;
	}

	public static MutableComponent chat(String text, ChatFormatting color) {
		return Component.literal(text).withStyle(color);
	}
}
