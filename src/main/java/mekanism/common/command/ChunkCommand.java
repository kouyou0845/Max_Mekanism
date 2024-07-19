package mekanism.common.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import mekanism.api.text.EnumColor;
import mekanism.api.text.ILangEntry;
import mekanism.common.MekanismLang;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ChunkCommand {

    private ChunkCommand() {
    }

    private static final LongSet chunkWatchers = new LongOpenHashSet();

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        MinecraftForge.EVENT_BUS.register(ChunkCommand.class);
        return Commands.literal("chunk")
              .requires(cs -> cs.hasPermission(2))
              .then(WatchCommand.register())
              .then(UnwatchCommand.register())
              .then(ClearCommand.register())
              .then(FlushCommand.register());
    }

    private static class WatchCommand {

        static ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("watch")
                  .executes(ctx -> {
                      CommandSourceStack source = ctx.getSource();
                      return watch(source, new ChunkPos(new BlockPos(source.getPosition())));
                  }).then(Commands.argument("pos", ColumnPosArgument.columnPos())
                        .executes(ctx -> {
                            ColumnPos column = ColumnPosArgument.getColumnPos(ctx, "pos");
                            return watch(ctx.getSource(), new ChunkPos(column.x, column.z));
                        }));
        }

        private static int watch(CommandSourceStack source, ChunkPos chunkPos) {
            chunkWatchers.add(ChunkPos.asLong(chunkPos.x, chunkPos.z));
            source.sendSuccess(MekanismLang.COMMAND_CHUNK_WATCH.translateColored(EnumColor.GRAY, EnumColor.INDIGO, getPosition(chunkPos)), true);
            return 0;
        }
    }

    private static class UnwatchCommand {

        static ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("unwatch")
                  .executes(ctx -> {
                      CommandSourceStack source = ctx.getSource();
                      return unwatch(source, new ChunkPos(new BlockPos(source.getPosition())));
                  }).then(Commands.argument("pos", ColumnPosArgument.columnPos())
                        .executes(ctx -> {
                            ColumnPos column = ColumnPosArgument.getColumnPos(ctx, "pos");
                            return unwatch(ctx.getSource(), new ChunkPos(column.x, column.z));
                        }));
        }

        private static int unwatch(CommandSourceStack source, ChunkPos chunkPos) {
            chunkWatchers.remove(ChunkPos.asLong(chunkPos.x, chunkPos.z));
            source.sendSuccess(MekanismLang.COMMAND_CHUNK_UNWATCH.translateColored(EnumColor.GRAY, EnumColor.INDIGO, getPosition(chunkPos)), true);
            return 0;
        }
    }

    private static class ClearCommand {

        static ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("clear")
                  .executes(ctx -> {
                      int count = chunkWatchers.size();
                      chunkWatchers.clear();
                      ctx.getSource().sendSuccess(MekanismLang.COMMAND_CHUNK_CLEAR.translateColored(EnumColor.GRAY, EnumColor.INDIGO, count), true);
                      return 0;
                  });
        }
    }

    private static class FlushCommand {

        static ArgumentBuilder<CommandSourceStack, ?> register() {
            return Commands.literal("flush")
                  .executes(ctx -> {
                      CommandSourceStack source = ctx.getSource();
                      ServerChunkCache sp = source.getLevel().getChunkSource();
                      int startCount = sp.getLoadedChunksCount();
                      //TODO: Check this
                      //sp.queueUnloadAll();
                      sp.tick(() -> false, false);
                      source.sendSuccess(MekanismLang.COMMAND_CHUNK_FLUSH.translateColored(EnumColor.GRAY, EnumColor.INDIGO, startCount - sp.getLoadedChunksCount()), true);
                      return 0;
                  });
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        handleChunkEvent(event, MekanismLang.COMMAND_CHUNK_LOADED);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        handleChunkEvent(event, MekanismLang.COMMAND_CHUNK_UNLOADED);
    }

    private static void handleChunkEvent(ChunkEvent event, ILangEntry direction) {
        if (event.getWorld() == null || event.getWorld().isClientSide()) {
            return;
        }
        ChunkPos pos = event.getChunk().getPos();
        if (chunkWatchers.contains(pos.toLong())) {
            Component message = direction.translateColored(EnumColor.GRAY, EnumColor.INDIGO, getPosition(pos));
            event.getWorld().players().forEach(player -> player.sendMessage(message, Util.NIL_UUID));
        }
    }

    private static Component getPosition(ChunkPos pos) {
        return MekanismLang.GENERIC_WITH_COMMA.translate(pos.x, pos.z);
    }
}