package com.example;

import com.example.command.ElementCommand;
import com.example.config.ConfigManager;
import com.example.config.ElementConfig;
import com.example.config.PlayerDataStore;
import com.example.util.CooldownManager;
import com.example.util.TaskScheduler;
import com.example.util.TemporaryBlocks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Elements SMP: purely server-side Fabric mod. Clients do not need the mod.
 */
public class Elementsmp implements ModInitializer {
	public static final String MOD_ID = "elementsmp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final CooldownManager COOLDOWNS = new CooldownManager();
	private static final TemporaryBlocks TEMP_BLOCKS = new TemporaryBlocks();
	private static final TaskScheduler SCHEDULER = new TaskScheduler();

	private static MinecraftServer server;
	private static ConfigManager configManager;
	private static PlayerDataStore store;

	@Override
	public void onInitialize() {
		Path dir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
		try {
			Files.createDirectories(dir);
		} catch (IOException ex) {
			LOGGER.error("Config folder could not be created", ex);
		}
		configManager = new ConfigManager(dir);
		configManager.load();
		store = new PlayerDataStore(dir.resolve("players.json"));
		store.load();

		ElementEvents.register();
		ElementCommand.register();
		LOGGER.info("Elements SMP loaded (server-side, no client mod needed).");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	static void setServer(MinecraftServer minecraftServer) {
		server = minecraftServer;
	}

	public static MinecraftServer server() {
		return server;
	}

	public static ElementConfig config() {
		return configManager.config();
	}

	public static boolean reloadConfig() {
		return configManager.load();
	}

	public static PlayerDataStore store() {
		return store;
	}

	public static CooldownManager cooldowns() {
		return COOLDOWNS;
	}

	public static TemporaryBlocks tempBlocks() {
		return TEMP_BLOCKS;
	}

	public static TaskScheduler scheduler() {
		return SCHEDULER;
	}
}
