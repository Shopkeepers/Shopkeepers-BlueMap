package de.blablubbabc.shopkeepers.bluemap.command;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;

import de.blablubbabc.shopkeepers.bluemap.ShopkeepersBlueMapPermission;
import de.blablubbabc.shopkeepers.bluemap.ShopkeepersBlueMapPlugin;
import de.blablubbabc.shopkeepers.bluemap.command.lib.Command;

public class ReloadCommand extends Command {

	private final ShopkeepersBlueMapPlugin plugin;

	public ReloadCommand(ShopkeepersBlueMapPlugin plugin) {
		super(
				"reload",
				Collections.emptyList(),
				ShopkeepersBlueMapPermission.RELOAD,
				"Reloads this plugin."
		);
		this.plugin = plugin;
	}

	@Override
	protected void execute(CommandSender sender, List<? extends String> args) {
		plugin.reload();
		sender.sendMessage(c("&aPlugin reloaded!"));
	}
}
