package top.dsbbs2.randro;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import com.google.common.collect.Lists;

public class Config {
  public static class WorldSetting{
	  public boolean dropItem=true;
	  public boolean dropExp=false;
	  public boolean showMsg=true;
	  public CopyOnWriteArrayList<Integer> slots=new CopyOnWriteArrayList<>();
	  public RandomNum slot_amount=new RandomNum(1,2);
	  public double money_possibility=0.01;
	  public double money_ratio=0.1;
	  public double exp_possibility=0.01;
	  public double exp_ratio=0.1;
	  public boolean isMoneyAvailable()
  	  {
  		return new SecureRandom().nextInt(100)+1<=Math.min(money_possibility, 1)*100;
  	  }
	  public boolean isExpAvailable()
  	  {
  		return new SecureRandom().nextInt(100)+1<=Math.min(exp_possibility, 1)*100;
  	  }
	  public CopyOnWriteArraySet<Integer> randomSlots(Inventory inv)
	  {
		  CopyOnWriteArraySet<Integer> r=new CopyOnWriteArraySet<>();
		  int num=this.slot_amount.getNum();
		  int nonempty_num=(int)Lists.newArrayList(inv.getContents()).parallelStream().filter(i->i!=null&&i.getAmount()>0&&i.getType()!=Material.AIR).count();
		  while(r.size()<num)
		  {
			  if (r.size()>=nonempty_num) {
				break;
			  }
			  int tmp=this.slots.get(new SecureRandom().nextInt(this.slots.size()));
			  if(inv.getItem(tmp)!=null&&inv.getItem(tmp).getAmount()>0&&inv.getItem(tmp).getType()!=Material.AIR)
			    r.add(tmp);
		  }
		  return r;
	  }
	  public static class RandomNum{
  		public int from,to;

			public RandomNum(int from, int to) {
				this.from = from;
				this.to = to;
			}
  		public int getNum()
  		{
  			return new SecureRandom().nextInt(Math.max(to,from)+1-Math.min(from,to))+Math.min(from,to);
  		}
			@Override
			public int hashCode() {
				return Objects.hash(from, to);
			}
			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (!(obj instanceof RandomNum))
					return false;
				RandomNum other = (RandomNum) obj;
				return from == other.from && to == other.to;
			}
			@Override
			public String toString() {
				return "RandomNum [from=" + from + ", to=" + to + "]";
			}
  		
  	}
  }
  public ConcurrentHashMap<String,WorldSetting> worlds=new ConcurrentHashMap<>();
  public boolean keepInvForUnconfiguredWorlds=true;
  public boolean keepExpForUnconfiguredWorlds=true;
  public String msg="由于死亡 你失去了以下物品: %s 失去货币: %f 失去经验: %d";
}
