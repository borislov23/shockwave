import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlockWaveClient implements ClientModInitializer {
    private static final List<ClientWave> waves = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (level.isClientSide() && player == Minecraft.getInstance().player) {
                waves.add(new ClientWave(target.position()));
            }
            return InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Iterator<ClientWave> it = waves.iterator();
            while (it.hasNext()) {
                ClientWave wave = it.next();
                if (!wave.tick()) {
                    it.remove();
                }
            }
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            if (waves.isEmpty()) return;

            PoseStack poseStack = context.matrixStack();
            Vec3 cameraPos = context.camera().getPosition();
            VertexConsumer buffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines());

            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            for (ClientWave wave : waves) {
                wave.render(poseStack, buffer);
            }

            poseStack.popPose();
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch(RenderType.lines());
        });
    }

    private static class ClientWave {
        private final Vec3 center;
        private float radius = 0.2f;
        private final float maxRadius = 7.0f; // Максимум 7 блоков

        public ClientWave(Vec3 center) {
            this.center = center;
        }

        public boolean tick() {
            // Пролетает 7 блоков ровно за 1 секунду (20 тиков)
            radius += 0.35f; 
            return radius <= maxRadius;
        }

        public void render(PoseStack poseStack, VertexConsumer buffer) {
            int segments = 60;
            float y = (float) center.y + 0.04f;

            for (int i = 0; i < segments; i++) {
                double angle1 = 2 * Math.PI * i / segments;
                double angle2 = 2 * Math.PI * (i + 1) / segments;

                float x1 = (float) (center.x + Math.cos(angle1) * radius);
                float z1 = (float) (center.z + Math.sin(angle1) * radius);
                float x2 = (float) (center.x + Math.cos(angle2) * radius);
                float z2 = (float) (center.z + Math.sin(angle2) * radius);

                float alpha = 1.0f - (radius / maxRadius);

                buffer.addVertex(poseStack.last().pose(), x1, y, z1)
                      .setColor(1.0f, 1.0f, 1.0f, alpha)
                      .setNormal(0.0f, 1.0f, 0.0f);

                buffer.addVertex(poseStack.last().pose(), x2, y, z2)
                      .setColor(1.0f, 1.0f, 1.0f, alpha)
                      .setNormal(0.0f, 1.0f, 0.0f);
            }
        }
    }
}
