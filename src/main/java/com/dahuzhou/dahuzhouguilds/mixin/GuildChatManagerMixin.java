package com.dahuzhou.dahuzhouguilds.mixin;

import com.dahuzhou.dahuzhouguilds.GuildsConfig;
import com.dahuzhou.dahuzhouguilds.data.GuildDataManager;
import com.dahuzhou.dahuzhouguilds.guild.Guild;
import com.dahuzhou.dahuzhouguilds.util.GuildDisplayUtil;
import com.dahuzhou.dahuzhouguilds.util.GuildHungerBoundPrefixHelper;
import java.util.UUID;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class GuildChatManagerMixin {
	@Inject(method = "handleDecoratedMessage", at = @At("HEAD"), cancellable = true)
	private void onGuildChat(SignedMessage message, CallbackInfo ci) {
		ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
		ServerPlayerEntity player = handler.player;
		UUID playerId = player.getUuid();
		Guild guild = GuildDataManager.getGuildByPlayer(playerId);
		if (guild == null || !GuildDataManager.ChatStateManager.isGuildChatEnabled(playerId)) {
			return;
		}
		String raw = message.getContent().getString();
		String rank = "";
		Guild.GuildMemberInfo info = guild.getMembers().get(playerId);
		if (info != null && info.rank != null) {
			rank = info.rank;
		}
		MutableText line = Text.empty();
		if (!rank.isEmpty()) {
			line.append(Text.literal("[" + rank + "] ").formatted(Formatting.GRAY));
		}
		MutableText namePart;
		if (GuildsConfig.enableHungerBoundIntegration) {
			String deathColor = GuildHungerBoundPrefixHelper.getDeathColorFromDeathCount(player);
			Formatting deathFormatting = Formatting.byName(deathColor.toUpperCase());
			if (deathFormatting == null) {
				deathFormatting = Formatting.WHITE;
			}
			namePart = Text.literal(player.getName().getString()).formatted(deathFormatting);
		} else {
			namePart = Text.literal(player.getName().getString());
		}
		line.append(namePart);
		line.append(GuildDisplayUtil.guildSuffixBracketText(guild));
		line.append(Text.literal(": " + raw).formatted(Formatting.WHITE));
		for (ServerPlayerEntity target : player.getServer().getPlayerManager().getPlayerList()) {
			if (GuildDataManager.getGuildByPlayer(target.getUuid()) == guild) {
				target.sendMessage(line);
			}
		}
		ci.cancel();
	}
}
