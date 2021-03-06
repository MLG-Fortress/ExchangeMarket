package com.cyprias.ExchangeMarket.listeners;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Econ;
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.Signs;
import com.cyprias.ExchangeMarket.Breeze.BlockUtil;
import com.cyprias.ExchangeMarket.Breeze.InventoryUtil;
import com.cyprias.ExchangeMarket.Breeze.MaterialUtil;
import com.cyprias.ExchangeMarket.Breeze.PriceUtil;
import com.cyprias.ExchangeMarket.command.BuyCommand;
import com.cyprias.ExchangeMarket.command.Command;
import com.cyprias.ExchangeMarket.command.CommandManager;
import com.cyprias.ExchangeMarket.command.ConfirmCommand;
import com.cyprias.ExchangeMarket.command.ConfirmCommand.pendingOrder;
import com.cyprias.ExchangeMarket.command.ConfirmCommand.pendingTranasction;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;
import com.cyprias.ExchangeMarket.database.Parcel;

import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;
import static org.bukkit.event.block.Action.LEFT_CLICK_BLOCK;

public class PlayerListener implements Listener {

	static public void unregisterEvents(JavaPlugin instance) {
		PlayerCommandPreprocessEvent.getHandlerList().unregister(instance);
		PlayerJoinEvent.getHandlerList().unregister(instance);
		PluginEnableEvent.getHandlerList().unregister(instance);
		PlayerInteractEvent.getHandlerList().unregister(instance);

	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoinEvent(PlayerJoinEvent event) throws SQLException, IOException, InvalidConfigurationException {
		List<Parcel> packages = Plugin.database.getPackages(event.getPlayer());

		if (packages.size() <= 0)
			return;

		ChatUtils.notify(event.getPlayer(), String.format("\u00a77You have \u00a7f%s \u00a77packages to collect.", packages.size()));

		ItemStack stock;
		for (Parcel parcel : packages) {
			stock = Plugin.getItemStack(parcel.getItemId(), parcel.getItemDur(), parcel.getItemEnchant());
			ChatUtils.sendSpam(event.getPlayer(), String.format("\u00a7f%s\u00a77x\u00a7f%s", Plugin.getItemName(stock), parcel.getAmount()));
		}

	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		String msg = event.getMessage();
		String command = msg.split(" ")[0].replace("/", "");

		if (Plugin.aliases.containsKey(command.toLowerCase())) {
			event.setMessage(msg.replaceFirst("/" + command, "/" + Plugin.aliases.get(command.toLowerCase())));
			return;
		}
	}

	public static HashMap<String, Double> lastUsed = new HashMap<String, Double>();
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public static void onInteract(PlayerInteractEvent event) throws IllegalArgumentException, SQLException, IOException, InvalidConfigurationException {
		//if (event.isCancelled())
		//	return;
		
		Player player = event.getPlayer();
		if (Config.getBoolean("properties.block-usage-in-creative") == true && player.getGameMode().getValue() == 1)
			return;

		String playerName = player.getName();

			
		
		
		
		
		
		Block block = event.getClickedBlock();

		if (block == null)
			return;

		Action action = event.getAction();

		if (!BlockUtil.isSign(block) || player.getItemInHand().getType() == Material.SIGN)
			return;

		Sign sign = (Sign) block.getState();

		if (!Signs.isValid(sign))
			return;

		if (!Plugin.checkPermission(player, Perm.USE_EXCHANGE_SIGN))
			return;

		if (lastUsed.containsKey(playerName))
			if ((lastUsed.get(playerName) + Config.getDouble("properties.exchange-sign-throttle")) > Plugin.getUnixTime()){
				ChatUtils.sendSpam(player, "\u00a77Please wait...");
				return;
			}
		lastUsed.put(playerName, Plugin.getUnixTime());
		
		
		Logger.debug("action " + action);

		String[] line = sign.getLines();

		
		
		ItemStack stock = Plugin.getItemStack(line[Signs.ITEM_LINE]);
		if (stock == null || stock.getTypeId() == 0) {
			ChatUtils.error(player, "Unknown item: " + line[Signs.ITEM_LINE]);
			return;
		}

		int amount = Integer.parseInt(line[Signs.QUANTITY_LINE]);
		// Logger.debug("amount " + amount);

		String formattedPrice = Signs.formatPriceLine(line[Signs.PRICE_LINE]);

		int dplaces = Config.getInt("properties.price-decmial-places");

		double buyPrice = PriceUtil.getBuyPrice(formattedPrice);
		double sellPrice = PriceUtil.getSellPrice(formattedPrice);
		
		if (buyPrice < 0) buyPrice = 0;
		if (sellPrice < 0) sellPrice = 0;
		
		//Update sign prices.
		
		double estBuyPrice = Plugin.getEstimatedBuyPrice(stock, amount);
		double estSellPrice = Plugin.getEstimatedSellPrice(stock, amount);
		if ((!Plugin.Round(estBuyPrice, 2).equalsIgnoreCase(Plugin.Round(buyPrice, 2))) || (!Plugin.Round(estSellPrice, 2).equalsIgnoreCase(Plugin.Round(sellPrice, 2)))) {
			String priceText = (estBuyPrice > 0) ? "B " + Plugin.Round(estBuyPrice, 2) : "";
			if (estSellPrice > 0)
				priceText += ((priceText != "") ? " : " : "") + Plugin.Round(estSellPrice, 2) + " S";
			
			sign.setLine(Signs.PRICE_LINE, priceText);
			sign.update();
			ChatUtils.send(player, String.format("\u00a77Updated sign price, try again."));
			return;
		}

		// ////////////////////////////////////////////////////
		if (action == RIGHT_CLICK_BLOCK) {
			event.setCancelled(true);
			if (Econ.getBalance(player.getName()) <= 0) {
				ChatUtils.send(player, String.format("\u00a77You have no money in your account."));
				return;
			}



			if (buyPrice <= 0 && estBuyPrice == 0) {
				ChatUtils.send(player, "\u00a77That exchange is not selling items.");
				return;
			}

			List<Order> orders = Plugin.database.search(stock, Order.SELL_ORDER);

			Order o;

			if (!Config.getBoolean("properties.trade-to-yourself"))
				for (int i = (orders.size() - 1); i >= 0; i--) {
					o = orders.get(i);
					if (player.getName().equalsIgnoreCase(o.getPlayer()))
						orders.remove(o);
				}

			if (orders.size() <= 0) {
				ChatUtils.send(player, String.format("\u00a77There are no sell orders for \u00a7f%s\u00a77.", Plugin.getItemName(stock)));
				return;
			}

			int playerCanFit = Plugin.getFitAmount(stock, player.getInventory());
			double moneySpent = 0;
			int itemsTraded = 0;

			pendingTranasction pT = new ConfirmCommand.pendingTranasction(player, new ArrayList<pendingOrder>(), Order.SELL_ORDER);
			ConfirmCommand.pendingTransactions.put(player.getName(), pT);
			List<pendingOrder> pending = pT.pendingOrders;

			for (int i = 0; i < orders.size(); i++) {
				if (amount <= 0)
					break;

				stock.setAmount(1);
				if (!InventoryUtil.fits(stock, player.getInventory()))
					break;

				o = orders.get(i);

				int canTrade = amount;
				if (!o.isInfinite())
					canTrade = Math.min(o.getAmount(), amount);

				canTrade = (int) Math.floor(Math.min(canTrade, Econ.getBalance(player.getName()) / o.getPrice()));

				canTrade = Math.min(canTrade, playerCanFit);

				if (canTrade <= 0)
					break;

				int traded = canTrade;// (canBuy - leftover);
				playerCanFit -= traded;

				double spend = (traded * o.getPrice());

				// if (spend > buyPrice)
				// continue;

				Logger.debug("traded: " + traded);
				Logger.debug("spend: " + spend);

				moneySpent += spend;

				pendingOrder po = new pendingOrder(o.getId(), traded);

				pending.add(po);

				Logger.debug(o.getId() + " x" + o.getAmount() + ", canTrade: " + canTrade + " (" + (canTrade * o.getPrice()) + ") traded: " + traded
					+ ", player: " + o.getPlayer());

				itemsTraded += traded;
				amount -= traded;

			}

			if (moneySpent > 0) {
				ChatUtils.send(player, String.format("\u00a7a[Estimate] \u00a7f%s\u00a77x\u00a7f%s\u00a77 will cost $\u00a7f%s\u00a77, type \u00a7d/em confirm \u00a77to confirm transaction.",
					Plugin.getItemName(stock), itemsTraded, Plugin.Round(moneySpent, dplaces)));
			} else {
				stock.setAmount(1);
				if (!InventoryUtil.fits(stock, player.getInventory())) {
					ChatUtils.send(player, "You have no bag space available.");
				} else {
					ChatUtils.send(
						player,
						String.format("\u00a77There are no sell orders for \u00a7f%s\u00a77x\u00a7f%s \u00a77at $\u00a7f%s\u00a77.", Plugin.getItemName(stock), amount,
							Plugin.Round(buyPrice, dplaces)));
					return;
				}

			}

			// ///////////////////////////////////////////////////////////////////////
		} else if (action == LEFT_CLICK_BLOCK) {

			Logger.debug("sellPrice " + sellPrice);



			if (sellPrice <= 0 && estSellPrice == 0) {
				ChatUtils.send(player, "\u00a77That exchange is not buying items.");
				return;
			}

			List<Order> orders = Plugin.database.search(stock, Order.BUY_ORDER);
			Order o;

			if (!Config.getBoolean("properties.trade-to-yourself"))
				for (int i = (orders.size() - 1); i >= 0; i--) {
					o = orders.get(i);
					if (player.getName().equalsIgnoreCase(o.getPlayer()))
						orders.remove(o);
				}

			if (orders.size() <= 0) {
				ChatUtils.send(player, String.format("\u00a77There are no buy orders for \u00a7f%s\u00a77.", Plugin.getItemName(stock)));
				return;
			}

			pendingTranasction pT = new ConfirmCommand.pendingTranasction(player, new ArrayList<pendingOrder>(), Order.BUY_ORDER);
			ConfirmCommand.pendingTransactions.put(player.getName(), pT);

			List<pendingOrder> pending = pT.pendingOrders; // ConfirmCommand.pendingOrders.get(sender.getName());

			double moneyProfited = 0.0;
			int itemsTraded = 0;
			for (int i = (orders.size() - 1); i >= 0; i--) {
				if (amount <= 0)
					break;

				o = orders.get(i);

				int canTrade = amount;
				if (!o.isInfinite())
					canTrade = Math.min(o.getAmount(), amount);

				Logger.debug("sell " + i + ", id: " + o.getId() + ", price: " + o.getPrice() + ", canTrade: " + canTrade);
				if (canTrade <= 0)
					break;

				int traded = canTrade;// (canBuy - leftover);

				double profit = (traded * o.getPrice());

				// Logger.debug("traded: " + traded);
				// Logger.debug("profit: " + profit);
				// Logger.debug("sellPrice: " + sellPrice);

				// if (profit < sellPrice)
				// continue;

				moneyProfited += profit;

				pendingOrder po = new pendingOrder(o.getId(), traded);

				pending.add(po);

				Logger.debug(o.getId() + " x" + o.getAmount() + ", canTrade: " + canTrade + " (" + (canTrade * o.getPrice()) + ") traded: " + traded
					+ ", player: " + o.getPlayer());

				// message = format.format(format, o.getItemType(), added,
				// Plugin.Round((added*o.getPrice()),dplaces),
				// Plugin.Round(o.getPrice(),dplaces));
				// ChatUtils.send(sender, "\u00a7a[Prevew] " + message);

				itemsTraded += traded;
				amount -= traded;

			}

			if (itemsTraded > 0) {

				ChatUtils.send(player, String.format("\u00a7a[Estimate] \u00a7f%s\u00a77x\u00a7f%s\u00a77 will earn $\u00a7f%s\u00a77, type \u00a7d/em confirm \u00a77to confirm estimate.",
					Plugin.getItemName(stock), itemsTraded, Plugin.Round(moneyProfited, Config.getInt("properties.price-decmial-places"))));

			} else {
				ChatUtils.send(
					player,
					String.format("\u00a77There are no buy orders for \u00a7f%s\u00a77x\u00a7f%s \u00a77at $\u00a7f%s\u00a77.", Plugin.getItemName(stock), amount,
						Plugin.Round(sellPrice, dplaces)));
				return;
			}

		}
	}
}
