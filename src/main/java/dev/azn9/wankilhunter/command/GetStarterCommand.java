package dev.azn9.wankilhunter.command;

import dev.azn9.wankilhunter.game.GameManager;
import dev.azn9.wankilhunter.injector.Inject;
import dev.azn9.wankilhunter.json.JsonStarter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

public class GetStarterCommand extends Command {

    @Inject
    private static GameManager gameManager;

    /**
     * Instantiate the /getstarter command.
     * <p>
     * This command gives the current starter configuration to the sender. 'Starter' means the inventory that is given when the game starts.
     *
     * @see SetStarterCommand
     * @since 1.0.0
     */
    public GetStarterCommand() {
        super("getstarter");
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

        inventory.setContents(starter.getInventory());

        player.sendMessage("§aStarter actuel récupéré !");

        return true;
    }
}
