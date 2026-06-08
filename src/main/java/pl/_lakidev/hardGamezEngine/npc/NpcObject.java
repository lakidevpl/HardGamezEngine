package pl._lakidev.hardGamezEngine.npc;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;
import pl._lakidev.hardGamezEngine.HardGamezEngine;

import java.util.UUID;
import java.util.function.Consumer;

public class NpcObject {

    private static final double PLAYER_WALK_SPEED = 0.15;
    private static final double MOB_WALK_SPEED    = 0.13;
    private static final int    LOOK_RANGE_SQ     = 900;

    private final UUID id = UUID.randomUUID();
    private final NpcType npcType;
    private String name;
    private Location spawnLocation;

    private NpcSkin skin;
    private boolean looking      = false;
    private Consumer<NpcClickEvent> clickHandler;
    private boolean hideNickname = false;
    private Inventory equipment;
    private boolean invincible   = true;
    private Location[] waypoints;
    private LoopType loopType    = LoopType.NONE;
    private float scale          = 1.0f;
    private boolean glowing      = false;
    private ChatColor glowColor  = ChatColor.WHITE;
    private boolean silent       = false;
    private boolean gravity      = true;
    private boolean collidable   = true;
    private boolean sneaking     = false;

    private Entity spawnedEntity;
    private Object nmsServerPlayer;

    private BukkitTask movementTask;
    private BukkitTask lookTask;
    private BukkitTask animationTask;

    private int waypointIndex    = 0;
    private int sheriffDirection = 1;

    NpcObject(NpcType npcType, String name) {
        this.npcType = npcType;
        this.name = name;
    }

    NpcObject(NpcType npcType, String name, Location spawnLocation) {
        this.npcType = npcType;
        this.name = name;
        this.spawnLocation = spawnLocation.clone();
    }

    // -------------------------------------------------------------------------
    // Builder / setter methods
    // -------------------------------------------------------------------------

    public NpcObject setSkinURL(String url) {
        NpcSkin.fetchByURL(url, skin -> { this.skin = skin; if (spawnedEntity != null) respawn(); });
        return this;
    }

    public NpcObject setSkinName(String playerName) {
        NpcSkin.fetchByName(playerName, skin -> { this.skin = skin; if (spawnedEntity != null) respawn(); });
        return this;
    }

    public NpcObject setSkinUUID(UUID uuid) {
        NpcSkin.fetchByUUID(uuid, skin -> { this.skin = skin; if (spawnedEntity != null) respawn(); });
        return this;
    }

    public NpcObject setLooking(boolean look) {
        this.looking = look;
        if (spawnedEntity != null) { if (look) startLookTask(); else stopLookTask(); }
        return this;
    }

    public NpcObject clickEvent(Consumer<NpcClickEvent> handler) {
        this.clickHandler = handler;
        return this;
    }

    public NpcObject hideNickname(boolean hide) {
        this.hideNickname = hide;
        if (npcType != NpcType.HUMAN && spawnedEntity instanceof LivingEntity l) {
            l.setCustomNameVisible(!hide);
        }
        return this;
    }

    public NpcObject setInventory(Inventory inventory) {
        this.equipment = inventory;
        if (spawnedEntity != null) applyEquipment();
        return this;
    }

    public NpcObject invicible(boolean inv) {
        this.invincible = inv;
        if (spawnedEntity instanceof LivingEntity l) l.setInvulnerable(inv);
        return this;
    }

    public NpcObject setWaypoints(Location[] waypoints) {
        this.waypoints = waypoints.clone();
        this.waypointIndex = 0;
        this.sheriffDirection = 1;
        if (spawnedEntity != null) restartMovement();
        return this;
    }

    public NpcObject loopType(LoopType type) {
        this.loopType = type;
        if (spawnedEntity != null) restartMovement();
        return this;
    }

    public NpcObject setScale(float scale) {
        this.scale = Math.max(0.01f, Math.min(scale, 10.0f));
        applyScaleAttribute();
        return this;
    }

    private void applyScaleAttribute() {
        if (!(spawnedEntity instanceof LivingEntity l)) return;
        try {
            java.lang.reflect.Method getAttr = l.getClass().getMethod("getAttribute",
                Class.forName("org.bukkit.attribute.Attribute"));
            Class<?> attrClass = Class.forName("org.bukkit.attribute.Attribute");
            Object scaleAttr = java.util.Arrays.stream(attrClass.getEnumConstants())
                .filter(e -> e.toString().contains("SCALE"))
                .findFirst().orElse(null);
            if (scaleAttr == null) return;
            Object instance = getAttr.invoke(l, scaleAttr);
            if (instance == null) return;
            instance.getClass().getMethod("setBaseValue", double.class).invoke(instance, (double) scale);
        } catch (Exception ignored) {}
    }

    public NpcObject setGlowing(boolean glow) {
        this.glowing = glow;
        if (spawnedEntity != null) { spawnedEntity.setGlowing(glow); refreshGlowTeam(); }
        return this;
    }

    public NpcObject setGlowColor(ChatColor color) {
        this.glowColor = color;
        if (spawnedEntity != null) refreshGlowTeam();
        return this;
    }

    public NpcObject setSilent(boolean silent) {
        this.silent = silent;
        if (spawnedEntity != null) spawnedEntity.setSilent(silent);
        return this;
    }

    public NpcObject setGravity(boolean gravity) {
        this.gravity = gravity;
        if (spawnedEntity != null) spawnedEntity.setGravity(gravity);
        return this;
    }

    public NpcObject setCollidable(boolean collidable) {
        this.collidable = collidable;
        if (spawnedEntity instanceof LivingEntity l) l.setCollidable(collidable);
        return this;
    }

    public NpcObject setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
        if (npcType == NpcType.HUMAN && nmsServerPlayer != null) {
            NpcNms.setCrouchingPose(nmsServerPlayer, sneaking);
            broadcastEntityData();
        }
        return this;
    }

    public NpcObject setName(String name) {
        this.name = name;
        if (spawnedEntity != null) refreshName();
        return this;
    }

    public NpcObject setLocation(Location location) {
        this.spawnLocation = location.clone();
        if (spawnedEntity != null) spawnedEntity.teleport(location);
        return this;
    }

    // -------------------------------------------------------------------------
    // Spawn
    // -------------------------------------------------------------------------

    public NpcObject spawn() {
        if (spawnLocation == null) throw new IllegalStateException("NPC has no spawn location");

        if (npcType == NpcType.HUMAN) {
            spawnHuman();
        } else {
            spawnMob();
        }

        NpcEngine.trackNpc(this);
        return this;
    }

    private void spawnHuman() {
        Object nmsServer = NpcNms.getNmsServer();
        Object nmsWorld  = NpcNms.getNmsWorld(spawnLocation.getWorld());
        Object profile   = NpcNms.createGameProfile(id, trimName(name));

        if (skin != null) {
            NpcNms.applyTextures(profile, skin.getValue(), skin.getSignature());
        }

        nmsServerPlayer = NpcNms.createServerPlayer(nmsServer, nmsWorld, profile);

        NpcNms.moveEntityTo(nmsServerPlayer,
            spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(),
            spawnLocation.getYaw(), spawnLocation.getPitch());

        NpcNms.setNoAi(nmsServerPlayer, true);
        NpcNms.setInvulnerable(nmsServerPlayer, invincible);
        NpcNms.setSilent(nmsServerPlayer, silent);
        NpcNms.setNoGravity(nmsServerPlayer, !gravity);

        if (sneaking) NpcNms.setCrouchingPose(nmsServerPlayer, true);

        spawnedEntity = NpcNms.getBukkitEntity(nmsServerPlayer);

        sendTabAndSpawnToAll();
        sendSkinLayersToAll();
        scheduleTabRemove();

        applyEquipment();
        afterSpawn();
    }

    private void spawnMob() {
        if (spawnLocation.getWorld() == null) return;
        spawnedEntity = spawnLocation.getWorld().spawnEntity(spawnLocation, mapNpcTypeToBukkit(npcType));

        if (spawnedEntity instanceof LivingEntity l) {
            l.setAI(false);
            l.setInvulnerable(invincible);
            l.setSilent(silent);
            l.setGravity(gravity);
            l.setCollidable(collidable);
            l.setRemoveWhenFarAway(false);
            l.setPersistent(false);
            l.customName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand().deserialize(name));
            l.setCustomNameVisible(!hideNickname);

            applyScaleAttribute();
        }

        if (glowing) { spawnedEntity.setGlowing(true); refreshGlowTeam(); }

        applyEquipment();
        afterSpawn();
    }

    private void afterSpawn() {
        if (looking) startLookTask();
        if (waypoints != null && waypoints.length > 0) startMovementTask();
        if (npcType == NpcType.HUMAN) startAnimationTask();
    }

    private void respawn() {
        Location loc = spawnedEntity != null ? spawnedEntity.getLocation() : spawnLocation;
        remove();
        spawnLocation = loc;
        spawn();
    }

    // -------------------------------------------------------------------------
    // Packet helpers (human NPC)
    // -------------------------------------------------------------------------

    private void sendTabAndSpawnToAll() {
        Object tabPacket   = NpcNms.buildPlayerInfoAddPacket(nmsServerPlayer);
        Object spawnPacket = NpcNms.buildAddPlayerPacket(nmsServerPlayer);
        byte yawByte = (byte) (spawnLocation.getYaw() * 256f / 360f);
        Object headPacket  = NpcNms.buildRotateHeadPacket(nmsServerPlayer, yawByte);

        for (Player p : Bukkit.getOnlinePlayers()) {
            NpcNms.sendPacket(p, tabPacket);
            NpcNms.sendPacket(p, spawnPacket);
            NpcNms.sendPacket(p, headPacket);
        }
    }

    private void sendSkinLayersToAll() {
        Object pkt = NpcNms.buildSkinLayersDataPacket(nmsServerPlayer);
        if (pkt == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) NpcNms.sendPacket(p, pkt);
    }

    private void scheduleTabRemove() {
        Bukkit.getScheduler().runTaskLater(HardGamezEngine.getInstance(), () -> {
            Object pkt = NpcNms.buildPlayerInfoRemovePacket(id);
            for (Player p : Bukkit.getOnlinePlayers()) NpcNms.sendPacket(p, pkt);
        }, 40L);
    }

    private void broadcastEntityData() {
        Object pkt = NpcNms.buildEntityDataPacket(nmsServerPlayer);
        if (pkt == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) NpcNms.sendPacket(p, pkt);
    }

    void sendSpawnTo(Player player) {
        if (npcType != NpcType.HUMAN || nmsServerPlayer == null) return;

        NpcNms.sendPacket(player, NpcNms.buildPlayerInfoAddPacket(nmsServerPlayer));
        NpcNms.sendPacket(player, NpcNms.buildAddPlayerPacket(nmsServerPlayer));
        float yaw = NpcNms.getYRot(nmsServerPlayer);
        NpcNms.sendPacket(player, NpcNms.buildRotateHeadPacket(nmsServerPlayer, (byte) (yaw * 256f / 360f)));

        Object skinPkt = NpcNms.buildSkinLayersDataPacket(nmsServerPlayer);
        if (skinPkt != null) NpcNms.sendPacket(player, skinPkt);

        Bukkit.getScheduler().runTaskLater(HardGamezEngine.getInstance(), () ->
            NpcNms.sendPacket(player, NpcNms.buildPlayerInfoRemovePacket(id)), 40L);
    }

    // -------------------------------------------------------------------------
    // Look at nearest player
    // -------------------------------------------------------------------------

    private void startLookTask() {
        stopLookTask();
        lookTask = Bukkit.getScheduler().runTaskTimer(HardGamezEngine.getInstance(), () -> {
            if (spawnedEntity == null || spawnedEntity.isDead()) return;
            if (movementTask != null) return;
            Player nearest = nearestPlayer();
            if (nearest == null) return;
            lookAt(nearest.getEyeLocation());
        }, 2L, 2L);
    }

    private void stopLookTask() {
        if (lookTask != null) { lookTask.cancel(); lookTask = null; }
    }

    private Player nearestPlayer() {
        Player nearest = null;
        double minDist = Double.MAX_VALUE;
        Location loc = spawnedEntity.getLocation();
        for (Player p : loc.getWorld().getPlayers()) {
            double d = p.getLocation().distanceSquared(loc);
            if (d < LOOK_RANGE_SQ && d < minDist) { minDist = d; nearest = p; }
        }
        return nearest;
    }

    private void lookAt(Location target) {
        Location cur = spawnedEntity.getLocation();
        double dx = target.getX() - cur.getX();
        double dy = target.getY() - cur.getY();
        double dz = target.getZ() - cur.getZ();
        double dxz = Math.sqrt(dx * dx + dz * dz);
        float yaw   = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dxz));

        if (npcType == NpcType.HUMAN && nmsServerPlayer != null) {
            NpcNms.setYRot(nmsServerPlayer, yaw);
            NpcNms.setXRot(nmsServerPlayer, pitch);
            NpcNms.setYHeadRot(nmsServerPlayer, yaw);

            byte yawB   = (byte) (yaw   * 256f / 360f);
            byte pitchB = (byte) (pitch * 256f / 360f);
            int eid = NpcNms.getEntityId(nmsServerPlayer);

            Object rotPkt  = NpcNms.buildMoveRotOnlyPacket(eid, yawB, pitchB, NpcNms.isOnGround(nmsServerPlayer));
            Object headPkt = NpcNms.buildRotateHeadPacket(nmsServerPlayer, yawB);

            for (Player p : Bukkit.getOnlinePlayers()) {
                NpcNms.sendPacket(p, rotPkt);
                NpcNms.sendPacket(p, headPkt);
            }
        } else if (spawnedEntity instanceof LivingEntity l) {
            spawnedEntity.teleport(spawnedEntity.getLocation().setDirection(
                target.toVector().subtract(cur.toVector()).normalize()));
        }
    }

    // -------------------------------------------------------------------------
    // Waypoint movement
    // -------------------------------------------------------------------------

    private void startMovementTask() {
        stopMovementTask();
        if (waypoints == null || waypoints.length == 0) return;

        double speed = (npcType == NpcType.HUMAN) ? PLAYER_WALK_SPEED : MOB_WALK_SPEED;

        movementTask = Bukkit.getScheduler().runTaskTimer(HardGamezEngine.getInstance(), () -> {
            if (spawnedEntity == null || spawnedEntity.isDead()) return;
            if (waypoints == null || waypoints.length == 0) return;

            Location cur    = spawnedEntity.getLocation();
            Location target = waypoints[waypointIndex];

            double dx = target.getX() - cur.getX();
            double dz = target.getZ() - cur.getZ();
            double dxz = Math.sqrt(dx * dx + dz * dz);

            if (dxz < 0.35) { advanceWaypoint(); return; }

            double nx = dx / dxz * speed;
            double nz = dz / dxz * speed;
            float  yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

            Location next = cur.clone().add(nx, 0, nz);
            next.setYaw(yaw);
            next.setPitch(0);
            next.setY(groundY(next));

            moveNpcTo(next);
        }, 1L, 1L);
    }

    private void stopMovementTask() {
        if (movementTask != null) { movementTask.cancel(); movementTask = null; }
    }

    private void restartMovement() {
        waypointIndex = 0;
        sheriffDirection = 1;
        stopMovementTask();
        startMovementTask();
    }

    private void advanceWaypoint() {
        if (waypoints == null || waypoints.length == 0) return;
        switch (loopType) {
            case FIRST_POINT -> waypointIndex = (waypointIndex + 1) % waypoints.length;
            case SHERIFF -> {
                int next = waypointIndex + sheriffDirection;
                if (next >= waypoints.length) { sheriffDirection = -1; waypointIndex = Math.max(0, waypoints.length - 2); }
                else if (next < 0)            { sheriffDirection =  1; waypointIndex = Math.min(1, waypoints.length - 1); }
                else                          { waypointIndex = next; }
            }
            case NONE -> {
                if (waypointIndex < waypoints.length - 1) waypointIndex++;
                else stopMovementTask();
            }
        }
    }

    private double groundY(Location loc) {
        if (loc.getWorld() == null) return loc.getY();
        int x = loc.getBlockX(), z = loc.getBlockZ();
        int maxY = (int) Math.min(loc.getY() + 2, loc.getWorld().getMaxHeight() - 1);
        for (int y = maxY; y >= loc.getWorld().getMinHeight(); y--) {
            if (loc.getWorld().getBlockAt(x, y, z).getType().isSolid()) return y + 1.0;
        }
        return loc.getY();
    }

    private void moveNpcTo(Location loc) {
        if (npcType == NpcType.HUMAN && nmsServerPlayer != null) {
            double ox = NpcNms.getXOld(nmsServerPlayer);
            double oy = NpcNms.getYOld(nmsServerPlayer);
            double oz = NpcNms.getZOld(nmsServerPlayer);

            NpcNms.moveEntityTo(nmsServerPlayer, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

            byte yawB = (byte) (loc.getYaw() * 256f / 360f);
            Object movePkt = NpcNms.buildMoveRotPacket(nmsServerPlayer,
                (short) ((loc.getX() - ox) * 4096),
                (short) ((loc.getY() - oy) * 4096),
                (short) ((loc.getZ() - oz) * 4096),
                yawB, (byte) 0,
                NpcNms.isOnGround(nmsServerPlayer));
            Object headPkt = NpcNms.buildRotateHeadPacket(nmsServerPlayer, yawB);

            for (Player p : Bukkit.getOnlinePlayers()) {
                NpcNms.sendPacket(p, movePkt);
                NpcNms.sendPacket(p, headPkt);
            }

            NpcNms.setXOld(nmsServerPlayer, loc.getX());
            NpcNms.setYOld(nmsServerPlayer, loc.getY());
            NpcNms.setZOld(nmsServerPlayer, loc.getZ());
        } else if (spawnedEntity != null) {
            spawnedEntity.teleport(loc);
        }
    }

    // -------------------------------------------------------------------------
    // Walking arm swing animation (human only, fires while moving)
    // -------------------------------------------------------------------------

    private void startAnimationTask() {
        stopAnimationTask();
        if (npcType != NpcType.HUMAN || nmsServerPlayer == null) return;

        animationTask = Bukkit.getScheduler().runTaskTimer(HardGamezEngine.getInstance(), () -> {
            if (movementTask == null || nmsServerPlayer == null) return;
            Object pkt = NpcNms.buildAnimatePacket(nmsServerPlayer, 0);
            if (pkt == null) return;
            for (Player p : Bukkit.getOnlinePlayers()) NpcNms.sendPacket(p, pkt);
        }, 0L, 12L);
    }

    private void stopAnimationTask() {
        if (animationTask != null) { animationTask.cancel(); animationTask = null; }
    }

    // -------------------------------------------------------------------------
    // Equipment
    // -------------------------------------------------------------------------

    private void applyEquipment() {
        if (equipment == null || !(spawnedEntity instanceof LivingEntity l)) return;
        EntityEquipment eq = l.getEquipment();
        if (eq == null) return;

        if (equipment.getSize() > 0 && equipment.getItem(0) != null) eq.setItemInMainHand(equipment.getItem(0));
        if (equipment.getSize() > 1 && equipment.getItem(1) != null) eq.setItemInOffHand(equipment.getItem(1));
        if (equipment.getSize() > 2 && equipment.getItem(2) != null) eq.setHelmet(equipment.getItem(2));
        if (equipment.getSize() > 3 && equipment.getItem(3) != null) eq.setChestplate(equipment.getItem(3));
        if (equipment.getSize() > 4 && equipment.getItem(4) != null) eq.setLeggings(equipment.getItem(4));
        if (equipment.getSize() > 5 && equipment.getItem(5) != null) eq.setBoots(equipment.getItem(5));
    }

    // -------------------------------------------------------------------------
    // Name refresh
    // -------------------------------------------------------------------------

    private void refreshName() {
        if (npcType == NpcType.HUMAN) {
            respawn();
        } else if (spawnedEntity instanceof LivingEntity l) {
            l.customName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand().deserialize(name));
        }
    }

    // -------------------------------------------------------------------------
    // Glow team
    // -------------------------------------------------------------------------

    private void refreshGlowTeam() {
        if (spawnedEntity == null) return;
        String teamName = "hg_npc_" + id.toString().substring(0, 12);
        org.bukkit.scoreboard.Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team team = sb.getTeam(teamName);
        if (team == null) team = sb.registerNewTeam(teamName);
        team.setColor(glowColor);
        String entry = (npcType == NpcType.HUMAN) ? trimName(name) : spawnedEntity.getUniqueId().toString();
        if (!team.hasEntry(entry)) team.addEntry(entry);
    }

    private void cleanupGlowTeam() {
        String teamName = "hg_npc_" + id.toString().substring(0, 12);
        org.bukkit.scoreboard.Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team team = sb.getTeam(teamName);
        if (team != null) team.unregister();
    }

    // -------------------------------------------------------------------------
    // Waypoints clear / remove
    // -------------------------------------------------------------------------

    public NpcObject clearWaypoints() {
        stopMovementTask();
        waypoints = null;
        waypointIndex = 0;
        sheriffDirection = 1;
        if (spawnedEntity != null && spawnLocation != null) spawnedEntity.teleport(spawnLocation);
        return this;
    }

    public void remove() {
        stopLookTask();
        stopMovementTask();
        stopAnimationTask();

        if (npcType == NpcType.HUMAN) {
            if (nmsServerPlayer != null) {
                int eid = NpcNms.getEntityId(nmsServerPlayer);
                Object removePkt = NpcNms.buildRemoveEntitiesPacket(eid);
                Object tabRemPkt = NpcNms.buildPlayerInfoRemovePacket(id);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    NpcNms.sendPacket(p, removePkt);
                    NpcNms.sendPacket(p, tabRemPkt);
                }
                nmsServerPlayer = null;
            }
        } else {
            if (spawnedEntity != null && !spawnedEntity.isDead()) spawnedEntity.remove();
        }

        if (glowing) cleanupGlowTeam();
        spawnedEntity = null;
        NpcEngine.untrack(id);
    }

    // -------------------------------------------------------------------------
    // Click handling
    // -------------------------------------------------------------------------

    void handleClick(Player player, NpcClickEvent.ClickType clickType) {
        if (clickHandler != null) clickHandler.accept(new NpcClickEvent(player, this, clickType));
    }

    // -------------------------------------------------------------------------
    // EntityType mapping
    // -------------------------------------------------------------------------

    private org.bukkit.entity.EntityType mapNpcTypeToBukkit(NpcType type) {
        return switch (type) {
            case ZOMBIE          -> org.bukkit.entity.EntityType.ZOMBIE;
            case SKELETON        -> org.bukkit.entity.EntityType.SKELETON;
            case CREEPER         -> org.bukkit.entity.EntityType.CREEPER;
            case SPIDER          -> org.bukkit.entity.EntityType.SPIDER;
            case CAVE_SPIDER     -> org.bukkit.entity.EntityType.CAVE_SPIDER;
            case ENDERMAN        -> org.bukkit.entity.EntityType.ENDERMAN;
            case BLAZE           -> org.bukkit.entity.EntityType.BLAZE;
            case WITCH           -> org.bukkit.entity.EntityType.WITCH;
            case WITHER_SKELETON -> org.bukkit.entity.EntityType.WITHER_SKELETON;
            case PILLAGER        -> org.bukkit.entity.EntityType.PILLAGER;
            case VINDICATOR      -> org.bukkit.entity.EntityType.VINDICATOR;
            case EVOKER          -> org.bukkit.entity.EntityType.EVOKER;
            case PHANTOM         -> org.bukkit.entity.EntityType.PHANTOM;
            case DROWNED         -> org.bukkit.entity.EntityType.DROWNED;
            case HUSK            -> org.bukkit.entity.EntityType.HUSK;
            case STRAY           -> org.bukkit.entity.EntityType.STRAY;
            case ZOMBIE_VILLAGER -> org.bukkit.entity.EntityType.ZOMBIE_VILLAGER;
            case PIGLIN          -> org.bukkit.entity.EntityType.PIGLIN;
            case PIGLIN_BRUTE    -> org.bukkit.entity.EntityType.PIGLIN_BRUTE;
            case ZOMBIFIED_PIGLIN -> org.bukkit.entity.EntityType.ZOMBIFIED_PIGLIN;
            case HOGLIN          -> org.bukkit.entity.EntityType.HOGLIN;
            case ZOGLIN          -> org.bukkit.entity.EntityType.ZOGLIN;
            case GUARDIAN        -> org.bukkit.entity.EntityType.GUARDIAN;
            case ELDER_GUARDIAN  -> org.bukkit.entity.EntityType.ELDER_GUARDIAN;
            case SHULKER         -> org.bukkit.entity.EntityType.SHULKER;
            case SILVERFISH      -> org.bukkit.entity.EntityType.SILVERFISH;
            case ENDERMITE       -> org.bukkit.entity.EntityType.ENDERMITE;
            case VEX             -> org.bukkit.entity.EntityType.VEX;
            case RAVAGER         -> org.bukkit.entity.EntityType.RAVAGER;
            case WARDEN          -> org.bukkit.entity.EntityType.WARDEN;
            default              -> org.bukkit.entity.EntityType.ZOMBIE;
        };
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String trimName(String name) {
        return name.length() > 16 ? name.substring(0, 16) : name;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public UUID getId()              { return id; }
    public NpcType getNpcType()      { return npcType; }
    public String getName()          { return name; }
    public Location getLocation()    { return spawnedEntity != null ? spawnedEntity.getLocation() : spawnLocation != null ? spawnLocation.clone() : null; }
    public Entity getEntity()        { return spawnedEntity; }
    public boolean isSpawned()       { return spawnedEntity != null && !spawnedEntity.isDead(); }
    public boolean isLooking()       { return looking; }
    public boolean isHideNickname()  { return hideNickname; }
    public boolean isInvincible()    { return invincible; }
    public LoopType getLoopType()    { return loopType; }
    public NpcSkin getSkin()         { return skin; }

    int getEntityId() {
        if (npcType == NpcType.HUMAN && nmsServerPlayer != null) return NpcNms.getEntityId(nmsServerPlayer);
        if (spawnedEntity != null) return spawnedEntity.getEntityId();
        return -1;
    }
}
