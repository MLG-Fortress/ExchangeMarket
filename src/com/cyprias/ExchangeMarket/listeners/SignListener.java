package com.cyprias.ExchangeMarket.listeners;

import java.io.IOException;
import java.sql.SQLException;

import org.bukkit.block.Block;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.Signs;
import com.cyprias.ExchangeMarket.Breeze.BlockUtil;
import com.cyprias.ExchangeMarket.Breeze.MaterialUtil;
import com.cyprias.ExchangeMarket.Breeze.PriceUtil;
import com.cyprias.ExchangeMarket.configuration.Config;



public class SignListener implements Listener {


	static public void unregisterEvents(JavaPlugin instance) {
		SignChangeEvent.getHandlerList().unregister(instance);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public static void onSignChange(SignChangeEvent event) throws SQLException, IOException, InvalidConfigurationException {
		if (event.isCancelled())
			return;
		
		Block signBlock = event.getBlock();
		String[] line = event.getLines();

		if (!BlockUtil.isSign(signBlock)) {
			Logger.severe("Player " + event.getPlayer().getName() + " tried to create a fake sign. Hacking?");
			return;
		}

		if (!Signs.isValidPreparedSign(line)) {
			return;
		}

		Player player = event.getPlayer();

		ItemStack stock = Plugin.getItemStack(line[Signs.ITEM_LINE]);

		if (stock == null || stock.getTypeId() == 0) {
			ChatUtils.error(player, "Unknown item: " + line[Signs.ITEM_LINE]);
			return;
		}
	//	Logger.debug("id " + stock.getTypeId() + ", " + stock.getDurability());
		
		event.setLine(Signs.ITEM_LINE, Plugin.getItemName(stock));
		
		int amount = Integer.parseInt(line[Signs.QUANTITY_LINE]);
		
		
		double estBuyPrice = Plugin.getEstimatedBuyPrice(stock, amount);
		double estSellPrice = Plugin.getEstimatedSellPrice(stock, amount);
		String priceText = (estBuyPrice > 0) ? "B " + Plugin.Round(estBuyPrice, 2) : "";
		if (estSellPrice > 0)
			priceText += ((priceText != "") ? " : " : "") + Plugin.Round(estSellPrice, 2) + " S";
		
		event.setLine(Signs.PRICE_LINE, priceText);
		
		
		String formattedPrice = Signs.formatPriceLine(line[Signs.PRICE_LINE]);

		if (formattedPrice == null) {
			ChatUtils.error(player, "Invalid price", line[Signs.PRICE_LINE]);
			return;
		}

		event.setLine(Signs.PRICE_LINE, formattedPrice);
		//event.setLine(Signs.ITEM_LINE, Signs.formatItemLine(line[Signs.ITEM_LINE], stock));

		double buyPrice = PriceUtil.getBuyPrice(formattedPrice);
		double sellPrice = PriceUtil.getSellPrice(formattedPrice);

		if (buyPrice == 0 && sellPrice == 0) {
			// sendMessageAndExit(YOU_CANNOT_CREATE_SHOP, event);
			ChatUtils.error(player, "You need to set either a buy or sell price.");
			return;
		}

		int pl = Config.getInt("properties.price-decmial-places");

		ChatUtils.send(player, String.format("\u00a77Created exchange sign for \u00a7f%s\u00a77x\u00a7f%s\u00a77.", Plugin.getItemName(stock), amount));
		if (buyPrice > 0) {
			ChatUtils.send(player, String.format("\u00a77Buy price: $\u00a7f%s \u00a77($\u00a7f%s\u00a77e)", Plugin.Round(buyPrice, pl), Plugin.Round(buyPrice / amount, pl)));
		}
		if (sellPrice > 0) {
			ChatUtils.send(player, String.format("\u00a77Sell price: $\u00a7f%s \u00a77($\u00a7f%s\u00a77e)", Plugin.Round(sellPrice, pl), Plugin.Round(sellPrice / amount, pl)));
		}

	}


}
