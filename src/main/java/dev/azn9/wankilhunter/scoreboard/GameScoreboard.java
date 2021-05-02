package dev.azn9.wankilhunter.scoreboard;

import dev.azn9.wankilhunter.player.GamePlayer;
import dev.azn9.wankilhunter.util.GameUtils;

import java.util.List;

public class GameScoreboard implements IScoreboard {

    @Override
    public void init(GamePlayer gamePlayer) {

    }

    @Override
    public void show(GamePlayer gamePlayer) {
        gamePlayer.getScoreboard().clearLines();
        this.init(gamePlayer);
    }

    public void updateDistance(GamePlayer gamePlayer, List<GamePlayer> gamePlayerList) {
        StringBuilder s = new StringBuilder();

        for (GamePlayer player : gamePlayerList) {
            if (player.equals(gamePlayer) || player.isDead() || player.getCraftPlayer() == null || !player.getCraftPlayer().isOnline()) {
                continue;
            }

            s.append("§a").append(player.getName()).append(" §f").append(GameUtils.getArrowCharAndDistanceBetweenPlayerAndLocation(gamePlayer.getCraftPlayer(), player.getCraftPlayer().getLocation())).append(" §7| ");
        }

        if (s.length() > 5) {
            gamePlayer.sendActionBar(s.substring(0, s.length() - 5));
        }
    }

}
