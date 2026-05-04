package com.dahuzhou.dahuzhouguilds;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * 服务端发送给客户端的翻译文本（客户端按语言设置解析 zh_cn / en_us 等）。
 */
public final class GuildTexts {
	public static final String P = "dahuzhouguilds.";

	private GuildTexts() {}

	public static MutableText t(String key, Object... args) {
		return Text.translatable(P + key, args);
	}
}
