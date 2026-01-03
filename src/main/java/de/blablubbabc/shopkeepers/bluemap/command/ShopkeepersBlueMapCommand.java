package de.blablubbabc.shopkeepers.bluemap.command;

import de.blablubbabc.shopkeepers.bluemap.ShopkeepersBlueMapPermission;
import de.blablubbabc.shopkeepers.bluemap.ShopkeepersBlueMapPlugin;
import de.blablubbabc.shopkeepers.bluemap.command.lib.RootCommand;

class ShopkeepersBlueMapCommand extends RootCommand {

	private static final String COMMAND_NAME = "shopkeepers-bluemap";

	public static ShopkeepersBlueMapCommand register(ShopkeepersBlueMapPlugin plugin) {
		var bukkitCommand = plugin.getCommand(COMMAND_NAME);
		return new ShopkeepersBlueMapCommand(plugin, bukkitCommand);
	}

	private ShopkeepersBlueMapCommand(
			ShopkeepersBlueMapPlugin plugin,
			org.bukkit.command.PluginCommand bukkitCommand
	) {
		super(
				plugin,
				bukkitCommand,
				ShopkeepersBlueMapPermission.HELP
		);

		this.addSubCommand(new HelpCommand(this));
		this.addSubCommand(new ReloadCommand(plugin));
	}
}
