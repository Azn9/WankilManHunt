package dev.azn9.wankilhunter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.azn9.wankilhunter.command.DebugCommand;
import dev.azn9.wankilhunter.command.GetStarterCommand;
import dev.azn9.wankilhunter.command.HunterCommand;
import dev.azn9.wankilhunter.command.RunnerCommand;
import dev.azn9.wankilhunter.command.SetStarterCommand;
import dev.azn9.wankilhunter.command.StartCommand;
import dev.azn9.wankilhunter.game.GameManager;
import dev.azn9.wankilhunter.injector.Inject;
import dev.azn9.wankilhunter.injector.Injector;
import dev.azn9.wankilhunter.json.JsonConfig;
import dev.azn9.wankilhunter.listeners.CustomItemListener;
import dev.azn9.wankilhunter.listeners.DamageListener;
import dev.azn9.wankilhunter.listeners.PlayerConnectionListener;
import dev.azn9.wankilhunter.listeners.WorldListener;
import dev.azn9.wankilhunter.util.json.ItemStackTypeAdapter;
import dev.azn9.wankilhunter.util.json.LocationTypeAdapter;
import dev.azn9.wankilhunter.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandMap;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;

public final class WankilHunter extends JavaPlugin {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Location.class, new LocationTypeAdapter())
            .registerTypeAdapter(ItemStack.class, new ItemStackTypeAdapter())
            .registerTypeAdapter(CraftItemStack.class, new ItemStackTypeAdapter())
            .create();
    @Inject
    private static WorldManager worldManager;
    @Inject
    private static GameManager gameManager;

    @Override
    public void onEnable() {
        JsonConfig jsonConfig = loadConfiguration();

        if (jsonConfig == null) {
            return;
        }

        Injector injector = new Injector();
        injector.registerInjection(this);
        injector.startInjection();

        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new PlayerConnectionListener(), this);
        pluginManager.registerEvents(new WorldListener(), this);
        pluginManager.registerEvents(new DamageListener(), this);
        pluginManager.registerEvents(new CustomItemListener(), this);

        CommandMap commandMap = this.getServer().getCommandMap();
        String pluginName = this.getDescription().getName();

        commandMap.register(pluginName, new StartCommand());
        commandMap.register(pluginName, new SetStarterCommand());
        commandMap.register(pluginName, new GetStarterCommand());
        commandMap.register(pluginName, new HunterCommand());
        commandMap.register(pluginName, new RunnerCommand());
        commandMap.register(pluginName, new DebugCommand());

        Bukkit.getScheduler().runTask(this, () -> {
            worldManager.init();

            if (!worldManager.ensureLobbyWorldExists()) {
                this.getLogger().severe("Could not load lobby world. Aborting load.");
                this.getServer().shutdown();
            }

            gameManager.init(jsonConfig);

            this.getServer().getOnlinePlayers().forEach(gameManager::addPlayer);
        });
    }

    private JsonConfig loadConfiguration() {
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdirs();
            this.saveResource("config.json", false);
        }

        JsonConfig config = null;
        Throwable throwable = null;

        try (FileReader reader = new FileReader(new File(this.getDataFolder(), "config.json"))) {
            config = GSON.fromJson(reader, JsonConfig.class);
        } catch (Exception ex) {
            throwable = ex;
        }

        if (config == null) {
            if (throwable != null) {
                this.getLogger().log(Level.SEVERE, "Could not read config.json", throwable);
            } else {
                this.getLogger().log(Level.SEVERE, "Could not read config.json");
            }

            return null;
        }

        return config;
    }

    @Override
    public void saveConfig() {
        try (PrintWriter writer = new PrintWriter(new File(getDataFolder(), "config.json"))) {
            GSON.toJson(gameManager.getConfig(), writer);
            writer.flush();
        } catch (IOException ex) {
            this.getLogger().log(Level.SEVERE, "Could not save config.json", ex);
        }
    }
}
