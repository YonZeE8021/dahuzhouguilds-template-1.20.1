package com.dahuzhou.dahuzhouguilds.mixin;

import com.dahuzhou.dahuzhouguilds.util.DelayedTeleportScheduler;
import com.dahuzhou.dahuzhouguilds.util.GuildBankScreenHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {
	@Inject(method = "damage", at = @At("RETURN"))
	private void dahuzhouguilds$onDamageReturn(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (!Boolean.TRUE.equals(cir.getReturnValue()) || amount <= 0.0f) {
			return;
		}
		if (!((Object) this instanceof ServerPlayerEntity player)) {
			return;
		}
		DelayedTeleportScheduler.handleDamage(player, source, amount);
		if (player.currentScreenHandler instanceof GuildBankScreenHandler) {
			player.closeHandledScreen();
			player.sendMessage(Text.literal("Your guild bank was closed because you took damage!"), false);
		}
	}
}
