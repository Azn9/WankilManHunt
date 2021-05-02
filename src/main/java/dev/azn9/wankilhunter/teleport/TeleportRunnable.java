package dev.azn9.wankilhunter.teleport;

import dev.azn9.wankilhunter.WankilHunter;
import dev.azn9.wankilhunter.game.GameManager;
import dev.azn9.wankilhunter.injector.Inject;
import dev.azn9.wankilhunter.player.GamePlayer;
import dev.azn9.wankilhunter.util.spread.SpreadPosition;
import dev.azn9.wankilhunter.util.spread.SpreadUtil;
import dev.azn9.wankilhunter.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class TeleportRunnable implements Runnable {

    @Inject
    private static WankilHunter plugin;

    @Inject
    private static GameManager gameManager;

    @Inject
    private static WorldManager worldManager;

    private final Runnable callback;
    private final Set<TeleportArea> areas;
    private final Queue<TeleportArea> queuedAreas;
    private final long start;
    private final BukkitTask task;

    private int logCounter;

    public TeleportRunnable(List<GamePlayer> players, Runnable callback) {
        this.callback = callback;

        this.areas = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.queuedAreas = new ConcurrentLinkedQueue<>();
        this.start = System.currentTimeMillis();

        World gameWorld = worldManager.createGameWorld();

        if (gameWorld == null) {
            Bukkit.broadcastMessage("§cUne erreur est survenue pendant la création du monde !");

            gameManager.resetGame();

            this.task = null;

            return;
        }

        SpreadPosition[] spawns = SpreadUtil.createSpawns(gameWorld, new Location(gameWorld, 0, 0, 0), 1, 100, 500);
        SpreadPosition spawn = spawns[0];
        this.areas.add(new TeleportArea(new Location(gameWorld, Location.locToBlock(spawn.x) + 0.5D, 0, Location.locToBlock(spawn.z) + 0.5D), players));

        this.queuedAreas.addAll(this.areas);

        this.task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this, 1L, 2L);
    }

    @Override
    public void run() {
        boolean finished = this.isFinished();
        int loadedCount = this.getLoadedCount();
        int totalAreas = this.areas.size();

        if (this.logCounter % 10 == 0) {
            plugin.getLogger().info("[TeleportRunnable] Task running (total areas: " + totalAreas + ", loaded areas: " + loadedCount + ", queued areas: " + this.queuedAreas.size() + ", finished: " + finished + ", tps: " + plugin.getServer().getTPS()[0] + ")");
        }

        this.logCounter = (this.logCounter + 1) % 10;

        int loadedChunks = 0;
        int chunksToLoad = 0;

        for (TeleportArea area : this.areas) {
            loadedChunks += area.getLoadedCount();
            chunksToLoad += area.getChunksToLoad();
        }

        this.sendProgress(loadedChunks / (float) chunksToLoad);

        if (!this.queuedAreas.isEmpty()) {
            this.queuedAreas.remove().load(area -> {
                area.generatePlatform();
                area.teleportPlayers();

                if (this.isFinished()) {
                    plugin.getLogger().info("[TeleportRunnable] All areas loaded (" + (System.currentTimeMillis() - this.start) + "ms)");
                }
            });
        } else if (finished) {
            this.task.cancel();
            plugin.getLogger().info("[TeleportRunnable] Task finished. Executing callback.");

            plugin.getServer().getScheduler().runTaskTimer(plugin, new Consumer<BukkitTask>() {
                private final TeleportRunnable runnable = TeleportRunnable.this;

                private int timer = 5;

                @Override
                public void accept(BukkitTask bukkitTask) {
                    if (--this.timer == 0) {
                        bukkitTask.cancel();

                        for (TeleportArea area : this.runnable.areas) {
                            area.breakPlatform();
                        }

                        this.runnable.callback.run();
                        return;
                    }

                    if (this.timer == 4) {
                        for (TeleportArea area : this.runnable.areas) {
                            area.teleportPlayers();
                        }
                    }

                    gameManager.sendTitle(gameManager.getAllPlayers(), "", "§e" + this.timer, 0, 30, 10);
                }
            }, 20L, 20L);
        }
    }

    private void sendProgress(float progress) {
        StringBuilder builder = new StringBuilder("§a||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");

        builder.insert(2 + (int) Math.floor(progress * 100.0F), "§c");

        gameManager.sendActionbar(gameManager.getAllPlayers(), builder.toString());
    }

    private boolean isFinished() {
        for (TeleportArea area : this.areas) {
            if (!area.isLoaded()) {
                return false;
            }
        }

        return true;
    }

    private int getLoadedCount() {
        int count = 0;

        for (TeleportArea area : this.areas) {
            if (area.isLoaded()) {
                count++;
            }
        }

        return count;
    }
}
