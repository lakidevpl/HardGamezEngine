package pl._lakidev.hardGamezEngine.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

final class NpcNms {

    private static final String CB = "org.bukkit.craftbukkit.v1_20_R1.";
    private static final String NM = "net.minecraft.";

    static Object createGameProfile(UUID id, String name) {
        try {
            Class<?> gpClass = Class.forName("com.mojang.authlib.GameProfile");
            Constructor<?> ctor = gpClass.getConstructor(UUID.class, String.class);
            return ctor.newInstance(id, name);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create GameProfile", e);
        }
    }

    static void applyTextures(Object gameProfile, String value, String signature) {
        try {
            Class<?> gpClass = gameProfile.getClass();
            Method getProps = gpClass.getMethod("getProperties");
            Object properties = getProps.invoke(gameProfile);

            Class<?> propClass = Class.forName("com.mojang.authlib.properties.Property");
            Constructor<?> propCtor;
            Object property;

            if (signature != null && !signature.isEmpty()) {
                propCtor = propClass.getConstructor(String.class, String.class, String.class);
                property = propCtor.newInstance("textures", value, signature);
            } else {
                propCtor = propClass.getConstructor(String.class, String.class);
                property = propCtor.newInstance("textures", value);
            }

            Method putMethod = properties.getClass().getMethod("put", Object.class, Object.class);
            putMethod.invoke(properties, "textures", property);
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply textures", e);
        }
    }

    static Object createServerPlayer(Object nmsServer, Object nmsWorld, Object gameProfile) {
        try {
            Class<?> serverPlayerClass = Class.forName(NM + "server.level.ServerPlayer");
            Class<?> serverClass       = Class.forName(NM + "server.MinecraftServer");
            Class<?> levelClass        = Class.forName(NM + "server.level.ServerLevel");
            Class<?> profileClass      = Class.forName("com.mojang.authlib.GameProfile");

            Constructor<?> ctor = serverPlayerClass.getConstructor(serverClass, levelClass, profileClass);
            return ctor.newInstance(nmsServer, nmsWorld, gameProfile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ServerPlayer", e);
        }
    }

    static Object getNmsServer() {
        try {
            Class<?> craftServerClass = Class.forName(CB + "CraftServer");
            Object server = Bukkit.getServer();
            Method getServer = craftServerClass.getMethod("getServer");
            return getServer.invoke(server);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get NMS server", e);
        }
    }

    static Object getNmsWorld(org.bukkit.World world) {
        try {
            Class<?> craftWorldClass = Class.forName(CB + "CraftWorld");
            Method getHandle = craftWorldClass.getMethod("getHandle");
            return getHandle.invoke(world);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get NMS world", e);
        }
    }

    static void moveEntityTo(Object serverPlayer, double x, double y, double z, float yaw, float pitch) {
        try {
            Class<?> spClass = serverPlayer.getClass();
            Method moveTo = spClass.getMethod("moveTo", double.class, double.class, double.class, float.class, float.class);
            moveTo.invoke(serverPlayer, x, y, z, yaw, pitch);
        } catch (Exception e) {
            throw new RuntimeException("Failed to move ServerPlayer", e);
        }
    }

    static int getEntityId(Object serverPlayer) {
        try {
            Method getId = serverPlayer.getClass().getMethod("getId");
            return (int) getId.invoke(serverPlayer);
        } catch (Exception e) {
            try {
                Field idField = findField(serverPlayer.getClass(), "id");
                idField.setAccessible(true);
                return (int) idField.get(serverPlayer);
            } catch (Exception ex) {
                return -1;
            }
        }
    }

    static void setNoAi(Object serverPlayer, boolean noAi) {
        try {
            Method method = serverPlayer.getClass().getMethod("setNoAi", boolean.class);
            method.invoke(serverPlayer, noAi);
        } catch (Exception ignored) {}
    }

    static void setInvulnerable(Object serverPlayer, boolean inv) {
        try {
            Method method = serverPlayer.getClass().getMethod("setInvulnerable", boolean.class);
            method.invoke(serverPlayer, inv);
        } catch (Exception ignored) {}
    }

    static void setSilent(Object serverPlayer, boolean silent) {
        try {
            Method method = serverPlayer.getClass().getMethod("setSilent", boolean.class);
            method.invoke(serverPlayer, silent);
        } catch (Exception ignored) {}
    }

    static void setNoGravity(Object serverPlayer, boolean noGravity) {
        try {
            Method method = serverPlayer.getClass().getMethod("setNoGravity", boolean.class);
            method.invoke(serverPlayer, noGravity);
        } catch (Exception ignored) {}
    }

    static org.bukkit.entity.Player getBukkitEntity(Object serverPlayer) {
        try {
            Method method = serverPlayer.getClass().getMethod("getBukkitEntity");
            return (org.bukkit.entity.Player) method.invoke(serverPlayer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get bukkit entity", e);
        }
    }

    static float getYRot(Object entity) {
        try {
            Method method = entity.getClass().getMethod("getYRot");
            return (float) method.invoke(entity);
        } catch (Exception e) {
            return 0f;
        }
    }

    static void setYRot(Object entity, float yaw) {
        try {
            Method method = entity.getClass().getMethod("setYRot", float.class);
            method.invoke(entity, yaw);
        } catch (Exception ignored) {}
    }

    static void setXRot(Object entity, float pitch) {
        try {
            Method method = entity.getClass().getMethod("setXRot", float.class);
            method.invoke(entity, pitch);
        } catch (Exception ignored) {}
    }

    static void setYHeadRot(Object entity, float yaw) {
        try {
            Field field = findField(entity.getClass(), "yHeadRot");
            if (field != null) {
                field.setAccessible(true);
                field.set(entity, yaw);
            }
        } catch (Exception ignored) {}
    }

    static double getXOld(Object entity) {
        try {
            Field field = findField(entity.getClass(), "xOld");
            if (field == null) return 0;
            field.setAccessible(true);
            return (double) field.get(entity);
        } catch (Exception e) {
            return 0;
        }
    }

    static double getYOld(Object entity) {
        try {
            Field field = findField(entity.getClass(), "yOld");
            if (field == null) return 0;
            field.setAccessible(true);
            return (double) field.get(entity);
        } catch (Exception e) {
            return 0;
        }
    }

    static double getZOld(Object entity) {
        try {
            Field field = findField(entity.getClass(), "zOld");
            if (field == null) return 0;
            field.setAccessible(true);
            return (double) field.get(entity);
        } catch (Exception e) {
            return 0;
        }
    }

    static void setXOld(Object entity, double v) {
        try {
            Field field = findField(entity.getClass(), "xOld");
            if (field != null) { field.setAccessible(true); field.set(entity, v); }
        } catch (Exception ignored) {}
    }

    static void setYOld(Object entity, double v) {
        try {
            Field field = findField(entity.getClass(), "yOld");
            if (field != null) { field.setAccessible(true); field.set(entity, v); }
        } catch (Exception ignored) {}
    }

    static void setZOld(Object entity, double v) {
        try {
            Field field = findField(entity.getClass(), "zOld");
            if (field != null) { field.setAccessible(true); field.set(entity, v); }
        } catch (Exception ignored) {}
    }

    static boolean isOnGround(Object entity) {
        try {
            Method method = entity.getClass().getMethod("onGround");
            return (boolean) method.invoke(entity);
        } catch (Exception e) {
            return true;
        }
    }

    static void sendPacket(Player player, Object packet) {
        try {
            Class<?> craftPlayerClass = Class.forName(CB + "entity.CraftPlayer");
            Method getHandle = craftPlayerClass.getMethod("getHandle");
            Object serverPlayer = getHandle.invoke(player);

            Field connField = findField(serverPlayer.getClass(), "connection");
            if (connField == null) return;
            connField.setAccessible(true);
            Object conn = connField.get(serverPlayer);
            if (conn == null) return;

            Method send = conn.getClass().getMethod("send", Class.forName(NM + "network.protocol.Packet"));
            send.invoke(conn, packet);
        } catch (Exception e) {
        }
    }

    static Object buildPlayerInfoAddPacket(Object serverPlayer) {
        try {
            Class<?> packetClass = Class.forName(NM + "network.protocol.game.ClientboundPlayerInfoUpdatePacket");
            Method factory = packetClass.getMethod("createPlayerInitializing", java.util.Collection.class);
            return factory.invoke(null, List.of(serverPlayer));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build tab add packet", e);
        }
    }

    static Object buildPlayerInfoRemovePacket(UUID id) {
        try {
            Class<?> packetClass = Class.forName(NM + "network.protocol.game.ClientboundPlayerInfoRemovePacket");
            Constructor<?> ctor = packetClass.getConstructor(List.class);
            return ctor.newInstance(List.of(id));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build tab remove packet", e);
        }
    }

    static Object buildAddPlayerPacket(Object serverPlayer) {
        try {
            Class<?> packetClass = Class.forName(NM + "network.protocol.game.ClientboundAddPlayerPacket");
            Class<?> entityClass = Class.forName(NM + "world.entity.player.Player");
            Constructor<?> ctor = packetClass.getConstructor(entityClass);
            return ctor.newInstance(serverPlayer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build add player packet", e);
        }
    }

    static Object buildRotateHeadPacket(Object entity, byte yawByte) {
        try {
            Class<?> packetClass = Class.forName(NM + "network.protocol.game.ClientboundRotateHeadPacket");
            Class<?> entityClass = Class.forName(NM + "world.entity.Entity");
            Constructor<?> ctor = packetClass.getConstructor(entityClass, byte.class);
            return ctor.newInstance(entity, yawByte);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build rotate head packet", e);
        }
    }

    static Object buildMoveRotPacket(Object entity, short dx, short dy, short dz, byte yaw, byte pitch, boolean onGround) {
        try {
            Class<?> packetClass = Class.forName(NM + "network.protocol.game.ClientboundMoveEntityPacket$PosRot");
            Constructor<?> ctor = packetClass.getConstructor(int.class, short.class, short.class, short.class, byte.class, byte.class, boolean.class);
            return ctor.newInstance(getEntityId(entity), dx, dy, dz, yaw, pitch, onGround);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build move packet", e);
        }
    }

    static Object buildMoveRotOnlyPacket(int entityId, byte yaw, byte pitch, boolean onGround) {
        try {
            Class<?> packetClass = Class.forName(NM + "network.protocol.game.ClientboundMoveEntityPacket$Rot");
            Constructor<?> ctor = packetClass.getConstructor(int.class, byte.class, byte.class, boolean.class);
            return ctor.newInstance(entityId, yaw, pitch, onGround);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build rot packet", e);
        }
    }

    static Object buildEntityDataPacket(Object entity) {
        try {
            Object synchedData = getSynchedEntityData(entity);
            if (synchedData == null) return null;

            Method packDirty = synchedData.getClass().getMethod("packDirty");
            List<?> dirtyValues = (List<?>) packDirty.invoke(synchedData);
            if (dirtyValues == null || dirtyValues.isEmpty()) return null;

            Class<?> packetClass = Class.forName(NM + "network.protocol.game.ClientboundSetEntityDataPacket");
            int id = getEntityId(entity);
            Constructor<?> ctor = packetClass.getConstructor(int.class, List.class);
            return ctor.newInstance(id, dirtyValues);
        } catch (Exception e) {
            return null;
        }
    }

    static Object buildSkinLayersDataPacket(Object serverPlayer) {
        try {
            Class<?> playerClass = Class.forName(NM + "world.entity.player.Player");
            Field skinField = findField(playerClass, "DATA_PLAYER_MODE_CUSTOMISATION");
            if (skinField == null) return null;
            skinField.setAccessible(true);
            Object accessor = skinField.get(null);

            Class<?> dataValueClass = Class.forName(NM + "network.syncher.SynchedEntityData$DataValue");
            Class<?> accessorClass  = Class.forName(NM + "network.syncher.EntityDataAccessor");
            Method createMethod = dataValueClass.getMethod("create", accessorClass, Object.class);
            Object dataValue = createMethod.invoke(null, accessor, (byte) 0x7F);

            Class<?> packetClass = Class.forName(NM + "network.protocol.game.ClientboundSetEntityDataPacket");
            int id = getEntityId(serverPlayer);
            Constructor<?> ctor = packetClass.getConstructor(int.class, List.class);
            return ctor.newInstance(id, List.of(dataValue));
        } catch (Exception e) {
            return null;
        }
    }

    static Object buildRemoveEntitiesPacket(int entityId) {
        try {
            Class<?> packetClass = Class.forName(NM + "network.protocol.game.ClientboundRemoveEntitiesPacket");
            Constructor<?> ctor = packetClass.getConstructor(int[].class);
            return ctor.newInstance(new int[]{entityId});
        } catch (Exception e) {
            throw new RuntimeException("Failed to build remove packet", e);
        }
    }

    static Object buildAnimatePacket(Object entity, int animationId) {
        try {
            Class<?> packetClass = Class.forName(NM + "network.protocol.game.ClientboundAnimatePacket");
            Class<?> entityClass = Class.forName(NM + "world.entity.Entity");
            Constructor<?> ctor = packetClass.getConstructor(entityClass, int.class);
            return ctor.newInstance(entity, animationId);
        } catch (Exception e) {
            return null;
        }
    }

    static void setCrouchingPose(Object serverPlayer, boolean crouching) {
        try {
            Class<?> poseClass = Class.forName(NM + "world.entity.Pose");
            Object pose = crouching
                ? poseClass.getField("CROUCHING").get(null)
                : poseClass.getField("STANDING").get(null);
            Method setPose = findMethod(serverPlayer.getClass(), "setPose", poseClass);
            if (setPose != null) setPose.invoke(serverPlayer, pose);
        } catch (Exception ignored) {}
    }

    private static Object getSynchedEntityData(Object entity) {
        try {
            Field field = findField(entity.getClass(), "entityData");
            if (field == null) return null;
            field.setAccessible(true);
            return field.get(entity);
        } catch (Exception e) {
            return null;
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field f = current.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... params) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Method m = current.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private NpcNms() {}
}
