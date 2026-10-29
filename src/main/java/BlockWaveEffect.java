import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlockWaveEffect implements ModInitializer {

    private static final List<ActiveWave> activeWaves = new ArrayList<>();

    @Override
    public void onInitialize() {
        // Перехватываем удар игрока
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                BlockPos centerPos = target.blockPosition();
                // Запускаем новую волну
                activeWaves.add(new ActiveWave(serverLevel, centerPos));
            }
            return InteractionResult.PASS;
        });

        // Обрабатываем движение волны каждый игровой тик
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<ActiveWave> iterator = activeWaves.iterator();
            while (iterator.hasNext()) {
                ActiveWave wave = iterator.next();
                if (!wave.tick()) {
                    iterator.remove(); // Удаляем волну, когда она дошла до конца
                }
            }
        });
    }

    private static class ActiveWave {
        private final ServerLevel level;
        private final BlockPos center;
        private double currentRadius = 1.0;
        private final double maxRadius = 6.0;
        private int tickDelay = 0;

        public ActiveWave(ServerLevel level, BlockPos center) {
            this.level = level;
            this.center = center;
        }

        public boolean tick() {
            // Задержка в 3 тика между шагами, чтобы волна шла плавно и небыстро
            if (tickDelay > 0) {
                tickDelay--;
                return true;
            }
            tickDelay = 3; 

            // Рисуем сплошное белое кольцо на текущем радиусе
            spawnSolidRing(level, center, currentRadius);

            // Увеличиваем радиус для следующего шага
            currentRadius += 0.8;
            
            // Работаем, пока не достигнем максимального радиуса (6 блоков)
            return currentRadius <= maxRadius;
        }

        private void spawnSolidRing(ServerLevel serverLevel, BlockPos center, double r) {
            // Делаем очень много точек на окружности, чтобы они сливались в сплошную линию
            int points = (int) (r * 24); 
            
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = center.getX() + 0.5 + Math.cos(angle) * r;
                double z = center.getZ() + 0.5 + Math.sin(angle) * r;
                
                BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos((int) x, center.getY(), (int) z);
                
                // Находим точную высоту блока земли под этой точкой
                while (mutablePos.getY() > center.getY() - 3 && serverLevel.isEmptyBlock(mutablePos.below())) {
                    mutablePos.move(0, -1, 0);
                }

                // Используем END_ROD — она дает яркий, красивый белый шлейф в виде линии
                serverLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    x, mutablePos.getY() + 0.1, z, 
                    1, 0.0, 0.0, 0.0, 0.0
                );
            }
        }
    }
}
