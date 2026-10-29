import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlockWaveEffect implements ModInitializer {

    private static final List<ActiveWave> activeWaves = new ArrayList<>();

    @Override
    public void onInitialize() {
        // Ловим удар игрока
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                BlockPos centerPos = target.blockPosition();
                // Запускаем новую волну от позиции цели
                activeWaves.add(new ActiveWave(serverLevel, centerPos));
            }
            return InteractionResult.PASS;
        });

        // Двигаем волну каждый игровой тик для плавности
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
        private double currentRadius = 0.5; // Начинаем из центра
        private final double maxRadius = 6.0;   // Радиус до 6 блоков
        private int tickDelay = 0;

        public ActiveWave(ServerLevel level, BlockPos center) {
            this.level = level;
            this.center = center;
        }

        public boolean tick() {
            // Задержка в 1 тик для максимальной плавности движения волны
            if (tickDelay > 0) {
                tickDelay--;
                return true;
            }
            tickDelay = 1; 

            // Рисуем сплошное плотное кольцо на текущем радиусе
            spawnWaveRing(level, center, currentRadius);

            // Увеличиваем радиус по чуть-чуть, чтобы волна шла плавно
            currentRadius += 0.4;
            
            // Останавливаемся, когда дошли до 6 блоков
            return currentRadius <= maxRadius;
        }

        private void spawnWaveRing(ServerLevel serverLevel, BlockPos center, double r) {
            if (r < 0.2) return;

            // Количество точек зависит от радиуса, чтобы линия была сплошной без просветов
            int points = (int) (r * 30);
            
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = center.getX() + 0.5 + Math.cos(angle) * r;
                double z = center.getZ() + 0.5 + Math.sin(angle) * r;
                
                BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos((int) x, center.getY(), (int) z);
                
                // Аккуратно находим поверхность блока земли под точкой
                while (mutablePos.getY() > center.getY() - 3 && serverLevel.isEmptyBlock(mutablePos.below())) {
                    mutablePos.move(0, -1, 0);
                }

                // Спавним белую яркую частицу ровно по поверхности блоков
                serverLevel.sendParticles(
                    ParticleTypes.FIREWORK, // Дает красивую плотную линию из искр
                    x, mutablePos.getY() + 0.1, z, 
                    1, 0.0, 0.0, 0.0, 0.0
                );
            }
        }
    }
}
