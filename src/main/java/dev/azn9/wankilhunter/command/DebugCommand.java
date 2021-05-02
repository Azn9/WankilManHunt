package dev.azn9.wankilhunter.command;

import dev.azn9.wankilhunter.game.GameManager;
import dev.azn9.wankilhunter.injector.Inject;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class DebugCommand extends Command {

    @Inject
    private static GameManager gameManager;

    public DebugCommand() {
        super("wdebug");
        setPermission("hunter.command.debug");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (!this.testPermission(sender)) {
            return false;
        }

        if (args.length == 0) {
            return true;
        }

        switch (args[0]) {
            case "settime": {
                if (args.length == 1) {
                    return true;
                }

                gameManager.setTimeElapsed(Integer.parseInt(args[1]));
            }
        }

        return true;
    }
}
