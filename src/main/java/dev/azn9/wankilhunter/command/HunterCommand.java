package dev.azn9.wankilhunter.command;

import dev.azn9.wankilhunter.game.GameManager;
import dev.azn9.wankilhunter.game.GameState;
import dev.azn9.wankilhunter.injector.Inject;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HunterCommand extends Command {

    @Inject
    private static GameManager gameManager;

    public HunterCommand() {
        super("hunter");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (gameManager.getState() != GameState.WAITING) {
            sender.sendMessage("§cUne partie est déjà en cours !");
            return false;
        }

        sender.sendMessage("§cVous avez rejoint l'équipe des hunters !");
        gameManager.setHunter((Player) sender);

        return true;
    }
}
