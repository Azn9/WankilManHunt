package dev.azn9.wankilhunter.listeners;

import dev.azn9.wankilhunter.game.GameManager;
import dev.azn9.wankilhunter.injector.Inject;
import dev.azn9.wankilhunter.player.GamePlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomItemListener implements Listener {

    private static final Map<UUID, GamePlayer> TRACKER = new HashMap<>();
    @Inject
    private static GameManager gameManager;

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        switch (event.getBlock().getType()) {
            case IRON_ORE:
            case GOLD_ORE:
                cutclean(event);

            default:
                break;
        }
    }

    @EventHandler
    public void onItemDrop(ItemSpawnEvent event) {
        switch (event.getEntity().getItemStack().getType()) {
            case MUTTON:
            case BEEF:
            case CHICKEN:
            case RABBIT:
            case PORKCHOP:
                cutclean(event);
                break;

            default:
                break;
        }
    }

    private void cutclean(ItemSpawnEvent event) {
        switch (event.getEntity().getItemStack().getType()) {
            case BEEF:
                event.getEntity().getItemStack().setType(Material.COOKED_BEEF);
                break;
            case PORKCHOP:
                event.getEntity().getItemStack().setType(Material.COOKED_PORKCHOP);
                break;
            case CHICKEN:
                event.getEntity().getItemStack().setType(Material.COOKED_CHICKEN);
                break;
            case MUTTON:
                event.getEntity().getItemStack().setType(Material.COOKED_MUTTON);
                break;
            case RABBIT:
                event.getEntity().getItemStack().setType(Material.COOKED_RABBIT);
                break;
        }
    }

    private void cutclean(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.IRON_ORE) {
            event.setCancelled(true);
            block.setType(Material.AIR);
            block.getWorld().dropItem(block.getLocation().toBlockLocation().add(0.5, 0.1, 0.5), new ItemStack(Material.IRON_INGOT));
        }
        if (block.getType() == Material.GOLD_ORE) {
            event.setCancelled(true);
            block.setType(Material.AIR);
            block.getWorld().dropItem(block.getLocation().toBlockLocation().add(0.5, 0.1, 0.5), new ItemStack(Material.GOLD_INGOT));
        }

        ExperienceOrb orb = (ExperienceOrb) block.getWorld().spawnEntity(block.getLocation(), EntityType.EXPERIENCE_ORB);
        orb.setExperience(3);
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack itemStack = event.getRecipe().getResult();
        GamePlayer gamePlayer = gameManager.getPlayer(player);

        if (gamePlayer == null || gamePlayer.isSpectator()) {
            return;
        }

        if (itemStack.getType() == Material.COMPASS) {
            if (itemStack.hasItemMeta() && itemStack.getItemMeta().getDisplayName().equalsIgnoreCase("§cTracker")) {
                if (!gamePlayer.isHunter()) {
                    event.setCancelled(true);
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                gameManager.getSpeedrunners().forEach(gamePlayer1 -> {
                    if (!gamePlayer1.isDead() && gamePlayer1.getCraftPlayer() != null && gamePlayer1.getCraftPlayer().isOnline()) {
                        gamePlayer1.getCraftPlayer().sendMessage("§cUn hunter a crafté une boussole pour vous tracker !");
                    }
                });
                player.sendMessage("§aCliquez avec cet item pour localiser un speedrunner. Si vous venez à mourir avec cet item sur vous, il disparaitra défénitivement.");
            }
        } else if (itemStack.getType() == Material.TOTEM_OF_UNDYING) {
            if (itemStack.hasItemMeta() && itemStack.getItemMeta().getDisplayName().equalsIgnoreCase("§aTotem de résurrection")) {
                if (!gamePlayer.isRunner()) {
                    event.setCancelled(true);
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                for (ItemStack content : player.getInventory().getContents()) {
                    if (content != null && content.hasItemMeta() && content.getItemMeta().getDisplayName().equalsIgnoreCase("§aTotem de résurrection")) {
                        event.setCancelled(true);
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        player.sendMessage("§cVous ne pouvez pas posséder plus d'un Totem à la fois !");
                        return;
                    }
                }

                gameManager.getHunters().forEach(gamePlayer1 -> {
                    if (!gamePlayer1.isDead() && gamePlayer1.getCraftPlayer() != null && gamePlayer1.getCraftPlayer().isOnline()) {
                        gamePlayer1.getCraftPlayer().sendMessage("§cUn speedrunner a crafté un totem de résurrection !");
                    }
                });
                player.sendMessage("§aCliquez avec cet item pour ressuciter un allié mort.");
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = gameManager.getPlayer(player);
        ItemStack itemStack = event.getItem();

        if (gamePlayer == null || gamePlayer.isSpectator() || itemStack == null || event.getAction() == Action.PHYSICAL) {
            return;
        }

        if (itemStack.getType() == Material.COMPASS) {
            if (itemStack.hasItemMeta() && itemStack.getItemMeta().getDisplayName().equalsIgnoreCase("§cTracker")) {
                if (!gamePlayer.isHunter()) {
                    event.setCancelled(true);
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                GamePlayer tracked;

                if (!TRACKER.containsKey(player.getUniqueId())) {
                    TRACKER.put(player.getUniqueId(), tracked = gameManager.getSpeedrunners().get(0));
                } else {
                    tracked = TRACKER.get(player.getUniqueId());
                }

                if (event.getAction().name().contains("RIGHT")) {
                    int index = gameManager.getSpeedrunners().indexOf(TRACKER.get(player.getUniqueId())) + 1;
                    if (index >= gameManager.getSpeedrunners().size()) {
                        index -= gameManager.getSpeedrunners().size();
                    }

                    tracked = gameManager.getSpeedrunners().get(index);
                }

                if (tracked.isDead()) {
                    player.sendMessage("§c" + tracked.getCraftPlayer().getName() + " est mort...");
                    return;
                }

                TRACKER.put(player.getUniqueId(), tracked);

                String distance = "?";
                if (tracked.getCraftPlayer() != null && player.getWorld().equals(tracked.getCraftPlayer().getWorld())) {
                    distance = "" + (int) tracked.getCraftPlayer().getLocation().distance(player.getLocation());
                }

                player.sendMessage("§fLa boussole pointe vers §a" + tracked.getName() + " §7(" + distance + " blocs)");

                CompassMeta compassMeta = ((CompassMeta) itemStack.getItemMeta());

                if (!tracked.getCraftPlayer().getWorld().equals(player.getWorld())) {
                    compassMeta.setLodestone(player.getLocation());
                } else {
                    Location location = tracked.getCraftPlayer().getLocation();
                    location.setY(0);
                    location.getBlock().setType(Material.LODESTONE);

                    compassMeta.setLodestone(location);
                }

                compassMeta.setLodestoneTracked(true);

                itemStack.setItemMeta(compassMeta);

                if (player.getInventory().getItemInMainHand().getType() == Material.COMPASS) {
                    player.getInventory().setItemInMainHand(itemStack);
                } else if (player.getInventory().getItemInOffHand().getType() == Material.COMPASS) {
                    player.getInventory().setItemInOffHand(itemStack);
                }
            }
        } else if (itemStack.getType() == Material.TOTEM_OF_UNDYING) {
            if (itemStack.hasItemMeta() && itemStack.getItemMeta().getDisplayName().equalsIgnoreCase("§aTotem de résurrection")) {
                if (!gamePlayer.isRunner()) {
                    event.setCancelled(true);
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                for (GamePlayer speedrunner : gameManager.getSpeedrunners()) {
                    if (speedrunner.equals(gamePlayer)) {
                        continue;
                    }

                    if (speedrunner.isDead()) {
                        if (speedrunner.getCraftPlayer() != null && speedrunner.getCraftPlayer().isOnline()) {
                            speedrunner.revive(gamePlayer.getCraftPlayer().getLocation());

                            itemStack.setType(Material.AIR);
                            if (gamePlayer.getCraftPlayer().getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING) {
                                gamePlayer.getCraftPlayer().getInventory().setItemInMainHand(itemStack);
                            } else if (gamePlayer.getCraftPlayer().getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
                                gamePlayer.getCraftPlayer().getInventory().setItemInOffHand(itemStack);
                            }

                            return;
                        }
                    }
                }

                player.sendMessage("§cIl n'y a personne a ressuciter...");
            }
        }
    }

}
