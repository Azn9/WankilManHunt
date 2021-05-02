package dev.azn9.wankilhunter.player;

import dev.azn9.wankilhunter.game.GameManager;
import dev.azn9.wankilhunter.game.GameState;
import dev.azn9.wankilhunter.injector.Inject;
import dev.azn9.wankilhunter.scoreboard.GameScoreboard;
import dev.azn9.wankilhunter.scoreboard.WaitingScoreboard;
import dev.azn9.wankilhunter.util.ScoreboardSign;
import dev.azn9.wankilhunter.world.WorldManager;
import net.minecraft.server.v1_16_R3.ChatComponentText;
import net.minecraft.server.v1_16_R3.Packet;
import net.minecraft.server.v1_16_R3.PacketPlayOutTitle;
import net.minecraft.server.v1_16_R3.PacketPlayOutTitle.EnumTitleAction;
import net.minecraft.server.v1_16_R3.PlayerConnection;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.time.Instant;
import java.util.UUID;

public class GamePlayer {

    @Inject
    private static GameManager gameManager;

    @Inject
    private static WorldManager worldManager;

    private final UUID uuid;
    private final String name;
    private final CraftPlayer craftPlayer;

    private final ScoreboardSign scoreboard;
    private final WaitingScoreboard waitingScoreboard;
    private final GameScoreboard gameScoreboard;

    private boolean isSpectator;
    private boolean isDead;
    private boolean isRunner;
    private boolean isHunter;
    private Location deathLocation;
    private Instant lastHit;

    public GamePlayer(Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.craftPlayer = (CraftPlayer) player;

        this.scoreboard = new ScoreboardSign(this, "temporary name");

        this.waitingScoreboard = new WaitingScoreboard();
        this.gameScoreboard = new GameScoreboard();
    }

    public void initialize(boolean lobbyStuff) {
        this.craftPlayer.getInventory().clear();
        this.craftPlayer.setMaxHealth(20.0D);
        this.craftPlayer.setHealth(20.0D);
        this.craftPlayer.setFoodLevel(20);
        this.craftPlayer.setSaturation(20.0F);
        this.craftPlayer.setExhaustion(20.0F);
        this.craftPlayer.setWalkSpeed(0.2F);
        this.craftPlayer.setLevel(0);
        this.craftPlayer.setExp(0.0F);
        this.craftPlayer.setAllowFlight(false);
        this.craftPlayer.setFlying(false);

        for (PotionEffect effect : this.craftPlayer.getActivePotionEffects()) {
            this.craftPlayer.removePotionEffect(effect.getType());
        }

        if (lobbyStuff) {
            this.waitingScoreboard.show(this);

            this.craftPlayer.setGameMode(GameMode.ADVENTURE);
            this.craftPlayer.teleport(worldManager.getLobbyLocation());
        }
    }

    public void initScoreboard(String title) {
        this.scoreboard.setObjectiveName(title);
        this.scoreboard.create();
    }

    public void reset() {
        this.isDead = false;
        this.isSpectator = false;
        this.isRunner = false;
        this.isHunter = false;

        this.craftPlayer.getHandle().setArrowCount(0, true);

        this.initialize(true);
    }

    public void onDeath() {
        if (gameManager.getState() != GameState.IN_GAME) {
            return;
        }

        this.deathLocation = this.craftPlayer.getLocation();
        this.isDead = true;
        gameManager.checkWin();
    }

    public void revive(Location location) {
        this.isDead = false;

        this.craftPlayer.spigot().respawn();
        this.craftPlayer.setGameMode(GameMode.SURVIVAL);
        this.craftPlayer.setHealth(this.craftPlayer.getMaxHealth());
        this.craftPlayer.setFoodLevel(40);
        this.craftPlayer.getHandle().setArrowCount(0, true);
        this.craftPlayer.teleport(location);

        Bukkit.broadcastMessage("§e" + this.getName() + " a été ramené à la vie !");

        gameManager.fixSpeedrunners(); //For safety
    }

    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        PlayerConnection playerConnection = this.craftPlayer.getHandle().playerConnection;

        if (title == null && subtitle == null) {
            playerConnection.sendPacket(new PacketPlayOutTitle(EnumTitleAction.CLEAR, null));
            return;
        }

        playerConnection.sendPacket(new PacketPlayOutTitle(fadeIn, stay, fadeOut));
        playerConnection.sendPacket(new PacketPlayOutTitle(EnumTitleAction.SUBTITLE, new ChatComponentText(subtitle)));
        playerConnection.sendPacket(new PacketPlayOutTitle(EnumTitleAction.TITLE, new ChatComponentText(title)));
    }

    public void sendActionBar(String message) {
        this.craftPlayer.getHandle().playerConnection.sendPacket(new PacketPlayOutTitle(EnumTitleAction.ACTIONBAR, new ChatComponentText(message)));
    }

    public void sendMessage(String message) {
        this.craftPlayer.sendMessage(message);
    }

    public void sendPacket(Packet<?> packet) {
        this.craftPlayer.getHandle().playerConnection.sendPacket(packet);
    }

    public void sendPacket(Packet<?> packet, Packet<?>... packets) {
        PlayerConnection playerConnection = this.craftPlayer.getHandle().playerConnection;

        playerConnection.sendPacket(packet);

        for (Packet<?> tempPacket : packets) {
            playerConnection.sendPacket(tempPacket);
        }
    }

    public boolean isSpectator() {
        return this.isSpectator;
    }

    public void setSpectator(boolean spectator) {
        this.isSpectator = spectator;
    }

    public boolean isDead() {
        return this.isDead;
    }

    public void setDead(boolean dead) {
        this.isDead = dead;
    }

    public ScoreboardSign getScoreboard() {
        return this.scoreboard;
    }

    public WaitingScoreboard getWaitingScoreboard() {
        return this.waitingScoreboard;
    }

    public GameScoreboard getGameScoreboard() {
        return this.gameScoreboard;
    }

    public CraftPlayer getCraftPlayer() {
        return this.craftPlayer;
    }

    public UUID getUniqueId() {
        return this.uuid;
    }

    public String getName() {
        return this.name;
    }

    public boolean isRunner() {
        return this.isRunner;
    }

    public void setRunner(boolean runner) {
        this.isRunner = runner;
    }

    public boolean isHunter() {
        return this.isHunter;
    }

    public void setHunter(boolean hunter) {
        this.isHunter = hunter;
    }

    public Location getDeathLocation() {
        return this.deathLocation;
    }

    public void setLastHit() {
        this.lastHit = Instant.now();
    }

    public Instant getLastHit() {
        return this.lastHit;
    }
}
