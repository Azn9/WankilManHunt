package dev.azn9.wankilhunter.listeners;

import dev.azn9.wankilhunter.WankilHunter;
import dev.azn9.wankilhunter.game.GameManager;
import dev.azn9.wankilhunter.game.GameState;
import dev.azn9.wankilhunter.injector.Inject;
import dev.azn9.wankilhunter.player.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

public class DamageListener implements Listener {

    @Inject
    private static GameManager gameManager;

    @Inject
    private static WankilHunter plugin;

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (gameManager.getState() != GameState.IN_GAME || gameManager.hasInvincibility()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof EnderSignal) {
            ((EnderSignal) event.getEntity()).setDropItem(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            gameManager.setObjectifCompleted(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GamePlayer gamePlayer = gameManager.getPlayer(event.getEntity());

        if (gamePlayer == null || gamePlayer.isSpectator()) {
            return;
        }

        if (gamePlayer.isRunner()) {
            if (gameManager.getSpeedrunners().stream().filter(gamePlayer1 -> !gamePlayer1.isDead()).count() == 1) {
                gamePlayer.onDeath();
                return;
            }

            player.setGameMode(GameMode.SPECTATOR);
        } else if (gamePlayer.isHunter()) {
            player.getInventory().remove(Material.COMPASS);

            if (player.getKiller() != null) {
                GamePlayer killerPlayer = gameManager.getPlayer(player.getKiller());

                if (killerPlayer != null && killerPlayer.isRunner()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.DIAMOND)).setMetadata("hunter_pickeable", new FixedMetadataValue(plugin, false));
                }
            }
        }

        gamePlayer.onDeath();

        if (gamePlayer.isHunter()) {
            gamePlayer.setDead(false);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        if (event.getItem().hasMetadata("hunter_pickeable")) {
            if (gameManager.getPlayer(event.getEntity().getUniqueId()).isHunter()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = gameManager.getPlayer(player);

        if (gamePlayer == null || gamePlayer.isSpectator()) {
            return;
        }

        if (gamePlayer.isHunter()) {
            event.setRespawnLocation(gameManager.getRespawnLocation());

            player.sendTitle("", "§cVous ne pouvez pas bouger pendant 30s", 20, 600, 20);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.teleport(gameManager.getRespawnLocation());
            }, 2L);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.teleport(player.getBedSpawnLocation() != null ? player.getBedSpawnLocation() : gameManager.getSpawnLocation());
            }, 600);
        } else if (gamePlayer.isRunner()) {
            event.setRespawnLocation(gamePlayer.getDeathLocation());
        }
    }

}
