package dev.azn9.wankilhunter.teleport;

import dev.azn9.wankilhunter.WankilHunter;
import dev.azn9.wankilhunter.game.GameManager;
import dev.azn9.wankilhunter.injector.Inject;
import dev.azn9.wankilhunter.player.GamePlayer;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

public class TeleportArea {

    @Inject
    private static WankilHunter plugin;

    @Inject
    private static GameManager gameManager;

    private final Location center;
    private final Location teleportLocation;
    private final Set<Chunk> chunks;
    private final Set<Block> platformBlocks;
    private final Set<UUID> players;

    private final int radius;
    private final int chunksToLoad;

    private boolean platformGenerated;
    private boolean playersTeleported;

    private boolean loaded;

    public TeleportArea(Location center, List<GamePlayer> players) {
        this.center = center;
        this.center.setY(this.center.getWorld().getHighestBlockYAt(this.center) + 3);
        this.teleportLocation = this.center.clone().add(0.0D, 1.0D, 0.0D);
        this.chunks = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.platformBlocks = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.players = new HashSet<>();

        this.radius = plugin.getServer().getViewDistance();
        this.chunksToLoad = (int) Math.pow(this.radius * 2 + 1, 2);

        for (GamePlayer player : players) {
            this.players.add(player.getUniqueId());
        }
    }

    public void load(Consumer<TeleportArea> callback) {
        Consumer<Chunk> chunkCallback = chunk -> {
            this.chunks.add(chunk);

            if (this.chunks.size() == chunksToLoad) {
                this.loaded = true;

                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(this));
            }
        };

        World world = this.center.getWorld();
        int baseX = this.center.getBlockX() >> 4;
        int baseZ = this.center.getBlockZ() >> 4;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                try {
                    world.getChunkAtAsync(baseX + x, baseZ + z, chunkCallback);
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.SEVERE, "Could not load chunk", ex);
                }
            }
        }
    }

    void breakPlatform() {
        if (!this.platformGenerated) {
            return;
        }

        World world = this.center.getWorld();

        for (Block block : this.platformBlocks) {
            if (gameManager.getRandom().nextInt(10) == 0) {
                world.playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());
            }

            block.setType(Material.AIR);
        }

        this.platformBlocks.clear();
    }

    void generatePlatform() {
        if (this.platformGenerated) {
            return;
        }

        this.platformGenerated = true;

        int radius = 4;
        World world = this.center.getWorld();
        int baseX = this.center.getBlockX();
        int baseY = this.center.getBlockY();
        int baseZ = this.center.getBlockZ();

        for (int x = -radius; x <= radius; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = world.getBlockAt(baseX + x, baseY + y, baseZ + z);

                    if (y == 0) {
                        block.setType(Material.WHITE_STAINED_GLASS, false);
                    } else if (x == -radius || x == radius || z == -radius || z == radius) {
                        block.setType(Material.BARRIER, false);
                    } else {
                        continue;
                    }

                    this.platformBlocks.add(block);
                }
            }
        }
    }

    void teleportPlayers() {
        if (this.playersTeleported) {
            return;
        }

        this.playersTeleported = true;

        for (UUID uuid : this.players) {
            GamePlayer player = gameManager.getPlayer(uuid);

            if (player != null) {
                player.getCraftPlayer().teleport(this.teleportLocation);
            }
        }
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    public int getLoadedCount() {
        return this.chunks.size();
    }

    public int getChunksToLoad() {
        return this.chunksToLoad;
    }
}
