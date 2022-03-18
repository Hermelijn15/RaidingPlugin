package com.gromit.antimine;

import com.massivecraft.factions.*;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;

import static com.gromit.antimine.Antimine.*;

public class Listeners implements Listener {

    private long lastBreachTimeMS = 0;
    private final Object2LongOpenHashMap<Faction> raidMap;
    private final Antimine plugin;
    private final World raidWorld = raidOutpost;
    private final Faction raidingOutpostFaction = Factions.getInstance().getByTag("RaidingOutpost");
    private final Faction fWild = Factions.getInstance().getFactionById("0");
    private final int minY;
    private final int maxY;
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;
    private final String startMSGTarget;
    private final String startMSGRaider;
    private final boolean preventMining;

    public Listeners(Antimine plugin, Configuration config, Object2LongOpenHashMap<Faction> raidMap) {
        this.plugin = plugin;
        this.minY = config.getInt("raiding-outpost-miny", 200);
        this.maxY = config.getInt("raiding-outpost-maxy", 100);
        this.minX = config.getInt("raiding-outpost-minx", -20);
        this.maxX = config.getInt("raiding-outpost-maxx", 20);
        this.minZ = config.getInt("raiding-outpost-minz", -20);
        this.maxZ = config.getInt("raiding-outpost-maxz", 20);
        this.startMSGTarget = config.getString("start-raid-msg");
        this.startMSGRaider = config.getString("you-started-raiding");
        this.preventMining = config.getBoolean("prevent-mining-spawner");
        this.raidMap = raidMap;
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        List<Block> blockList = event.blockList();
        if (blockList.isEmpty()) return;

        Entity entity = event.getEntity();

        if (!(entity instanceof TNTPrimed)) return;

        Location eventLoc = event.getLocation();
        FLocation eventLocation = new FLocation(eventLoc);
        Faction eventFaction = Board.getInstance().getFactionAt(eventLocation);

        if (eventFaction.equals(fWild)) return;

        if(entity.getWorld().equals(raidWorld)){
            //check if in claims of the raiding outpost

            if(eventFaction.equals(raidingOutpostFaction)){

                if(System.currentTimeMillis()>(lastBreachTimeMS + 100000)){
                    event.setCancelled(true);
                    return;
                }else{
                    if(eventLoc.getX()>(minX-2) && eventLoc.getX()<(maxX+2) && eventLoc.getZ()>(minZ+2)&& eventLoc.getZ()<(maxZ+2)){
                        boolean blockBroken = false;
                        for(Block block : blockList){
                            Location location = block.getLocation();
                            if(location.getY()<maxY && location.getY()>minY && location.getX()<maxX && location.getX()>minX && location.getZ()<maxZ && location.getZ()>minZ){
                                blockBroken=true;
                                lastBreachTimeMS = System.currentTimeMillis();
                                break;
                            }
                        }
                    }
                }
            }
            //regenerate outpost

        }



        TNTPrimed tntPrimed = (TNTPrimed) entity;

        Faction spawnFaction = Board.getInstance().getFactionAt(new FLocation(tntPrimed.getSpawnLocation()));

        if (eventFaction.equals(spawnFaction)) return;

        long currentTime = System.currentTimeMillis();


        if (raidMap.containsKey(eventFaction)){
            long lastEntry = raidMap.get(eventFaction);

            if(currentTime - lastEntry < 100) return;



            if (blockList.contains(eventLoc.getBlock())){
                raidMap.put(eventFaction, currentTime);
            }

        } else{

            raidMap.put(eventFaction, currentTime);

            spawnFaction.sendMessage(startMSGRaider);

            eventFaction.sendMessage(startMSGTarget);

        }

    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (preventMining && event.getBlock().getType().equals(Material.SPAWNER) && raidMap.containsKey(FPlayers.getInstance().getByPlayer(event.getPlayer()).getFaction())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot mine spawners while being raided!");
        }
    }
}
