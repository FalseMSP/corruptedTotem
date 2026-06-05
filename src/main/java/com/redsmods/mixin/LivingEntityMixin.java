package com.redsmods.mixin;

import com.redsmods.CorruptedTotem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

	@Inject(
			method = "checkTotemDeathProtection",
			at = @At("RETURN")
	)
	private void onTotemPop(DamageSource source,
	                        CallbackInfoReturnable<Boolean> cir) {

		if (!cir.getReturnValue()) return;

		if (!((Object)this instanceof ServerPlayer player)) return;

		AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
		if (attr == null) return;

		double newMax = attr.getBaseValue() - 2.0; // 2.0 = 1 heart

		attr.setBaseValue(Math.max(1.0, newMax));

		// IMPORTANT: clamp current health so it updates visually
		if (player.getHealth() > newMax) {
			player.setHealth((float)newMax);
		}
	}
}