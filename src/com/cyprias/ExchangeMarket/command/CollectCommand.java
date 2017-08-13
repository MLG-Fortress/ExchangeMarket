package com.cyprias.ExchangeMarket.command;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Logger;
import com.cyprias.ExchangeMarket.Perm;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.Breeze.InventoryUtil;
import com.cyprias.ExchangeMarket.configuration.Config;
import com.cyprias.ExchangeMarket.database.Parcel;

public class CollectCommand  implements Command {

	public void listCommands(CommandSender sender, List<String> list) throws SQLException {
		if (Plugin.hasPermission(sender, Perm.COLLECT))
			if (Plugin.database.getPlayerPackageCount(sender) > 0)
				list.add("/%s collect - Collect pending items in your mailbox.");
	}

	public CommandAccess getAccess() {
		return CommandAccess.PLAYER;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.COLLECT, "/%s collect", cmd);
	}

	public boolean hasValues() {
		return false;
	}

	public boolean execute(CommandSender sender, org.bukkit.command.Command cmd, String[] args) throws IllegalArgumentException, SQLException, IOException, InvalidConfigurationException {
		if (!Plugin.checkPermission(sender, Perm.CONFIRM)) {
			return false;
		}
		Player player = (Player) sender;
		if (Config.getBoolean("properties.block-usage-in-creative") == true && player.getGameMode().getValue() == 1) {
			ChatUtils.send(sender, "Cannot use ExchangeMarket while in creative mode.");
			return true;
		}
		
		List<Parcel> packages = Plugin.database.getPackages(sender);
		
		if (packages.size() <= 0){
			ChatUtils.send(sender, "\u00a77You have no items to collect.");
			return true;
		}
		
		ItemStack stock;
		int leftover, canTake;
		boolean noFound = true;
		for (Parcel parcel : packages){
		
			stock = parcel.getItemStack();

			//RoboMWM - getFitAmount seems quite useless here...
			//canTake = Plugin.getFitAmount(stock, parcel.getAmount(), player.getInventory());
            canTake = parcel.getAmount();
			
			stock.setAmount(canTake);


            if (parcel.setAmount(parcel.getAmount() - canTake)){
				leftover = InventoryUtil.add(stock, player.getInventory());
				if (leftover > 0)
					parcel.setAmount(parcel.getAmount() + leftover);
				
				Logger.debug("canTake: " + canTake + ", leftover: " + leftover);
				
				
				//ChatUtils.send(sender, "Received " + stock.getType() + "x" + (canTake - leftover) + ", you have " + parcel.getAmount() +" left in your inbox.");
				
				ChatUtils.send(sender, String.format("\u00a77Received \u00a7f%s\u00a77x\u00a7f%s\u00a77, you have \u00a7f%s \u00a77remaining in your inbox.",
					Plugin.getItemName(stock), (canTake - leftover), parcel.getAmount()));
				
				noFound = false;
			}
			
			/*
			if (!InventoryUtil.fits(stock, player.getInventory()))
				continue;
				
			leftover = InventoryUtil.add(stock, player.getInventory());
			
			parcel.setAmount(leftover);*/
			
			
		}
		Plugin.database.cleanMailboxEmpties();
		
		
		if (noFound)
			ChatUtils.send(sender, "\u00a77Failed to receive mail.");
		
		
		return true;
	}



}
