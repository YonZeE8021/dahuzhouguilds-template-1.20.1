package com.dahuzhou.dahuzhouguilds.mixin;

import com.dahuzhou.dahuzhouguilds.GuildTexts;
import com.dahuzhou.dahuzhouguilds.data.GuildDataManager;
import com.dahuzhou.dahuzhouguilds.guild.Guild;
import com.dahuzhou.dahuzhouguilds.util.AllyChatBridgeManager;
import java.util.UUID;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
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
public class AllyChatManagerMixin {
	@Inject(method = "handleDecoratedMessage", at = @At("HEAD"), cancellable = true)
	private void onAllyChat(SignedMessage message, CallbackInfo ci) {
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
		String targetShort = targetGuild.getShortenedId();
		if (!AllyChatBridgeManager.isBridgeActive(senderShort, targetShort)) {
			player.sendMessage(GuildTexts.t("ally_chat.error.bridge_inactive").formatted(Formatting.RED));
			AllyChatBridgeManager.clearAllyChatToggle(playerId);
			return;
		}
		if (!senderGuild.getAllies().containsKey(targetShort) || !targetGuild.getAllies().containsKey(senderShort)) {
			player.sendMessage(GuildTexts.t("ally_chat.error.not_mutual").formatted(Formatting.RED));
			AllyChatBridgeManager.clearAllyChatToggle(playerId);
			return;
		}
		MutableText formatted = GuildTexts.t("ally_chat.prefix").formatted(Formatting.LIGHT_PURPLE)
				.append(player.getDisplayName().copy())
				.append(GuildTexts.t("ally_chat.colon"))
				.append(Text.literal(message.getContent().getString()).formatted(Formatting.WHITE));
		for (UUID memberId : senderGuild.getMembers().keySet()) {
			ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
			if (member != null) {
				member.sendMessage(formatted);
			}
		}
		for (UUID memberId : targetGuild.getMembers().keySet()) {
			if (memberId.equals(playerId)) {
				continue;
			}
			ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
			if (member != null) {
				member.sendMessage(formatted);
			}
		}
		ci.cancel();
	}
}
