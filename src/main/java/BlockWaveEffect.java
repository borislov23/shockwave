import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlockWaveEffect implements ModInitializer {

    // Список активных волн, которые расширяются со временем
    private static final List<ActiveWave> activeWaves = new ArrayList<>();

    @Override
    public void onInitialize() {
        // Подписываемся на событие атаки
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                BlockPos centerPos = target.blockPosition();
                // Добавляем новую волну в список для анимации
                activeWaves.add(new ActiveWave(serverLevel, centerPos));
            }
            return InteractionResult.PASS;
        });

        // Каждые 20 раз в секунду (каждый тик) обновляем все активные волны
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

    // Класс для управления отдельной расходящейся волной
    private static class ActiveWave {
        private final ServerLevel level;
        private final BlockPos center;
        private int currentRadius = 1;
        private final int maxRadius = 6;
        private int tickDelay = 0;

        public ActiveWave(ServerLevel level, BlockPos center) {
            this.level = level;
            this.center = center;
        }

        public boolean tick() {
            // Задержка в 2 тика между расширениями кольца (чтобы волна шла плавно)
            if (tickDelay > 0) {
                tickDelay--;
                return true;
            }
            tickDelay = 2; 

            // Спавним текущее кольцо волны
            spawnCircle(level, center, currentRadius);

            currentRadius++;
            // Возвращаем true, пока не дошли до максимального радиуса
            return currentRadius <= maxRadius;
        }

        private void spawnCircle(ServerLevel serverLevel, BlockPos center, double r) {
            int points = (int) (r * 8);
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = center.getX() + 0.5 + Math.cos(angle) * r;
                double z = center.getZ() + 0.5 + Math.sin(angle) * r;
                
                BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos((int) x, center.getY(), (int) z);
                
                while (mutablePos.getY() > center.getY() - 3 && serverLevel.isEmptyBlock(mutablePos.below())) {
                    mutablePos.move(0, -1, 0);
                }

                serverLevel.sendParticles(
                    ParticleTypes.FLASH,
                    x, mutablePos.getY() + 0.1, z, 
                    1, 0.0, 0.0, 0.0, 0.0
                );
            }
        }
    }
}
