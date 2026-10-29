import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class BlockWaveEffect implements ModInitializer {

    @Override
    public void onInitialize() {
        // Подписываемся на событие атаки сущности в Fabric API
        AttackEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                BlockPos centerPos = target.blockPosition();
                spawnWaveOnGround(serverLevel, centerPos);
            }
            return InteractionResult.PASS;
        });
    }

    private static void spawnWaveOnGround(ServerLevel serverLevel, BlockPos center) {
        int maxRadius = 6;

        for (double r = 1.0; r <= maxRadius; r += 0.5) {
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
