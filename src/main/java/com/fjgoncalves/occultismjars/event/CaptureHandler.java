package com.fjgoncalves.occultismjars.event;

import com.fjgoncalves.occultismjars.ModComponents;
import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.content.CrusherType;
import com.fjgoncalves.occultismjars.item.CrusherJarItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = OccultismJars.MODID)
public final class CaptureHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (tryCapture(event.getEntity(), event.getItemStack(), event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (tryCapture(event.getEntity(), event.getItemStack(), event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        }
    }

    private static boolean tryCapture(Player player, ItemStack stack, Entity target) {
        if (!(stack.getItem() instanceof CrusherJarItem) || stack.has(ModComponents.CONTAINED_CRUSHER.get())) {
            return false;
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        CrusherType type = CrusherType.byEntityId(entityId);
        if (type == null) {
            return false;
        }

        // The spirit's job NBT only exists on the server. Let the click through on the
        // client so the interaction packet still reaches the server.
        if (player.level().isClientSide()) {
            return false;
        }

        CompoundTag entityData = target.saveWithoutId(new CompoundTag());
        String job = entityData.getCompound("spiritJob").getString("factoryId");
        if (!job.startsWith(CrusherType.CRUSHER_JOB_PREFIX)) {
            return false;
        }

        CompoundTag stored = new CompoundTag();
        stored.putInt("tier", type.tier());
        stored.putString("entity", entityId.toString());
        stored.put("data", entityData);

        ItemStack filled = stack.copyWithCount(1);
        filled.set(ModComponents.CONTAINED_CRUSHER.get(), stored);
        stack.shrink(1);
        if (!player.addItem(filled)) {
            player.drop(filled, false);
        }

        target.discard();
        target.playSound(SoundEvents.BOTTLE_FILL, 1.0F, 1.0F);
        return true;
    }
}
