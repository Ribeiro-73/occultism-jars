package com.fjgoncalves.occultismjars.event;

import com.fjgoncalves.occultismjars.ModComponents;
import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.content.CrusherType;
import com.fjgoncalves.occultismjars.item.CrusherJarItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = OccultismJars.MODID)
public final class CaptureHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof CrusherJarItem) || stack.has(ModComponents.CONTAINED_CRUSHER.get())) {
            return;
        }

        Entity target = event.getTarget();
        CrusherType type = CrusherType.byEntityId(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()));
        if (type == null) {
            return;
        }

        CompoundTag entityData = target.saveWithoutId(new CompoundTag());
        String job = entityData.getCompound("spiritJob").getString("factoryId");
        if (!CrusherType.CRUSHER_JOB.equals(job)) {
            return;
        }

        if (!event.getLevel().isClientSide()) {
            Player player = event.getEntity();

            CompoundTag stored = new CompoundTag();
            stored.putInt("tier", type.tier());
            stored.putString("entity", type.entityId().toString());
            stored.put("data", entityData);

            ItemStack filled = stack.copyWithCount(1);
            filled.set(ModComponents.CONTAINED_CRUSHER.get(), stored);
            stack.shrink(1);
            if (!player.addItem(filled)) {
                player.drop(filled, false);
            }

            target.discard();
            target.playSound(SoundEvents.BOTTLE_FILL, 1.0F, 1.0F);
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
    }
}
