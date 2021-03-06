package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.cyprias.ExchangeMarket.Econ;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class PriceCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.PRICE))
			list.add("/%s price - Get the price of an item..");
	}

	public CommandAccess getAccess() {
		return CommandAccess.BOTH;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.PRICE, "/%s price <item> [amount]", cmd);
	}

	public boolean hasValues() {
		return false;
	}

	public boolean execute(CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws IOException, InvalidConfigurationException, SQLException {
		if (!Plugin.checkPermission(sender, Perm.PRICE))
			return false;

		if (args.length >= 3) {
			getCommands(sender, cmd);
			return true;
		}

		ItemStack stock;// = Plugin.getItemStack(args[0]);
		if (args.length > 0){
			stock = Plugin.getItemStack(args[0]);
			if (stock == null || stock.getTypeId() == 0) {
				ChatUtils.error(sender, "Unknown item: " + args[0]);
				return true;
			}
		}else{
		//	Player player = (Player) sender;
			stock = ((Player) sender).getItemInHand();
			if (stock == null || stock.getTypeId() == 0) {
				ChatUtils.error(sender, "There's no item in your hand.");
				return true;
			}
		}
		

		int amount = 1;// InventoryUtil.getAmount(item, player.getInventory());
		if (args.length > 1) {
			if (Plugin.isInt(args[1]) && Integer.parseInt(args[1]) >= amount) {
				amount = Integer.parseInt(args[1]);
			} else {
				// ExchangeMarket.sendMessage(sender, F("invalidAmount",
				// args[2]));
				ChatUtils.error(sender, "Invalid amount: " + args[1]);
				return true;
			}
		}
		
			List<Order> orders = Plugin.database.search(stock);

		//	ChatUtils.send(sender, "Orders: " + orders.size());

			Order order;
			int totalAmount = 0;
			double totalPrice = 0;

			int lowestAmount, highestAmount;
			double lowest, highest;
			
			//List<Double> prices = new ArrayList<Double>();
			if (orders.size() > 0){
				lowest = orders.get(0).getPrice();
				lowestAmount = orders.get(0).getAmount();
				
				highest = orders.get(orders.size()-1).getPrice();
				highestAmount = orders.get(orders.size()-1).getAmount();
				double[] dPrices = new double[orders.size()];
				
				for (int i = 0; i < orders.size(); i++) {
					order = orders.get(i);
					totalAmount += order.getAmount();
					totalPrice += order.getPrice() * order.getAmount();
					dPrices[i] = order.getPrice();
					
					
				}
				
				Logger.debug("totalAmount: " + totalAmount + ", totalPrice: " + totalPrice);
				
				
				ChatUtils.send(sender, String.format("\u00a77There are \u00a7f%s \u00a77orders containing \u00a7f%s %s\u00a77.", orders.size(), totalAmount, Plugin.getItemName(stock)));
				
				
				
				int dplaces = Config.getInt("properties.price-decmial-places");
				
				if (orders.size() > 1)
					ChatUtils.send(sender, String.format("\u00a77Lowest price: \u00a7f%s \u00a77(x\u00a7f%s\u00a77), Highest price: \u00a7f%s \u00a77(x\u00a7f%s\u00a77)",  Econ.format(lowest*amount), lowestAmount, Econ.format(highest*amount), highestAmount));
				
				
				double average = totalPrice / totalAmount;
				
				
				
				String mean = Econ.format(mean(dPrices)*amount);
				String median = Econ.format(median(dPrices)*amount);
				String mode = Econ.format(mode(dPrices)*amount);
				
				ChatUtils.send(sender, String.format("\u00a77Average: \u00a7f%s\u00a77, mean:\u00a7f%s\u00a77, med:\u00a7f%s\u00a77, mod:\u00a7f%s\u00a77.", Econ.format(average*amount), mean, median, mode));
				
				
			}else{
				ChatUtils.send(sender, String.format("\u00a77There are no orders containing \u00a7f%s\u00a77.", Plugin.getItemName(stock)));
			}
			
			


	

		return true;
	}

	public static double mean(double[] p) {
		double sum = 0; // sum of all the elements
		for (int i = 0; i < p.length; i++) {
			sum += p[i];
		}
		return sum / p.length;
	}// end method mean

	public static double median(double[] m) {
		int middle = m.length / 2;
		if (m.length % 2 == 1) {
			return m[middle];
		} else {
			return (m[middle - 1] + m[middle]) / 2.0;
		}
	}

	public static double mode(double[] prices) {
		double maxValue = 0, maxCount = 0;

		for (int i = 0; i < prices.length; ++i) {
			int count = 0;
			for (int j = 0; j < prices.length; ++j) {
				if (prices[j] == prices[i])
					++count;
			}
			if (count > maxCount) {
				maxCount = count;
				maxValue = prices[i];
			}
		}

		return maxValue;
	}
	
}
