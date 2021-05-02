package dev.azn9.wankilhunter.util.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.minecraft.server.v1_16_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Base64;

/**
 * This file is a part of TheWalls project.
 *
 * @author roro1506_HD
 */
public class ItemStackTypeAdapter implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {

    @Override
    public ItemStack deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
        try {
            return CraftItemStack.asCraftMirror(net.minecraft.server.v1_16_R3.ItemStack.fromCompound(NBTCompressedStreamTools.a((DataInput) new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(element.getAsString()))))));
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public JsonElement serialize(ItemStack itemStack, Type type, JsonSerializationContext context) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        NBTTagCompound tag = new NBTTagCompound();

        CraftItemStack.asNMSCopy(itemStack).save(tag);

        try {
            NBTCompressedStreamTools.a(tag, (DataOutput) new DataOutputStream(outputStream));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return new JsonPrimitive(Base64.getEncoder().encodeToString(outputStream.toByteArray()));
    }
}
