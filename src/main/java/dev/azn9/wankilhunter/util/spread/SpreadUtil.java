package dev.azn9.wankilhunter.util.spread;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

public class SpreadUtil {

    private static final Random RANDOM = new Random();
    private static final Logger LOGGER = Bukkit.getLogger();
    private static final int MAX_HEIGHT = 230;

    public static SpreadPosition[] createSpawns(World level, Location center, int amount, float spreadDistance, float maxRange) {
        double minX = center.getBlockX() - maxRange;
        double minZ = center.getBlockZ() - maxRange;
        double maxX = center.getBlockX() + maxRange;
        double maxZ = center.getBlockZ() + maxRange;

        SpreadPosition[] initialPositions = SpreadUtil.createInitialPositions(amount, minX, minZ, maxX, maxZ);

        SpreadUtil.spreadPositions(center, spreadDistance, level, minX, minZ, maxX, maxZ, initialPositions);

        LOGGER.info("Created " + initialPositions.length + " spawn positions.");

        return initialPositions;
    }

    private static SpreadPosition[] createInitialPositions(int amount, double minX, double minZ, double maxX, double maxZ) {
        SpreadPosition[] positions = new SpreadPosition[amount];

        for (int i = 0; i < positions.length; i++) {
            (positions[i] = new SpreadPosition()).randomize(RANDOM, minX, minZ, maxX, maxZ);
        }

        LOGGER.info(Arrays.toString(positions));

        return positions;
    }

    private static void spreadPositions(Location center, float spreadDistance, World level, double minX, double minZ, double maxX, double maxZ, SpreadPosition[] positions) {
        boolean shouldContinue = true;
        double distance = Double.MAX_VALUE;
        int tries;

        for (tries = 0; tries < 10000 && shouldContinue; tries++) {
            shouldContinue = false;
            distance = Double.MAX_VALUE;

            for (int i = 0; i < positions.length; i++) {
                SpreadPosition position = positions[i];
                int closePositions = 0;
                SpreadPosition tempPosition = new SpreadPosition();

                for (int j = 0; j < positions.length; j++) {
                    if (j == i) {
                        continue;
                    }

                    SpreadPosition otherPosition = positions[j];
                    double dist = position.dist(otherPosition);

                    distance = Math.min(dist, distance);
                    if (dist >= spreadDistance) {
                        continue;
                    }

                    closePositions++;

                    tempPosition.x = tempPosition.x + (otherPosition.x - position.x);
                    tempPosition.z = tempPosition.z + (otherPosition.z - position.z);
                }

                if (closePositions > 0) {
                    tempPosition.x = tempPosition.x / (double) closePositions;
                    tempPosition.z = tempPosition.z / (double) closePositions;

                    double length = tempPosition.getLength();
                    if (length > 0.0D) {
                        tempPosition.normalize();
                        position.moveAway(tempPosition);
                    } else {
                        position.randomize(RANDOM, minX, minZ, maxX, maxZ);
                    }

                    shouldContinue = true;
                }

                if (!position.clamp(minX, minZ, maxX, maxZ)) {
                    continue;
                }

                shouldContinue = true;
            }

            if (shouldContinue) {
                continue;
            }

            for (SpreadPosition position : positions) {
                if (position.isSafe(level, MAX_HEIGHT)) {
                    continue;
                }

                position.randomize(RANDOM, minX, minZ, maxX, maxZ);
                shouldContinue = true;
            }
        }

        if (distance == Double.MAX_VALUE) {
            distance = 0.0D;
        }

        if (tries >= 10000) {
            LOGGER.info(String.format("Could not spread %s entities around %s, %s (too many entities for space - try using spread of at most %s)", positions.length, center.getBlockX(), center.getBlockZ(), String.format("%.2f", distance)));
        }
    }
}
