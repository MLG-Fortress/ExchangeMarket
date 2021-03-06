package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Order;

public class SearchCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.SEARCH))
			list.add("/%s search - Search orders.");
	}

	
	@Override
	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws IOException, InvalidConfigurationException, SQLException {
		if (!Plugin.checkPermission(sender, Perm.SEARCH))
			return false;

		if (args.length >= 3){
			getCommands(sender, cmd);
			return true;
		}

		ItemStack stock;
		if (args.length == 0 || args[0].equalsIgnoreCase("hand"))
		{
			stock = ((Player)sender).getInventory().getItemInMainHand();
			if (stock == null)
            {
                ChatUtils.error(sender, "There's no item in your hand");
                getCommands(sender, cmd);
                return true;
            }
		}
		else
			stock = Plugin.getItemStack(args[0]);

		if (stock == null || stock.getTypeId() == 0) {
			ChatUtils.error(sender, "Unknown item: " + args[0]);
			return true;
		}

		//RoboMWM - warn regarding selling of enchanted/custom items
		if (!stock.getEnchantments().isEmpty() || stock.getItemMeta().hasDisplayName())
		{
			ChatUtils.error(sender, ChatColor.GRAY + "Note: ExchangeMarket cannot accept damaged, enchanted, or custom items. Use the shops to trade those.");
		}

		
		
			List<Order> orders = Plugin.database.search(stock);
			
			
			
			ChatUtils.send(sender, String.format("\u00a77There are \u00a7f%s \u00a77orders for \u00a7f%s\u00a77.", orders.size(), Plugin.getItemName(stock)) );
			
			if (orders.size() < 0)
				return true;
			
			Order order;
			String format = Config.getColouredString("properties.list-row-format");
			String message;
			for (int i=0; i<orders.size();i++){
				order = orders.get(i);
				//stock = order.getItemStack();
				
				message = order.formatString(format, sender);
				
				if (sender instanceof ConsoleCommandSender){
					ChatUtils.sendSpam(sender, order.getId() + ": "+message);
				}else
					ChatUtils.sendSpam(sender, message);
				
			}
			return true;
			
	
	}

	@Override
	public CommandAccess getAccess() {
		return CommandAccess.BOTH;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.SEARCH, "/%s search [item/hand] - Search orders.", cmd);
		//ChatUtils.sendCommandHelp(sender, Perm.SEARCH, "Item "+ChatColor.WHITE+"i:"+ChatColor.GRAY+" - Item name or id", cmd);
		//ChatUtils.sendCommandHelp(sender, Perm.SEARCH, "Player "+ChatColor.WHITE+"w:"+ChatColor.GRAY+" - Search by writer", cmd);
		//ChatUtils.sendCommandHelp(sender, Perm.SEARCH, "Keyword "+ChatColor.WHITE+"k:"+ChatColor.GRAY+" - Search by note keyword", cmd);

		
		
	}

}
