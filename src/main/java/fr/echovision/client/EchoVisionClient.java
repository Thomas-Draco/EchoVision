package fr.echovision.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;

public final class EchoVisionClient implements ClientModInitializer {
    private static SonicEdgeRenderer renderer;
    private SoundManager registeredSoundManager;

    @Override
    public void onInitializeClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        EchoSystem echoes = new EchoSystem(client);
        EchoLattice lattice = new EchoLattice(client);
        SourceEchoLattice sourceLattice = new SourceEchoLattice(client);
        EchoProjector projector = new EchoProjector();
        EntityEchoLattice entityLattice = new EntityEchoLattice(client);
        renderer = new SonicEdgeRenderer(echoes, lattice, sourceLattice, entityLattice, projector, client);

        // Commande locale, non transmise au serveur.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("echovision")
                        .then(ClientCommandManager.literal("on").executes(context -> {
                            EchoVisionState.setEnabled(true);
                            context.getSource().sendFeedback(Text.translatable("message.echovision.on"));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("off").executes(context -> {
                            EchoVisionState.setEnabled(false);
                            context.getSource().sendFeedback(Text.translatable("message.echovision.off"));
                            return 1;
                        }))));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, disconnectingClient) ->
                EchoVisionState.setEnabled(false));

        // Rechargement des shaders Fabric.
        CoreShaderRegistrationCallback.EVENT.register(registration ->
                registration.register(Identifier.of("echovision", "sonic_edge"),
                        VertexFormats.POSITION_TEXTURE_COLOR, SonicEdgeRenderer::setShader));
        WorldRenderEvents.LAST.register(projector::capture);
        // Initialisation audio différée.
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            SoundManager manager = c.getSoundManager();
            if (manager != null && manager != registeredSoundManager) {
                manager.registerListener(echoes);
                registeredSoundManager = manager;
            }
            echoes.tick();
            lattice.tick(echoes.isActive());
            sourceLattice.tick(echoes.isActive());
            if (echoes.isActive()) sourceLattice.prewarmOne(echoes.snapshot());
            entityLattice.tick(echoes.isActive(), echoes.snapshot());
        });
    }

    public static boolean isHearingEnhanced() { return EchoVisionState.isEnabled(); }

    public static void renderUnderHud(DrawContext context) {
        if (renderer != null) renderer.render(context);
    }
}
