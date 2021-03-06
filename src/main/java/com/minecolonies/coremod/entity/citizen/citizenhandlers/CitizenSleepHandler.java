package com.minecolonies.coremod.entity.citizen.citizenhandlers;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenSleepHandler;
import com.minecolonies.coremod.entity.citizen.EntityCitizen;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static com.minecolonies.api.entity.citizen.AbstractEntityCitizen.DATA_BED_POS;
import static com.minecolonies.api.entity.citizen.AbstractEntityCitizen.DATA_IS_ASLEEP;
import static com.minecolonies.api.util.constant.Constants.*;

/**
 * Handles the sleep of the citizen.
 */
public class CitizenSleepHandler implements ICitizenSleepHandler
{
    /**
     * The citizen assigned to this manager.
     */
    private final EntityCitizen citizen;

    /**
     * Constructor for the experience handler.
     * @param citizen the citizen owning the handler.
     */
    public CitizenSleepHandler(final EntityCitizen citizen)
    {
        this.citizen = citizen;
    }

    /**
     * Is the citizen a sleep?
     *
     * @return true when a sleep.
     */
    @Override
    public boolean isAsleep()
    {
        return citizen.getDataManager().get(DATA_IS_ASLEEP);
    }

    /**
     * Sets if the citizen is a sleep.
     * Caution: Use trySleep(BlockPos) for better control
     *
     * @param isAsleep True to make the citizen sleep.
     */
    private void setIsAsleep(final boolean isAsleep)
    {
        if (citizen.getCitizenData() != null)
        {
            citizen.getCitizenData().setAsleep(isAsleep);
        }
        citizen.getDataManager().set(DATA_IS_ASLEEP, isAsleep);
    }

    /**
     * Returns the orientation of the bed in degrees.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public float getBedOrientationInDegrees()
    {
        final IBlockState state = getBedLocation() == null ? null : citizen.world.getBlockState(getBedLocation());
        if (state != null && state.getBlock().isBed(state, citizen.world, getBedLocation(), citizen))
        {
            final EnumFacing enumfacing = state.getBlock().getBedDirection(state, citizen.world, getBedLocation());

            switch (enumfacing)
            {
                case SOUTH:
                    return NINETY_DEGREE;
                case WEST:
                    return 0.0F;
                case NORTH:
                    return THREE_QUARTERS;
                case EAST:
                    return HALF_ROTATION;
                default:
                    return 0F;
            }
        }

        return 0.0F;
    }

    /**
     * Attempts a sleep interaction with the citizen and the given bed.
     *
     * @param bedLocation The possible location to sleep.
     */
    @Override
    public boolean trySleep(final BlockPos bedLocation)
    {
        final IBlockState state = citizen.world.isBlockLoaded(bedLocation) ? citizen.world.getBlockState(bedLocation) : null;
        final boolean isBed = state != null && state.getBlock().isBed(state, citizen.world, bedLocation, citizen);

        if (!isBed)
        {
            return false;
        }

        citizen.setPosition( ((float) bedLocation.getX() + HALF_BLOCK),
          (float) bedLocation.getY(),
          ((float) bedLocation.getZ() + HALF_BLOCK));

        citizen.motionX = 0.0D;
        citizen.motionY = 0.0D;
        citizen.motionZ = 0.0D;

        //Remove item while citizen is asleep.
        citizen.getCitizenItemHandler().removeHeldItem();

        setIsAsleep(true);

        if (citizen.getCitizenData() != null)
        {
            citizen.getCitizenData().setBedPos(bedLocation);
        }
        citizen.getDataManager().set(DATA_BED_POS, bedLocation);
        return true;
    }

    /**
     * Called when the citizen wakes up.
     */
    @Override
    public void onWakeUp()
    {
        notifyCitizenHandlersOfWakeUp();

        //Only do this if he really sleeps
        if (!isAsleep())
        {
            return;
        }

        spawnCitizenFromBed();
    }

    private void notifyCitizenHandlersOfWakeUp()
    {
        if (citizen.getCitizenColonyHandler().getWorkBuilding() != null)
        {
            citizen.getCitizenStatusHandler().setLatestStatus(new TextComponentTranslation("com.minecolonies.coremod.status.working"));
            citizen.getCitizenColonyHandler().getWorkBuilding().onWakeUp();
        }
        if (citizen.getCitizenJobHandler().getColonyJob() != null)
        {
            citizen.getCitizenJobHandler().getColonyJob().onWakeUp();
        }

        final IBuilding homeBuilding = citizen.getCitizenColonyHandler().getHomeBuilding();
        if (homeBuilding != null)
        {
            homeBuilding.onWakeUp();
        }
    }

    private void spawnCitizenFromBed()
    {
        final BlockPos spawn;
        if (!getBedLocation().equals(BlockPos.ORIGIN) && citizen.world.getBlockState(getBedLocation()).getBlock() == Blocks.BED)
        {
            spawn = BlockBed.getSafeExitLocation(citizen.world, getBedLocation(), 0);
        }
        else
        {
            spawn = citizen.getPosition();
        }

        if (spawn != null && !spawn.equals(BlockPos.ORIGIN))
        {
            citizen.setPosition(spawn.getX() + HALF_BLOCK, spawn.getY() + HALF_BLOCK, spawn.getZ() + HALF_BLOCK);
        }

        setIsAsleep(false);
        if (citizen.getCitizenData() != null)
        {
            citizen.getCitizenData().setBedPos(new BlockPos(0, 0, 0));
        }
        citizen.getDataManager().set(DATA_BED_POS, new BlockPos(0, 0, 0));
    }

    /**
     * Get the bed location of the citizen.
     * @return the bed location.
     */
    @Override
    public BlockPos getBedLocation()
    {
        return citizen.getDataManager().get(DATA_BED_POS);
    }

    /**
     * Get the X render offset.
     * @return the offset.
     */
    @Override
    public float getRenderOffsetX()
    {
        if (!isAsleep())
        {
            return 0;
        }

        final IBlockState state = citizen.world.isBlockLoaded(getBedLocation()) ? citizen.world.getBlockState(getBedLocation()) : null;
        final boolean isBed = state != null && state.getBlock().isBed(state, citizen.world, getBedLocation(), citizen);
        final EnumFacing enumfacing = isBed && state.getBlock() instanceof BlockHorizontal ? state.getValue(BlockHorizontal.FACING) : null;

        if (enumfacing == null)
        {
            return 0;
        }

        return SLEEPING_RENDER_OFFSET * (float) enumfacing.getXOffset();
    }

    /**
     * Get the z render offset.
     * @return the offset.
     */
    @Override
    public float getRenderOffsetZ()
    {
        if (!isAsleep())
        {
            return 0;
        }

        final IBlockState state = citizen.world.isBlockLoaded(getBedLocation()) ? citizen.world.getBlockState(getBedLocation()) : null;
        final boolean isBed = state != null && state.getBlock().isBed(state, citizen.world, getBedLocation(), citizen);
        final EnumFacing enumfacing = isBed && state.getBlock() instanceof BlockHorizontal ? state.getValue(BlockHorizontal.FACING) : null;

        if (enumfacing == null)
        {
            return 0;
        }

        return SLEEPING_RENDER_OFFSET * (float) enumfacing.getZOffset();
    }
}
