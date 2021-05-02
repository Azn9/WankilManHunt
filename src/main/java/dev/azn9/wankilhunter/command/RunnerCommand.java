package dev.azn9.wankilhunter.command;

import dev.azn9.wankilhunter.game.GameManager;
import dev.azn9.wankilhunter.game.GameState;
import dev.azn9.wankilhunter.injector.Inject;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RunnerCommand extends Command {

    @Inject
    private static GameManager gameManager;

    public RunnerCommand() {
        super("runner");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (gameManager.getState() != GameState.WAITING) {
            sender.sendMessage("§cUne partie est déjà en cours !");
            return false;
        }

        sender.sendMessage("§aVous avez rejoint l'équipe des speedrunners !");
        gameManager.setRunner((Player) sender);

        return true;
    }
}
