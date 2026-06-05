package com.redsmods.mixin;

import com.redsmods.network.TotemPopPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

	private static final Logger LOGGER = LoggerFactory.getLogger("CorruptedTotem");

	@Inject(
			method = "checkTotemDeathProtection",
			at = @At("RETURN")
	)
	private void onTotemPop(DamageSource source,
	                        CallbackInfoReturnable<Boolean> cir) {

		if (!cir.getReturnValue()) return;

		if (!((Object) this instanceof ServerPlayer player)) return;

		// ── existing max-health reduction ────────────────────────────────
		AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
		if (attr == null) return;

		double newMax = attr.getBaseValue() - 2.0;
		attr.setBaseValue(Math.max(1.0, newMax));

		if (player.getHealth() > newMax) {
			player.setHealth((float) newMax);
		}

		boolean canSend = ServerPlayNetworking.canSend(player, TotemPopPayload.TYPE);

		if (canSend) {
			ServerPlayNetworking.send(player, new TotemPopPayload());
		}
	}
}