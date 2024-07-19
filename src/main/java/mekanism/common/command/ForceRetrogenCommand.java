package mekanism.common.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import mekanism.api.text.EnumColor;
import mekanism.common.Mekanism;
import mekanism.common.MekanismLang;
import mekanism.common.config.MekanismConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class ForceRetrogenCommand {

    private static final SimpleCommandExceptionType RETROGEN_NOT_ENABLED = new SimpleCommandExceptionType(MekanismLang.COMMAND_ERROR_RETROGEN_DISABLED.translate());
    private static final SimpleCommandExceptionType NO_CHUNKS_QUEUED = new SimpleCommandExceptionType(MekanismLang.COMMAND_ERROR_RETROGEN_FAILURE.translate());

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("retrogen")
              .requires(cs -> cs.hasPermission(2))
              .executes(ctx -> {
                  ColumnPos pos = new ColumnPos(new BlockPos(ctx.getSource().getPosition()));
                  return addChunksToRegen(ctx.getSource(), pos, pos);
              }).then(Commands.argument("from", ColumnPosArgument.columnPos())
                    .executes(ctx -> {
                        ColumnPos from = ColumnPosArgument.getColumnPos(ctx, "from");
                        return addChunksToRegen(ctx.getSource(), from, from);
                    }).then(Commands.argument("to", ColumnPosArgument.columnPos())
                          .executes(ctx -> addChunksToRegen(ctx.getSource(), ColumnPosArgument.getColumnPos(ctx, "from"),
                                ColumnPosArgument.getColumnPos(ctx, "to")))));
    }

    private static int addChunksToRegen(CommandSourceStack source, ColumnPos start, ColumnPos end) throws CommandSyntaxException {
        if (!MekanismConfig.world.enableRegeneration.get()) {
            throw RETROGEN_NOT_ENABLED.create();
        }
        int xStart = Math.min(start.x, end.x);
        int xEnd = Math.max(start.x, end.x);
        int zStart = Math.min(start.z, end.z);
        int zEnd = Math.max(start.z, end.z);
        //TODO: Switch this to something like !World.isValidXZPosition (issue is it is private)
        if (xStart < -30000000 || zStart < -30000000 || xEnd >= 30000000 || zEnd >= 30000000) {
            throw BlockPosArgument.ERROR_OUT_OF_WORLD.create();
        }
        int chunkXStart = xStart >> 4;
        int chunkXEnd = xEnd >> 4;
        int chunkZStart = zStart >> 4;
        int chunkZEnd = zEnd >> 4;
        ServerLevel world = source.getLevel();
        ResourceKey<Level> registryKey = world.dimension();
        boolean hasChunks = false;
        for (int chunkX = chunkXStart; chunkX <= chunkXEnd; chunkX++) {
            for (int chunkZ = chunkZStart; chunkZ <= chunkZEnd; chunkZ++) {
                if (world.hasChunk(chunkX, chunkZ)) {
                    Mekanism.worldTickHandler.addRegenChunk(registryKey, new ChunkPos(chunkX, chunkZ));
                    source.sendSuccess(MekanismLang.COMMAND_RETROGEN_CHUNK_QUEUED.translateColored(EnumColor.GRAY, EnumColor.INDIGO,
                          MekanismLang.GENERIC_WITH_COMMA.translate(chunkX, chunkZ), EnumColor.INDIGO, registryKey.location()), true);
                    hasChunks = true;
                }
            }
        }
        if (!hasChunks) {
            throw NO_CHUNKS_QUEUED.create();
        }
        return 0;
    }
}