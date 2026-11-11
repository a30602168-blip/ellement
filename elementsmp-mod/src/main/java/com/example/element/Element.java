package com.example.element;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Locale;

/**
 * The eight elements of the server.
 */
public enum Element {
	WIND("wind", "Wind", ChatFormatting.AQUA, Items.FEATHER, 1.0, false,
		"Fast and agile. Controls space but deals little damage."),
	SAND("sand", "Sand", ChatFormatting.YELLOW, Items.GLOWSTONE_DUST, 1.0, false,
		"The controller. Blinds, slows and blocks, but has little damage potential."),
	LAVA("lava", "Lava", ChatFormatting.GOLD, Items.BLAZE_ROD, 1.0, false,
		"High fire damage, but little mobility and almost no healing."),
	WATER("water", "Water", ChatFormatting.BLUE, Items.HEART_OF_THE_SEA, 1.0, false,
		"The supporter. Heals and pushes back, but deals little damage."),
	EARTH("earth", "Earth", ChatFormatting.GREEN, Items.FLINT, 1.0, false,
		"The tank. Walls and armor, but slow with no way to escape."),
	VOID("void", "Void", ChatFormatting.DARK_PURPLE, Items.ECHO_SHARD, 1.0, false,
		"Teleportation and dark energy. Powerful, but the power costs health."),
	NUKE("nuke", "Nuke", ChatFormatting.RED, Items.NETHER_STAR, 0.05, true,
		"LEGENDARY. Weak at first, but every player kill unlocks more destructive power."),
	ORBITAL("orbital", "Orbital", ChatFormatting.DARK_AQUA, Items.AMETHYST_SHARD, 0.08, false,
		"Rare. Calls down devastating strikes from orbit and drills tunnels with piercing blasts.");

	private final String key;
	private final String displayName;
	private final ChatFormatting color;
	private final Item icon;
	private final double defaultProbability;
	private final boolean legendary;
	private final String description;

	Element(String key, String displayName, ChatFormatting color, Item icon,
			double defaultProbability, boolean legendary, String description) {
		this.key = key;
		this.displayName = displayName;
		this.color = color;
		this.icon = icon;
		this.defaultProbability = defaultProbability;
		this.legendary = legendary;
		this.description = description;
	}

	public String key() {
		return key;
	}

	public String displayName() {
		return displayName;
	}

	public ChatFormatting color() {
		return color;
	}

	public Item icon() {
		return icon;
	}

	/** Default base probability (used when the config has no entry). */
	public double defaultProbability() {
		return defaultProbability;
	}

	/** Legendary elements get a special title when pulled. */
	public boolean legendary() {
		return legendary;
	}

	public String description() {
		return description;
	}

	/** Finds an element by its key (also accepts the old German keys). */
	public static Element fromKey(String input) {
		if (input == null) {
			return null;
		}
		String lower = input.toLowerCase(Locale.ROOT);
		if (lower.equals("wasser")) {
			return WATER;
		}
		if (lower.equals("erde")) {
			return EARTH;
		}
		for (Element element : values()) {
			if (element.key.equals(lower) || element.name().toLowerCase(Locale.ROOT).equals(lower)) {
				return element;
			}
		}
		return null;
	}
}
