package com.fjgoncalves.occultismjars.event;

import com.fjgoncalves.occultismjars.ModComponents;
import com.fjgoncalves.occultismjars.OccultismJars;
import com.fjgoncalves.occultismjars.content.SpiritJob;
import com.fjgoncalves.occultismjars.content.SpiritType;
import com.fjgoncalves.occultismjars.item.SpiritJarItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
        if (!(stack.getItem() instanceof SpiritJarItem) || stack.has(ModComponents.CONTAINED_SPIRIT.get())) {
            return false;
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        SpiritType type = SpiritType.byEntityId(entityId);
        if (type == null) {
            return false;
        }

        // job nbt is server-only; let the click through so the packet still reaches the server
        if (player.level().isClientSide()) {
            return false;
        }

        CompoundTag probe = new CompoundTag();
        if (!target.save(probe)) {
            return false;
        }
        String factoryId = probe.getCompound("spiritJob").getString("factoryId");
        SpiritJob job = SpiritJob.byFactoryId(factoryId);
        if (job == null) {
            return false;
        }
        int tier = job.tierOf(factoryId);
        if (tier < 1) {
            return false;
        }
        if (tier > job.jarMaxTier()) {
            String message = job.jarMaxTier() == 0 ? "message.occultismjars.base_only" : "message.occultismjars.too_strong";
            player.displayClientMessage(Component.translatable(message), true);
            return true;
        }

        // pull the in-progress item off the entity so it becomes the jar's input (given back on release)
        ItemStack held = ItemStack.EMPTY;
        if (target instanceof LivingEntity living) {
            held = living.getMainHandItem().copy();
            if (!held.isEmpty()) {
                living.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
        }

        CompoundTag entityData = new CompoundTag();
        if (!target.save(entityData)) {
            return false;
        }

        CompoundTag stored = new CompoundTag();
        stored.putInt("tier", tier);
        stored.putString("job", job.getName());
        stored.putString("entity", entityId.toString());
        stored.put("data", entityData);
        if (!held.isEmpty()) {
            stored.put("heldItem", held.save(player.level().registryAccess()));
        }

        ItemStack filled = stack.copyWithCount(1);
        filled.set(ModComponents.CONTAINED_SPIRIT.get(), stored);
        stack.shrink(1);
        if (!player.addItem(filled)) {
            player.drop(filled, false);
        }

        target.discard();
        target.playSound(SoundEvents.BOTTLE_FILL, 1.0F, 1.0F);
        return true;
    }
}
