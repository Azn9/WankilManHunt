package dev.azn9.wankilhunter.scoreboard;

import dev.azn9.wankilhunter.player.GamePlayer;

public class WaitingScoreboard implements IScoreboard {

    @Override
    public void init(GamePlayer gamePlayer) {
        int index = 0;

        gamePlayer.getScoreboard().setLine(index++, "§a");
        gamePlayer.getScoreboard().setLine(index++, "Mode de jeu réimaginé");
        gamePlayer.getScoreboard().setLine(index++, "à la §eWankil §fet");
        gamePlayer.getScoreboard().setLine(index++, "développé par §bAzn9");
        gamePlayer.getScoreboard().setLine(index++, "§a");
        gamePlayer.getScoreboard().setLine(index, "§ewankil.fr");
    }

    @Override
    public void show(GamePlayer gamePlayer) {
        gamePlayer.getScoreboard().clearLines();
        this.init(gamePlayer);
    }
}
