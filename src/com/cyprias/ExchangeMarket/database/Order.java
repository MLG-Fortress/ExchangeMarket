package com.cyprias.ExchangeMarket.database;

import java.sql.SQLException;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cyprias.Breeze.MaterialUtil;
import com.cyprias.ExchangeMarket.ChatUtils;
import com.cyprias.ExchangeMarket.Plugin;
import com.cyprias.ExchangeMarket.configuration.Config;

public class Order {

	public static int SELL_ORDER = 1;
	public static int BUY_ORDER = 2;
	
	
	private String player;
	private int id, type, itemId, amount;
	private short itemDur;
	private boolean infinite = false;
	private String itemEnchants = null;
	private Double price;

	private ItemStack stock = null;
	public Order(int type, boolean infinite, String player, int itemId, short itemDur, String itemEnchants, int amount, double price) {
		this.type = type;
		this.infinite = infinite;
		this.player = player;
		this.itemId = itemId;
		this.itemDur = itemDur;
		this.itemEnchants = itemEnchants;
		this.price = price;
		this.amount = amount;
		
		this.stock = Plugin.getItemStack(itemId, itemDur, amount, itemEnchants);
	}
	public Order(int type, boolean infinite, String player, int itemId, short itemDur, Map<Enchantment, Integer> enchantments, int amount, double price) {
		this(type, infinite, player, itemId, itemDur, MaterialUtil.Enchantment.encodeEnchantment(enchantments), amount, price);
	}
	public Order(int type, boolean infinite, String player, ItemStack stock, double price) {
		this(type, infinite, player, stock.getTypeId(), stock.getDurability(), MaterialUtil.Enchantment.encodeEnchantment(stock), stock.getAmount(), price);
	}
	
	public void setId(int id){
		this.id = id;
	}

	public boolean hasEnchantments(){
		return (itemEnchants != null);
	}
	
	public String getCId(CommandSender sender){//Coloured
		if (sender.getName().equalsIgnoreCase(player))
			return ChatColor.GREEN.toString() + this.id + ChatColor.RESET;
			
		return ChatColor.WHITE.toString() + this.id + ChatColor.RESET;
	}
	
	public int getId(){
		return this.id;
	}
	
	public Material getItemType(){
		return this.stock.getType();
	}
	
	public ItemStack getItemStack(){
		return this.stock;
	}

	public Map<Enchantment, Integer> getEnchantments(){
		return MaterialUtil.Enchantment.getEnchantments(itemEnchants);
	}
	
	public String getEncodedEnchantments(){
		return this.itemEnchants;
	}

	public int getOrderType() {
		return this.type;
	}

	public String getOrderTypeString() {
		if (this.type == SELL_ORDER){
			return "SELL";
		}else if (this.type == BUY_ORDER){
			return "BUY";
		}
		return "OTHER";
	}
	
	public String getOrderTypeColouredString() {
		if (this.type == SELL_ORDER){
			return ChatColor.RED+"Sell" + ChatColor.RESET;
		}else if (this.type == BUY_ORDER){
			return ChatColor.GREEN+"Buy" + ChatColor.RESET;
		}
		return "OTHER";
	}
	
	public boolean isInfinite() {
		return this.infinite;
	}

	public String getPlayer() {
		return this.player;
	}

	public int getItemId(){
		return this.itemId;
	}
	public short getDurability(){
		return this.itemDur;
	}
	
	public double getPrice(){
		return this.price;
	}
	public int getAmount(){
		return this.amount;
	}
	
	public void notifyPlayerOfTransaction(int amount){
		Player p = Plugin.getInstance().getServer().getPlayer(this.player);
		if (p != null && p.isOnline()){
			Double tPrice = amount * this.price;
			if (this.type == Order.SELL_ORDER){
				ChatUtils.send(p, ChatUtils.getChatPrefix()+"You sold " + stock.getType() +"x" + amount + " for $" + Plugin.Round(tPrice, Config.getInt("properties.price-decmial-places")) + ".");
			}
		}
		
	}
	
	public Boolean reduceAmount(int byAmount) throws IllegalArgumentException, SQLException{
		if ((this.amount - byAmount) < 0)
			throw new IllegalArgumentException("Cannot reduce amount below zero.");
			
		return setAmount(this.amount - byAmount);
	}
	
	public Boolean increaseAmount(int byAmount) throws SQLException{
		return setAmount(this.amount + byAmount);
	}
	
	public Boolean setAmount(int amount) throws SQLException{
		if (id>0){
			if (Plugin.database.setAmount(id, amount)){
				this.amount = amount;
				this.stock.setAmount(amount);
				return true;
			}
		}else{
			this.stock.setAmount(amount);
			this.amount = amount;
			return true;
		}

		return false;
	}
	
	public Boolean remove() throws SQLException{
		return Plugin.database.remove(id);
	}
	
	public Boolean setPrice(double price) throws SQLException{
		if (id>0){
			if (Plugin.database.setPrice(id, price)){
				this.price = price;
				return true;
			}
		}else{
			this.price = price;
			return true;
		}

		return false;
	}
	
	public String formatString(String format, CommandSender sender){
		String message = format.replace("<id>", String.valueOf(getId()));
		message = message.replace("<cid>", getCId(sender));
		message = message.replace("<otype>", getOrderTypeColouredString());
		message = message.replace("<item>", getItemType().toString());
		message = message.replace("<player>", getPlayer());
		message = message.replace("<amount>", String.valueOf(getAmount()));
		int dplaces = Config.getInt("properties.price-decmial-places");
		message = message.replace("<price>", Plugin.Round(getPrice() * getAmount(), dplaces));
		message = message.replace("<priceeach>", Plugin.Round(getPrice(),dplaces));
		return message;
	}
	
}
