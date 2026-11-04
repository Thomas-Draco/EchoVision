package fr.echovision.mixin;

import fr.echovision.client.EchoVisionClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
    // Sons déjà reçus uniquement ; volumes muets inchangés.
    @Inject(method = "getAdjustedVolume(Lnet/minecraft/client/sound/SoundInstance;)F",
            at = @At("RETURN"), cancellable = true)
    private void echovision$boostVolume(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        if (!EchoVisionClient.isHearingEnhanced()) return;
        SoundCategory category = sound.getCategory();
        if (category == SoundCategory.MUSIC || category == SoundCategory.RECORDS
                || category == SoundCategory.MASTER || category == SoundCategory.VOICE) return;
        float original = cir.getReturnValue();
        if (original <= 0.0F) return;
        float boost = category == SoundCategory.HOSTILE || category == SoundCategory.PLAYERS ? 1.70F : 1.43F;
        cir.setReturnValue(Math.min(1.0F, original * boost));
    }
}
