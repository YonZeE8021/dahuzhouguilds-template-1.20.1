package com.dahuzhou.dahuzhouguilds.util;

import com.dahuzhou.dahuzhouguilds.data.GuildDataManager;
import com.dahuzhou.dahuzhouguilds.guild.Guild;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Spell Engine 等模组的目标关系判定：公会盟友之间不应被记分板队伍误判为敌对。
 */
public final class GuildSpellCompat {
	private GuildSpellCompat() {
	}

	/**
	 * @return Spell Engine {@code TargetHelper.Relation} 常量名 {@code FRIENDLY} / {@code SEMI_FRIENDLY}，无需覆盖时返回 null
	 */
	@Nullable
	public static String allyRelationOverrideName(LivingEntity attacker, Entity target) {
		if (attacker.getWorld().isClient()) {
			return null;
		}
		if (!(attacker instanceof ServerPlayerEntity attackerPlayer)) {
			return null;
		}
		if (!(target instanceof ServerPlayerEntity targetPlayer)) {
			return null;
		}
		Guild attackerGuild = GuildDataManager.getGuildByPlayer(attackerPlayer.getUuid());
		Guild targetGuild = GuildDataManager.getGuildByPlayer(targetPlayer.getUuid());
		if (attackerGuild == null || targetGuild == null) {
			return null;
		}
		if (attackerGuild.getId().equals(targetGuild.getId())) {
			return null;
		}
		String attackerShort = attackerGuild.getShortenedId();
		String targetShort = targetGuild.getShortenedId();
		if (!attackerGuild.getAllies().containsKey(targetShort) || !targetGuild.getAllies().containsKey(attackerShort)) {
			return null;
		}
		boolean bothPvpDisabled = attackerGuild.isPvpDisabledWith(targetShort) && targetGuild.isPvpDisabledWith(attackerShort);
		return bothPvpDisabled ? "FRIENDLY" : "SEMI_FRIENDLY";
	}
}
