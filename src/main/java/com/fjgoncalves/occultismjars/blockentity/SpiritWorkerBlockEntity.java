package com.fjgoncalves.occultismjars.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.fjgoncalves.occultismjars.Config;
import com.fjgoncalves.occultismjars.content.SpiritJob;
import com.fjgoncalves.occultismjars.menu.SpiritWorkerMenu;
import com.klikli_dev.occultism.Occultism;
import com.klikli_dev.occultism.crafting.recipe.CrushingRecipe;
import com.klikli_dev.occultism.crafting.recipe.CrystallizeRecipe;
import com.klikli_dev.occultism.crafting.recipe.TieredSingleRecipeInput;
import com.klikli_dev.occultism.crafting.recipe.TraderRecipeInput;
import com.klikli_dev.occultism.crafting.recipe.result.WeightedRecipeResult;
import com.klikli_dev.occultism.registry.OccultismItems;
import com.klikli_dev.occultism.registry.OccultismRecipes;
import com.klikli_dev.occultism.registry.OccultismSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

// shared by the jar and the holographic base: inventory, automation, the spirit's job, menu
public abstract class SpiritWorkerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INPUT_SLOT = 0;
    public static final int FIRST_OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 3;

    // ticks a demonic partner takes per item, and its sweet honey heart cooldown (same as Occultism's)
    private static final int PARTNER_TIME = 20;
    private static final long HEART_COOLDOWN = 12000L;

    private int progress;
    private int maxProgress;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? progress : maxProgress;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progress = value;
            } else {
                maxProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    protected final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // what hoppers/pipes see: insert into input, extract from outputs
    private final IItemHandler automationView = new AutomationView();

    // client-only caches for the renderers
    private Entity displayEntity;
    protected boolean displayDirty = true;
    private int spiritVersion;
    private Object renderCache;

    // last recipe lookup, see recipeFor
    private ItemStack cachedInput = ItemStack.EMPTY;
    private Object cachedRecipe;
    private int cachedTier;
    private int cachedVersion = -1;

    protected SpiritWorkerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // full entity nbt (with "id") of the spirit doing the work, null when there is none
    @Nullable
    public abstract CompoundTag getSpiritData();

    @Nullable
    public abstract SpiritJob getJob();

    public abstract int getTier();

    protected abstract Component getBlockName();

    public boolean hasSpirit() {
        return this.getSpiritData() != null;
    }

    public IItemHandler getAutomationView() {
        return this.automationView;
    }

    public IItemHandler getInventory() {
        return this.inventory;
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    protected void stopProgress() {
        this.progress = 0;
        this.maxProgress = 0;
    }

    // bumped whenever the spirit changes, so renderers know to rebuild what they cached
    public int getSpiritVersion() {
        return this.spiritVersion;
    }

    public Object getRenderCache() {
        return this.renderCache;
    }

    public void setRenderCache(Object cache) {
        this.renderCache = cache;
    }

    protected void onSpiritChanged() {
        this.displayDirty = true;
        this.spiritVersion++;
        this.stopProgress();
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    public ItemStack takeInput() {
        ItemStack input = this.inventory.getStackInSlot(INPUT_SLOT);
        this.inventory.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
        return input;
    }

    public void dropInventory() {
        if (this.level == null) {
            return;
        }
        for (int slot = 0; slot < this.inventory.getSlots(); slot++) {
            ItemStack stack = this.inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Block.popResource(this.level, this.worldPosition, stack);
                this.inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        Component name = this.getBlockName();
        CompoundTag data = this.getSpiritData();
        SpiritJob job = this.getJob();
        if (data == null || job == null) {
            return name;
        }
        return Component.translatable("container.occultismjars.spirit_worker", name, job.describe(data.getString("id"), SpiritJob.factoryIdOf(data)));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new SpiritWorkerMenu(id, playerInventory, this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpiritWorkerBlockEntity worker) {
        ItemStack input = worker.inventory.getStackInSlot(INPUT_SLOT);
        Work work = input.isEmpty() ? null : worker.findWork(input);
        if (work == null || work.waiting() || work.result().isEmpty() || !worker.canFitInOutput(work.result())) {
            worker.stopProgress();
            return;
        }

        worker.maxProgress = work.time();
        worker.progress++;
        if (worker.progress % 40 == 0) {
            playWorkSound(level, pos, worker.getJob(), false);
        }

        if (worker.progress >= work.time()) {
            ItemStack produced = work.produce(level.getRandom());
            // a gambler can roll something that doesn't fit; hold at full until there's room
            if (!worker.canFitInOutput(produced)) {
                worker.progress = work.time();
                return;
            }
            worker.progress = 0;
            if (work.onFinish() != null) {
                work.onFinish().run();
            }
            worker.inventory.extractItem(INPUT_SLOT, work.consumed(), false);
            worker.pushToOutput(produced);
            worker.setChanged();
            playWorkSound(level, pos, worker.getJob(), true);
        }
    }

    // one operation: what comes out, how many ticks it takes, how many inputs it eats;
    // traders roll their result from a weighted list when the operation finishes,
    // partners run onFinish (effects on the owner) and can be waiting for the right moment
    private record Work(ItemStack result, int time, int consumed, @Nullable List<WeightedRecipeResult> rolls,
                        boolean waiting, @Nullable Runnable onFinish) {
        Work(ItemStack result, int time, int consumed, @Nullable List<WeightedRecipeResult> rolls) {
            this(result, time, consumed, rolls, false, null);
        }

        ItemStack produce(RandomSource random) {
            if (this.rolls == null) {
                return this.result.copy();
            }
            return WeightedRandom.getRandomItem(random, this.rolls)
                    .map(roll -> roll.getStack().copyWithCount(roll.getStack().getCount() * this.consumed))
                    .orElse(ItemStack.EMPTY);
        }
    }

    // same numbers as the spirit doing the job in the world, read live from occultism-server.toml
    @Nullable
    private Work findWork(ItemStack input) {
        SpiritJob job = this.getJob();
        if (this.level == null || job == null || input.isEmpty()) {
            return null;
        }
        int tier = Mth.clamp(this.getTier(), 1, 4);
        var jobs = Occultism.SERVER_CONFIG.spiritJobs;
        float jarTime = Config.JAR_TIME_MULTIPLIER.get().floatValue();
        var registries = this.level.registryAccess();

        switch (job) {
            case CRUSHER -> {
                var settings = pick(tier, jobs.crusherFoliot, jobs.crusherDjinni, jobs.crusherAfrit, jobs.crusherMarid);
                if (!(this.recipeFor(job, input, settings.tier.get()) instanceof RecipeHolder<?> holder)
                        || !(holder.value() instanceof CrushingRecipe recipe)) {
                    return null;
                }
                float output = recipe.getIgnoreCrushingMultiplier() ? 1.0F : settings.outputMultiplier.get().floatValue();
                return work(recipe.getResultItem(registries), input, settings.operationCount.get(), output,
                        recipe.getCrushingTime() * settings.timeMultiplier.get().floatValue() * jarTime);
            }
            case CRYSTALLIZER -> {
                var settings = pick(tier, jobs.crystallizerFoliot, jobs.crystallizerDjinni, jobs.crystallizerAfrit, jobs.crystallizerMarid);
                if (!(this.recipeFor(job, input, settings.tier.get()) instanceof RecipeHolder<?> holder)
                        || !(holder.value() instanceof CrystallizeRecipe recipe)) {
                    return null;
                }
                float output = recipe.getIgnoreCrystallizeMultiplier() ? 1.0F : settings.outputMultiplier.get().floatValue();
                return work(recipe.getResultItem(registries), input, settings.operationCount.get(), output,
                        recipe.getCrystallizeTime() * settings.timeMultiplier.get().floatValue() * jarTime);
            }
            case SMELTER -> {
                var settings = pick(tier, jobs.smelterFoliot, jobs.smelterDjinni, jobs.smelterAfrit, jobs.smelterMarid);
                if (!(this.recipeFor(job, input, 0) instanceof RecipeHolder<?> holder)
                        || !(holder.value() instanceof AbstractCookingRecipe recipe)) {
                    return null;
                }
                return work(recipe.getResultItem(registries), input, settings.operationCount.get(), 1.0F,
                        recipe.getCookingTime() * settings.timeMultiplier.get().floatValue() * jarTime);
            }
            case TRADER -> {
                var settings = switch (this.getFactoryId()) {
                    case "occultism:trader_otherrock" -> jobs.traderOtherrock;
                    case "occultism:trader_otherworld_saplings" -> jobs.traderSapling;
                    case "occultism:gambler" -> jobs.traderGem;
                    default -> jobs.traderOtherstone;
                };
                if (!(this.recipeFor(job, input, 0) instanceof List<?> found) || found.isEmpty()) {
                    return null;
                }
                @SuppressWarnings("unchecked")
                List<WeightedRecipeResult> rolls = (List<WeightedRecipeResult>) found;
                int operations = Math.min(settings.operationCount.get(), input.getCount());
                ItemStack preview = rolls.get(0).getStack();
                return new Work(preview.copyWithCount(preview.getCount() * operations),
                        Math.max(1, Mth.ceil(settings.operationTimer.get() * jarTime)), operations, rolls);
            }
            case PARTNER -> {
                return this.partnerWork(input);
            }
        }
        return null;
    }

    // a demonic wife/husband does for its owner what it would do if handed the item
    @Nullable
    private Work partnerWork(ItemStack input) {
        CompoundTag data = this.getSpiritData();
        if (data == null || this.level == null) {
            return null;
        }

        // potions and suspicious stew: the owner gets the effects for much longer
        boolean stew = input.has(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (stew || input.has(DataComponents.POTION_CONTENTS)) {
            List<MobEffectInstance> effects = partnerEffects(input, stew);
            if (effects.isEmpty()) {
                return null;
            }
            ItemStack container = input.getCraftingRemainingItem();
            if (container.isEmpty()) {
                container = new ItemStack(stew ? Items.BOWL : Items.GLASS_BOTTLE);
            }
            ServerPlayer owner = this.partnerOwner(data);
            // keep the effect up without burning potions: wait until it's about to run out
            boolean waiting = owner == null || effects.stream().allMatch(effect -> stillActive(owner, effect));
            return new Work(container, PARTNER_TIME, 1, null, waiting, () -> {
                ServerPlayer target = this.partnerOwner(data);
                if (target == null) {
                    return;
                }
                effects.forEach(effect -> target.addEffect(new MobEffectInstance(effect)));
                if (stew) {
                    target.getFoodData().eat(Foods.SUSPICIOUS_STEW);
                    target.getFoodData().eat(Foods.SUSPICIOUS_STEW);
                }
            });
        }

        // cursed honey becomes a sweet honey heart, with the partner's own cooldown
        if (input.is(OccultismItems.CURSED_HONEY.get())) {
            long now = this.level.getGameTime();
            boolean waiting = data.getLong("heartLastTime") + HEART_COOLDOWN > now;
            return new Work(new ItemStack(OccultismItems.SWEET_HONEY_HEART.get()), PARTNER_TIME, 1, null, waiting,
                    () -> this.updateSpiritData(tag -> tag.putLong("heartLastTime", now)));
        }

        // raw food gets cooked, like the smoker
        if (this.recipeFor(SpiritJob.PARTNER, input, 0) instanceof RecipeHolder<?> holder
                && holder.value() instanceof SmokingRecipe recipe) {
            return new Work(recipe.getResultItem(this.level.registryAccess()).copy(), PARTNER_TIME, 1, null);
        }
        return null;
    }

    // the partner's multiplied effects; harmful and instant ones are left out, so nobody can poison
    // or hurt the owner from afar by dropping a potion into the base
    private static List<MobEffectInstance> partnerEffects(ItemStack input, boolean stew) {
        List<MobEffectInstance> effects = new ArrayList<>();
        if (stew) {
            for (SuspiciousStewEffects.Entry entry : input.getOrDefault(DataComponents.SUSPICIOUS_STEW_EFFECTS,
                    SuspiciousStewEffects.EMPTY).effects()) {
                effects.add(new MobEffectInstance(entry.effect(), entry.duration() * 50, 0, false, false));
            }
        } else {
            for (MobEffectInstance effect : input.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                    .getAllEffects()) {
                effects.add(new MobEffectInstance(effect.getEffect(), effect.getDuration() * 5, effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible()));
            }
        }
        effects.removeIf(effect -> effect.getEffect().value().isInstantenous()
                || effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL);
        return effects;
    }

    private static boolean stillActive(ServerPlayer owner, MobEffectInstance wanted) {
        MobEffectInstance current = owner.getEffect(wanted.getEffect());
        return current != null && current.getAmplifier() >= wanted.getAmplifier()
                && (current.isInfiniteDuration() || current.getDuration() > Config.POTION_REFRESH_SECONDS.get() * 20);
    }

    // the owner, wherever they are on the server, or null when offline
    @Nullable
    private ServerPlayer partnerOwner(CompoundTag data) {
        if (this.level == null || this.level.getServer() == null || !data.hasUUID("Owner")) {
            return null;
        }
        return this.level.getServer().getPlayerList().getPlayer(data.getUUID("Owner"));
    }

    // lets a job change the stored spirit (only the base can, its spirit lives in a gem)
    protected void updateSpiritData(Consumer<CompoundTag> edit) {
    }

    private String getFactoryId() {
        CompoundTag data = this.getSpiritData();
        return data == null ? "" : SpiritJob.factoryIdOf(data);
    }

    private static Work work(ItemStack recipeResult, ItemStack input, int operationCount, float outputMultiplier, float time) {
        int operations = Math.min(operationCount, input.getCount());
        ItemStack result = recipeResult.copy();
        result.setCount(Mth.floor(result.getCount() * operations * outputMultiplier));
        return new Work(result, Math.max(1, Mth.ceil(time)), operations, null);
    }

    private static <T> T pick(int tier, T foliot, T djinni, T afrit, T marid) {
        return switch (tier) {
            case 2 -> djinni;
            case 3 -> afrit;
            case 4 -> marid;
            default -> foliot;
        };
    }

    // the last lookup is remembered, so a stack of the same item doesn't search the recipes every tick;
    // a RecipeHolder, or for traders the list of weighted results
    @Nullable
    private Object recipeFor(SpiritJob job, ItemStack input, int recipeTier) {
        if (this.cachedVersion == this.spiritVersion && this.cachedTier == recipeTier
                && ItemStack.isSameItemSameComponents(this.cachedInput, input)) {
            return this.cachedRecipe;
        }
        Object found = this.lookupRecipe(job, input, recipeTier);
        this.cachedVersion = this.spiritVersion;
        this.cachedTier = recipeTier;
        this.cachedInput = input.copyWithCount(1);
        this.cachedRecipe = found;
        return found;
    }

    @Nullable
    private Object lookupRecipe(SpiritJob job, ItemStack input, int recipeTier) {
        RecipeManager recipes = this.level.getRecipeManager();
        return switch (job) {
            // every trade this trader has for the item, like the spirit does
            case TRADER -> recipes.getRecipesFor(OccultismRecipes.SPIRIT_TRADE_TYPE.get(),
                            new TraderRecipeInput(input, this.getFactoryId()), this.level).stream()
                    .map(holder -> holder.value().getWeightedResult())
                    .toList();
            case CRUSHER -> recipes.getRecipeFor(OccultismRecipes.CRUSHING_TYPE.get(),
                    new TieredSingleRecipeInput(input, recipeTier), this.level).orElse(null);
            case CRYSTALLIZER -> recipes.getRecipeFor(OccultismRecipes.CRYSTALLIZE_TYPE.get(),
                    new TieredSingleRecipeInput(input, recipeTier), this.level).orElse(null);
            // furnace first, then the other cookers, like the smelter spirit
            case SMELTER -> {
                SingleRecipeInput single = new SingleRecipeInput(input);
                RecipeHolder<?> found = recipes.getRecipeFor(RecipeType.SMELTING, single, this.level).orElse(null);
                if (found == null) {
                    found = recipes.getRecipeFor(RecipeType.BLASTING, single, this.level).orElse(null);
                }
                if (found == null) {
                    found = recipes.getRecipeFor(RecipeType.SMOKING, single, this.level).orElse(null);
                }
                if (found == null) {
                    found = recipes.getRecipeFor(RecipeType.CAMPFIRE_COOKING, single, this.level).orElse(null);
                }
                yield found;
            }
            // the partner cooks food with smoker recipes
            case PARTNER -> recipes.getRecipeFor(RecipeType.SMOKING, new SingleRecipeInput(input), this.level).orElse(null);
        };
    }

    private static void playWorkSound(Level level, BlockPos pos, @Nullable SpiritJob job, boolean finished) {
        // traders and partners only sound off when they finish, repeating it would get old fast
        if (job == null || !Config.PLAY_CRUSHING_SOUND.get()
                || ((job == SpiritJob.TRADER || job == SpiritJob.PARTNER) && !finished)) {
            return;
        }
        SoundEvent sound = switch (job) {
            case CRUSHER -> OccultismSounds.CRUNCHING.get();
            case SMELTER -> SoundEvents.FIRE_AMBIENT;
            case CRYSTALLIZER -> SoundEvents.AMETHYST_CLUSTER_STEP;
            case TRADER -> OccultismSounds.START_RITUAL.get();
            case PARTNER -> SoundEvents.BREWING_STAND_BREW;
        };
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F + 0.5F * level.random.nextFloat());
    }

    private boolean canFitInOutput(ItemStack stack) {
        for (int slot = FIRST_OUTPUT_SLOT; slot < SLOT_COUNT; slot++) {
            ItemStack remainder = this.inventory.insertItem(slot, stack, true);
            if (remainder.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void pushToOutput(ItemStack stack) {
        for (int slot = FIRST_OUTPUT_SLOT; slot < SLOT_COUNT && !stack.isEmpty(); slot++) {
            stack = this.inventory.insertItem(slot, stack, false);
        }
    }

    // can the spirit inside do its job on this item at its tier
    public boolean canProcess(ItemStack stack) {
        Work work = this.findWork(stack);
        return work != null && !work.result().isEmpty();
    }

    @Nullable
    public Entity getDisplayEntity() {
        CompoundTag data = this.getSpiritData();
        if (data == null) {
            this.displayEntity = null;
            return null;
        }
        if (this.level == null || !this.level.isClientSide || !Config.RENDER_TRAPPED_SPIRIT.get()) {
            return null;
        }
        if (this.displayEntity == null || this.displayDirty) {
            this.displayDirty = false;
            try {
                this.displayEntity = EntityType.create(data, this.level).orElse(null);
            } catch (Exception e) {
                this.displayEntity = null;
            }
            if (this.displayEntity != null) {
                this.displayEntity.setNoGravity(true);
                this.displayEntity.setCustomName(null);
                BlockState state = this.getBlockState();
                float yaw = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                        ? state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() : 0.0F;
                this.displayEntity.setYRot(yaw);
                if (this.displayEntity instanceof LivingEntity living) {
                    living.yBodyRot = yaw;
                    living.yBodyRotO = yaw;
                    living.yHeadRot = yaw;
                    living.yHeadRotO = yaw;
                }
                this.displayEntity.setPos(this.worldPosition.getX() + 0.5, this.worldPosition.getY(), this.worldPosition.getZ() + 0.5);
            }
        }
        return this.displayEntity;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.progress = tag.getInt("progress");
        if (tag.contains("inventory")) {
            this.inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        this.displayDirty = true;
        this.spiritVersion++;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // empty update packets get dropped, so always write something
        tag.putBoolean("hasSpirit", this.hasSpirit());
        tag.putInt("progress", this.progress);
        tag.put("inventory", this.inventory.serializeNBT(registries));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private final class AutomationView implements IItemHandler {
        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != INPUT_SLOT || !canProcess(stack)) {
                return stack;
            }
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == INPUT_SLOT ? ItemStack.EMPTY : inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT_SLOT && canProcess(stack);
        }
    }
}
