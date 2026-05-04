package com.dahuzhou.dahuzhouguilds.util;

/** 公会名长度按 Unicode 码点计数，便于中文等非 BMP 字符。 */
public final class GuildNameUtil {
	public static final int MAX_GUILD_NAME_CODE_POINTS = 16;

	private GuildNameUtil() {}

	public static int codePointLength(String s) {
		if (s == null) {
			return 0;
		}
		return s.codePointCount(0, s.length());
	}

	public static boolean isWithinMaxLength(String s) {
		return codePointLength(s) <= MAX_GUILD_NAME_CODE_POINTS;
	}
}
