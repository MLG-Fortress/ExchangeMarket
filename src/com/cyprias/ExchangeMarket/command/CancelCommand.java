package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Econ;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.Breeze.InventoryUtil;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class CancelCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) throws SQLException {
		if (Plugin.hasPermission(sender, Perm.CANCEL))
			if (Plugin.database.getPlayerOrderCount(sender) > 0)
				list.add("/%s cancel - Cancel one of your orders.");
		
		
	}

	public CommandAccess getAccess() {
		return CommandAccess.PLAYER;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.CANCEL, "/%s cancel <id> [amount]", cmd);
	}


	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws SQLException, IOException, InvalidConfigurationException {
		if (!Plugin.checkPermission(sender, Perm.CANCEL)) 
			return false;
		
		Player player = (Player) sender;
		if (Config.getBoolean("properties.block-usage-in-creative") == true && player.getGameMode().getValue() == 1) {
			ChatUtils.send(sender, "Cannot use ExchangeMarket while in creative mode.");
			return true;
		}
		
		if (args.length <= 0 || args.length >= 3){
			getCommands(sender, cmd);
			return true;
		}
		
		int id = 0;// InventoryUtil.getAmount(item, player.getInventory());
		if (args.length > 0) {
			if (Plugin.isInt(args[0])) {
				id = Integer.parseInt(args[0]);
			} else {
				// ExchangeMarket.sendMessage(sender, F("invalidAmount",
				// args[2]));
				ChatUtils.error(sender, "Invalid id: " + args[0]);
				return true;
			}
		}
		
		int amount = 0;// InventoryUtil.getAmount(item, player.getInventory());
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
		
		Order order = Plugin.database.getOrder(id);
		if (order == null){
			ChatUtils.send(sender, "\u00a77That order does not exist.");
			return true;
		}
		
		if (!sender.getName().equalsIgnoreCase(order.getPlayer())){
			ChatUtils.send(sender, "\u00a77That order does not belong to you.");
			return true;
		}
		
		if (order.getOrderType() == Order.SELL_ORDER){
		
			ItemStack stock = order.getItemStack();
			
			int receive = Plugin.getFitAmount(stock, order.getAmount(), player.getInventory());
			if (receive <= 0){
				ChatUtils.send(sender, "\u00a77You have no bag space available.");
				return true;
			}

			if (amount>0)
				receive = Math.min(receive, amount);
			

			stock.setAmount(receive);
			
		
			if (Config.getDouble("taxes.sellCancellation") > 0 && (!Config.getBoolean("taxes.exemptFullCancel") || args.length > 1)){
				//Logger.debug("taxes.sellOrder: " + Config.getDouble("taxes.sellOrder"));
				//Logger.debug("amount: " + amount);
				//Logger.debug("getPrice: " + preOrder.getPrice());

				double taxAmount = Config.getDouble("taxes.sellCancellation") * (amount * order.getPrice());
				//Logger.debug("taxAmount: " + taxAmount);
				
				if (Econ.getBalance(sender.getName()) < taxAmount){
					ChatUtils.send(sender, String.format("You do not have $\u00a7f%s \u00a77(\u00a7f%s\u00a77%%) needed to cancel that order.", Plugin.Round(taxAmount, Config.getInt("properties.price-decmial-places")), Plugin.Round(Config.getDouble("taxes.sellCancellation") * 100)));
					return true;
				}
				
				EconomyResponse r = Econ.withdrawPlayer(sender.getName(), taxAmount);
				if (r.transactionSuccess()) {
					ChatUtils.send(sender, String.format("$\u00a7f%s \u00a77(\u00a7f%s\u00a77%%) cancellation fee has been withdrawn from your account.", Plugin.Round(r.amount, Config.getInt("properties.price-decmial-places")), Plugin.Round(Config.getDouble("taxes.sellCancellation") * 100)));
				} else {
					ChatUtils.send(sender, String.format("An error occured: %s", r.errorMessage));
				}
			}
			
			
			receive = order.giveAmount(player, receive);

			ChatUtils.send(sender, String.format("\u00a77Returned \u00a7f%s\u00a77x\u00a7f%s\u00a77, there's \u00a7f%s \u00a77remaining in order #\u00a7f%s\u00a77.", Plugin.getItemName(stock), receive, order.getAmount(), order.getId()));
			
			
		}else if (order.getOrderType() == Order.BUY_ORDER){
			//ChatUtils.send(sender, "Can't cancel buy yet.");
			
			double money = order.getPrice() * order.getAmount();

			Econ.depositPlayer(sender.getName(), money);
			order.setAmount(0);
			
			ChatUtils.send(sender, String.format("\u00a77Returned your $\u00a7f%s\u00a77.", Plugin.Round(money, Config.getInt("properties.price-decmial-places"))));
			
			
			
		}
		Plugin.database.cleanEmpties();
		
		return true;
	}
	
	
}
