package org.cneko.toneko.common.mod.items;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class GuideBookItem extends Item {
    public static final String ID = "toneko_guide";
    public static final ResourceLocation BOOK_ID = ResourceLocation.fromNamespaceAndPath("toneko", "toneko_guide");
    private static final ResourceLocation PATCHOULI_BOOK_ITEM_ID = ResourceLocation.fromNamespaceAndPath("patchouli", "guide_book");
    private static final ResourceLocation PATCHOULI_BOOK_COMPONENT_ID = ResourceLocation.fromNamespaceAndPath("patchouli", "book");

    private static final boolean PATCHOULI_LOADED;

    static {
        boolean loaded;
        try {
            Class.forName("vazkii.patchouli.api.PatchouliAPI");
            loaded = true;
        } catch (ClassNotFoundException e) {
            loaded = false;
        }
        PATCHOULI_LOADED = loaded;
    }

    public GuideBookItem(Properties properties) {
        super(properties);
    }

    /**
     * Create a Patchouli guide book ItemStack for this mod's guide.
     * Returns ItemStack.EMPTY if Patchouli is not installed.
     */
    @SuppressWarnings("unchecked")
    public static ItemStack createGuideBookStack() {
        if (!PATCHOULI_LOADED) return ItemStack.EMPTY;

        Item guideBookItem = BuiltInRegistries.ITEM.get(PATCHOULI_BOOK_ITEM_ID);
        if (guideBookItem == Items.AIR) return ItemStack.EMPTY;

        DataComponentType<ResourceLocation> bookComponent = (DataComponentType<ResourceLocation>)
                BuiltInRegistries.DATA_COMPONENT_TYPE.get(PATCHOULI_BOOK_COMPONENT_ID);
        if (bookComponent == null) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(guideBookItem);
        stack.set(bookComponent, BOOK_ID);
        return stack;
    }

    /**
     * Check if the given ItemStack is our mod's guide book.
     */
    @SuppressWarnings("unchecked")
    public static boolean isOurGuideBook(ItemStack stack) {
        Item guideBookItem = BuiltInRegistries.ITEM.get(PATCHOULI_BOOK_ITEM_ID);
        if (!stack.is(guideBookItem)) return false;
        DataComponentType<ResourceLocation> bookComponent = (DataComponentType<ResourceLocation>)
                BuiltInRegistries.DATA_COMPONENT_TYPE.get(PATCHOULI_BOOK_COMPONENT_ID);
        if (bookComponent == null) return false;
        return BOOK_ID.equals(stack.get(bookComponent));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (PATCHOULI_LOADED) {
            // 帕秋莉已安装，由帕秋莉客户端接管书籍打开逻辑
            return super.use(level, player, hand);
        }

        // 帕秋莉未安装，提示玩家
        if (!level.isClientSide()) {
            player.displayClientMessage(
                Component.translatable("message.toneko.guide.need_patchouli"),
                true
            );
        }
        return InteractionResultHolder.success(stack);
    }
}
