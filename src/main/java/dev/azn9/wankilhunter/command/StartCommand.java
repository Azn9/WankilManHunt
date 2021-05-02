package dev.azn9.wankilhunter.command;

import dev.azn9.wankilhunter.game.GameManager;
import dev.azn9.wankilhunter.game.GameState;
import dev.azn9.wankilhunter.injector.Inject;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class StartCommand extends Command {

    @Inject
    private static GameManager gameManager;

    public StartCommand() {
        super("start");
        setPermission("hunter.command.start");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (!this.testPermission(sender)) {
            return false;
        }

        if (gameManager.getState() != GameState.WAITING) {
            sender.sendMessage("§cUne partie est déjà en cours !");
            return false;
        }

        if (gameManager.getPlayers().size() < 2) {
            sender.sendMessage("§cVous ne pouvez pas lancer une partie avec seulement un joueur !");
            //return false;
        }

        gameManager.startGame();

        return true;
    }
}
