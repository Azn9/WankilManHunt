package dev.azn9.wankilhunter.listeners;

import dev.azn9.wankilhunter.game.GameManager;
import dev.azn9.wankilhunter.game.GameState;
import dev.azn9.wankilhunter.injector.Inject;
import dev.azn9.wankilhunter.player.GamePlayer;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    @Inject
    private static GameManager gameManager;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = gameManager.getPlayer(player);

        event.setJoinMessage("§a" + player.getName() + " §7a rejoint la partie");

        if (gameManager.getState() == GameState.IN_GAME) {
            if (gamePlayer == null || gamePlayer.isRunner() || gamePlayer.isSpectator()) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        } else if (gameManager.getState() == GameState.WAITING) {
            gameManager.addPlayer(player);

            if (player.isOp()) {
                player.sendMessage("§fTu peux récupérer l'inventaire de départ avec la commande §e/getstarter§f, le modifier en créatif et le sauvegarder avec la commande §e/setstarter");
            }

            player.sendMessage("§fTu peux rejoindre les §cHunters §favec la commande §c/hunter§f ou les §aSpeedrunners§f avec la commande §a/runner§f. Si tu ne choisis rien, tu seras en spectateur");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = gameManager.getPlayer(player);

        if (gameManager.getState() == GameState.IN_GAME && gamePlayer.isRunner()) {
            gamePlayer.setDead(true);
            event.setQuitMessage("§e" + player.getName() + " §7s'est déconnecté, et est par conséquent éliminé.");
            gameManager.checkWin();
        } else {
            event.setQuitMessage("§c" + player.getName() + " §7a quitté la partie");
        }

        if (gameManager.getState() == GameState.WAITING) {
            gameManager.removePlayer(player);
        }
    }

}
