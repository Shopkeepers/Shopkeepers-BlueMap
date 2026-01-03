package de.blablubbabc.shopkeepers.bluemap.command.lib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public class RootCommand extends Command implements CommandExecutor, TabCompleter {

	private final JavaPlugin plugin;
	private final List<Command> subCommands = new ArrayList<>();

	public RootCommand(
			JavaPlugin plugin,
			org.bukkit.command.PluginCommand bukkitCommand,
			String permission
	) {
		super(
				bukkitCommand.getName(),
				bukkitCommand.getAliases(),
				permission,
				bukkitCommand.getDescription()
		);
		this.plugin = plugin;

		bukkitCommand.setExecutor(this);
		bukkitCommand.setTabCompleter(this);
	}

	protected final void addSubCommand(Command command) {
		subCommands.add(command);
	}

	@Override
	public boolean onCommand(
			CommandSender sender,
			org.bukkit.command.Command bukkitCommand,
			String label,
			String[] args
	) {
		var arguments = Arrays.asList(args);
		if (!arguments.isEmpty()) {
			var firstArgument = arguments.getFirst();
			var subCommand = this.findSubCommand(firstArgument);
			if (subCommand != null) {
				var subArguments = arguments.subList(1, arguments.size());
				subCommand.invoke(sender, subArguments);
				return true;
			}
		}

		// No arguments specified or no matching sub-command found:
		this.invoke(sender, arguments);
		return true;
	}

	private Command findSubCommand(String input) {
		var normalizedInput = input.toLowerCase(Locale.ROOT);
		for (var command : subCommands) {
			if (command.matches(normalizedInput)) {
				return command;
			}
		}

		return null;
	}

	// Invoked when no argument is specified or no matching sub-command was found.
	@Override
	protected void execute(CommandSender sender, List<? extends String> args) {
		if (!args.isEmpty()) {
			sender.sendMessage(c("&cUnknown command: &e" + args.getFirst()));
		}

		this.sendHelp(sender);
	}

	@Override
	public List<String> onTabComplete(
			CommandSender sender,
			org.bukkit.command.Command bukkitCommand,
			String label,
			String[] args
	) {
		var firstArgument = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
		var completions = new ArrayList<String>();
		for (var subCommand : subCommands) {
			var subCommandName = subCommand.getName();
			var nameCompleted = false;
			if (subCommandName.startsWith(firstArgument)) {
				nameCompleted = true;
				completions.add(subCommandName);
			}

			// Include at most one command name completion:
			if (!nameCompleted) {
				for (var subCommandAlias : subCommand.getAliases()) {
					if (subCommandAlias.startsWith(firstArgument)) {
						completions.add(subCommandAlias);
						break; // Include at most one command name completion
					}
				}
			}
		}

		return completions;
	}

	public void sendHelp(CommandSender sender) {
		var pluginDescription = plugin.getDescription();
		var pluginName = pluginDescription.getName();
		var pluginVersion = pluginDescription.getVersion();
		var pluginWebsite = pluginDescription.getWebsite();

		sender.sendMessage(c("&8[&6" + pluginName + "&8]"));
		sender.sendMessage(c("&eVersion: &f" + pluginVersion));
		sender.sendMessage(c("&eWebsite: &f" + pluginWebsite));

		if (!subCommands.isEmpty()) {
			sender.sendMessage(c("&6Commands:"));
			for (var subCommand : subCommands) {
				var subCommandLabel = "/" + this.getName() + " " + subCommand.getName();
				var subCommandDescription = subCommand.getDescription();
				sender.sendMessage(c("&e" + subCommandLabel + "&8 - &f" + subCommandDescription));
			}
		}
	}
}
