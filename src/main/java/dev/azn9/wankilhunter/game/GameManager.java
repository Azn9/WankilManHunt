package dev.azn9.wankilhunter.game;

import dev.azn9.wankilhunter.WankilHunter;
import dev.azn9.wankilhunter.injector.Inject;
import dev.azn9.wankilhunter.injector.ToInject;
import dev.azn9.wankilhunter.json.JsonConfig;
import dev.azn9.wankilhunter.player.GamePlayer;
import dev.azn9.wankilhunter.teleport.TeleportRunnable;
import dev.azn9.wankilhunter.util.ScoreboardTitleAnimator;
import net.minecraft.server.v1_16_R3.ChatComponentText;
import net.minecraft.server.v1_16_R3.PacketPlayOutTitle;
import net.minecraft.server.v1_16_R3.PacketPlayOutTitle.EnumTitleAction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice.MaterialChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@ToInject
public class GameManager {

    @Inject
    private static WankilHunter plugin;

    private final List<GamePlayer> hunters = new ArrayList<>();
    private final List<GamePlayer> speedrunners = new ArrayList<>();

    private JsonConfig config;
    private Map<UUID, GamePlayer> playersByUuid;
    private Random random;
    private ScoreboardTitleAnimator titleAnimator;
    private GameLoop gameLoop;
    private GameState state;
    private int timeElapsed;
    private boolean invincibility;
    private Team hunterTeam;
    private Team runnerTeam;
    private boolean objectifCompleted;
    private Location spawnLocation;

    public void init(JsonConfig config) {
        this.config = config;

        this.playersByUuid = new HashMap<>();
        this.random = new SecureRandom();
        this.titleAnimator = new ScoreboardTitleAnimator("WANKIL MANHUNT");

        this.gameLoop = new GameLoop();

        this.state = GameState.WAITING;
        this.timeElapsed = 0;

        this.invincibility = true;
        this.objectifCompleted = false;

        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            boolean changed = this.titleAnimator.next();

            if (!changed) {
                return;
            }

            String title = this.titleAnimator.getCurrentTitle();

            for (GamePlayer player : this.getAllPlayers()) {
                player.getScoreboard().setObjectiveName(title);
            }
        }, 2L, 2L);

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        this.hunterTeam = scoreboard.getTeam("hunters");
        if (this.hunterTeam == null) {
            this.hunterTeam = scoreboard.registerNewTeam("hunters");
            this.hunterTeam.setColor(ChatColor.RED);
            this.hunterTeam.setAllowFriendlyFire(true);
        }

        this.runnerTeam = scoreboard.getTeam("runners");
        if (this.runnerTeam == null) {
            this.runnerTeam = scoreboard.registerNewTeam("runners");
            this.runnerTeam.setColor(ChatColor.GREEN);
            this.runnerTeam.setAllowFriendlyFire(false);
        }

        for (String entry : this.runnerTeam.getEntries()) {
            this.runnerTeam.removeEntry(entry);
        }

        for (String entry : this.hunterTeam.getEntries()) {
            this.hunterTeam.removeEntry(entry);
        }

        ItemStack reviveItem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta reviveItemMeta = reviveItem.getItemMeta();
        reviveItemMeta.setDisplayName("§aTotem de résurrection");
        reviveItem.setItemMeta(reviveItemMeta);

        ShapedRecipe reviveRecipe = new ShapedRecipe(NamespacedKey.minecraft("revive"), reviveItem);
        reviveRecipe = reviveRecipe.shape("DWD", "WWW", "DWD");
        reviveRecipe.setIngredient('D', Material.DIAMOND);
        reviveRecipe.setIngredient('W', new MaterialChoice(Material.ACACIA_PLANKS, Material.BIRCH_PLANKS, Material.CRIMSON_PLANKS, Material.OAK_PLANKS, Material.DARK_OAK_PLANKS, Material.JUNGLE_PLANKS, Material.SPRUCE_PLANKS, Material.WARPED_PLANKS));

        plugin.getServer().addRecipe(reviveRecipe);

        ItemStack compassItem = new ItemStack(Material.COMPASS);
        ItemMeta compassItemMeta = compassItem.getItemMeta();
        compassItemMeta.setDisplayName("§cTracker");
        compassItemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        compassItemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
        compassItem.setItemMeta(compassItemMeta);

        ShapedRecipe compassRecipe = new ShapedRecipe(NamespacedKey.minecraft("tracker"), compassItem);
        compassRecipe = compassRecipe.shape("DID", "IRI", "DID");
        compassRecipe.setIngredient('D', Material.DIAMOND);
        compassRecipe.setIngredient('I', Material.IRON_INGOT);
        compassRecipe.setIngredient('R', Material.REDSTONE);

        plugin.getServer().addRecipe(compassRecipe);
    }

    public void startGame() {
        this.state = GameState.TELEPORTING;

        List<GamePlayer> spectators = this.getPlayers().parallelStream().filter(gamePlayer -> !gamePlayer.isHunter() && !gamePlayer.isRunner()).collect(Collectors.toList());

        List<GamePlayer> players = this.getPlayers();
        players.removeAll(spectators);

        new TeleportRunnable(players, () -> {
            this.spawnLocation = players.get(0).getCraftPlayer().getLocation();

            spectators.forEach(gamePlayer -> {
                gamePlayer.setSpectator(true);
                gamePlayer.getCraftPlayer().setGameMode(GameMode.SPECTATOR);
                gamePlayer.getCraftPlayer().teleport(this.spawnLocation);
            });

            for (GamePlayer player : players) {
                player.initialize(false);
                player.setDead(false);
                player.getGameScoreboard().show(player);

                player.getCraftPlayer().setGameMode(GameMode.SURVIVAL);

                if (player.isRunner()) {
                    player.getCraftPlayer().getInventory().setContents(this.config.getStarter().getInventory());
                    player.getCraftPlayer().setMaxHealth(26);
                }

                if (player.isHunter()) {
                    player.getCraftPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 400, 6, false, false));
                    player.getCraftPlayer().setWalkSpeed(0);
                }
            }

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "recipe give @a *");

            for (World world : Bukkit.getWorlds()) {
                world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                world.setGameRule(GameRule.KEEP_INVENTORY, true);
            }

            this.state = GameState.IN_GAME;

            this.gameLoop.startTask();

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                this.invincibility = false;

                plugin.getServer().broadcastMessage("§aLa période d'invincibilité est terminée !");
                plugin.getServer().broadcastMessage("§cLes hunters ont été relâchés !");

                for (GamePlayer gamePlayer : hunters) {
                    gamePlayer.getCraftPlayer().setWalkSpeed(0.2f);
                }
            }, 600L);
        });
    }

    public void checkWin() {
        if (objectifCompleted) {
            plugin.getServer().broadcastMessage("§fLes §aSpeedrunners §font gagné la partie !");
            this.sendTitle(this.speedrunners, "§6§l< GAGNANT >", "", 5, 65, 10);
            this.sendTitle(this.hunters, "", "§c< perdant >", 5, 65, 10);

            this.finishGame();
            return;
        }

        if (timeElapsed > 10800 || getSpeedrunners().parallelStream().allMatch(GamePlayer::isDead)) {
            plugin.getServer().broadcastMessage("§fLes §cHunters §font gagné la partie !");
            this.sendTitle(this.hunters, "§6§l< GAGNANT >", "", 5, 65, 10);
            this.sendTitle(this.speedrunners, "", "§c< perdant >", 5, 65, 10);

            this.finishGame();
        }
    }

    public void finishGame() {
        this.state = GameState.FINISHED;
        this.invincibility = true;
        this.objectifCompleted = false;

        plugin.getServer().getScheduler().cancelTask(this.gameLoop.getTaskId());

        for (GamePlayer player : this.getAllPlayers()) {
            player.getCraftPlayer().setGameMode(GameMode.SPECTATOR);
            player.getCraftPlayer().resetMaxHealth();
        }

        plugin.getServer().getScheduler().runTaskTimer(plugin, new Consumer<BukkitTask>() {
            private int timer;

            @Override
            public void accept(BukkitTask bukkitTask) {
                if (++this.timer == 60) {
                    Bukkit.getServer().shutdown();
                }
            }
        }, 20, 20);
    }

    public void resetGame() {
        this.state = GameState.WAITING;
        this.timeElapsed = 0;

        for (GamePlayer player : this.getAllPlayers()) {
            player.reset();
            player.getWaitingScoreboard().show(player);

            for (String entry : this.runnerTeam.getEntries()) {
                this.runnerTeam.removeEntry(entry);
            }

            for (String entry : this.hunterTeam.getEntries()) {
                this.hunterTeam.removeEntry(entry);
            }

            hunters.clear();
            speedrunners.clear();
        }
    }

    public int getTimeElapsed() {
        return this.timeElapsed;
    }

    public void setTimeElapsed(int timeElapsed) {
        this.timeElapsed = timeElapsed;
    }

    void increaseTimeElapsed() {
        ++this.timeElapsed;

        if (this.timeElapsed == 3600) {
            Bukkit.broadcastMessage("§cIl ne reste que 2 heures !");
        }

        if (this.timeElapsed == 7200) {
            Bukkit.broadcastMessage("§cIl ne reste que 1 heure !");
        }

        if (this.timeElapsed == 9000) {
            Bukkit.broadcastMessage("§cIl ne reste que 30 min !");
        }

        if (this.timeElapsed == 9900) {
            Bukkit.broadcastMessage("§cIl ne reste que 15 min !");
        }

        if (this.timeElapsed == 10500) {
            Bukkit.broadcastMessage("§cIl ne reste que 5 min !");
        }
    }

    public boolean hasInvincibility() {
        return this.invincibility;
    }

    public void addPlayer(Player player) {
        GamePlayer gamePlayer = this.playersByUuid.computeIfAbsent(player.getUniqueId(), uuid -> new GamePlayer(player));

        gamePlayer.initScoreboard(this.titleAnimator.getCurrentTitle());
        gamePlayer.initialize(true);

    }

    public void removePlayer(Player player) {
        GamePlayer gamePlayer = getPlayer(player);

        if (this.state == GameState.IN_GAME || this.state == GameState.TELEPORTING) {
            if (gamePlayer != null) {
                gamePlayer.setDead(true);
            }
        } else {
            this.playersByUuid.remove(player.getUniqueId());
            this.speedrunners.removeIf(gamePlayer1 -> gamePlayer1.getUniqueId().equals(gamePlayer.getUniqueId()));
            this.hunters.removeIf(gamePlayer1 -> gamePlayer1.getUniqueId().equals(gamePlayer.getUniqueId()));
            this.runnerTeam.removeEntry(player.getName());
            this.hunterTeam.removeEntry(player.getName());
        }

    }

    public void sendActionbar(Collection<GamePlayer> players, String message) {
        PacketPlayOutTitle packet = new PacketPlayOutTitle(EnumTitleAction.ACTIONBAR, new ChatComponentText(message));

        for (GamePlayer player : players) {
            player.sendPacket(packet);
        }
    }

    public void sendTitle(Collection<GamePlayer> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (title == null && subtitle == null) {
            PacketPlayOutTitle packet = new PacketPlayOutTitle(EnumTitleAction.CLEAR, null);

            for (GamePlayer player : players) {
                player.sendPacket(packet);
            }
            return;
        }

        PacketPlayOutTitle timesPacket = new PacketPlayOutTitle(fadeIn, stay, fadeOut);
        PacketPlayOutTitle subtitlePacket = new PacketPlayOutTitle(EnumTitleAction.SUBTITLE, new ChatComponentText(subtitle));
        PacketPlayOutTitle titlePacket = new PacketPlayOutTitle(EnumTitleAction.TITLE, new ChatComponentText(title));

        for (GamePlayer player : players) {
            player.sendPacket(timesPacket, subtitlePacket, titlePacket);
        }
    }

    public Random getRandom() {
        return this.random;
    }

    public GameState getState() {
        return this.state;
    }

    public List<GamePlayer> getPlayers() {
        List<GamePlayer> players = new ArrayList<>();

        for (GamePlayer player : this.playersByUuid.values()) {
            if (!player.isSpectator() && !player.isDead()) {
                players.add(player);
            }
        }

        return players;
    }

    public void setRunner(Player player) {
        GamePlayer gamePlayer = getPlayer(player);

        gamePlayer.setRunner(true);
        gamePlayer.setHunter(false);

        runnerTeam.addEntry(player.getName());
        hunterTeam.removeEntry(player.getName());

        hunters.remove(gamePlayer);
        if (!speedrunners.contains(gamePlayer)) {
            speedrunners.add(gamePlayer);
        }
    }

    public void setHunter(Player player) {
        GamePlayer gamePlayer = getPlayer(player);

        gamePlayer.setRunner(false);
        gamePlayer.setHunter(true);

        runnerTeam.removeEntry(player.getName());
        hunterTeam.addEntry(player.getName());

        if (!hunters.contains(gamePlayer)) {
            hunters.add(gamePlayer);
        }
        speedrunners.remove(gamePlayer);
    }

    public List<GamePlayer> getAllPlayers() {
        return new ArrayList<>(this.playersByUuid.values());
    }

    public void forAllPlayers(Consumer<GamePlayer> consumer) {
        this.getAllPlayers().listIterator().forEachRemaining(consumer);
    }

    public GamePlayer getPlayer(Player player) {
        return this.playersByUuid.get(player.getUniqueId());
    }

    public GamePlayer getPlayer(UUID uuid) {
        return this.playersByUuid.get(uuid);
    }

    public JsonConfig getConfig() {
        return this.config;
    }

    public void fixSpeedrunners() {
        List<GamePlayer> runners = new ArrayList<>(this.speedrunners);
        this.speedrunners.clear();
        runners.parallelStream().map(GamePlayer::getCraftPlayer).map(CraftPlayer::getUniqueId).distinct().map(playersByUuid::get).forEach(this.speedrunners::add);
    }

    public List<GamePlayer> getSpeedrunners() {
        return this.speedrunners;
    }

    public List<GamePlayer> getHunters() {
        return this.hunters;
    }

    public void setObjectifCompleted(boolean objectifCompleted) {
        this.objectifCompleted = objectifCompleted;
    }

    public Location getSpawnLocation() {
        return this.spawnLocation;
    }
}
