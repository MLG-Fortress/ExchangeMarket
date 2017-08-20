package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;

public class ItemInfoCommand implements Command {

	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.ITEM_INFO))
			list.add("/%s iteminfo - Get info on an item.");
	}
	
	public CommandAccess getAccess() {
		return CommandAccess.PLAYER;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.ITEM_INFO, "/%s iteminfo [item]", cmd);
	}

	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws SQLException, IOException, InvalidConfigurationException {
		if (!Plugin.checkPermission(sender, Perm.ITEM_INFO)) 
			return false;
		
		if (args.length > 1){
			getCommands(sender, cmd);
			return true;
		}
		
		ItemStack stock;
		if (args.length > 0 && !args[0].equalsIgnoreCase("hand")){
			stock = Plugin.getItemStack(args[0]);
			if (stock == null || stock.getTypeId() == 0) {
				ChatUtils.error(sender, "Unknown item: " + args[0]);
				return true;
			}
		}else{
		//	Player player = (Player) sender;
			stock = ((Player) sender).getInventory().getItemInMainHand();
			if (stock == null || stock.getTypeId() == 0) {
				ChatUtils.error(sender, "There's no item in your hand.");
				return true;
			}
		}

		//RoboMWM - warn regarding selling of enchanted/custom items
		if (!stock.getEnchantments().isEmpty() || stock.getItemMeta().hasDisplayName())
		{
			ChatUtils.error(sender, ChatColor.GRAY + "Note: ExchangeMarket cannot accept damaged, enchanted, or custom items. Use the shops to trade those.");
		}
			
		//
		
		
		ChatUtils.send(sender, String.format("\u00a77Item: \u00a7f%s\u00a77, id: \u00a7f%s", Plugin.getItemName(stock), stock.getTypeId() + ((stock.getDurability() > 0) ? ":"+stock.getDurability() : "")));
		
		if (stock.getEnchantments().size() > 0){
			//\u00a77
			//ChatUtils.send(sender, %s: %s");
			
			for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : stock.getEnchantments().entrySet()) {
			//	ExchangeMarket.sendMessage(sender, entry.getKey().getName() + " " + entry.getValue());
				
				ChatUtils.send(sender, String.format("\u00a7f%s\u00a77: \u00a7f%s", entry.getKey().getName(), entry.getValue()));
				
			}
			
		}
		//ChatUtils.send(sender, "Item: %s, id: %s");
		
		return CommandManager.commands.get("price").execute(sender, cmd, args);
	}
}
