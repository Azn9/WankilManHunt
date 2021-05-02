package dev.azn9.wankilhunter.game;

import dev.azn9.wankilhunter.WankilHunter;
import dev.azn9.wankilhunter.injector.Inject;
import dev.azn9.wankilhunter.scoreboard.GameScoreboard;

class GameLoop implements Runnable {

    @Inject
    private static WankilHunter plugin;

    @Inject
    private static GameManager gameManager;
    int i = 0;
    private int taskId;

    void startTask() {
        this.taskId = plugin.getServer().getScheduler().runTaskTimer(plugin, this, 20L, 5L).getTaskId();
    }

    @Override
    public void run() {
        i++;
        if (i >= 4) {
            gameManager.increaseTimeElapsed();
            i = 0;
        }

        gameManager.forAllPlayers(gamePlayer -> {
            GameScoreboard scoreboard = gamePlayer.getGameScoreboard();

            if (scoreboard == null) {
                return;
            }

            if (gamePlayer.isRunner()) {
                scoreboard.updateDistance(gamePlayer, gameManager.getSpeedrunners());
            }

            if (gamePlayer.isHunter()) {
                scoreboard.updateDistance(gamePlayer, gameManager.getHunters());
            }
        });

        gameManager.checkWin();
    }

    int getTaskId() {
        return this.taskId;
    }
}
