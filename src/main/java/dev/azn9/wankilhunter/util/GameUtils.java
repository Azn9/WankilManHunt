package dev.azn9.wankilhunter.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class GameUtils {

    public static String getDistanceBetweenLocations(Location firstLocation, Location secondLocation) {
        if (firstLocation.getWorld().equals(secondLocation.getWorld())) {
            double distancez;
            double xp = firstLocation.getBlockX();
            double zp = firstLocation.getBlockZ();
            double xl = secondLocation.getBlockX();
            double zl = secondLocation.getBlockZ();
            double distancex = xp - xl;

            if (distancex < 0.0) {
                distancex = -distancex;
            }
            if ((distancez = zp - zl) < 0.0) {
                distancez = -distancez;
            }

            double distance = Math.sqrt(Math.pow(distancex, 2.0) + Math.pow(distancez, 2.0));

            return "" + (int) distance;
        }
        return "?";
    }

    public static String getDistanceBetweenPlayerAndLocation(Player player, Location location) {
        return getDistanceBetweenLocations(player.getLocation(), location);
    }

    public static String getArrowCharByAngle(double angle) {
        String c = "";
        if (angle == -2.0) {
            c = "";
        } else if (angle == -1.0) {
            c = "✖";
        } else if (angle < 22.5 && angle >= 0.0 || angle > 337.5) {
            c = "⬆";
        } else if (angle < 67.5 && angle > 22.5) {
            c = "⬈";
        } else if (angle < 112.5 && angle > 67.5) {
            c = "➡";
        } else if (angle < 157.5 && angle > 112.5) {
            c = "⬊";
        } else if (angle < 202.5 && angle > 157.5) {
            c = "⬇";
        } else if (angle < 247.5 && angle > 202.5) {
            c = "⬋";
        } else if (angle < 292.5 && angle > 247.5) {
            c = "⬅";
        } else if (angle < 337.5 && angle > 292.5) {
            c = "⬉";
        }
        return c;
    }

    public static double getAngleBetweenPlayerAndLocation(Player p, Location Loc) {
        Location Locp = p.getLocation();
        if (Locp.getWorld().equals(Loc.getWorld())) {
            if (Locp.getBlockX() != Loc.getBlockX() || Locp.getBlockZ() != Loc.getBlockZ()) {
                double xp = Locp.getBlockX();
                double zp = Locp.getBlockZ();
                double xl = Loc.getBlockX();
                double zl = Loc.getBlockZ();
                double distancex = xp - xl;
                double distancecx = distancex < 0.0 ? -distancex : distancex;
                double distancez = zp - zl;
                double distancecz = distancez < 0.0 ? -distancez : distancez;
                double angle = 180.0 * Math.atan(distancecz / distancecx) / 3.141592653589793;

                if (distancex < 0.0 || distancez < 0.0) {
                    if (distancex < 0.0 && distancez >= 0.0) {
                        angle = 90.0 - angle + 90.0;
                    } else if (distancex <= 0.0 && distancez < 0.0) {
                        angle += 180.0;
                    } else if (distancex > 0.0 && distancez < 0.0) {
                        angle = 90.0 - angle + 270.0;
                    }
                }
                if ((angle += 270.0) >= 360.0) {
                    angle -= 360.0;
                }
                if ((angle -= p.getEyeLocation().getYaw() + 180.0f) <= 0.0) {
                    angle += 360.0;
                }
                if (angle <= 0.0) {
                    angle += 360.0;
                }
                return angle;
            }
            return -1.0;
        }
        return -2.0;
    }

    public static String getArrowCharByAngleBetweenPlayerAndLocation(Player player, Location location) {
        return getArrowCharByAngle(getAngleBetweenPlayerAndLocation(player, location));
    }

    public static String getArrowCharAndDistanceBetweenPlayerAndLocation(Player player, Location location) {
        String arrowChar = getArrowCharByAngleBetweenPlayerAndLocation(player, location);
        return (arrowChar.isEmpty() ? "" : arrowChar + " ") + getDistanceBetweenPlayerAndLocation(player, location);
    }

}
