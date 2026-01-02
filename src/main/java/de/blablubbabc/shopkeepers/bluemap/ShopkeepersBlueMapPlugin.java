package de.blablubbabc.shopkeepers.bluemap;

import org.bukkit.plugin.java.JavaPlugin;

public class ShopkeepersBlueMapPlugin extends JavaPlugin {

	private final Settings settings = new Settings(this);
	private final ShopkeepersBlueMap shopkeepersBlueMap = new ShopkeepersBlueMap(this);

	@Override
	public void onLoad() {
		this.saveDefaultConfig();
	}

	@Override
	public void onEnable() {
		shopkeepersBlueMap.enable();
	}

	@Override
	public void onDisable() {
		shopkeepersBlueMap.disable();
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
