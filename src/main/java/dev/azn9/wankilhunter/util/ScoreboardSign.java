package dev.azn9.wankilhunter.util;

import dev.azn9.wankilhunter.player.GamePlayer;
import dev.azn9.wankilhunter.util.reflect.FieldAccessor;
import dev.azn9.wankilhunter.util.reflect.ReflectionUtil;
import net.minecraft.server.v1_16_R3.ChatComponentText;
import net.minecraft.server.v1_16_R3.EnumChatFormat;
import net.minecraft.server.v1_16_R3.IChatBaseComponent;
import net.minecraft.server.v1_16_R3.IScoreboardCriteria.EnumScoreboardHealthDisplay;
import net.minecraft.server.v1_16_R3.Packet;
import net.minecraft.server.v1_16_R3.PacketListenerPlayOut;
import net.minecraft.server.v1_16_R3.PacketPlayOutScoreboardDisplayObjective;
import net.minecraft.server.v1_16_R3.PacketPlayOutScoreboardObjective;
import net.minecraft.server.v1_16_R3.PacketPlayOutScoreboardScore;
import net.minecraft.server.v1_16_R3.PacketPlayOutScoreboardTeam;
import net.minecraft.server.v1_16_R3.ScoreboardServer.Action;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ScoreboardSign {

    private static final FieldAccessor<String> OBJECTIVE__NAME = ReflectionUtil.getField(PacketPlayOutScoreboardObjective.class, "a");
    private static final FieldAccessor<IChatBaseComponent> OBJECTIVE__DISPLAY_NAME = ReflectionUtil.getField(PacketPlayOutScoreboardObjective.class, "b");
    private static final FieldAccessor<EnumScoreboardHealthDisplay> OBJECTIVE__HEALTH_DISPLAY = ReflectionUtil.getField(PacketPlayOutScoreboardObjective.class, "c");
    private static final FieldAccessor<Integer> OBJECTIVE__MODE = ReflectionUtil.getField(PacketPlayOutScoreboardObjective.class, "d");

    private static final FieldAccessor<Integer> DISPLAY_OBJECTIVE__SLOT = ReflectionUtil.getField(PacketPlayOutScoreboardDisplayObjective.class, "a");
    private static final FieldAccessor<String> DISPLAY_OBJECTIVE__NAME = ReflectionUtil.getField(PacketPlayOutScoreboardDisplayObjective.class, "b");

    private static final FieldAccessor<String> TEAM__NAME = ReflectionUtil.getField(PacketPlayOutScoreboardTeam.class, "a");
    private static final FieldAccessor<IChatBaseComponent> TEAM__DISPLAY_NAME = ReflectionUtil.getField(PacketPlayOutScoreboardTeam.class, "b");
    private static final FieldAccessor<IChatBaseComponent> TEAM__PREFIX = ReflectionUtil.getField(PacketPlayOutScoreboardTeam.class, "c");
    private static final FieldAccessor<IChatBaseComponent> TEAM__SUFFIX = ReflectionUtil.getField(PacketPlayOutScoreboardTeam.class, "d");
    private static final FieldAccessor<String> TEAM__NAME_TAG = ReflectionUtil.getField(PacketPlayOutScoreboardTeam.class, "e");
    private static final FieldAccessor<String> TEAM__PUSH = ReflectionUtil.getField(PacketPlayOutScoreboardTeam.class, "f");
    private static final FieldAccessor<EnumChatFormat> TEAM__COLOR = ReflectionUtil.getField(PacketPlayOutScoreboardTeam.class, "g");
    private static final FieldAccessor<Collection<String>> TEAM__ENTRIES = ReflectionUtil.getField(PacketPlayOutScoreboardTeam.class, "h");
    private static final FieldAccessor<Integer> TEAM__MODE = ReflectionUtil.getField(PacketPlayOutScoreboardTeam.class, "i");
    private static final FieldAccessor<Integer> TEAM__OPTIONS = ReflectionUtil.getField(PacketPlayOutScoreboardTeam.class, "j");

    private final VirtualTeam[] lines = new VirtualTeam[15];
    private final GamePlayer player;

    private boolean created = false;
    private String objectiveName;

    public ScoreboardSign(GamePlayer player, String objectiveName) {
        this.player = player;
        this.objectiveName = objectiveName;
    }

    public void create() {
        if (this.created) {
            return;
        }

        this.player.sendPacket(createObjectivePacket(0, this.objectiveName), setObjectiveSlot());

        int i = 0;
        while (i < this.lines.length) {
            sendLine(i++);
        }

        this.created = true;
    }

    public void destroy() {
        if (!this.created) {
            return;
        }

        this.player.sendPacket(createObjectivePacket(1, null));

        for (VirtualTeam team : this.lines) {
            if (team != null) {
                this.player.sendPacket(team.removeTeam());
            }
        }

        this.created = false;
    }

    public void setObjectiveName(String name) {
        this.objectiveName = name;

        if (this.created) {
            this.player.sendPacket(createObjectivePacket(2, name));
        }
    }

    public void setLine(int line, String value) {
        getOrCreateTeam(line).setValue(value);
        sendLine(line);
    }

    public void removeLine(int line) {
        VirtualTeam team = getOrCreateTeam(line);
        String old = team.getCurrentPlayer();

        if (old != null && this.created) {
            this.player.sendPacket(removeLine(old), team.removeTeam());
        }

        this.lines[line] = null;
    }

    public void clearLines() {
        for (int i = 0; i < this.lines.length; i++) {
            if (this.lines[i] != null) {
                this.removeLine(i);
            }
        }
    }

    public String getLine(int line) {
        if (line > 14 || line < 0) {
            return null;
        }

        return getOrCreateTeam(line).getValue();
    }

    public VirtualTeam getTeam(int line) {
        if (line > 14 || line < 0) {
            return null;
        }

        return getOrCreateTeam(line);
    }

    public String[] getLines() {
        return Arrays.stream(this.lines)
                .filter(Objects::nonNull)
                .map(VirtualTeam::getValue)
                .toArray(String[]::new);
    }

    private void sendLine(int line) {
        if (line > 14 || line < 0 || !this.created) {
            return;
        }

        VirtualTeam team = getOrCreateTeam(line);

        for (Packet<PacketListenerPlayOut> packet : team.sendLine()) {
            this.player.sendPacket(packet);
        }

        this.player.sendPacket(sendScore(team.getCurrentPlayer(), 14 - line));
        team.reset();
    }

    private VirtualTeam getOrCreateTeam(int line) {
        if (this.lines[line] == null) {
            this.lines[line] = new VirtualTeam("__fakeScore" + line, line);
        }

        return this.lines[line];
    }

    private PacketPlayOutScoreboardObjective createObjectivePacket(int mode, String displayName) {
        PacketPlayOutScoreboardObjective packet = new PacketPlayOutScoreboardObjective();

        OBJECTIVE__NAME.set(packet, this.player.getName());
        OBJECTIVE__MODE.set(packet, mode);

        if (mode == 0 || mode == 2) {
            OBJECTIVE__DISPLAY_NAME.set(packet, new ChatComponentText(displayName));
            OBJECTIVE__HEALTH_DISPLAY.set(packet, EnumScoreboardHealthDisplay.INTEGER);
        }

        return packet;
    }

    private PacketPlayOutScoreboardDisplayObjective setObjectiveSlot() {
        PacketPlayOutScoreboardDisplayObjective packet = new PacketPlayOutScoreboardDisplayObjective();

        DISPLAY_OBJECTIVE__SLOT.set(packet, 1);
        DISPLAY_OBJECTIVE__NAME.set(packet, this.player.getName());

        return packet;
    }

    private PacketPlayOutScoreboardScore sendScore(String line, int score) {
        return new PacketPlayOutScoreboardScore(Action.CHANGE, this.player.getName(), line, score);
    }

    private PacketPlayOutScoreboardScore removeLine(String line) {
        return new PacketPlayOutScoreboardScore(Action.REMOVE, null, line, 0);
    }

    static class VirtualTeam {

        private final String name;
        private final String currentPlayer;
        private final int line;
        private String value;
        private boolean valueChanged = false;
        private boolean first = true;

        private VirtualTeam(String name, String value, int line) {
            this.name = name;
            this.value = value;
            this.line = line;
            this.currentPlayer = ChatColor.values()[line].toString();
        }

        private VirtualTeam(String name, int line) {
            this(name, "", line);
        }

        public String getName() {
            return this.name;
        }

        String getValue() {
            return this.value;
        }

        void setValue(String value) {
            if (this.value == null || !this.value.equals(value)) {
                this.valueChanged = true;
            }

            this.value = value;
        }

        public int getLine() {
            return this.line;
        }

        private PacketPlayOutScoreboardTeam createPacket(int mode) {
            PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam();

            TEAM__NAME.set(packet, this.name);
            TEAM__DISPLAY_NAME.set(packet, new ChatComponentText(""));
            TEAM__PREFIX.set(packet, new ChatComponentText(this.value));
            TEAM__SUFFIX.set(packet, new ChatComponentText(""));
            TEAM__NAME_TAG.set(packet, "always");
            TEAM__COLOR.set(packet, EnumChatFormat.WHITE);
            TEAM__MODE.set(packet, mode);
            TEAM__OPTIONS.set(packet, 0);

            return packet;
        }

        PacketPlayOutScoreboardTeam createTeam() {
            return createPacket(0);
        }

        PacketPlayOutScoreboardTeam updateTeam() {
            return createPacket(2);
        }

        PacketPlayOutScoreboardTeam removeTeam() {
            PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam();

            TEAM__NAME.set(packet, this.name);
            TEAM__MODE.set(packet, 1);

            this.first = true;
            return packet;
        }

        Iterable<PacketPlayOutScoreboardTeam> sendLine() {
            List<PacketPlayOutScoreboardTeam> packets = new ArrayList<>();

            if (this.first) {
                packets.add(createTeam());
            } else if (this.valueChanged) {
                packets.add(updateTeam());
            }

            if (this.first) {
                packets.add(changePlayer());
            }

            if (this.first) {
                this.first = false;
            }

            return packets;
        }

        void reset() {
            this.valueChanged = false;
        }

        PacketPlayOutScoreboardTeam changePlayer() {
            return addOrRemovePlayer(3, this.currentPlayer);
        }

        PacketPlayOutScoreboardTeam addOrRemovePlayer(int mode, String playerName) {
            PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam();

            TEAM__NAME.set(packet, this.name);
            TEAM__ENTRIES.get(packet).add(playerName);
            TEAM__MODE.set(packet, mode);

            return packet;
        }

        String getCurrentPlayer() {
            return this.currentPlayer;
        }
    }
}
