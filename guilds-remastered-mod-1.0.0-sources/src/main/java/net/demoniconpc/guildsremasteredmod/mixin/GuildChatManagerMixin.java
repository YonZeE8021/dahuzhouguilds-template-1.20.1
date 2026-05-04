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
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.demoniconpc.guildsremasteredmod.mixin;

import java.util.UUID;
import net.demoniconpc.guildsremasteredmod.GuildsConfig;
import net.demoniconpc.guildsremasteredmod.data.GuildDataManager;
import net.demoniconpc.guildsremasteredmod.guild.Guild;
import net.demoniconpc.guildsremasteredmod.util.GuildHungerBoundPrefixHelper;
import net.minecraft.util.Formatting;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.MutableText;
import net.minecraft.network.message.SignedMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ServerPlayNetworkHandler.class})
public class GuildChatManagerMixin {
    @Inject(method={"handleDecoratedMessage"}, at={@At(value="HEAD")}, cancellable=true)
    private void onGuildChat(SignedMessage message, CallbackInfo ci) {
        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler)this;
        ServerPlayerEntity player = handler.player;
        UUID playerId = player.getUuid();
        Guild guild = GuildDataManager.getGuildByPlayer(playerId);
        if (guild != null && GuildDataManager.ChatStateManager.isGuildChatEnabled(playerId)) {
            MutableText playerNameText;
            MutableText rankText;
            Formatting guildColor;
            String raw = message.getContent().getString();
            String rank = "";
            Guild.GuildMemberInfo info = guild.getMembers().get(playerId);
            if (info != null && info.rank != null) {
                rank = info.rank;
            }
            try {
                guildColor = Formatting.valueOf((String)guild.getColor().toUpperCase());
            }
            catch (IllegalArgumentException | NullPointerException e) {
                guildColor = Formatting.field_1068;
            }
            MutableText guildNameText = Text.literal((String)("[" + guild.getName() + "] ")).formatted(guildColor);
            MutableText class_52502 = rankText = rank.isEmpty() ? Text.empty() : Text.literal((String)("[" + rank + "] ")).formatted(guildColor);
            if (GuildsConfig.enableHungerBoundIntegration) {
                String deathColor = GuildHungerBoundPrefixHelper.getDeathColorFromDeathCount(player);
                Formatting deathFormatting = Formatting.byName((String)deathColor.toUpperCase());
                playerNameText = Text.literal((String)player.getName().getString()).formatted(deathFormatting);
            } else {
                Formatting guildFormatting = Formatting.byName((String)guild.getColor().toUpperCase());
                playerNameText = Text.literal((String)player.getName().getString()).formatted(guildFormatting);
            }
            MutableText messageText = Text.literal((String)(": " + raw)).formatted(Formatting.field_1068);
            MutableText finalMessage = Text.empty().append((Text)guildNameText).append((Text)rankText).append((Text)playerNameText).append((Text)messageText);
            for (ServerPlayerEntity target : player.getServer().getPlayerManager().getPlayerList()) {
                if (GuildDataManager.getGuildByPlayer(target.getUuid()) != guild) continue;
                target.sendMessage((Text)finalMessage);
            }
            ci.cancel();
        }
    }
}

