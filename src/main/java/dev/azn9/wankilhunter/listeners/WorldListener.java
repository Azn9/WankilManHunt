package dev.azn9.wankilhunter.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.PortalCreateEvent;

import java.util.List;
import java.util.stream.Collectors;

public class WorldListener implements Listener {

    private Location firstNetherPortal;
    private Location firstOverworldPortal;

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        event.setCancelled(event.toWeatherState());
    }

    @EventHandler
    public void onEntitySpawn(EntityAddToWorldEvent event) {
        if (event.getEntityType() == EntityType.PIGLIN_BRUTE) {
            event.getEntity().remove();
        }
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        List<BlockState> blocks = event.getBlocks().stream().filter(blockState -> blockState.getType() == Material.NETHER_PORTAL).collect(Collectors.toList());

        if (event.getReason() != PortalCreateEvent.CreateReason.NETHER_PAIR) {
            if (event.getReason() == PortalCreateEvent.CreateReason.FIRE && this.firstOverworldPortal == null) {
                this.firstOverworldPortal = blocks.get(0).getLocation().add(0.5, 1, 0.5);
            }

            return;
        }

        if (event.getWorld().getEnvironment() == World.Environment.NETHER) {
            if (this.firstNetherPortal == null) {
                this.firstNetherPortal = blocks.get(0).getLocation().add(0.5, 1, 0.5);
            } else {
                if (this.firstNetherPortal.getBlock().getType() != Material.NETHER_PORTAL) {
                    this.firstNetherPortal = blocks.get(0).getLocation().add(0.5, 1, 0.5);
                } else {
                    event.setCancelled(true);
                }
            }
        } else if (event.getWorld().getEnvironment() == World.Environment.NORMAL) {
            if (this.firstOverworldPortal == null) {
                this.firstOverworldPortal = blocks.get(0).getLocation().add(0.5, 1, 0.5);
            } else {
                if (this.firstOverworldPortal.getBlock().getType() != Material.NETHER_PORTAL) {
                    this.firstOverworldPortal = blocks.get(0).getLocation().add(0.5, 1, 0.5);
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            return;
        }

        if (event.getFrom().getWorld().getEnvironment() == World.Environment.NETHER) {
            if (this.firstOverworldPortal != null) {
                event.setTo(this.firstOverworldPortal);
            }
        } else if (event.getFrom().getWorld().getEnvironment() == World.Environment.NORMAL) {
            if (this.firstNetherPortal != null) {
                event.setTo(this.firstNetherPortal);
            }
        }
    }

}
