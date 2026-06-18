package pl._lakidev.hardGamezEngine.npc;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

final class NpcNms {

    private static final String CB = "org.bukkit.craftbukkit.v1_20_R1.";
    private static final String NM = "net.minecraft.";

    // Runtime obfuscated class names (Paper 1.20.1 uses these at runtime)
    private static final String CLS_SERVER_PLAYER   = NM + "server.level.EntityPlayer";
    private static final String CLS_MC_SERVER        = NM + "server.MinecraftServer";
    private static final String CLS_SERVER_LEVEL     = NM + "server.level.WorldServer";
    private static final String CLS_PLAYER_BASE      = NM + "world.entity.player.EntityHuman";
    private static final String CLS_ENTITY           = NM + "world.entity.Entity";
    private static final String CLS_POSE             = NM + "world.entity.EntityPose";
    private static final String CLS_PACKET           = NM + "network.protocol.Packet";
    private static final String CLS_PLAYER_CONN      = NM + "server.network.PlayerConnection";
    private static final String CLS_DATA_WATCHER     = NM + "network.syncher.DataWatcher";
    private static final String CLS_DATA_WATCHER_OBJ = NM + "network.syncher.DataWatcherObject";

    // Packets
    private static final String PKT_NAMED_SPAWN   = NM + "network.protocol.game.PacketPlayOutNamedEntitySpawn";
    private static final String PKT_ENTITY_DESTROY = NM + "network.protocol.game.PacketPlayOutEntityDestroy";
    private static final String PKT_HEAD_ROT       = NM + "network.protocol.game.PacketPlayOutEntityHeadRotation";
    private static final String PKT_REL_MOVE_LOOK  = NM + "network.protocol.game.PacketPlayOutEntity$PacketPlayOutRelEntityMoveLook";
    private static final String PKT_LOOK           = NM + "network.protocol.game.PacketPlayOutEntity$PacketPlayOutEntityLook";
    private static final String PKT_ANIMATE        = NM + "network.protocol.game.PacketPlayOutAnimation";
    private static final String PKT_ENTITY_META    = NM + "network.protocol.game.PacketPlayOutEntityMetadata";
    // These kept their Mojang-mapped names in Paper 1.20.1
    private static final String PKT_TAB_ADD        = NM + "network.protocol.game.ClientboundPlayerInfoUpdatePacket";
    private static final String PKT_TAB_REMOVE     = NM + "network.protocol.game.ClientboundPlayerInfoRemovePacket";

    // -------------------------------------------------------------------------
    // GameProfile / Authlib
    // -------------------------------------------------------------------------

    static Object createGameProfile(UUID id, String name) {
        try {
            Class<?> gpClass = cls("com.mojang.authlib.GameProfile");
            return gpClass.getConstructor(UUID.class, String.class).newInstance(id, name);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create GameProfile", e);
        }
    }

    static void applyTextures(Object gameProfile, String value, String signature) {
        try {
            Object properties = gameProfile.getClass().getMethod("getProperties").invoke(gameProfile);
            Class<?> propClass = cls("com.mojang.authlib.properties.Property");
            Object property;
            if (signature != null && !signature.isEmpty()) {
                property = propClass.getConstructor(String.class, String.class, String.class)
                    .newInstance("textures", value, signature);
            } else {
                property = propClass.getConstructor(String.class, String.class)
                    .newInstance("textures", value);
            }
            properties.getClass().getMethod("put", Object.class, Object.class)
                .invoke(properties, "textures", property);
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply textures", e);
        }
    }

    // -------------------------------------------------------------------------
    // ServerPlayer creation
    // -------------------------------------------------------------------------

    static Object createServerPlayer(Object nmsServer, Object nmsWorld, Object gameProfile) {
        try {
            Class<?> spClass  = cls(CLS_SERVER_PLAYER);
            Class<?> srvClass = cls(CLS_MC_SERVER);
            Class<?> lvlClass = cls(CLS_SERVER_LEVEL);
            Class<?> gpClass  = cls("com.mojang.authlib.GameProfile");
            return spClass.getConstructor(srvClass, lvlClass, gpClass)
                .newInstance(nmsServer, nmsWorld, gameProfile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ServerPlayer", e);
        }
    }

    // -------------------------------------------------------------------------
    // CraftServer / CraftWorld unwrap
    // -------------------------------------------------------------------------

    static Object getNmsServer() {
        try {
            return cls(CB + "CraftServer").getMethod("getServer").invoke(Bukkit.getServer());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get NMS server", e);
        }
    }

    static Object getNmsWorld(org.bukkit.World world) {
        try {
            return cls(CB + "CraftWorld").getMethod("getHandle").invoke(world);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get NMS world", e);
        }
    }

    // -------------------------------------------------------------------------
    // Entity manipulation
    // -------------------------------------------------------------------------

    static void moveEntityTo(Object entity, double x, double y, double z, float yaw, float pitch) {
        try {
            entity.getClass()
                .getMethod("moveTo", double.class, double.class, double.class, float.class, float.class)
                .invoke(entity, x, y, z, yaw, pitch);
        } catch (Exception e) {
            throw new RuntimeException("Failed to move entity", e);
        }
    }

    static int getEntityId(Object entity) {
        try {
            return (int) entity.getClass().getMethod("getId").invoke(entity);
        } catch (Exception e) {
            Field f = findField(entity.getClass(), "id");
            if (f == null) return -1;
            try { return (int) f.get(entity); } catch (Exception ex) { return -1; }
        }
    }

    static void setNoAi(Object entity, boolean v)         { invokeVoid(entity, "setNoAi", boolean.class, v); }
    static void setInvulnerable(Object entity, boolean v)  { invokeVoid(entity, "setInvulnerable", boolean.class, v); }
    static void setSilent(Object entity, boolean v)        { invokeVoid(entity, "setSilent", boolean.class, v); }
    static void setNoGravity(Object entity, boolean v)     { invokeVoid(entity, "setNoGravity", boolean.class, v); }

    static org.bukkit.entity.Player getBukkitEntity(Object serverPlayer) {
        try {
            return (org.bukkit.entity.Player) serverPlayer.getClass()
                .getMethod("getBukkitEntity").invoke(serverPlayer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get bukkit entity", e);
        }
    }

    static float getYRot(Object entity) {
        try { return (float) entity.getClass().getMethod("getYRot").invoke(entity); }
        catch (Exception e) { return 0f; }
    }

    static void setYRot(Object entity, float v)   { invokeVoid(entity, "setYRot", float.class, v); }
    static void setXRot(Object entity, float v)   { invokeVoid(entity, "setXRot", float.class, v); }

    static void setYHeadRot(Object entity, float yaw) {
        Field f = findField(entity.getClass(), "yHeadRot");
        if (f == null) return;
        try { f.set(entity, yaw); } catch (Exception ignored) {}
    }

    static double getXOld(Object e) { return getDouble(e, "xOld"); }
    static double getYOld(Object e) { return getDouble(e, "yOld"); }
    static double getZOld(Object e) { return getDouble(e, "zOld"); }
    static void   setXOld(Object e, double v) { setDouble(e, "xOld", v); }
    static void   setYOld(Object e, double v) { setDouble(e, "yOld", v); }
    static void   setZOld(Object e, double v) { setDouble(e, "zOld", v); }

    static boolean isOnGround(Object entity) {
        try { return (boolean) entity.getClass().getMethod("onGround").invoke(entity); }
        catch (Exception e) { return true; }
    }

    // -------------------------------------------------------------------------
    // Packet sending
    // -------------------------------------------------------------------------

    static void sendPacket(Player player, Object packet) {
        if (packet == null) return;
        try {
            Object serverPlayer = cls(CB + "entity.CraftPlayer")
                .getMethod("getHandle").invoke(player);
            Field connField = findField(serverPlayer.getClass(), "connection");
            if (connField == null) return;
            Object conn = connField.get(serverPlayer);
            if (conn == null) return;
            conn.getClass().getMethod("sendPacket", cls(CLS_PACKET)).invoke(conn, packet);
        } catch (Exception ignored) {}
    }

    // -------------------------------------------------------------------------
    // Packet builders
    // -------------------------------------------------------------------------

    static Object buildPlayerInfoAddPacket(Object serverPlayer) {
        try {
            Class<?> pkt = cls(PKT_TAB_ADD);
            return pkt.getMethod("createPlayerInitializing", java.util.Collection.class)
                .invoke(null, List.of(serverPlayer));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build tab add packet", e);
        }
    }

    static Object buildPlayerInfoRemovePacket(UUID id) {
        try {
            Class<?> pkt = cls(PKT_TAB_REMOVE);
            return pkt.getConstructor(List.class).newInstance(List.of(id));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build tab remove packet", e);
        }
    }

    static Object buildAddPlayerPacket(Object serverPlayer) {
        try {
            Class<?> pkt = cls(PKT_NAMED_SPAWN);
            Class<?> ep  = cls(CLS_SERVER_PLAYER);
            return pkt.getConstructor(ep).newInstance(serverPlayer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build named spawn packet", e);
        }
    }

    static Object buildRotateHeadPacket(Object entity, byte yawByte) {
        try {
            Class<?> pkt = cls(PKT_HEAD_ROT);
            Class<?> ent = cls(CLS_ENTITY);
            return pkt.getConstructor(ent, byte.class).newInstance(entity, yawByte);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build head rot packet", e);
        }
    }

    static Object buildMoveRotPacket(Object entity, short dx, short dy, short dz, byte yaw, byte pitch, boolean onGround) {
        try {
            Class<?> pkt = cls(PKT_REL_MOVE_LOOK);
            Constructor<?> ctor = pkt.getConstructor(int.class, short.class, short.class, short.class, byte.class, byte.class, boolean.class);
            return ctor.newInstance(getEntityId(entity), dx, dy, dz, yaw, pitch, onGround);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build move packet", e);
        }
    }

    static Object buildMoveRotOnlyPacket(int entityId, byte yaw, byte pitch, boolean onGround) {
        try {
            Class<?> pkt = cls(PKT_LOOK);
            Constructor<?> ctor = pkt.getConstructor(int.class, byte.class, byte.class, boolean.class);
            return ctor.newInstance(entityId, yaw, pitch, onGround);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build look packet", e);
        }
    }

    static Object buildEntityDataPacket(Object entity) {
        try {
            Object dw = getDataWatcher(entity);
            if (dw == null) return null;
            List<?> dirty = (List<?>) dw.getClass().getMethod("packDirty").invoke(dw);
            if (dirty == null || dirty.isEmpty()) return null;
            Class<?> pkt = cls(PKT_ENTITY_META);
            Constructor<?> ctor = pkt.getConstructor(int.class, List.class);
            return ctor.newInstance(getEntityId(entity), dirty);
        } catch (Exception e) {
            return null;
        }
    }

    static Object buildSkinLayersDataPacket(Object serverPlayer) {
        try {
            Class<?> playerClass = cls(CLS_PLAYER_BASE);
            Field skinField = findFieldByType(playerClass, cls(CLS_DATA_WATCHER_OBJ));
            if (skinField == null) skinField = findField(playerClass, "bi");
            if (skinField == null) return null;
            Object accessor = skinField.get(null);

            Class<?> itemClass = cls(CLS_DATA_WATCHER + "$Item");
            Object item = itemClass.getConstructor(cls(CLS_DATA_WATCHER_OBJ), Object.class)
                .newInstance(accessor, (byte) 0x7F);

            Class<?> pkt = cls(PKT_ENTITY_META);
            Constructor<?> ctor = pkt.getConstructor(int.class, List.class);
            return ctor.newInstance(getEntityId(serverPlayer), List.of(item));
        } catch (Exception e) {
            return null;
        }
    }

    static Object buildRemoveEntitiesPacket(int entityId) {
        try {
            Class<?> pkt = cls(PKT_ENTITY_DESTROY);
            return pkt.getConstructor(int[].class).newInstance(new int[]{entityId});
        } catch (Exception e) {
            throw new RuntimeException("Failed to build remove packet", e);
        }
    }

    static Object buildAnimatePacket(Object entity, int animationId) {
        try {
            Class<?> pkt = cls(PKT_ANIMATE);
            Class<?> ent = cls(CLS_ENTITY);
            return pkt.getConstructor(ent, int.class).newInstance(entity, animationId);
        } catch (Exception e) {
            return null;
        }
    }

    static void setCrouchingPose(Object serverPlayer, boolean crouching) {
        try {
            Class<?> poseClass = cls(CLS_POSE);
            Object pose = poseClass.getField(crouching ? "f" : "a").get(null);
            Method setPose = findMethod(serverPlayer.getClass(), "setPose", poseClass);
            if (setPose != null) setPose.invoke(serverPlayer, pose);
        } catch (Exception ignored) {}
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name, true, Bukkit.getServer().getClass().getClassLoader());
    }

    private static Object getDataWatcher(Object entity) {
        Field f = findField(entity.getClass(), "entityData");
        if (f == null) return null;
        try { return f.get(entity); } catch (Exception e) { return null; }
    }

    private static double getDouble(Object obj, String name) {
        Field f = findField(obj.getClass(), name);
        if (f == null) return 0;
        try { return (double) f.get(obj); } catch (Exception e) { return 0; }
    }

    private static void setDouble(Object obj, String name, double v) {
        Field f = findField(obj.getClass(), name);
        if (f == null) return;
        try { f.set(obj, v); } catch (Exception ignored) {}
    }

    private static void invokeVoid(Object obj, String method, Class<?> pt, Object value) {
        try { obj.getClass().getMethod(method, pt).invoke(obj, value); }
        catch (Exception ignored) {}
    }

    static Field findField(Class<?> clazz, String name) {
        Class<?> cur = clazz;
        while (cur != null) {
            try {
                Field f = cur.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                cur = cur.getSuperclass();
            }
        }
        return null;
    }

    private static Field findFieldByType(Class<?> clazz, Class<?> type) {
        for (Field f : clazz.getDeclaredFields()) {
            if (f.getType().equals(type)) {
                f.setAccessible(true);
                return f;
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... params) {
        Class<?> cur = clazz;
        while (cur != null) {
            try {
                Method m = cur.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                cur = cur.getSuperclass();
            }
        }
        return null;
    }

    private NpcNms() {}
}
