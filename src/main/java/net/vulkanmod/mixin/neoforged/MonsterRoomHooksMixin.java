package net.vulkanmod.mixin.neoforged;

import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weight;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.MonsterRoomHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * &#064;Author: KSmc_brigade
 * &#064;Date: 2025/9/20 下午7:07
 */
@Mixin(MonsterRoomHooks.class)
public abstract class MonsterRoomHooksMixin {
    @Shadow private static List<MonsterRoomHooks.MobEntry> monsterRoomMobs;

    @Inject(method = "getRandomMonsterRoomMob",at = @At("HEAD"))
    private static void get(RandomSource rand, CallbackInfoReturnable<EntityType<?>> cir){
        if(!monsterRoomMobs.isEmpty()) return;
        monsterRoomMobs = new ArrayList<>();
        for (Field declaredField : EntityType.class.getDeclaredFields()) {
            try {
                if(EntityType.class.isAssignableFrom(declaredField.getType())){
                    declaredField.setAccessible(true);
                    monsterRoomMobs.add(new MonsterRoomHooks.MobEntry((EntityType<?>) declaredField.get(null), Weight.of(rand.nextInt(1,10))));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
