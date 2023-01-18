package com.github.inzan123.mixin;


import com.github.inzan123.SimulateTimePassing;
import com.github.inzan123.UnloadedActivity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static java.lang.Math.pow;

@Mixin(CropBlock.class)
public abstract class CropSimulateTimeMixin extends PlantBlock implements SimulateTimePassing {

    public CropSimulateTimeMixin(Settings settings) {
        super(settings);
    }

    @Shadow
    protected static float getAvailableMoisture(Block block, BlockView world, BlockPos pos) {
        return 0;
    }

    @Shadow
    protected abstract int getAge(BlockState state);

    @Shadow
    public abstract int getMaxAge();

    @Shadow
    public abstract BlockState withAge(int age);

    @Override
    public double getGrowthOdds(ServerWorld world, BlockPos pos) {
        float f = getAvailableMoisture(this, world, pos);
        return 1.0/(double)((int)(25.0F / f) + 1);
    }

    @Override
    public void simulateTime(BlockState state, ServerWorld world, BlockPos pos, Random random, long timePassed, int randomTickSpeed) {

        if (!UnloadedActivity.CONFIG.growCrops()) return;

        if (world.getBaseLightLevel(pos, 0) < 9) return;

        int currentAge = this.getAge(state);
        int maxAge = this.getMaxAge();
        int ageDifference = maxAge-currentAge;

        if (ageDifference <= 0) return;

        double randomPickChance = 1.0-pow(1.0 - 1.0 / 4096.0, randomTickSpeed); //chance to get picked by random ticks (this will unfortunately not take into account crops being picked twice on high random tick speeds)

        double randomGrowChance = getGrowthOdds(world,pos); //chance to grow for every pick

        double totalChance = randomPickChance*randomGrowChance;

        double invertedTotalChance = 1-totalChance;

        double randomFloat = random.nextDouble();

        double totalProbability = 0;

        for (int i = 0; i<ageDifference;i++) {

            double choose = getChoose(i,timePassed);

            double finalProbability = choose * pow(totalChance, i) * pow(invertedTotalChance, timePassed-i); //Probability of it growing "i" steps

            //UnloadedActivity.LOGGER.info("odds for growing " + i + " times: " + finalProbability);

            totalProbability += finalProbability;

            if (randomFloat < totalProbability) {
                world.setBlockState(pos, this.withAge(currentAge + i), 2);
                return;
            }
        }
        //UnloadedActivity.LOGGER.info("odds for growing to max age: " + (1 - totalProbability));
        world.setBlockState(pos, this.withAge(maxAge), 2);
    }
}