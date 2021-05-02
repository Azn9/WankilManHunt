package dev.azn9.wankilhunter.util;

import org.bukkit.ChatColor;

public class ScoreboardTitleAnimator {

    private final char[] chars;

    private final boolean bold;

    private final ChatColor mainColor;
    private final ChatColor secondaryColor;

    private int index;
    private String currentTitle;

    public ScoreboardTitleAnimator(String toAnimate) {
        this(toAnimate, true, ChatColor.YELLOW, ChatColor.GOLD);
    }

    public ScoreboardTitleAnimator(String toAnimate, boolean bold, ChatColor mainColor, ChatColor secondaryColor) {
        this.chars = toAnimate.toCharArray();

        this.bold = bold;

        this.mainColor = mainColor;
        this.secondaryColor = secondaryColor;

        this.index = -10;
        this.currentTitle = toAnimate;
    }

    public boolean next() {
        StringBuilder builder;

        if (this.index >= -32 && this.index <= -30 || this.index >= -26 && this.index <= -24 || this.index >= -20 && this.index <= 0) {
            builder = new StringBuilder(this.mainColor + (this.bold ? "§l" : ""));
        } else {
            builder = new StringBuilder("§f" + (this.bold ? "§l" : ""));
        }

        int i = 0;
        for (char c : this.chars) {
            i++;
            if (this.index == i) {
                builder.append(this.secondaryColor).append(this.bold ? "§l" : "").append(c).append(this.mainColor).append(this.bold ? "§l" : "");
            } else {
                builder.append(c);
            }
        }

        if (this.chars.length == this.index) {
            this.index = -36;
        } else {
            this.index++;
        }

        String oldText = this.currentTitle;
        this.currentTitle = builder.toString();

        return !oldText.equals(this.currentTitle);
    }

    public String getCurrentTitle() {
        return this.currentTitle;
    }
}
