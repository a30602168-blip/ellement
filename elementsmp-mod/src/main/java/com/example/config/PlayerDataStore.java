package com.example.config;

import com.example.Elementsmp;
import com.example.element.Element;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores the element assignment per player UUID in players.json.
 */
public final class PlayerDataStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() { }.getType();

	private final Path file;
	private final Path killsFile;
	private final Map<UUID, Element> elements = new HashMap<>();
	private final Map<UUID, Integer> kills = new HashMap<>();

	public PlayerDataStore(Path file) {
		this.file = file;
		this.killsFile = file.resolveSibling("kills.json");
	}

	public synchronized void load() {
		loadKills();
		elements.clear();
		if (!Files.exists(file)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			Map<String, String> raw = GSON.fromJson(reader, MAP_TYPE);
			if (raw == null) {
				return;
			}
			for (Map.Entry<String, String> entry : raw.entrySet()) {
				try {
					Element element = Element.fromKey(entry.getValue());
					if (element != null) {
						elements.put(UUID.fromString(entry.getKey()), element);
					}
				} catch (IllegalArgumentException ex) {
					Elementsmp.LOGGER.warn("Invalid entry in players.json: {}", entry.getKey());
				}
			}
		} catch (IOException | JsonParseException ex) {
			Elementsmp.LOGGER.error("players.json could not be read", ex);
		}
	}

	public synchronized void save() {
		Map<String, String> raw = new HashMap<>();
		for (Map.Entry<UUID, Element> entry : elements.entrySet()) {
			raw.put(entry.getKey().toString(), entry.getValue().key());
		}
		Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(tmp)) {
				GSON.toJson(raw, writer);
			}
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ex) {
			Elementsmp.LOGGER.error("players.json could not be saved", ex);
		}
	}

	public synchronized Element get(UUID uuid) {
		return elements.get(uuid);
	}

	public synchronized void set(UUID uuid, Element element) {
		elements.put(uuid, element);
		save();
	}

	public synchronized void remove(UUID uuid) {
		if (elements.remove(uuid) != null) {
			save();
		}
	}

	/** Number of already assigned elements. */
	public synchronized Map<Element, Integer> counts() {
		Map<Element, Integer> counts = new EnumMap<>(Element.class);
		for (Element element : elements.values()) {
			counts.merge(element, 1, Integer::sum);
		}
		return counts;
	}

	// ------------------------------------------------------------ player kills (Nuke element)

	public synchronized int getKills(UUID uuid) {
		return kills.getOrDefault(uuid, 0);
	}

	public synchronized void setKills(UUID uuid, int amount) {
		if (amount <= 0) {
			kills.remove(uuid);
		} else {
			kills.put(uuid, amount);
		}
		saveKills();
	}

	public synchronized int addKill(UUID uuid) {
		int value = kills.merge(uuid, 1, Integer::sum);
		saveKills();
		return value;
	}

	public synchronized void spendKills(UUID uuid, int amount) {
		if (amount <= 0) {
			return;
		}
		int value = Math.max(0, kills.getOrDefault(uuid, 0) - amount);
		if (value == 0) {
			kills.remove(uuid);
		} else {
			kills.put(uuid, value);
		}
		saveKills();
	}

	private void loadKills() {
		kills.clear();
		if (!Files.exists(killsFile)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(killsFile)) {
			Map<String, Integer> raw = GSON.fromJson(reader, new TypeToken<Map<String, Integer>>() { }.getType());
			if (raw == null) {
				return;
			}
			for (Map.Entry<String, Integer> entry : raw.entrySet()) {
				try {
					kills.put(UUID.fromString(entry.getKey()), entry.getValue());
				} catch (IllegalArgumentException ex) {
					Elementsmp.LOGGER.warn("Invalid entry in kills.json: {}", entry.getKey());
				}
			}
		} catch (IOException | JsonParseException ex) {
			Elementsmp.LOGGER.error("kills.json could not be read", ex);
		}
	}

	private void saveKills() {
		Map<String, Integer> raw = new HashMap<>();
		for (Map.Entry<UUID, Integer> entry : kills.entrySet()) {
			raw.put(entry.getKey().toString(), entry.getValue());
		}
		Path tmp = killsFile.resolveSibling(killsFile.getFileName() + ".tmp");
		try {
			Files.createDirectories(killsFile.getParent());
			try (Writer writer = Files.newBufferedWriter(tmp)) {
				GSON.toJson(raw, writer);
			}
			Files.move(tmp, killsFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ex) {
			Elementsmp.LOGGER.error("kills.json could not be saved", ex);
		}
	}
}
