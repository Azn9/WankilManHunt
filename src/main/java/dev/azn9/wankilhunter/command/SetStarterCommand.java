package dev.azn9.wankilhunter.command;

import dev.azn9.wankilhunter.WankilHunter;
import dev.azn9.wankilhunter.game.GameManager;
import dev.azn9.wankilhunter.injector.Inject;
import dev.azn9.wankilhunter.json.JsonStarter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

public class SetStarterCommand extends Command {

    @Inject
    private static WankilHunter plugin;

    @Inject
    private static GameManager gameManager;

    public SetStarterCommand() {
        super("setstarter");
        setPermission("deathswap.command.starter");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (!this.testPermission(sender) || !(sender instanceof Player)) {
            return false;
        }

        JsonStarter starter = gameManager.getConfig().getStarter();
        Player player = (Player) sender;
        PlayerInventory inventory = player.getInventory();

        // No need to get Armor Contents/Extra Contents (Off hand) because getContents() already includes them
        starter.setInventory(inventory.getContents());
        inventory.clear();

        plugin.saveConfig();

        player.sendMessage("§aStarter sauvegardé !");

        return true;
    }
}
