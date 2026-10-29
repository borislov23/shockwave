import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.VertexConsumer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import com.mojang.bl3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlockWaveClient implements ClientModInitializer {
    private static final List<ClientWave> waves = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        // Перехватываем удар игрока на клиенте
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (level.isClientSide() && player == Minecraft.getInstance().player) {
                waves.add(new ClientWave(target.position()));
            }
            return InteractionResult.PASS;
        });

        // Двигаем анимацию волны каждый игровой тик
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Iterator<ClientWave> it = waves.iterator();
            while (it.hasNext()) {
                ClientWave wave = it.next();
                if (!wave.tick()) {
                    it.remove();
                }
            }
        });

        // Отрисовываем гладкие векторные линии поверх мира игры
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
        private final float maxRadius = 6.0f; // Радиус расширения до 6 блоков

        public ClientWave(Vec3 center) {
            this.center = center;
        }

        public boolean tick() {
            radius += 0.18f; // Скорость расширения волны
            return radius <= maxRadius;
        }

        public void render(PoseStack poseStack, VertexConsumer buffer) {
            int segments = 48; // Плавность окружности
            float y = (float) center.y + 0.04f; // Чуть-чуть выше блоков, чтобы не было мерцания

            for (int i = 0; i < segments; i++) {
                double angle1 = 2 * Math.PI * i / segments;
                double angle2 = 2 * Math.PI * (i + 1) / segments;

                float x1 = (float) (center.x + Math.cos(angle1) * radius);
                float z1 = (float) (center.z + Math.sin(angle1) * radius);
                float x2 = (float) (center.x + Math.cos(angle2) * radius);
                float z2 = (float) (center.z + Math.sin(angle2) * radius);

                // Плавное затухание прозрачности по краям волны
                float alpha = 1.0f - (radius / maxRadius);

                buffer.vertex(poseStack.last().pose(), x1, y, z1)
                      .color(1.0f, 1.0f, 1.0f, alpha)
                      .normal(0.0f, 1.0f, 0.0f)
                      .endVertex();

                buffer.vertex(poseStack.last().pose(), x2, y, z2)
                      .color(1.0f, 1.0f, 1.0f, alpha)
                      .normal(0.0f, 1.0f, 0.0f)
                      .endVertex();
            }
        }
    }
}
