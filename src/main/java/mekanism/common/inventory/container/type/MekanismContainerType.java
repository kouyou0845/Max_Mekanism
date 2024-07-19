package mekanism.common.inventory.container.type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.common.inventory.container.entity.IEntityContainer;
import mekanism.common.inventory.container.type.MekanismContainerType.IMekanismContainerFactory;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.WorldUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.IContainerFactory;

public class MekanismContainerType<T, CONTAINER extends AbstractContainerMenu> extends BaseMekanismContainerType<T, CONTAINER, IMekanismContainerFactory<T, CONTAINER>> {

    public static <TILE extends TileEntityMekanism, CONTAINER extends AbstractContainerMenu> MekanismContainerType<TILE, CONTAINER> tile(Class<TILE> type,
          IMekanismContainerFactory<TILE, CONTAINER> constructor) {
        return new MekanismContainerType<>(type, constructor, (id, inv, buf) -> constructor.create(id, inv, getTileFromBuf(buf, type)));
    }

    public static <TILE extends TileEntityMekanism, CONTAINER extends AbstractContainerMenu> MekanismContainerType<TILE, CONTAINER> tile(Class<TILE> type,
          IMekanismSidedContainerFactory<TILE, CONTAINER> constructor) {
        return new MekanismContainerType<>(type, constructor, (id, inv, buf) -> constructor.create(id, inv, getTileFromBuf(buf, type), true));
    }

    public static <ENTITY extends Entity, CONTAINER extends AbstractContainerMenu & IEntityContainer<ENTITY>> MekanismContainerType<ENTITY, CONTAINER> entity(Class<ENTITY> type,
          IMekanismContainerFactory<ENTITY, CONTAINER> constructor) {
        return new MekanismContainerType<>(type, constructor, (id, inv, buf) -> constructor.create(id, inv, getEntityFromBuf(buf, type)));
    }

    public static <ENTITY extends Entity, CONTAINER extends AbstractContainerMenu & IEntityContainer<ENTITY>> MekanismContainerType<ENTITY, CONTAINER> entity(Class<ENTITY> type,
          IMekanismSidedContainerFactory<ENTITY, CONTAINER> constructor) {
        return new MekanismContainerType<>(type, constructor, (id, inv, buf) -> constructor.create(id, inv, getEntityFromBuf(buf, type), true));
    }

    protected MekanismContainerType(Class<T> type, IMekanismContainerFactory<T, CONTAINER> mekanismConstructor, IContainerFactory<CONTAINER> constructor) {
        super(type, mekanismConstructor, constructor);
    }

    @Nullable
    public CONTAINER create(int id, Inventory inv, Object data) {
        if (type.isInstance(data)) {
            return mekanismConstructor.create(id, inv, type.cast(data));
        }
        return null;
    }

    @Nullable
    public MenuConstructor create(Object data) {
        if (type.isInstance(data)) {
            T d = type.cast(data);
            return (id, inv, player) -> mekanismConstructor.create(id, inv, d);
        }
        return null;
    }

    @Nonnull
    private static <TILE extends BlockEntity> TILE getTileFromBuf(FriendlyByteBuf buf, Class<TILE> type) {
        if (buf == null) {
            throw new IllegalArgumentException("Null packet buffer");
        }
        return DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> {
            BlockPos pos = buf.readBlockPos();
            TILE tile = WorldUtils.getTileEntity(type, Minecraft.getInstance().level, pos);
            if (tile == null) {
                throw new IllegalStateException("Client could not locate tile at " + pos + " for tile container. "
                                                + "This is likely caused by a mod breaking client side tile lookup");
            }
            return tile;
        });
    }

    @Nonnull
    private static <ENTITY extends Entity> ENTITY getEntityFromBuf(FriendlyByteBuf buf, Class<ENTITY> type) {
        if (buf == null) {
            throw new IllegalArgumentException("Null packet buffer");
        }
        return DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> {
            if (Minecraft.getInstance().level == null) {
                throw new IllegalStateException("Client world is null.");
            }
            int entityId = buf.readVarInt();
            Entity e = Minecraft.getInstance().level.getEntity(entityId);
            if (type.isInstance(e)) {
                //noinspection unchecked
                return (ENTITY) e;
            }
            throw new IllegalStateException("Client could not locate entity (id: " + entityId + ")  for entity container or the entity was of an invalid type. "
                                            + "This is likely caused by a mod breaking client side entity lookup.");
        });
    }

    @FunctionalInterface
    public interface IMekanismContainerFactory<T, CONTAINER extends AbstractContainerMenu> {

        CONTAINER create(int id, Inventory inv, T data);
    }

    @FunctionalInterface
    public interface IMekanismSidedContainerFactory<T, CONTAINER extends AbstractContainerMenu> extends IMekanismContainerFactory<T, CONTAINER> {


        CONTAINER create(int id, Inventory inv, T data, boolean remote);

        @Override
        default CONTAINER create(int id, Inventory inv, T data) {
            return create(id, inv, data, false);
        }
    }
}