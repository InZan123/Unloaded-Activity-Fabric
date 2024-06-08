package com.github.inzan17.mixin;

import com.github.inzan17.UnloadedActivity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.ReadOnlyChunk;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.Arrays;

@Mixin(value = ChunkSerializer.class, priority = 999)
public abstract class ChunkSerializerMixin {
    @Inject(method = "serialize", at = @At("RETURN"))
    private static void serialize(ServerWorld world, Chunk chunk, CallbackInfoReturnable<NbtCompound> cir) {
        NbtCompound nbtCompound = cir.getReturnValue();

        NbtCompound chunkData = new NbtCompound();

        chunkData.putLong("last_tick", chunk.getLastTick());
        chunkData.putLong("ver", chunk.getSimulationVersion());
        chunkData.putLongArray("sim_blocks", chunk.getSimulationBlocks());

        nbtCompound.put("unloaded_activity", chunkData);
    }

    @Inject(method = "deserialize", at = @At("RETURN"))
    private static void deserialize(ServerWorld world, PointOfInterestStorage poiStorage, ChunkPos chunkPos, NbtCompound nbtCompound, CallbackInfoReturnable<ProtoChunk> cir) {
        ProtoChunk protoChunktemp = cir.getReturnValue();

        Chunk protoChunk = (protoChunktemp instanceof ReadOnlyChunk readOnlyChunk) ? readOnlyChunk.getWrappedChunk() : protoChunktemp;

        NbtCompound chunkData = nbtCompound.getCompound("unloaded_activity");

        boolean isEmpty = chunkData.isEmpty();

        if (isEmpty) {
            protoChunk.setNeedsSaving(true);
        } else {
            protoChunk.setLastTick(chunkData.getLong("last_tick"));
            protoChunk.setSimulationVersion(chunkData.getLong("ver"));
            protoChunk.setSimulationBlocks(chunkData.getLongArray("sim_blocks"));
        }


        if (UnloadedActivity.instance.config.convertCCAData && isEmpty) {
            NbtCompound cardinalData = nbtCompound.getCompound("cardinal_components");

            if (!cardinalData.isEmpty()) {
                NbtCompound lastChunkTick = cardinalData.getCompound("unloadedactivity:last-chunk-tick");
                if (!lastChunkTick.isEmpty()) {
                    protoChunk.setLastTick(lastChunkTick.getLong("last-tick"));
                }

                NbtCompound chunkSimVer = cardinalData.getCompound("unloadedactivity:chunk-sim-ver");
                if (!chunkSimVer.isEmpty()) {
                    protoChunk.setSimulationVersion(chunkSimVer.getLong("sim-ver"));
                }

                NbtCompound chunkSimBlocks = cardinalData.getCompound("unloadedactivity:chunk-sim-blocks");
                if (!chunkSimBlocks.isEmpty()) {
                    protoChunk.setSimulationBlocks(chunkSimBlocks.getLongArray("sim-blocks"));
                }

                // This is so that cardinal components doesn't start sending warnings.
                cardinalData.remove("unloadedactivity:last-chunk-tick");
                cardinalData.remove("unloadedactivity:chunk-sim-ver");
                cardinalData.remove("unloadedactivity:chunk-sim-blocks");
                cardinalData.remove("unloadedactivity:magik");
            }
        }

        if (protoChunk.getLastTick() == 0) {
            protoChunk.setNeedsSaving(true);
            protoChunk.setLastTick(world.getTimeOfDay());
        }
    }
}