package de.blablubbabc.shopkeepers.bluemap;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;

import de.blablubbabc.shopkeepers.bluemap.util.SchedulerUtils;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;

/**
 * Shopkeepers BlueMap integration.
 */
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

	// Fair reentrant lock to ensure consistent ordering of enable and disable callbacks:
	private final ReentrantLock blueMapLock = new ReentrantLock(true);
	// Do not access this except while holding the blueMapLock (see runWithBlueMapLock and
	// runBlueMapOperation):
	private BlueMapAPI blueMapApi = null;

	private boolean enabled = false;
	private boolean assetsWritten = false;

	public ShopkeepersBlueMap(ShopkeepersBlueMapPlugin plugin) {
		this.plugin = plugin;
	}

	/**
	 * Enables the integration.
	 * <p>
	 * For example called during plugin enable.
	 */
	public void enable() {
		if (enabled) {
			return;
		}

		if (!plugin.getSettings().isEnabled()) {
			return;
		}

		enabled = true;
		// Try to write the assets again after each reload:
		assetsWritten = false;

		// Called immediately if the BlueMap API is currently enabled:
		BlueMapAPI.onEnable(this::onBlueMapEnabledAsync);
		BlueMapAPI.onDisable(this::onBlueMapDisabledAsync);

		Bukkit.getPluginManager().registerEvents(shopkeeperListener, plugin);
	}

	/**
	 * Disables the integration.
	 * <p>
	 * For example called during plugin disable.
	 */
	public void disable() {
		if (!enabled) {
			return;
		}

		HandlerList.unregisterAll(shopkeeperListener);

		BlueMapAPI.unregisterListener(this::onBlueMapEnabledAsync);
		BlueMapAPI.unregisterListener(this::onBlueMapDisabledAsync);

		this.onBlueMapDisabledAsync();

		enabled = false;
	}

	/**
	 * Synchronizes access to {@link ShopkeepersBlueMap#blueMapApi} to only allow one operation at a
	 * time.
	 * <p>
	 * This is used to resolve race conditions. For example, when the BlueMap API is disabled off
	 * the main thread, we wait for any in-progress marker updates on the main thread to complete
	 * before we cleanup all markers. Otherwise, we could end up cleaning up the markers off the
	 * main thread just for new markers to be subsequently added on the main thread.
	 * <p>
	 * The operations are run in FIFO order.
	 * 
	 * @param operation
	 *            the operation to run inside the BlueMap lock
	 */
	private void runWithBlueMapLock(Runnable operation) {
		blueMapLock.lock();
		try {
			operation.run();
		} finally {
			blueMapLock.unlock();
		}
	}

	/**
	 * See {@link #runWithBlueMapLock(Runnable)}.
	 * <p>
	 * The operation is skipped if the BlueMap API is not available currently.
	 * 
	 * @param operation
	 *            the BlueMap operation to run
	 */
	private void runBlueMapOperation(Consumer<BlueMapAPI> operation) {
		this.runWithBlueMapLock(() -> {
			var blueMapApi = this.blueMapApi;
			if (blueMapApi == null) {
				return;
			}

			operation.accept(blueMapApi);
		});
	}

	// According to the documentation, this may be called off the main server thread!
	private void onBlueMapEnabledAsync(BlueMapAPI blueMapApi) {
		SchedulerUtils.runOnMainThreadOrOmit(plugin, () -> {
			this.runWithBlueMapLock(() -> {
				// Only enable if the BlueMap API is still enabled:
				BlueMapAPI.getInstance().ifPresent(this::onBlueMapEnabled);
			});
		});
	}

	// According to the documentation, this may be called off the main server thread!
	private void onBlueMapDisabledAsync(BlueMapAPI _unused) {
		// Note: BlueMapAPI.getInstance is already null at this point.
		// Note: We handle the cleanup immediately, potentially off the main thread, because it is
		// not guaranteed that the API can still be used after this method returns (e.g. after
		// synchronization to the main thread).
		// This intentionally blocks while waiting for other BlueMap operations to complete.
		this.runWithBlueMapLock(() -> {
			this.onBlueMapDisabledAsync();
		});
	}

	// Called on the main thread inside the BlueMap lock.
	private void onBlueMapEnabled(BlueMapAPI newBlueMapApi) {
		if (!enabled) {
			return;
		}
		// This is called on the main thread and we disable the integration during plugin disable:
		assert plugin.isEnabled();

		if (this.blueMapApi != null) {
			// Already enabled.
			// Unexpected, because API enable and disable callbacks are run in FIFO order and
			// disable blocks while waiting for the lock.
			return;
		}

		this.blueMapApi = newBlueMapApi;

		if (!assetsWritten) {
			assetsWritten = true;

			// Write the assets asynchronously and add the shopkeeper markers afterwards (on the
			// main thread):
			SchedulerUtils.runAsyncTaskOrOmit(plugin, () -> {
				this.runBlueMapOperation(blueMapApi -> {
					this.writeAssets(blueMapApi);
				});

				SchedulerUtils.runOnMainThreadOrOmit(plugin, () -> {
					this.runBlueMapOperation(blueMapApi -> {
						this.addAllShopkeepers(blueMapApi);
					});
				});
			});
			return;
		}

		this.addAllShopkeepers(newBlueMapApi);
	}

	// Potentially called off the main thread, while holding the BlueMap lock.
	private void onBlueMapDisabledAsync() {
		var blueMapApi = this.blueMapApi;
		if (blueMapApi == null) {
			return; // Already disabled
		}
		// blueMapApi is only assigned when the integration is enabled:
		assert enabled;

		this.removeAllShopkeepersAsync(blueMapApi);

		this.blueMapApi = null;
	}

	// Potentially called off the main thread.
	private void writeAssets(BlueMapAPI blueMapApi) {
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

	private MarkerSet getOrCreateMarkerSet(BlueMapMap map) {
		return map.getMarkerSets().computeIfAbsent(
				MARKERSET_ID,
				key -> MarkerSet.builder()
						.label(plugin.getSettings().getMarkerSetName())
						.defaultHidden(false)
						.toggleable(true)
						.build()
		);
	}

	private MarkerSet getMarkerSet(BlueMapMap map) {
		return map.getMarkerSets().get(MARKERSET_ID);
	}

	private MarkerSet removeMarkerSet(BlueMapMap map) {
		return map.getMarkerSets().remove(MARKERSET_ID);
	}

	private void addAllShopkeepers(BlueMapAPI blueMapApi) {
		// Note: If the Shopkeepers API is later enabled, the shopkeepers will be added one-by-one
		// via the ShopkeeperAddedEvent.
		if (!ShopkeepersAPI.isEnabled()) {
			return;
		}

		var allShopkeepers = ShopkeepersAPI.getShopkeeperRegistry().getAllShopkeepers();
		allShopkeepers.forEach(shopkeeper -> this.addShopkeeper(blueMapApi, shopkeeper));
		plugin.getLogger().info("Added BlueMap markers for all shopkeepers: "
				+ allShopkeepers.size());
	}

	// Potentially called off the main thread. Do not access the ShopkeepersAPI here.
	private void removeAllShopkeepersAsync(BlueMapAPI blueMapApi) {
		// Remove the shopkeepers marker set from all maps:
		var markerCount = 0;
		for (var world : blueMapApi.getWorlds()) {
			for (BlueMapMap map : world.getMaps()) {
				var markerSet = this.removeMarkerSet(map);
				if (markerSet != null) {
					markerCount += markerSet.getMarkers().size();
				}
			}
		}

		plugin.getLogger().info("Removed " + markerCount + " BlueMap markers for all shopkeepers.");
	}

	void addShopkeeper(Shopkeeper shopkeeper) {
		this.runBlueMapOperation(blueMapApi -> {
			this.addShopkeeper(blueMapApi, shopkeeper);
		});
	}

	private void addShopkeeper(BlueMapAPI blueMapApi, Shopkeeper shopkeeper) {
		assert blueMapApi != null;
		assert shopkeeper != null;

		var worldName = shopkeeper.getWorldName();
		if (worldName == null) {
			// E.g. the case for virtual shopkeepers.
			plugin.debug(shopkeeper.getLogPrefix()
					+ "Not adding BlueMap markers for virtual shopkeeper.");
			return;
		}

		var shopTypeId = shopkeeper.getType().getIdentifier();
		var markerIcon = plugin.getSettings().getMarkerIcon(shopTypeId);
		if (markerIcon == null || markerIcon.isBlank()) {
			// Skip if no marker icon is defined:
			plugin.debug(shopkeeper.getLogPrefix()
					+ "Not adding BlueMap markers: No icon defined.");
			return;
		}

		blueMapApi.getWorld(worldName).map(BlueMapWorld::getMaps).ifPresent(maps -> {
			int anchorX = plugin.getSettings().getMarkerAnchorX(shopTypeId);
			int anchorY = plugin.getSettings().getMarkerAnchorY(shopTypeId);
			var markerLabel = this.getMarkerLabel(shopkeeper);
			var detail = this.getShopkeeperDetail(shopkeeper);

			for (BlueMapMap map : maps) {
				MarkerSet markerSet = this.getOrCreateMarkerSet(map);

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
		this.runBlueMapOperation(blueMapApi -> {
			this.removeShopkeeper(blueMapApi, shopkeeper);
		});
	}

	private void removeShopkeeper(BlueMapAPI blueMapApi, Shopkeeper shopkeeper) {
		assert blueMapApi != null;
		assert shopkeeper != null;

		// Not skipping virtual shopkeepers here: Maybe the shopkeeper object type changed in the
		// meantime from previously non-virtual to now virtual.

		var markerId = this.getMarkerId(shopkeeper);

		// Check all worlds: We currently don't remember the world we previously added the marker
		// to and we cannot use the shopkeeper's world since it might have changed since the marker
		// was added.
		var markerCount = 0;
		for (var world : blueMapApi.getWorlds()) {
			for (BlueMapMap map : world.getMaps()) {
				MarkerSet markerSet = this.getMarkerSet(map);
				if (markerSet == null) {
					continue;
				}

				var marker = markerSet.getMarkers().remove(markerId);
				if (marker != null) {
					markerCount += 1;
				}
			}
		}

		plugin.debug(shopkeeper.getLogPrefix() + "Removed BlueMap markers from " + markerCount
				+ " maps.");
	}

	void updateShopkeeper(Shopkeeper shopkeeper) {
		this.runBlueMapOperation(blueMapApi -> {
			this.removeShopkeeper(blueMapApi, shopkeeper);
			this.addShopkeeper(blueMapApi, shopkeeper);
		});
	}
}
