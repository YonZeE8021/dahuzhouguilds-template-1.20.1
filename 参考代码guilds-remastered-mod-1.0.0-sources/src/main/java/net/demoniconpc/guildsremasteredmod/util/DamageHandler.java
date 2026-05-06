/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
 *  net.minecraft.Formatting
 *  net.minecraft.Entity
 *  net.minecraft.Text
 *  net.minecraft.ServerPlayerEntity
 */
package net.demoniconpc.guildsremasteredmod.util;

import net.demoniconpc.guildsremasteredmod.data.GuildDataManager;
import net.demoniconpc.guildsremasteredmod.guild.Guild;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.util.Formatting;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

public class DamageHandler {
    public static void register() {
        System.out.println("[GuildsRemastered] DamageHandler registered.");
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((target, source, amount) -> {
            if (!(target instanceof ServerPlayerEntity)) {
                return true;
            }
            ServerPlayerEntity victim = (ServerPlayerEntity)target;
            Entity patt0$temp = source.getAttacker();
            if (!(patt0$temp instanceof ServerPlayerEntity)) {
                return true;
            }
            ServerPlayerEntity attacker = (ServerPlayerEntity)patt0$temp;
            Guild attackerGuild = GuildDataManager.getGuildByPlayer(attacker.getUuid());
            Guild victimGuild = GuildDataManager.getGuildByPlayer(victim.getUuid());
            if (attackerGuild == null || victimGuild == null) {
                return true;
            }
            if (attackerGuild.getId().equals(victimGuild.getId())) {
                return true;
            }
            String attackerShortId = attackerGuild.getShortenedId();
            String victimShortId = victimGuild.getShortenedId();
            boolean attackerDisabled = attackerGuild.isPvpDisabledWith(victimShortId);
            boolean victimDisabled = victimGuild.isPvpDisabledWith(attackerShortId);
            if (attackerDisabled && victimDisabled) {
                attacker.sendMessage((Text)Text.literal((String)"You cannot harm members of your allied guild").formatted(Formatting.field_1061));
                return false;
            }
            return true;
        });
    }
}

