package de.blablubbabc.shopkeepers.bluemap;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperRemoveEvent;

class ShopkeepersListener implements Listener {

	private final ShopkeepersBlueMap shopkeepersBluemap;

	public ShopkeepersListener(ShopkeepersBlueMap shopkeepersBluemap) {
		this.shopkeepersBluemap = shopkeepersBluemap;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void onShopkeeperAdded(ShopkeeperAddedEvent event) {
		shopkeepersBluemap.addShopkeeper(event.getShopkeeper());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void onShopkeeperRemove(ShopkeeperRemoveEvent event) {
		shopkeepersBluemap.removeShopkeeper(event.getShopkeeper());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void onShopkeeperEdited(ShopkeeperEditedEvent event) {
		shopkeepersBluemap.updateShopkeeper(event.getShopkeeper());
	}
}
