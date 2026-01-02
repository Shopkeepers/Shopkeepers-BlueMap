package de.blablubbabc.shopkeepers.bluemap;

import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;

public class ShopkeepersBlueMap {

	private static final String WEB_SHOPKEEPERS_ASSETS = "assets/shopkeepers";
	private static final String JAR_ASSETS = "assets";
	private static final String[] ASSETS = new String[] {
			"admin16.png",
			"admin24.png",
			"admin32.png",
			"book16.png",
			"book24.png",
			"book32.png",
			"buy16.png",
			"buy24.png",
			"buy32.png",
			"sell16.png",
			"sell24.png",
			"sell32.png",
			"trade16.png",
			"trade24.png",
			"trade32.png"
	};

	private static final String MARKERSET_ID = "shopkeepers.markerset";

	private final ShopkeepersBlueMapPlugin plugin;
	private final ShopkeepersListener shopkeeperListener = new ShopkeepersListener(this);

	private boolean enabled = false;

	private BlueMapAPI blueMapApi = null;

	public ShopkeepersBlueMap(ShopkeepersBlueMapPlugin plugin) {
		this.plugin = plugin;
	}

	public void enable() {
		if (enabled) {
			return;
		}

		if (!plugin.getSettings().isEnabled()) {
			return;
		}

		enabled = true;

		// Called immediately if the BlueMap API is currently enabled:
		BlueMapAPI.onEnable(this::onBlueMapEnabledAsync);
		BlueMapAPI.onDisable(this::onBlueMapDisabledAsync);

		Bukkit.getPluginManager().registerEvents(shopkeeperListener, plugin);
	}

	public void disable() {
		if (!enabled) {
			return;
		}

		HandlerList.unregisterAll(shopkeeperListener);

		BlueMapAPI.unregisterListener(this::onBlueMapEnabledAsync);
		BlueMapAPI.unregisterListener(this::onBlueMapDisabledAsync);

		this.removeAllShopkeepers();

		enabled = false;
	}

	// According to the documentation, this may be called off the main server thread!
	private void onBlueMapEnabledAsync(BlueMapAPI bluemap) {
		if (!plugin.isEnabled()) {
			return;
		}

		if (Bukkit.isPrimaryThread()) {
			this.onBlueMapEnabled(bluemap);
			return;
		}

		Bukkit.getScheduler().runTask(plugin, () -> {
			BlueMapAPI.getInstance().ifPresent(this::onBlueMapEnabled);
		});
	}

	// According to the documentation, this may be called off the main server thread!
	private void onBlueMapDisabledAsync(BlueMapAPI bluemap) {
		if (!plugin.isEnabled()) {
			return;
		}

		if (Bukkit.isPrimaryThread()) {
			this.onBlueMapDisabled(bluemap);
			return;
		}

		Bukkit.getScheduler().runTask(plugin, () -> {
			BlueMapAPI.getInstance().ifPresent(this::onBlueMapDisabled);
		});
	}

	private void onBlueMapEnabled(BlueMapAPI bluemap) {
		if (!enabled) {
			return;
		}

		if (this.blueMapApi != null) {
			return;
		}

		this.blueMapApi = bluemap;

		this.writeAssets();
		this.addAllShopkeepers();
	}

	private void onBlueMapDisabled(BlueMapAPI _unused) {
		if (!enabled) {
			return;
		}

		if (this.blueMapApi != null) {
			return;
		}

		this.removeAllShopkeepers();

		this.blueMapApi = null;
	}

	private void writeAssets() {
		assert this.enabled && this.blueMapApi != null;

		plugin.getLogger().info("Writing web assets.");

		// The marker icons are the same across all worlds and maps. We therefore save them to the
		// app's base assets directory.
		var shopkeeperAssetsPath = blueMapApi.getWebApp().getWebRoot()
				.resolve(WEB_SHOPKEEPERS_ASSETS);
		try {
			Files.createDirectories(shopkeeperAssetsPath);
		} catch (IOException e) {
			plugin.getLogger().log(
					Level.SEVERE,
					"Failed to create assets directory: " + shopkeeperAssetsPath,
					e
			);
		}

		for (var asset : ASSETS) {
			var targetPath = shopkeeperAssetsPath.resolve(asset);

			// Note: We don't replace existing assets, since the user might have replaced them
			// with their own assets.
			if (Files.exists(targetPath)) {
				continue;
			}

			var sourcePath = JAR_ASSETS + "/" + asset;
			var inputStream = plugin.getResource(sourcePath);

			try {
				Files.copy(inputStream, targetPath);
			} catch (IOException e) {
				plugin.getLogger().log(
						Level.SEVERE,
						"Failed to write asset (aborting): " + targetPath,
						e
				);
				break;
			}
		}
	}

	private void addAllShopkeepers() {
		// Note: If the Shopkeepers API is later enabled, the shopkeepers will be added one-by-one
		// via the ShopkeeperAddedEvent.
		if (!ShopkeepersAPI.isEnabled() || this.blueMapApi == null) {
			return;
		}

		ShopkeepersAPI.getShopkeeperRegistry().getAllShopkeepers().forEach(this::addShopkeeper);
	}

	private void removeAllShopkeepers() {
		if (!ShopkeepersAPI.isEnabled() || this.blueMapApi == null) {
			return;
		}

		ShopkeepersAPI.getShopkeeperRegistry().getAllShopkeepers().forEach(this::removeShopkeeper);
	}

	void addShopkeeper(Shopkeeper shopkeeper) {
		if (this.blueMapApi == null) {
			return;
		}

		var worldName = shopkeeper.getWorldName();
		if (worldName == null) {
			// E.g. the case for virtual shopkeepers.
			plugin.debug(shopkeeper.getLogPrefix()
					+ "Not adding BlueMap markers for virtual shopkeeper.");
			return;
		}

		blueMapApi.getWorld(worldName).map(BlueMapWorld::getMaps).ifPresent(maps -> {
			var shopTypeId = shopkeeper.getType().getIdentifier();
			var markerIcon = plugin.getSettings().getMarkerIcon(shopTypeId);
			if (markerIcon == null || markerIcon.isBlank()) {
				return; // Skip if no marker icon is defined
			}

			int anchorX = plugin.getSettings().getMarkerAnchorX(shopTypeId);
			int anchorY = plugin.getSettings().getMarkerAnchorY(shopTypeId);
			var markerLabel = this.getMarkerLabel(shopkeeper);
			var detail = this.getShopkeeperDetail(shopkeeper);

			for (BlueMapMap map : maps) {
				MarkerSet markerSet = map.getMarkerSets().computeIfAbsent(
						MARKERSET_ID,
						key -> MarkerSet.builder()
								.label(plugin.getSettings().getMarkerSetName())
								.defaultHidden(false)
								.toggleable(true)
								.build()
				);

				POIMarker marker = POIMarker.builder()
						.label(markerLabel)
						.detail(detail)
						.icon(markerIcon, anchorX, anchorY)
						.position(
								shopkeeper.getX() + 0.5D,
								shopkeeper.getY() + 0.5D,
								shopkeeper.getZ() + 0.5D
						)
						.build();

				markerSet.getMarkers().put(this.getMarkerId(shopkeeper), marker);
			}

			plugin.debug(shopkeeper.getLogPrefix()
					+ "Added BlueMap markers to " + maps.size() + " maps.");
		});
	}

	private String getMarkerId(Shopkeeper shopkeeper) {
		return "shopkeeper_" + shopkeeper.getId();
	}

	private String getMarkerLabel(Shopkeeper shopkeeper) {
		var ownerName = "";
		if (shopkeeper instanceof PlayerShopkeeper playerShopkeeper) {
			ownerName = playerShopkeeper.getOwnerName();
		}

		return plugin.getSettings().getMarkerLabel(shopkeeper.getType().getIdentifier())
				.replace("{shop_id}", Integer.toString(shopkeeper.getId()))
				.replace("{shop_uuid}", shopkeeper.getUniqueId().toString())
				.replace("{shop_name}", ChatColor.stripColor(shopkeeper.getDisplayName()))
				.replace("{shop_owner_name}", ownerName);
	}

	private String getShopkeeperDetail(Shopkeeper shopkeeper) {
		// Note: Don't include unescaped user input (e.g. the shopkeeper name) here.

		var shopObjectTypeName = shopkeeper.getShopObject().getType().getDisplayName();
		var offersCount = shopkeeper.getTradingRecipes(null).size();

		var ownerName = "";
		if (shopkeeper instanceof PlayerShopkeeper playerShopkeeper) {
			ownerName = playerShopkeeper.getOwnerName();
		}

		var detail = plugin.getSettings().getMarkerDetailText()
				.replaceAll("\\r\\n|\\r|\\n", "<br>")
				.replace("{shop_id}", Integer.toString(shopkeeper.getId()))
				.replace("{shop_uuid}", shopkeeper.getUniqueId().toString())
				.replace("{shop_type}", shopkeeper.getType().getDisplayName())
				.replace("{shop_object_type}", shopObjectTypeName)
				.replace("{shop_owner_name}", ownerName)
				.replace("{shop_offers_count}", Integer.toString(offersCount));

		return detail;
	}

	void removeShopkeeper(Shopkeeper shopkeeper) {
		if (this.blueMapApi == null) {
			return;
		}

		var worldName = shopkeeper.getWorldName();
		if (worldName == null) {
			// E.g. the case for virtual shopkeepers.
			plugin.debug(shopkeeper.getLogPrefix()
					+ "Not removing BlueMap markers for virtual shopkeeper.");
			return;
		}

		blueMapApi.getWorld(worldName).map(BlueMapWorld::getMaps).ifPresent(maps -> {
			for (BlueMapMap map : maps) {
				MarkerSet markerSet = map.getMarkerSets().get(MARKERSET_ID);
				if (markerSet == null) {
					continue;
				}

				markerSet.getMarkers().remove(this.getMarkerId(shopkeeper));
			}

			plugin.debug(shopkeeper.getLogPrefix()
					+ "Removed BlueMap markers from " + maps.size() + " maps.");
		});
	}

	void updateShopkeeper(Shopkeeper shopkeeper) {
		this.removeShopkeeper(shopkeeper);
		this.addShopkeeper(shopkeeper);
	}
}
