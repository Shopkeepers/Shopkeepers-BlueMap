package de.blablubbabc.shopkeepers.bluemap;

public class Settings {

	private final ShopkeepersBlueMapPlugin plugin;

	public Settings(ShopkeepersBlueMapPlugin plugin) {
		this.plugin = plugin;
	}

	public boolean isEnabled() {
		return plugin.getConfig().getBoolean("enabled");
	}

	public boolean isDebugging() {
		return plugin.getConfig().getBoolean("debug");
	}

	public String getMarkerSetName() {
		return plugin.getConfig().getString("marker-set-name");
	}

	public String getMarkerIcon(String shopTypeId) {
		return plugin.getConfig().getString("markers." + shopTypeId + ".icon", "");
	}

	public int getMarkerAnchorX(String shopTypeId) {
		return plugin.getConfig().getInt("markers." + shopTypeId + ".anchor-x");
	}

	public int getMarkerAnchorY(String shopTypeId) {
		return plugin.getConfig().getInt("markers." + shopTypeId + ".anchor-y");
	}

	public String getMarkerLabel(String shopTypeId) {
		return plugin.getConfig().getString("markers." + shopTypeId + ".label", "");
	}

	public String getMarkerDetailText() {
		return plugin.getConfig().getString("marker-detail");
	}
}
