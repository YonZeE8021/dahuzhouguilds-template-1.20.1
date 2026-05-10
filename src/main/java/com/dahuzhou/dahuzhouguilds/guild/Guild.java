/*
 * Decompiled with CFR 0.152.
 */
package com.dahuzhou.dahuzhouguilds.guild;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Guild {
    private final UUID id;
    private String name;
    private String color;
    private final Instant createdAt;
    private UUID ownerId;
    private String ownerName;
    private final boolean friendlyFire;
    private String prefix;
    private final Relationships relationships = new Relationships();
    public Map<String, Map<String, Boolean>> rankPermissions = new HashMap<String, Map<String, Boolean>>();
    private final Map<String, AllyInfo> allies = new HashMap<String, AllyInfo>();
    private final Set<UUID> enemies = new HashSet<UUID>();
    private final List<UUID> pendingAllyRequests = new ArrayList<UUID>();
    private final Map<UUID, GuildMemberInfo> members = new HashMap<UUID, GuildMemberInfo>();
    private String motd = "Default Guild MOTD - change with /guild motd [message]";
    private final Map<Integer, Integer> bankTabUnlockProgress = new HashMap<Integer, Integer>();
    private int unlockedBankTabs = 1;
    private Double homeX = null;
    private Double homeY = null;
    private Double homeZ = null;
    private String homeWorld = null;
    /** 0–9999，用於指令與存檔中的四位公會編號；載入後由 {@link com.dahuzhou.dahuzhouguilds.data.GuildDataManager} 保證已分配。 */
    private int numericPublicId = -1;

    public Guild(UUID id, String name, String color, Instant createdAt, UUID ownerId, String ownerName, boolean friendlyFire) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.prefix = name;
        this.createdAt = createdAt;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.friendlyFire = friendlyFire;
        this.initializeRankPermissions();
    }

    private void initializeRankPermissions() {
        HashMap<String, Boolean> guildMasterPermissions = new HashMap<String, Boolean>();
        guildMasterPermissions.put("canInvite", true);
        guildMasterPermissions.put("canKick", true);
        guildMasterPermissions.put("canPromote", true);
        guildMasterPermissions.put("canDemote", true);
        guildMasterPermissions.put("canSetHome", true);
        guildMasterPermissions.put("canUseHome", true);
        guildMasterPermissions.put("canUseBank", true);
        guildMasterPermissions.put("canTogglePvP", true);
        guildMasterPermissions.put("canDeposit", true);
        guildMasterPermissions.put("canWithdraw", true);
        guildMasterPermissions.put("canUseBankTab", true);
        this.rankPermissions.put("Guild Master", guildMasterPermissions);
        HashMap<String, Boolean> officerPermissions = new HashMap<String, Boolean>();
        officerPermissions.put("canInvite", true);
        officerPermissions.put("canKick", true);
        officerPermissions.put("canPromote", true);
        officerPermissions.put("canDemote", true);
        officerPermissions.put("canSetHome", true);
        officerPermissions.put("canUseHome", true);
        officerPermissions.put("canUseBank", true);
        officerPermissions.put("canTogglePvP", true);
        officerPermissions.put("canDeposit", true);
        officerPermissions.put("canWithdraw", true);
        officerPermissions.put("canUseBankTab", true);
        this.rankPermissions.put("Officer", officerPermissions);
        HashMap<String, Boolean> memberPermissions = new HashMap<String, Boolean>();
        memberPermissions.put("canInvite", true);
        memberPermissions.put("canKick", false);
        memberPermissions.put("canPromote", false);
        memberPermissions.put("canDemote", false);
        memberPermissions.put("canSetHome", false);
        memberPermissions.put("canUseHome", true);
        memberPermissions.put("canUseBank", false);
        memberPermissions.put("canTogglePvP", false);
        memberPermissions.put("canDeposit", false);
        memberPermissions.put("canWithdraw", false);
        memberPermissions.put("canUseBankTab", false);
        this.rankPermissions.put("Member", memberPermissions);
        HashMap<String, Boolean> initiatePermissions = new HashMap<String, Boolean>();
        initiatePermissions.put("canInvite", false);
        initiatePermissions.put("canKick", false);
        initiatePermissions.put("canPromote", false);
        initiatePermissions.put("canDemote", false);
        initiatePermissions.put("canSetHome", false);
        initiatePermissions.put("canUseHome", false);
        initiatePermissions.put("canUseBank", false);
        initiatePermissions.put("canTogglePvP", false);
        initiatePermissions.put("canDeposit", false);
        initiatePermissions.put("canWithdraw", false);
        initiatePermissions.put("canUseBankTab", false);
        this.rankPermissions.put("Initiate", initiatePermissions);
    }

    public static String formatPublicId(int numericId) {
        if (numericId < 0 || numericId > 9999) {
            throw new IllegalArgumentException("numericId out of range 0..9999: " + numericId);
        }
        return String.format("%04d", numericId);
    }

    public int getNumericPublicId() {
        return this.numericPublicId;
    }

    public void setNumericPublicId(int numericPublicId) {
        this.numericPublicId = numericPublicId;
    }

    /**
     * 銀行檔、盟友 Map 鍵、結盟橋接等使用的短識別符；為四位數字（含前導零）。
     */
    public String getShortenedId() {
        if (this.numericPublicId < 0) {
            throw new IllegalStateException("Guild numericPublicId not assigned yet: " + this.name);
        }
        return Guild.formatPublicId(this.numericPublicId);
    }

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getColor() {
        return this.color;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public void setColor(String newColor) {
        this.color = newColor;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public String getFormattedDate() {
        return DateTimeFormatter.ofPattern("MMMM dd, yyyy").withZone(ZoneId.systemDefault()).format(this.createdAt);
    }

    public UUID getOwnerId() {
        return this.ownerId;
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    public void setOwner(UUID newOwnerId, String newOwnerName) {
        this.ownerId = newOwnerId;
        this.ownerName = newOwnerName;
    }

    public boolean isFriendlyFireEnabled() {
        return this.friendlyFire;
    }

    public Map<String, Boolean> getRankPermissions(String rankName) {
        return this.rankPermissions.getOrDefault(rankName, new HashMap());
    }

    public void setRankPermissions(String rankName, Map<String, Boolean> permissions) {
        this.rankPermissions.put(rankName, permissions);
    }

    public void addPermissionToRank(String rankName, String permission, boolean value) {
        this.rankPermissions.computeIfAbsent(rankName, k -> new HashMap()).put(permission, value);
    }

    public boolean isGuildMaster(UUID playerId) {
        return playerId.equals(this.ownerId);
    }

    public boolean hasRankPermission(UUID playerId, String permission) {
        if (playerId.equals(this.ownerId)) {
            return true;
        }
        String rank = this.getRank(playerId);
        Map<String, Boolean> permissions = this.getRankPermissions(rank);
        return permissions.getOrDefault(permission, false);
    }

    public Map<UUID, GuildMemberInfo> getMembers() {
        return this.members;
    }

    public void addMember(UUID memberId, String name, String rank) {
        this.members.put(memberId, new GuildMemberInfo(name, rank));
    }

    public GuildMemberInfo getMemberInfo(UUID memberId) {
        return this.members.get(memberId);
    }

    public void removeMember(UUID playerId) {
        this.members.remove(playerId);
    }

    public boolean isMember(UUID playerId) {
        return this.members.containsKey(playerId);
    }

    public String getRank(UUID playerId) {
        GuildMemberInfo info = this.members.get(playerId);
        return info != null ? info.rank : "Initiate";
    }

    public void setRank(UUID playerId, String rank) {
        GuildMemberInfo info = this.members.get(playerId);
        if (info != null) {
            info.rank = rank;
        }
    }

    public String getMotd() {
        return this.motd;
    }

    public void setMotd(String motd) {
        this.motd = motd;
    }

    public Relationships getRelationships() {
        return this.relationships;
    }

    public Map<String, AllyInfo> getAllies() {
        return this.allies;
    }

    public Set<UUID> getEnemies() {
        return this.enemies;
    }

    public List<UUID> getPendingAllyRequests() {
        return this.pendingAllyRequests;
    }

    public void addAlly(Guild otherGuild, String allyDisplayName) {
        this.allies.put(otherGuild.getShortenedId(), new AllyInfo(allyDisplayName, false));
    }

    public void removeAlly(Guild otherGuild) {
        this.allies.remove(otherGuild.getShortenedId());
    }

    public void addEnemy(UUID guildId) {
        this.enemies.add(guildId);
    }

    public void removeEnemy(UUID guildId) {
        this.enemies.remove(guildId);
    }

    public void setPvpToggle(String allyShortId, boolean value) {
        AllyInfo info = this.allies.get(allyShortId);
        if (info != null) {
            info.pvpDisabled = value;
        }
    }

    public boolean isPvpDisabledWith(String allyShortId) {
        AllyInfo info = this.allies.get(allyShortId);
        return info != null && info.pvpDisabled;
    }

    public void setHome(double x, double y, double z, String world) {
        this.homeX = x;
        this.homeY = y;
        this.homeZ = z;
        this.homeWorld = world;
    }

    public boolean hasHome() {
        return this.homeX != null && this.homeY != null && this.homeZ != null && this.homeWorld != null;
    }

    public double getHomeX() {
        return this.homeX;
    }

    public double getHomeY() {
        return this.homeY;
    }

    public double getHomeZ() {
        return this.homeZ;
    }

    public String getHomeWorld() {
        return this.homeWorld;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public int getUnlockedBankTabs() {
        return this.unlockedBankTabs;
    }

    public void setUnlockedBankTabs(int count) {
        this.unlockedBankTabs = count;
    }

    public int getBankUnlockProgress(int tabIndex) {
        return this.bankTabUnlockProgress.getOrDefault(tabIndex, 0);
    }

    public void addBankUnlockProgress(int tabIndex, int amount) {
        int current = this.bankTabUnlockProgress.getOrDefault(tabIndex, 0);
        this.bankTabUnlockProgress.put(tabIndex, current + amount);
    }

    public int getRequiredNetheriteForTab(int tabIndex) {
        int baseCost = 8;
        int costMultiplier = 8;
        if (tabIndex <= 1) {
            return baseCost;
        }
        return baseCost + (tabIndex - 1) * costMultiplier;
    }

    public int getNetheriteDonatedForTab(int tabIndex) {
        return this.bankTabUnlockProgress.getOrDefault(tabIndex, 0);
    }

    public Map<Integer, Integer> getBankUnlockMap() {
        return this.bankTabUnlockProgress;
    }

    public static class Relationships {
        private final List<UUID> allies = new ArrayList<UUID>();
        private final List<UUID> enemies = new ArrayList<UUID>();

        public List<UUID> getAllies() {
            return this.allies;
        }

        public List<UUID> getEnemies() {
            return this.enemies;
        }
    }

    public static class GuildMemberInfo {
        public String name;
        public String rank;

        public GuildMemberInfo(String name, String rank) {
            this.name = name;
            this.rank = rank;
        }
    }

    public static class AllyInfo {
        public String name;
        public boolean pvpDisabled = false;

        public AllyInfo(String name) {
            this.name = name;
        }

        public AllyInfo(String name, boolean pvpDisabled) {
            this.name = name;
            this.pvpDisabled = pvpDisabled;
        }
    }
}

