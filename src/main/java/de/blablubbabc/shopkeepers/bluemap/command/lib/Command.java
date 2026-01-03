package de.blablubbabc.shopkeepers.bluemap.command.lib;

import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public abstract class Command {

	protected static String c(String text) {
		return ChatColor.translateAlternateColorCodes('&', text);
	}

	private final String name;
	private final List<? extends String> aliases; // Can be empty
	private final String permission; // Can be null
	private final String description;

	public Command(
			String name,
			List<? extends String> aliases,
			String permission,
			String description
	) {
		this.name = name;
		this.aliases = Collections.unmodifiableList(aliases);
		this.permission = permission;
		this.description = description;
	}

	public final String getName() {
		return name;
	}

	public final List<? extends String> getAliases() {
		return aliases;
	}

	public final String getDescription() {
		return description;
	}

	protected void noPermission(CommandSender sender) {
		sender.sendMessage(c("&cMissing permission."));
	}

	protected final boolean checkPermission(CommandSender sender) {
		return this.checkPermission(sender, permission);
	}

	protected final boolean checkPermission(CommandSender sender, String permission) {
		if (permission != null && !sender.hasPermission(permission)) {
			this.noPermission(sender);
			return false;
		}

		return true;
	}

	public final boolean matches(String input) {
		if (name.equals(input)) {
			return true;
		}

		for (var alias : aliases) {
			if (alias.equals(input)) {
				return true;
			}
		}

		return false;
	}

	public final void invoke(CommandSender sender, List<? extends String> args) {
		if (!this.checkPermission(sender)) {
			return;
		}

		this.execute(sender, args);
	}

	protected abstract void execute(CommandSender sender, List<? extends String> args);
}
