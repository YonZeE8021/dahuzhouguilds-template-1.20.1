package com.dahuzhou.dahuzhouguilds.mixin.compat;

import com.dahuzhou.dahuzhouguilds.util.GuildSpellCompat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.spell_engine.utils.TargetHelper")
public class TargetHelperMixin {
	@Inject(method = "getRelation", at = @At("HEAD"), cancellable = true)
	private static void dahuzhouguilds$guildAllyRelation(LivingEntity attacker, Entity target, CallbackInfoReturnable<Object> cir) {
		String relationName = GuildSpellCompat.allyRelationOverrideName(attacker, target);
		if (relationName == null) {
			return;
		}
		try {
			Class<?> relationClass = Class.forName("net.spell_engine.utils.TargetHelper$Relation");
			@SuppressWarnings({"unchecked", "rawtypes"})
			Object value = Enum.valueOf((Class<? extends Enum>) relationClass, relationName);
			cir.setReturnValue(value);
		} catch (ClassNotFoundException ignored) {
			// spell_engine 未加载时不应应用此 Mixin；此处兜底避免崩溃
		}
	}
}
