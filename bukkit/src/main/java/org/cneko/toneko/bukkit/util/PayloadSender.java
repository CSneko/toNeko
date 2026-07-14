package org.cneko.toneko.bukkit.util;

import org.bukkit.entity.Player;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PayloadSender {

    public static void sendChatHistory(Player player, String nekoUuid, List<String> messages) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            writeUtf8(out, nekoUuid);
            writeVarInt(out, messages.size());
            for (String msg : messages) writeUtf8(out, msg);
            out.flush();
            send(player, "toneko:chat_history_response", bos.toByteArray());
        } catch (IOException ignored) {}
    }

    public static void sendTTS(Player player, String text) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            writeUtf8(out, text);
            out.flush();
            send(player, "toneko:tts_send", bos.toByteArray());
        } catch (IOException ignored) {}
    }

    public static void sendNekoInfoSync(Player player, float energy, float maxEnergy,
                                         double interactionRaw, double combatRaw, double baseRaw,
                                         boolean isNeko, int age) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeFloat(energy); out.writeFloat(maxEnergy);
            out.writeDouble(interactionRaw); out.writeDouble(combatRaw); out.writeDouble(baseRaw);
            out.writeBoolean(isNeko); out.writeInt(age);
            out.flush();
            send(player, "toneko:neko_info_sync", bos.toByteArray());
        } catch (IOException ignored) {}
    }

    public static void sendNekoInfoUpdate(Player player, float energy, float maxEnergy, boolean isNeko, int age) {
        sendNekoInfoSync(player, energy, maxEnergy, 0, 0, 0, isNeko, age);
    }

    public static void sendManagementData(Player player, byte[] nbtData) {
        send(player, "toneko:management_data", nbtData);
    }

    /** Send quirk list response to modded client (QuirkQueryPayload) */
    public static void sendQuirkResponse(Player player, List<String> quirks, List<String> allQuirks, boolean openScreen) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            writeStringList(out, quirks);
            writeStringList(out, allQuirks);
            out.writeBoolean(openScreen);
            out.flush();
            send(player, "toneko:quirk_query", bos.toByteArray());
        } catch (IOException ignored) {}
    }

    private static void writeStringList(DataOutputStream out, List<String> list) throws IOException {
        writeVarInt(out, list.size());
        for (String s : list) writeUtf8(out, s);
    }

    public static void sendOpenNekoInfoScreen(Player player) {
        send(player, "toneko:open_neko_info_screen", new byte[0]);
    }

    public static void sendChatModeSync(Player player, boolean area) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeBoolean(area);
            out.flush();
            send(player, "toneko:chat_mode", bos.toByteArray());
        } catch (IOException ignored) {}
    }

    private static void send(Player player, String channel, byte[] bytes) {
        player.sendPluginMessage(org.cneko.toneko.bukkit.ToNeko.INSTANCE, channel, bytes);
    }
    private static void writeUtf8(DataOutputStream out, String s) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, b.length); out.write(b);
    }
    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & 0xFFFFFF80) != 0) { out.writeByte((value & 0x7F) | 0x80); value >>>= 7; }
        out.writeByte(value & 0x7F);
    }
}
