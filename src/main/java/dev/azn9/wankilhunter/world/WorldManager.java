package dev.azn9.wankilhunter.world;

import dev.azn9.wankilhunter.WankilHunter;
import dev.azn9.wankilhunter.injector.Inject;
import dev.azn9.wankilhunter.injector.ToInject;
import dev.azn9.wankilhunter.util.IOUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;

@ToInject
public class WorldManager {

    private static final String LOBBY_URL = "https://i.roro.ovh/minecraft/lobby-1.16.tar.gz";
    @Inject
    private static WankilHunter plugin;
    private World gameWorld;
    private Location lobbyLocation;

    private World lobbyWorld;

    public void init() {
        this.gameWorld = plugin.getServer().getWorlds().get(0);
        this.lobbyLocation = new Location(null, 0.5D, 68.5D, 0.5D, 0.0F, 0.0F);
    }

    /**
     * Ensures the Lobby world is existing and loaded.
     * <p>
     * Downloads the lobby if no folder is found, and creates a new world if the world is not loaded.
     * <p>
     * A lobby world is defined by its name being "lobby" and by being loaded in the server.
     *
     * @return {@code true} if the lobby world is successfully loaded or already loaded. {@code false} if download failed or decompression failed
     */
    public boolean ensureLobbyWorldExists() {
        World world = plugin.getServer().getWorld("lobbyworld");

        if (world == null) { // Lobby world does not exist
            plugin.getLogger().info("Lobby world not found...");

            File worldFolder = new File("lobbyworld");

            if (!worldFolder.exists()) { // Lobby world folder does not exist
                plugin.getLogger().info("Lobby folder not found...");

                File tempFile = new File("lobbyworld.tar.gz");

                if (!tempFile.exists()) { // Compressed lobby does not exist, download it
                    try {
                        plugin.getLogger().info("Downloading lobby...");
                        IOUtil.download(LOBBY_URL, tempFile);
                        plugin.getLogger().info("Downloaded lobby!");
                    } catch (IOException ex) {
                        plugin.getLogger().log(Level.SEVERE, "Could not download lobby", ex);
                        return false;
                    }
                }

                // Extract lobby world
                try {
                    plugin.getLogger().info("Extracting lobby...");
                    IOUtil.decompressGzip(tempFile, worldFolder);
                    plugin.getLogger().info("Extracted lobby!");
                } catch (IOException ex) {
                    plugin.getLogger().log(Level.SEVERE, "Could not extract lobby", ex);
                    return false;
                }
            }

            // Load lobby world
            World lobbyWorld = plugin.getServer().createWorld(WorldCreator.name("lobbyworld").environment(Environment.NORMAL));

            if (lobbyWorld != null) {
                plugin.getLogger().info("Lobby successfully loaded!");
            } else {
                plugin.getLogger().severe("Lobby failed to load.");
            }

            this.setLobbyWorld(lobbyWorld);

            return lobbyWorld != null;
        }

        plugin.getLogger().info("Lobby world already loaded!");

        this.setLobbyWorld(world);

        return true;
    }

    public World getGameWorld() {
        return this.gameWorld;
    }

    public World getLobbyWorld() {
        return this.lobbyWorld;
    }

    private void setLobbyWorld(World lobbyWorld) {
        this.lobbyWorld = lobbyWorld;
        this.lobbyLocation.setWorld(lobbyWorld);
    }

    public Location getLobbyLocation() {
        return this.lobbyLocation;
    }

    public World createGameWorld() {
        if (true) {
            return Bukkit.getWorld("world");
        }

        String worldName = "game";

        World nether = Bukkit.getWorld("world_nether");
        if (nether != null) {
            Bukkit.unloadWorld(nether, false);
            nether.getWorldFolder().delete();

            new WorldCreator("world_nether").environment(Environment.NETHER).createWorld();
        }

        World end = Bukkit.getWorld("world_the_end");
        if (end != null) {
            Bukkit.unloadWorld(end, false);
            end.getWorldFolder().delete();

            new WorldCreator("world_the_end").environment(Environment.THE_END).createWorld();
        }

        World world = Bukkit.getWorld(worldName);

        if (world != null) {
            Bukkit.unloadWorld(world, false);

            File worldFolder = world.getWorldFolder();

            try {
                if (!worldFolder.delete()) {
                    throw new IOException();
                }
            } catch (IOException exception) {
                exception.printStackTrace();

                plugin.getLogger().severe("Could not remove the 'game' folder ! This game will use a random world name !");

                worldName += "-" + new Random().nextInt(100000);
            }
        }

        WorldCreator worldCreator = new WorldCreator(worldName);
        world = worldCreator.createWorld();

        if (world != null) {
            world.getWorldFolder().deleteOnExit();
        }

        return world;
    }
}
