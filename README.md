# Shopkeepers BlueMap Integration ![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/Shopkeepers/Shopkeepers-BlueMap/total?style=flat-square&color=green)


This is a Bukkit plugin that shows shopkeepers as markers on the [BlueMap](https://www.spigotmc.org/resources/bluemap.83557/).

## Prerequisites

- An up-to-date [Spigot](https://www.spigotmc.org/) or [Paper](https://papermc.io/) Minecraft server. Tested on: Spigot 1.21.11
- [Shopkeepers plugin](https://www.spigotmc.org/resources/shopkeepers.80756/). Tested on: 2.25.0
- [BlueMap plugin](https://www.spigotmc.org/resources/bluemap.83557/). Tested on: 5.15

## Installation

- Drop the plugin jar into your Bukkit server's `plugins` folder.
- Restart your server.
- Adjust the `plugins/Shopkeepers-BlueMap/config.yml` file as needed.

## Commands

Base command: `/shopkeepers-bluemap`  
Aliases: `shopkeeper-bluemap`, `skbm`

- `/shopkeepers-bluemap help`: Shows the help page.  
  Permission: `shopkeepers-bluemap.help` (default: `true`)
- `/shopkeepers-bluemap reload`: Reloads the plugin and config.  
  Permission: `shopkeepers-bluemap.reload` (default: `op`)
