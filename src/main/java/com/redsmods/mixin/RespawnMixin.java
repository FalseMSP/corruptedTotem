package com.redsmods.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class RespawnMixin {

    @Inject(
            method = "restoreFrom",
            at = @At("TAIL")
    )
    private void onRespawn(ServerPlayer oldPlayer,
                           boolean alive,
                           CallbackInfo ci) {

        ServerPlayer self = (ServerPlayer)(Object)this;

        AttributeInstance attr = self.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;

        // reset to vanilla default
        attr.setBaseValue(20.0);

        // ensure health is clamped
        self.setHealth(20.0f);
    }
}