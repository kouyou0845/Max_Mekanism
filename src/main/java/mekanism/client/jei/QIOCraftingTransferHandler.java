package mekanism.client.jei;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteArraySet;
import it.unimi.dsi.fastutil.bytes.ByteIterator;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.bytes.ByteSet;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.math.MathUtils;
import mekanism.common.Mekanism;
import mekanism.common.MekanismLang;
import mekanism.common.content.qio.QIOCraftingTransferHelper;
import mekanism.common.content.qio.QIOCraftingTransferHelper.BaseSimulatedInventory;
import mekanism.common.content.qio.QIOCraftingTransferHelper.HashedItemSource;
import mekanism.common.content.qio.QIOCraftingTransferHelper.SingularHashedItemSource;
import mekanism.common.content.qio.QIOCraftingWindow;
import mekanism.common.content.qio.QIOFrequency;
import mekanism.common.inventory.container.QIOItemViewerContainer;
import mekanism.common.inventory.container.slot.HotBarSlot;
import mekanism.common.inventory.container.slot.MainInventorySlot;
import mekanism.common.inventory.slot.CraftingWindowInventorySlot;
import mekanism.common.lib.inventory.HashedItem;
import mekanism.common.network.to_server.PacketQIOFillCraftingWindow;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.StackUtils;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

public class QIOCraftingTransferHandler<CONTAINER extends QIOItemViewerContainer> implements IRecipeTransferHandler<CONTAINER, CraftingRecipe> {

    private final IRecipeTransferHandlerHelper handlerHelper;
    private final Class<CONTAINER> containerClass;
    private final IStackHelper stackHelper;

    public QIOCraftingTransferHandler(IRecipeTransferHandlerHelper handlerHelper, IStackHelper stackHelper, Class<CONTAINER> containerClass) {
        this.handlerHelper = handlerHelper;
        this.stackHelper = stackHelper;
        this.containerClass = containerClass;
    }

    @Override
    public Class<CONTAINER> getContainerClass() {
        return containerClass;
    }

    @Override
    public Class<CraftingRecipe> getRecipeClass() {
        return CraftingRecipe.class;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(CONTAINER container, CraftingRecipe recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer,
          boolean doTransfer) {
        byte selectedCraftingGrid = container.getSelectedCraftingGrid();
        if (selectedCraftingGrid == -1) {
            //Note: While the java docs recommend logging a message to the console when returning an internal error,
            // this isn't actually an error state here, and is just one where we want to make sure the plus button is hidden
            // as there are no crafting grids being shown
            return handlerHelper.createInternalError();
        }
        QIOCraftingWindow craftingWindow = container.getCraftingWindow(selectedCraftingGrid);
        //Note: This variable is only used for when doTransfer is false
        byte nonEmptyCraftingSlots = 0;
        if (!doTransfer) {
            CraftingContainer dummy = MekanismUtils.getDummyCraftingInv();
            for (int slot = 0; slot < 9; slot++) {
                CraftingWindowInventorySlot inputSlot = craftingWindow.getInputSlot(slot);
                if (!inputSlot.isEmpty()) {
                    //Copy it in case any recipe does weird things and tries to mutate the stack
                    dummy.setItem(slot, StackUtils.size(inputSlot.getStack(), 1));
                    //Count how many crafting slots are not empty
                    nonEmptyCraftingSlots++;
                }
            }
            if (recipe.matches(dummy, player.level)) {
                //If we are not transferring things, and the crafting window's contents already matches the given recipe,
                // then we can just early exit knowing that we have something that will work. If we are transferring items
                // then we need to actually do all the checks as we may be transferring more items if maxTransfer is true,
                // or we may be transferring different items if different ones are shown in JEI
                return null;
            }
        }
        //TODO: It may be nice to eventually implement some sort of caching for this, it isn't drastically needed because JEI is smart
        // and only calls it once per recipe to decide if it should display the button rather than say calling it every render tick in
        // case something changed and the render state should be different. We probably could add some sort of listeners to
        // inventory, QIO, and crafting window that if one changes it invalidates the cache of what ingredients are stored, though then
        // we wouldn't be able to directly modify the map as we find inputs, and also we still would have to do a lot of this comparison
        // logic, unless we can also somehow cache the recipe layout and how it interacts with the other information
        List<IRecipeSlotView> slotViews = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        int maxInputCount = slotViews.size();
        if (maxInputCount > 9) {
            //I don't believe this ever will happen with a normal crafting recipe but just in case it does, error
            // if we have more than nine inputs, as there should never be
            // a case where this actually happens except potentially with some really obscure modded recipe
            Mekanism.logger.warn("Error evaluating recipe transfer handler for recipe: {}, had more than 9 inputs: {}", recipe.getId(), maxInputCount);
            return handlerHelper.createInternalError();
        }
        int inputCount = 0;
        record TrackedIngredients(IRecipeSlotView view, Set<HashedItem> representations) {
        }
        //We will have at most the same number of ingredients as we have input slot views
        Byte2ObjectMap<TrackedIngredients> hashedIngredients = new Byte2ObjectArrayMap<>(maxInputCount);
        for (int index = 0; index < maxInputCount; index++) {
            IRecipeSlotView slotView = slotViews.get(index);
            List<ItemStack> validIngredients = slotView.getIngredients(VanillaTypes.ITEM_STACK).toList();
            if (!validIngredients.isEmpty()) {
                //If there are valid ingredients, increment the count
                inputCount++;
                // and convert them to HashedItems
                // Note: we use a linked hash set to preserve the order of the ingredients as done in JEI
                LinkedHashSet<HashedItem> representations = new LinkedHashSet<>(validIngredients.size());
                //Note: We shouldn't need to convert the item that is part of the recipe to a "reduced" stack form based
                // on what the server would send, as the item should already be like that from when the server sent the
                // client the recipe. If this turns out to be incorrect due to how some mod does recipes, then we may need
                // to change this
                // Unchecked cast as we only requested views for item types
                ItemStack displayed = slotView.getDisplayedIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
                //Note: We use raw hashed items as none of this stuff should or will be modified while doing these checks,
                // so we may as well remove some unneeded copies
                if (!displayed.isEmpty()) {
                    //Start by adding the displayed ingredient if there is one to prioritize it
                    representations.add(HashedItem.raw(displayed));
                }
                //Then add all valid ingredients in the order they appear in JEI. Because we are using a set
                // we will just end up merging with the displayed ingredient when we get to it as a valid ingredient
                for (ItemStack validIngredient : validIngredients) {
                    representations.add(HashedItem.raw(validIngredient));
                }
                hashedIngredients.put((byte) index, new TrackedIngredients(slotView, representations));
            }
        }
        //Get all our available items in the QIO frequency, we flatten the cache to stack together items that
        // as far as the client is concerned are the same instead of keeping them UUID separated, and add all
        // the items in the currently selected crafting window and the player's inventory to our available items
        QIOCraftingTransferHelper qioTransferHelper = container.getTransferHelper(player, craftingWindow);
        if (qioTransferHelper.isInvalid()) {
            Mekanism.logger.warn("Error initializing QIO transfer handler for crafting window: {}", selectedCraftingGrid);
            return handlerHelper.createInternalError();
        }
        //Note: We do this in a reversed manner (HashedItem -> slots, vs slot -> HashedItem) so that we can more easily
        // calculate the split for how we handle maxTransfer by quickly being able to see how many of each type we have
        Map<HashedItem, ByteList> matchedItems = new HashMap<>(inputCount);
        ByteSet missingSlots = new ByteArraySet(inputCount);
        for (Byte2ObjectMap.Entry<TrackedIngredients> entry : hashedIngredients.byte2ObjectEntrySet()) {
            //TODO: Eventually we probably will want to add in some handling for if an item is valid for more than one slot and one combination
            // has it being valid and one combination it is not valid. For example if we have a single piece of stone and it is valid in either
            // slot 1 or 2 but slot 2 only allows for stone, and slot 1 can accept granite instead and we have granite available. When coming
            // up with a solution to this, we also will need to handle the slower comparison method, and make sure that if maxTransfer is true
            // then we pick the one that has the most elements we can assign to all slots evenly so that we can craft as many things as possible.
            // We currently don't bother with any handling related to this as JEI's own transfer handler it registers for things like the crafting
            // table don't currently handle this, though it is something that would be nice to handle and is something I believe vanilla's recipe
            // book transfer handler is able to do (RecipeItemHelper/ServerRecipePlayer)
            boolean matchFound = false;
            for (HashedItem validInput : entry.getValue().representations()) {
                HashedItemSource source = qioTransferHelper.getSource(validInput);
                if (source != null && source.hasMoreRemaining()) {
                    //We found a match for this slot, reduce how much of the item we have as an input
                    source.matchFound();
                    // mark that we found a match
                    matchFound = true;
                    // and which HashedItem the slot's index corresponds to
                    matchedItems.computeIfAbsent(validInput, item -> new ByteArrayList()).add(entry.getByteKey());
                    // and stop checking the other possible inputs
                    break;
                }
            }
            if (!matchFound) {
                //If we didn't find a match for the slot, add it as a slot we may be missing
                missingSlots.add(entry.getByteKey());
            }
        }
        if (!missingSlots.isEmpty()) {
            //After doing the quicker exact match lookup checks, go through any potentially missing slots
            // and do the slower more "accurate" check of if the stacks match. This allows us to use JEI's
            // system for letting mods declare what things match when it comes down to NBT
            Map<HashedItem, String> cachedIngredientUUIDs = new HashMap<>();
            for (Map.Entry<HashedItem, HashedItemSource> entry : qioTransferHelper.reverseLookup.entrySet()) {
                HashedItemSource source = entry.getValue();
                if (source.hasMoreRemaining()) {
                    //Only look at the source if we still have more items available in it
                    HashedItem storedHashedItem = entry.getKey();
                    ItemStack storedItem = storedHashedItem.getStack();
                    Item storedItemType = storedItem.getItem();
                    String storedItemUUID = null;
                    for (ByteIterator missingIterator = missingSlots.iterator(); missingIterator.hasNext(); ) {
                        byte index = missingIterator.nextByte();
                        for (HashedItem validIngredient : hashedIngredients.get(index).representations()) {
                            //Compare the raw item types
                            if (storedItemType == validIngredient.getStack().getItem()) {
                                //If they match, compute the identifiers for both stacks as needed
                                if (storedItemUUID == null) {
                                    //If we haven't retrieved a UUID for the stored stack yet because none of our previous ingredients
                                    // matched the basic item type, retrieve it
                                    storedItemUUID = stackHelper.getUniqueIdentifierForStack(storedItem, UidContext.Recipe);
                                }
                                //Next compute the UUID for the ingredient we are missing if we haven't already calculated it
                                // either in a previous iteration or for a different slot
                                String ingredientUUID = cachedIngredientUUIDs.computeIfAbsent(validIngredient,
                                      ingredient -> stackHelper.getUniqueIdentifierForStack(ingredient.getStack(), UidContext.Recipe));
                                if (storedItemUUID.equals(ingredientUUID)) {
                                    //If the items are equivalent, reduce how much of the item we have as an input
                                    source.matchFound();
                                    // unmark that the slot is missing a match
                                    missingIterator.remove();
                                    // and mark which HashedItem the slot's index corresponds to
                                    matchedItems.computeIfAbsent(storedHashedItem, item -> new ByteArrayList()).add(index);
                                    // and stop checking the other possible inputs
                                    break;
                                }
                            }
                        }
                        if (!source.hasMoreRemaining()) {
                            //If we have "used up" all the input we have available then continue onto the next stored stack
                            break;
                        }
                    }
                    if (missingSlots.isEmpty()) {
                        //If we have accounted for all the slots, stop checking for matches
                        break;
                    }
                }
            }
            if (!missingSlots.isEmpty()) {
                //If we have any missing slots, report that they are missing to the user and don't allow transferring
                List<IRecipeSlotView> missing = missingSlots.intStream().mapToObj(slot -> hashedIngredients.get((byte) slot).view()).toList();
                return handlerHelper.createUserErrorForMissingSlots(MekanismLang.JEI_MISSING_ITEMS.translate(), missing);
            }
        }
        if (doTransfer || (nonEmptyCraftingSlots > 0 && nonEmptyCraftingSlots >= qioTransferHelper.getEmptyInventorySlots())) {
            //Note: If all our crafting inventory slots are not empty, and we don't "obviously" have enough room due to empty slots,
            // then we need to calculate how much we can actually transfer and where it is coming from so that we are able to calculate
            // if we actually have enough room to shuffle the items around, even though otherwise we would only need to do these
            // calculations for when we are transferring items
            int toTransfer;
            if (maxTransfer) {
                //Calculate how much we can actually transfer if we want to transfer as many full sets as possible
                long maxToTransfer = Long.MAX_VALUE;
                for (Map.Entry<HashedItem, ByteList> entry : matchedItems.entrySet()) {
                    HashedItem hashedItem = entry.getKey();
                    HashedItemSource source = qioTransferHelper.getSource(hashedItem);
                    if (source == null) {
                        //If something went wrong, and we don't actually have the item we think we do, error
                        return invalidSource(hashedItem);
                    }
                    int maxStack = hashedItem.getStack().getMaxStackSize();
                    //If we have something that only stacks to one, such as a bucket. Don't limit the max stack size
                    // of other items to one
                    long max = maxStack == 1 ? maxToTransfer : Math.min(maxToTransfer, maxStack);
                    //Note: This will always be at least one as the int list should not be able to become
                    // larger than the number of items we have available
                    maxToTransfer = Math.min(max, source.getAvailable() / entry.getValue().size());
                }
                toTransfer = MathUtils.clampToInt(maxToTransfer);
            } else {
                toTransfer = 1;
            }
            QIOFrequency frequency = container.getFrequency();
            Byte2ObjectMap<List<SingularHashedItemSource>> sources = new Byte2ObjectArrayMap<>(inputCount);
            Map<HashedItemSource, List<List<SingularHashedItemSource>>> shuffleLookup = frequency == null ? Collections.emptyMap() : new HashMap<>(inputCount);
            for (Map.Entry<HashedItem, ByteList> entry : matchedItems.entrySet()) {
                HashedItem hashedItem = entry.getKey();
                HashedItemSource source = qioTransferHelper.getSource(hashedItem);
                if (source == null) {
                    //If something went wrong, and we don't actually have the item we think we do, error
                    return invalidSource(hashedItem);
                }
                //Cap the amount to transfer at the max tack size. This way we allow for transferring buckets
                // and other stuff with it. This only actually matters if the max stack size is one, due to
                // the logic done above when calculating how much to transfer, but we do this regardless here
                // as there is no reason not to and then if we decide to widen it up we only have to change one spot
                int transferAmount = Math.min(toTransfer, hashedItem.getStack().getMaxStackSize());
                for (byte slot : entry.getValue()) {
                    //Try to use the item and figure out where it is coming from
                    List<SingularHashedItemSource> actualSources = source.use(transferAmount);
                    if (actualSources.isEmpty()) {
                        //If something went wrong, and we don't actually have enough of the item for some reason, error
                        return invalidSource(hashedItem);
                    }
                    sources.put(slot, actualSources);
                    if (frequency != null) {
                        //The shuffle lookup only comes into play if we have a frequency so might end up having to check if there is room in it
                        int elements = entry.getValue().size();
                        if (elements == 1) {
                            shuffleLookup.put(source, Collections.singletonList(actualSources));
                        } else {
                            shuffleLookup.computeIfAbsent(source, s -> new ArrayList<>(elements)).add(actualSources);
                        }
                    }
                }
            }
            if (!hasRoomToShuffle(qioTransferHelper, frequency, craftingWindow, container.getHotBarSlots(), container.getMainInventorySlots(), shuffleLookup)) {
                return handlerHelper.createUserErrorWithTooltip(MekanismLang.JEI_INVENTORY_FULL.translate());
            }
            if (doTransfer) {
                //Note: We skip doing a validation check on if the recipe matches or not, as there is a chance that for some recipes
                // things may not fully be accurate on the client side with the stacks that JEI lets us know match the recipe, as
                // they may require extra NBT that is server side only.
                //TODO: If the sources are all from the crafting window and are already in the correct spots, there is no need to send this packet
                Mekanism.packetHandler().sendToServer(new PacketQIOFillCraftingWindow(recipe.getId(), maxTransfer, sources));
            }
        }
        return null;
    }

    private IRecipeTransferError invalidSource(@Nonnull HashedItem type) {
        ItemStack stack = type.getStack();
        Mekanism.logger.warn("Error finding source for: {} with nbt: {}. This should not be possible happen.", stack.getItem(), stack.getTag());
        return handlerHelper.createInternalError();
    }

    /**
     * Loosely based on how {@link mekanism.common.content.qio.QIOServerCraftingTransferHandler}'s hasRoomToShuffle method works.
     */
    private static boolean hasRoomToShuffle(QIOCraftingTransferHelper qioTransferHelper, @Nullable QIOFrequency frequency, QIOCraftingWindow craftingWindow,
          List<HotBarSlot> hotBarSlots, List<MainInventorySlot> mainInventorySlots, Map<HashedItemSource, List<List<SingularHashedItemSource>>> shuffleLookup) {
        //Map used to keep track of inputs while also merging identical inputs, so we can cut down
        // on how many times we have to check if things can stack
        Object2IntMap<HashedItem> leftOverInput = new Object2IntArrayMap<>(9);
        for (byte slotIndex = 0; slotIndex < 9; slotIndex++) {
            IInventorySlot slot = craftingWindow.getInputSlot(slotIndex);
            if (!slot.isEmpty()) {
                //Note: We can use raw as we are not modifying the stack or persisting the reference
                HashedItem type = HashedItem.raw(slot.getStack());
                HashedItemSource source = qioTransferHelper.getSource(type);
                if (source == null) {
                    //Something went wrong, this should never be null for the things in the crafting slots
                    return false;
                }
                int remaining = source.getSlotRemaining(slotIndex);
                if (remaining > 0) {
                    //Don't bother adding any that we fully used
                    leftOverInput.mergeInt(type, remaining, Integer::sum);
                }
            }
        }
        if (!leftOverInput.isEmpty()) {
            //If we have any leftover inputs in the crafting inventory, then get a simulated view of what the player's inventory
            // will look like after things are changed
            BaseSimulatedInventory simulatedInventory = new BaseSimulatedInventory(hotBarSlots, mainInventorySlots) {
                @Override
                protected int getRemaining(int slot, ItemStack currentStored) {
                    HashedItemSource source = qioTransferHelper.getSource(HashedItem.raw(currentStored));
                    if (source == null) {
                        return currentStored.getCount();
                    }
                    return source.getSlotRemaining((byte) (slot + 9));
                }
            };
            Object2IntMap<HashedItem> stillLeftOver = simulatedInventory.shuffleInputs(leftOverInput, frequency != null);
            if (stillLeftOver == null) {
                //If we have remaining items and no frequency then we don't have room to shuffle
                return false;
            }
            if (!stillLeftOver.isEmpty() && frequency != null) {
                //If we still have left over things try adding them to the frequency. We only are able to do a rough check and estimate
                // on if the frequency has room or not as depending on how things are stored in the drives there is a chance that we
                // do not actually have as much item space or types available, but this is the best we can do on the client side
                // Note: We validate the frequency is not null, even though it shouldn't be null if we have anything still left over
                //Note: We calculate these numbers as a difference so that it is easier to make sure none of the numbers accidentally overflow
                int availableItemTypes = frequency.getTotalItemTypeCapacity() - frequency.getTotalItemTypes(true);
                long availableItemSpace = frequency.getTotalItemCountCapacity() - frequency.getTotalItemCount();
                Object2BooleanMap<HashedItemSource> usedQIOSource = new Object2BooleanArrayMap<>(shuffleLookup.size());
                for (Map.Entry<HashedItemSource, List<List<SingularHashedItemSource>>> entry : shuffleLookup.entrySet()) {
                    HashedItemSource source = entry.getKey();
                    boolean usedQIO = false;
                    for (List<SingularHashedItemSource> usedSources : entry.getValue()) {
                        for (SingularHashedItemSource usedSource : usedSources) {
                            UUID qioSource = usedSource.getQioSource();
                            if (qioSource != null) {
                                //Free up however much space as we used of the item
                                availableItemSpace += usedSource.getUsed();
                                if (source.getQIORemaining(qioSource) == 0) {
                                    //If we used all that is available, we need to also free up an item type
                                    availableItemTypes++;
                                    usedQIO = true;
                                }
                            }
                        }
                    }
                    usedQIOSource.put(source, usedQIO);
                }
                for (Object2IntMap.Entry<HashedItem> entry : stillLeftOver.object2IntEntrySet()) {
                    availableItemSpace -= entry.getIntValue();
                    if (availableItemSpace <= 0) {
                        //No room for all our items, fail
                        return false;
                    }
                    HashedItemSource source = qioTransferHelper.getSource(entry.getKey());
                    if (source == null) {
                        //Something went wrong, this should never be null for the things in the crafting slots
                        return false;
                    } else if (source.hasQIOSources()) {
                        //It is stored, check to make sure it isn't a type we are removing at least one of fully
                        if (usedQIOSource.containsKey(source) && usedQIOSource.getBoolean(source)) {
                            // if it is, then we need to reclaim the item type as being available
                            availableItemTypes--;
                            if (availableItemTypes <= 0) {
                                //Not enough room for types
                                return false;
                            }
                        }
                    } else {
                        //The item is not stored in the QIO frequency, we need to use an item type up
                        // Note: This is not super accurate due to the fact that we don't know for
                        // certain if our used source actually matched or differed in server side only
                        // NBT, but it is the best we can do on the client side
                        availableItemTypes--;
                        if (availableItemTypes <= 0) {
                            //Not enough room for types
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}