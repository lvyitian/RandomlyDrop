package top.dsbbs2.randro;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Objects;

import org.apache.logging.log4j.core.util.KeyValuePair;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;
import top.dsbbs2.common.config.IConfig;
import top.dsbbs2.common.config.SimpleConfig;
import top.dsbbs2.common.lambda.IAdvancedRunnable;
import top.dsbbs2.common.lambda.INoThrowsRunnable;

public class Main extends JavaPlugin implements Listener {
    public Object eco;
    public IConfig<Config> config=new SimpleConfig<Config>(getDataFolder().getAbsolutePath().toString()+File.separator+"config.json","UTF8",Config.class) {{
		
		INoThrowsRunnable.invoke(this::loadConfig);
		
	}};
    @SuppressWarnings("unchecked")
	public IConfig<LinkedHashMap<String,String>> m=new SimpleConfig<LinkedHashMap<String,String>>(getDataFolder().getAbsolutePath().toString()+File.separator+"material.json","UTF8",(Class<LinkedHashMap<String,String>>)new LinkedHashMap<>().getClass()) {{
		
		INoThrowsRunnable.invoke(this::loadConfig);
		
	}};
    @Override
    public void onEnable()
    {
    	try {
        	RegisteredServiceProvider<Economy> tmp=Bukkit.getServicesManager().getRegistration(Economy.class);
        	if(tmp!=null)
        		eco=tmp.getProvider();
        	}catch (Throwable e) {
    			// ignore
    		}
    	Bukkit.getPluginManager().registerEvents(this, this);
    }
    public <T> T runIfVaultAvailable(IAdvancedRunnable<T> r,Object... args)
    {
    	if (eco!=null) {
    		return r.invoke(this.eco,args);
		}
    	return null;
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=false)
    public void onPlayerDeath(PlayerDeathEvent e)
    {
    	if (config.getConfig().keepExpForUnconfiguredWorlds) {
			e.setKeepLevel(true);
			e.setDroppedExp(0);
		}
    	if (config.getConfig().keepInvForUnconfiguredWorlds) {
			e.setKeepInventory(true);
			e.getDrops().clear();
		}
    	Config.WorldSetting tmp=config.getConfig().worlds.getOrDefault(e.getEntity().getWorld().getName(), null);
    	if (tmp!=null) {
    		e.setKeepLevel(true);
    		e.setDroppedExp(0);
    		e.setKeepInventory(true);
    		e.getDrops().clear();
    		String msg=this.config.getConfig().msg;
    		int exp_drop=0;
    		if (tmp.isExpAvailable()) {
    			int nte=e.getEntity().getTotalExperience();
    			exp_drop=(int) Math.round(tmp.exp_ratio*nte);
    			if (nte-exp_drop<0) {
					exp_drop=e.getEntity().getTotalExperience();
				}
				e.getEntity().setTotalExperience(nte-exp_drop);
				if (tmp.dropExp) {
					e.setDroppedExp(exp_drop);
				}
			}
    		double[] money_drop= {0};
    		if (tmp.isMoneyAvailable()) {
				runIfVaultAvailable(pwq->{
					Economy eco=(Economy)pwq[0];
					money_drop[0]=tmp.money_ratio*Math.abs(eco.getBalance(e.getEntity()));
					eco.withdrawPlayer(e.getEntity(), money_drop[0]);
					return null;
				});
			}
    		StringBuilder its=new StringBuilder();
    		tmp.randomSlots(e.getEntity().getInventory()).parallelStream().forEach(i->{
    			String in=e.getEntity().getInventory().getItem(i).getAmount()+"*"+(e.getEntity().getInventory().getItem(i).getItemMeta().hasDisplayName()?e.getEntity().getInventory().getItem(i).getItemMeta().getDisplayName():(e.getEntity().getInventory().getItem(i).getItemMeta().hasLocalizedName()?e.getEntity().getInventory().getItem(i).getItemMeta().getLocalizedName():(e.getEntity().getInventory().getItem(i).getType().name()+this.m.getConfig().entrySet().parallelStream().map(i2->new KeyValuePair(i2.getKey(), "("+i2.getValue()+")")).filter(i2->Objects.equals(i2.getKey(),e.getEntity().getInventory().getItem(i).getType().name())).map(i2->i2.getValue()).findFirst().orElse("")+":"+e.getEntity().getInventory().getItem(i).getDurability())));
    			its.append(ChatColor.RESET+in+ChatColor.RESET+",");
    			Bukkit.getScheduler().runTask(this, ()->{
    				if(tmp.dropItem)
        				e.getEntity().getWorld().dropItem(e.getEntity().getLocation(), e.getEntity().getInventory().getItem(i));
        			e.getEntity().getInventory().setItem(i, new ItemStack(Material.AIR));
    			});
    		});
    		if (tmp.showMsg) {
    			String itsn=its.length()>0?its.substring(0,its.length()-1):"null";
        		msg=String.format(msg, itsn,money_drop[0],exp_drop);
        		e.getEntity().sendMessage(msg);
			}
		}
    }
}
