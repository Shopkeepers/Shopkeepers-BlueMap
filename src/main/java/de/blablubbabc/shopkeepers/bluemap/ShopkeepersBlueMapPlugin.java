package de.blablubbabc.shopkeepers.bluemap;

import org.bukkit.plugin.java.JavaPlugin;

import de.blablubbabc.shopkeepers.bluemap.command.Commands;

public class ShopkeepersBlueMapPlugin extends JavaPlugin {

	private final Settings settings = new Settings(this);
	private final Commands commands = new Commands(this);
	private final ShopkeepersBlueMap shopkeepersBlueMap = new ShopkeepersBlueMap(this);

	@Override
	public void onLoad() {
	}

	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		this.reloadConfig();

		commands.register();

		shopkeepersBlueMap.enable();
	}

	@Override
	public void onDisable() {
		shopkeepersBlueMap.disable();
	}

	/**
	 * Reloads the plugin.
	 */
	public void reload() {
		this.onDisable();
		this.onEnable();
	}

	public Settings getSettings() {
		return settings;
	}

	public void debug(String message) {
		if (!settings.isDebugging()) {
			return;
		}

		this.getLogger().info(message);
	}
}
