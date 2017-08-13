package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.Breeze.MaterialUtil;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class OrderInfoCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) throws SQLException {
		if (Plugin.hasPermission(sender, Perm.INFO))
			if (Plugin.database.getPlayerOrderCount(sender) > 0)
				list.add("/%s orderinfo - Get info on a specific order.");
	}
	
	public CommandAccess getAccess() {
		return CommandAccess.BOTH;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.INFO, "/%s orderinfo <orderID>", cmd);
	}

	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws SQLException, IOException, InvalidConfigurationException {
		if (!Plugin.checkPermission(sender, Perm.INFO)) 
			return false;
		
		if (args.length < 1 || args.length > 1){
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
		
		Order order = Plugin.database.getOrder(id);
		if (order == null){
			ChatUtils.send(sender, "\u00a77That order does not exist.");
			return true;
		}
		
		
		if ((sender instanceof Player) && !sender.getName().equalsIgnoreCase(order.getPlayer())){
			ChatUtils.send(sender, "\u00a77That order does not belong to you.");
			return true;
		}
		

		String message = order.formatString(Config.getColouredString("properties.orders-row-format"), sender);
		
		
		if (sender instanceof ConsoleCommandSender){
			ChatUtils.sendSpam(sender, order.getId() + ": "+message);
		}else
			ChatUtils.sendSpam(sender, message);
		
		if (order.hasEnchantments())
			for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : order.getEnchantments().entrySet()) 
				ChatUtils.sendSpam(sender, String.format("\u00a7f%s\u00a77: \u00a7f%s", entry.getKey().getName(), entry.getValue()));
			
			
		
		
		return true;
	}
	
}
