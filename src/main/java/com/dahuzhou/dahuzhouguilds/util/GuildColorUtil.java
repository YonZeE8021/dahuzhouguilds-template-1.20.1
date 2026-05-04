/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.Formatting
 */
package com.dahuzhou.dahuzhouguilds.util;

import java.util.logging.Logger;
import net.minecraft.util.Formatting;

public class GuildColorUtil {
    private static final Logger logger = Logger.getLogger(GuildColorUtil.class.getName());
    private static final Formatting DEFAULT_COLOR = Formatting.RED;

    public static Formatting getFormatting(String color) {
        try {
            logger.info("[GuildColorUtil] Attempting to apply color: " + color);
            Formatting formatting = Formatting.byName((String)color.toUpperCase());
            if (formatting == null) {
                throw new IllegalArgumentException("Invalid color: " + color);
            }
            logger.info("[GuildColorUtil] Successfully applied color: " + color);
            return formatting;
        }
        catch (IllegalArgumentException | NullPointerException e) {
            logger.warning("[GuildColorUtil] Invalid or null color provided: " + color + ". Falling back to default color.");
            return DEFAULT_COLOR;
        }
    }
}

