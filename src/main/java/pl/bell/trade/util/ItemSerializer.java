package pl.bell.trade.util;

import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ItemSerializer {

    private ItemSerializer() {}

    public static byte[] toBytes(ItemStack item) {
        if (item == null || item.getType().isAir()) return new byte[0];
        try {
            return item.serializeAsBytes();
        } catch (NoSuchMethodError | UnsupportedOperationException e) {
            return legacySerialize(item);
        }
    }

    public static ItemStack fromBytes(byte[] data) {
        if (data == null || data.length == 0) return null;
        try {
            return ItemStack.deserializeBytes(data);
        } catch (NoSuchMethodError | UnsupportedOperationException e) {
            return legacyDeserialize(data);
        }
    }

    private static byte[] legacySerialize(ItemStack item) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(item.serialize());
            return out.toByteArray();
        } catch (IOException e) {
            Logger.getLogger("BellTrade").log(Level.SEVERE, "Item serialize failed", e);
            return new byte[0];
        }
    }

    @SuppressWarnings("unchecked")
    private static ItemStack legacyDeserialize(byte[] data) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            Object obj = ois.readObject();
            if (obj instanceof java.util.Map<?, ?> map) {
                return ItemStack.deserialize((java.util.Map<String, Object>) map);
            }
        } catch (Exception e) {
            Logger.getLogger("BellTrade").log(Level.SEVERE, "Item deserialize failed", e);
        }
        return null;
    }

    public static String toBase64(ItemStack item) {
        return Base64.getEncoder().encodeToString(toBytes(item));
    }
}
