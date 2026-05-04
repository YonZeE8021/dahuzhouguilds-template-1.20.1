/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.Formatting
 *  net.minecraft.Text
 *  net.minecraft.ServerPlayerEntity
 *  net.minecraft.ServerPlayNetworkHandler
 *  net.minecraft.MutableText
 *  net.minecraft.SignedMessage
 *  net.minecraft.server.MinecraftServer
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.dahuzhou.dahuzhouguilds.mixin;

import java.util.UUID;
import com.dahuzhou.dahuzhouguilds.data.GuildDataManager;
import com.dahuzhou.dahuzhouguilds.guild.Guild;
import com.dahuzhou.dahuzhouguilds.util.AllyChatBridgeManager;
import net.minecraft.util.Formatting;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.MutableText;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ServerPlayNetworkHandler.class})
public class AllyChatManagerMixin {
    @Inject(method={"handleDecoratedMessage"}, at={@At(value="HEAD")}, cancellable=true)
    private void onAllyChat(SignedMessage message, CallbackInfo ci) {
        ServerPlayerEntity member;
        String targetShort;
        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
        ServerPlayerEntity player = handler.player;
        MinecraftServer server = player.getServer();
        UUID playerId = player.getUuid();
        if (!AllyChatBridgeManager.isChatToggled(playerId)) {
            return;
        }
        Guild senderGuild = GuildDataManager.getGuildByPlayer(playerId);
        UUID targetGuildId = AllyChatBridgeManager.getTargetGuild(playerId);
        Guild targetGuild = GuildDataManager.getGuildById(targetGuildId);
        if (senderGuild == null || targetGuild == null) {
            return;
        }
        String senderShort = senderGuild.getShortenedId();
        if (!AllyChatBridgeManager.isBridgeActive(senderShort, targetShort = targetGuild.getShortenedId())) {
            player.sendMessage((Text)Text.literal((String)"Ally chat disabled: Chat bridge no longer active.").formatted(Formatting.RED));
            AllyChatBridgeManager.clearAllyChatToggle(playerId);
            return;
        }
        if (!senderGuild.getAllies().containsKey(targetShort) || !targetGuild.getAllies().containsKey(senderShort)) {
            player.sendMessage((Text)Text.literal((String)"Ally chat disabled: Alliance no longer mutual.").formatted(Formatting.RED));
            AllyChatBridgeManager.clearAllyChatToggle(playerId);
            return;
        }
        MutableText formatted = Text.literal((String)"[ALLY] ").formatted(Formatting.LIGHT_PURPLE).append((Text)player.getDisplayName().copy()).append((Text)Text.literal((String)": ").formatted(Formatting.WHITE)).append((Text)Text.literal((String)message.getContent().getString()).formatted(Formatting.WHITE));
        for (UUID memberId : senderGuild.getMembers().keySet()) {
            member = server.getPlayerManager().getPlayer(memberId);
            if (member == null) continue;
            member.sendMessage((Text)formatted);
        }
        for (UUID memberId : targetGuild.getMembers().keySet()) {
            if (memberId.equals(playerId) || (member = server.getPlayerManager().getPlayer(memberId)) == null) continue;
            member.sendMessage((Text)formatted);
        }
        ci.cancel();
    }
}

