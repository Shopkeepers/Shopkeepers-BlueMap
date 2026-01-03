package de.blablubbabc.shopkeepers.bluemap.command;

import de.blablubbabc.shopkeepers.bluemap.ShopkeepersBlueMapPlugin;

public class Commands {

	private final ShopkeepersBlueMapPlugin plugin;

	public Commands(ShopkeepersBlueMapPlugin plugin) {
		this.plugin = plugin;
	}

	public void register() {
		ShopkeepersBlueMapCommand.register(plugin);
	}
}
