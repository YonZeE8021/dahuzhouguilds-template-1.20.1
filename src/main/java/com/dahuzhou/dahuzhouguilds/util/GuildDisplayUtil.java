package com.dahuzhou.dahuzhouguilds.util;

import com.dahuzhou.dahuzhouguilds.guild.Guild;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * 玩家列表等处的公会展示：玩家名保持默认色，仅后缀 {@code <公会名>} 使用公会颜色。
 */
public final class GuildDisplayUtil {
	private GuildDisplayUtil() {}

	public static MutableText guildSuffixBracketText(Guild guild) {
		return guildSuffixBracketText(guild.getName(), guild.getColor());
	}

	public static MutableText guildSuffixBracketText(String guildName, String colorName) {
		String safe = guildName == null ? "" : guildName;
		return Text.literal("<" + safe + ">").formatted(GuildColorUtil.getFormatting(colorName));
	}

	/**
	 * Essentials /nickname 等使用的 JSON 片段：玩家名无色，后缀带色。
	 */
	public static String essentialsNicknameJson(String playerName, Guild guild) {
		String suffix = "<" + jsonEscape(guild.getName()) + ">";
		String color = guild.getColor() == null ? "white" : guild.getColor().toLowerCase();
		return "[{\"text\":\"" + jsonEscape(playerName) + "\"},{\"text\":\"" + suffix + "\",\"color\":\"" + jsonEscape(color) + "\"}]";
	}

	public static String jsonEscape(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	/** HungerBound 等使用 § 序列时的彩色公会后缀（玩家名保持死亡色，不受公会色覆盖）。 */
	public static String legacyColoredGuildSuffix(Guild guild) {
		Formatting f = GuildColorUtil.getFormatting(guild.getColor());
		String raw = guild.getName() == null ? "" : guild.getName();
		String safe = raw.replace("<", "").replace(">", "");
		return "\u00a7" + f.getCode() + "<" + safe + ">\u00a7r";
	}

	public static String stripBracketsForChat(String guildName) {
		if (guildName == null) {
			return "";
		}
		return guildName.replace("<", "").replace(">", "");
	}
}
