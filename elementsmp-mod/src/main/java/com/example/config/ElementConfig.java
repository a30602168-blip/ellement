package com.example.config;

import com.example.ability.Ability;
import com.example.ability.AbilityRegistry;
import com.example.element.Element;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Content of config.json.
 */
public class ElementConfig {
	/** Base probability per element (key = element key). */
	public Map<String, Double> probabilities = new LinkedHashMap<>();
	/** 0 = pure chance by probability. Higher = elements that were already assigned often are rolled less often. */
	public double balanceStrength = 0.6;
	/** Automatically gives players their focus item. */
	public boolean giveFocusItem = true;
	/** If true, abilities with a kill requirement spend those kills when used. */
	public boolean consumeKills = true;
	/** If true, Orbital explosions destroy blocks. */
	public boolean orbitalBlockDamage = true;
	/** Settings per ability (key = ability id). */
	public Map<String, AbilitySettings> abilities = new LinkedHashMap<>();

	/** Fills in missing entries with default values. */
	public void fillDefaults() {
		if (probabilities == null) {
			probabilities = new LinkedHashMap<>();
		}
		if (abilities == null) {
			abilities = new LinkedHashMap<>();
		}
		for (Element element : Element.values()) {
			probabilities.putIfAbsent(element.key(), element.defaultProbability());
		}
		for (Element element : Element.values()) {
			for (Ability ability : AbilityRegistry.get(element)) {
				abilities.putIfAbsent(ability.id(), ability.defaults().copy());
			}
		}
	}

	public AbilitySettings settingsFor(Ability ability) {
		AbilitySettings s = abilities.get(ability.id());
		return s != null ? s : ability.defaults();
	}

	public double probabilityOf(Element element) {
		Double v = probabilities.get(element.key());
		return v == null ? element.defaultProbability() : Math.max(0.0, v);
	}
}
