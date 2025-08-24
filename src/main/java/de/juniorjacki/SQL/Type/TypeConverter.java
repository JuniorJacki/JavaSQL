package de.juniorjacki.SQL.Type;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

public class TypeConverter {

    public static void main() {
        UUID uuid = UUID.randomUUID();
        System.out.println(Base64.getEncoder().encodeToString(convertUUIDToBytes(uuid)));
        System.out.println(Arrays.toString(convertUUIDToBytes(uuid)));
        System.out.println(uuid.toString());
        System.out.println(convertBytesToUUID(convertUUIDToBytes(uuid)));

    }

    public static byte[] convertUUIDToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static UUID convertBytesToUUID(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long high = byteBuffer.getLong();
        long low = byteBuffer.getLong();
        return new UUID(high, low);
    }

}
