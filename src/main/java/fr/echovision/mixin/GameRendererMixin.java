package fr.echovision.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.echovision.client.EchoVisionState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    // Masquage du framebuffer avant l'overlay.
    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void echovision$eraseWorld(RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || !EchoVisionState.isEnabled()) return;
        client.getFramebuffer().beginWrite(false);
        RenderSystem.clearColor(2F/255F, 4F/255F, 10F/255F, 1.0F);
        RenderSystem.clear(0x00004000, false);
    }
}
