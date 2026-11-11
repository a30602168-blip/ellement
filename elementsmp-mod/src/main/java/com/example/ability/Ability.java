package com.example.ability;

import com.example.config.AbilitySettings;
import net.minecraft.world.item.Item;

public record Ability(String id, String name, String description, Item icon, AbilitySettings defaults, AbilityAction action) {
}
