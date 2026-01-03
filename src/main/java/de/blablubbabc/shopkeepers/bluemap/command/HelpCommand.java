package de.blablubbabc.shopkeepers.bluemap.command;

import java.util.Arrays;
import java.util.List;

import org.bukkit.command.CommandSender;

import de.blablubbabc.shopkeepers.bluemap.ShopkeepersBlueMapPermission;
import de.blablubbabc.shopkeepers.bluemap.command.lib.Command;
import de.blablubbabc.shopkeepers.bluemap.command.lib.RootCommand;

public class HelpCommand extends Command {

	private final RootCommand rootCommand;

	public HelpCommand(RootCommand rootCommand) {
		super(
				"help",
				Arrays.asList("?"),
				ShopkeepersBlueMapPermission.HELP,
				"Shows this help page."
		);
		this.rootCommand = rootCommand;
	}

	@Override
	protected void execute(CommandSender sender, List<? extends String> args) {
		rootCommand.sendHelp(sender);
	}
}
