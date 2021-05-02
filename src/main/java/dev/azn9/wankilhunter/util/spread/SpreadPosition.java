package dev.azn9.wankilhunter.util.spread;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.Random;

public class SpreadPosition {

    public double x;
    public double z;

    public double dist(SpreadPosition other) {
        double xDistance = this.x - other.x;
        double zDistance = this.z - other.z;

        return Math.sqrt(xDistance * xDistance + zDistance * zDistance);
    }

    public void normalize() {
        double length = this.getLength();

        this.x /= length;
        this.z /= length;
    }

    double getLength() {
        return Math.sqrt(this.x * this.x + this.z * this.z);
    }

    public void moveAway(SpreadPosition position) {
        this.x -= position.x;
        this.z -= position.z;
    }

    public boolean clamp(double minX, double minZ, double maxX, double maxZ) {
        boolean changed = false;

        if (this.x < minX) {
            this.x = minX;
            changed = true;
        } else if (this.x > maxX) {
            this.x = maxX;
            changed = true;
        }

        if (this.z < minZ) {
            this.z = minZ;
            changed = true;
        } else if (this.z > maxZ) {
            this.z = maxZ;
            changed = true;
        }

        return changed;
    }

    public int getSpawnY(World level, int maxHeight) {
        Location pos = new Location(level, this.x, maxHeight + 1, this.z);

        boolean headAir = pos.getBlock().getType() == Material.AIR;
        pos.setY(pos.getY() - 1);
        boolean feetAir = pos.getBlock().getType() == Material.AIR;

        while (pos.getY() > 0) {
            pos.setY(pos.getY() - 1);

            boolean air = pos.getBlock().getType() == Material.AIR;

            if (!air && headAir && feetAir) {
                return pos.getBlockY() + 1;
            }

            headAir = feetAir;
            feetAir = air;
        }

        return maxHeight + 1;
    }

    public boolean isSafe(World level, int maxHeight) {
        Location pos = new Location(level, this.x, this.getSpawnY(level, maxHeight) - 1, this.z);
        Material material = pos.getBlock().getType();
        return pos.getY() < maxHeight && material.isSolid() && material != Material.FIRE;
    }

    public void randomize(Random random, double minX, double minZ, double maxX, double maxZ) {
        this.x = random.nextDouble() * (maxX - minX) + minX;
        this.z = random.nextDouble() * (maxZ - minZ) + minZ;
    }

    @Override
    public String toString() {
        return "SpreadPosition{" +
                "x=" + this.x +
                ", z=" + this.z +
                '}';
    }
}
