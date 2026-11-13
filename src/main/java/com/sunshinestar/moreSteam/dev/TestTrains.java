package com.sunshinestar.moreSteam.dev;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlockEntity;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackShape;
import com.sunshinestar.moreSteam.FireboxBlock;
import com.sunshinestar.moreSteam.MoreSteamBlocks;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import org.slf4j.Logger;

/**
 * Development aid, only registered outside production: {@code /moresteam testtrains [pos] [direction]}
 * builds one small assembled train for every registered bogey style and size, so that the steam of
 * all of them can be looked at side by side.
 *
 * <p>Each train stands on its own lane of the matching track and consists of the bogey (two for
 * styles that cannot carry a carriage alone), a casing platform, train controls and a firebox, all
 * glued together. The lanes run in the given direction and are lined up to the right of it.
 */
public final class TestTrains {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** Blocks of track per lane. */
	private static final int LANE_LENGTH = 24;
	/** Distance between neighbouring lanes. */
	private static final int LANE_SPACING = 6;
	/** Lanes side by side before a new row is started further ahead. */
	private static final int LANES_PER_ROW = 12;
	/** Free blocks between two rows. */
	private static final int ROW_GAP = 6;
	/** Track block the station points at; the front bogey stands on the next one. */
	private static final int STATION_INDEX = 10;
	/** Distance between the two bogeys of styles that need a second one. */
	private static final int BOGEY_SPACING = 8;

	/** Ticks to let Create build the track graphs before assembling, and to settle before checking. */
	private static final int ASSEMBLE_AFTER = 40;
	private static final int REPORT_AFTER = 60;

	private static final List<Job> JOBS = new ArrayList<>();

	private TestTrains() {}

	public static void register() {
		NeoForge.EVENT_BUS.addListener(TestTrains::onRegisterCommands);
		NeoForge.EVENT_BUS.addListener(TestTrains::onServerTick);
	}

	private static void onRegisterCommands(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> testTrains = Commands.literal("testtrains")
			.executes(ctx -> start(ctx.getSource(), BlockPos.containing(ctx.getSource().getPosition()),
				facing(ctx.getSource())))
			.then(Commands.argument("pos", BlockPosArgument.blockPos())
				.executes(ctx -> start(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "pos"), facing(ctx.getSource())))
				.then(Commands.argument("direction", StringArgumentType.word())
					.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("north", "east", "south", "west"), builder))
					.executes(ctx -> {
						Direction direction = Direction.byName(StringArgumentType.getString(ctx, "direction"));
						if (direction == null || direction.getAxis() == Axis.Y) {
							ctx.getSource().sendFailure(Component.literal("Direction must be north, east, south or west"));
							return 0;
						}
						return start(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "pos"), direction);
					})));

		event.getDispatcher().register(Commands.literal("moresteam")
			.requires(source -> source.hasPermission(2))
			.then(testTrains));
	}

	private static Direction facing(CommandSourceStack source) {
		return Direction.fromYRot(source.getRotation().y);
	}

	private record Entry(BogeyStyle style, BogeySizes.BogeySize size, AbstractBogeyBlock<?> block) {
		String name() {
			return style.id + " / " + size.id().getPath();
		}
	}

	private record Lane(Entry entry, BlockPos station) {}

	private static final class Job {
		final ServerLevel level;
		final CommandSourceStack source;
		final UUID owner;
		final List<Lane> lanes = new ArrayList<>();
		final List<String> failures = new ArrayList<>();
		final Set<ChunkPos> forcedChunks = new HashSet<>();
		int total;
		int age;

		Job(ServerLevel level, CommandSourceStack source) {
			this.level = level;
			this.source = source;
			this.owner = source.getEntity() != null ? source.getEntity().getUUID() : UUID.randomUUID();
		}

		/** Keeps the chunk loaded and ticking until the job is done, so stations far from any player work too. */
		void keepLoaded(BlockPos pos) {
			ChunkPos chunk = new ChunkPos(pos);
			if (level.setChunkForced(chunk.x, chunk.z, true))
				forcedChunks.add(chunk);
		}
	}

	private static int start(CommandSourceStack source, BlockPos origin, Direction forward) {
		ServerLevel level = source.getLevel();
		Direction right = forward.getClockWise();

		List<Entry> entries = new ArrayList<>();
		AllBogeyStyles.BOGEY_STYLES.values().stream()
			.sorted(Comparator.comparing(style -> style.id.toString()))
			.forEach(style -> style.validSizes().stream()
				.sorted(Comparator.comparing(BogeySizes.BogeySize::wheelRadius))
				.forEach(size -> entries.add(new Entry(style, size, style.getBlockForSize(size)))));

		Job job = new Job(level, source);
		for (int i = 0; i < entries.size(); i++) {
			Entry entry = entries.get(i);
			int row = i / LANES_PER_ROW;
			int column = i % LANES_PER_ROW;
			BlockPos laneStart = origin.relative(forward, 2 + row * (LANE_LENGTH + ROW_GAP))
				.relative(right, column * LANE_SPACING);
			try {
				buildLane(job, entry, laneStart, forward, right);
			} catch (RuntimeException e) {
				LOGGER.error("Test train {}: could not be built", entry.name(), e);
				job.failures.add(entry.name() + " (could not be built: " + e + ")");
			}
		}
		job.total = entries.size();
		JOBS.add(job);

		int rows = (entries.size() + LANES_PER_ROW - 1) / LANES_PER_ROW;
		int count = entries.size();
		source.sendSuccess(() -> Component.literal("Building " + count + " test trains in " + rows + " rows of up to "
			+ LANES_PER_ROW + ", heading " + forward.getName() + " and extending to the " + right.getName()
			+ ". Assembling in a moment..."), true);
		return count;
	}

	private static void buildLane(Job job, Entry entry, BlockPos laneStart, Direction forward, Direction right) {
		ServerLevel level = job.level;

		TrackMaterial.TrackType trackType = entry.block.getTrackType(entry.style);
		TrackMaterial material = TrackMaterial.ALL.values().stream()
			.filter(candidate -> candidate.trackType == trackType)
			.min(Comparator.comparing((TrackMaterial candidate) -> !candidate.id.getPath().contains("andesite"))
				.thenComparing(candidate -> candidate.id.toString()))
			.orElse(null);
		if (material == null) {
			job.failures.add(entry.name() + " (no track of type " + (trackType == null ? "null" : trackType.id) + ")");
			return;
		}

		// The lane itself, with the space above it cleared.
		BlockState track = material.getBlock().defaultBlockState()
			.setValue(TrackBlock.SHAPE, forward.getAxis() == Axis.X ? TrackShape.XO : TrackShape.ZO);
		for (int i = 0; i < LANE_LENGTH; i++) {
			BlockPos pos = laneStart.relative(forward, i);
			job.keepLoaded(pos);
			for (int y = 1; y <= 4; y++)
				level.setBlock(pos.above(y), Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(pos, track, 3);
		}

		// A sign at the near end of the lane says which bogey this is.
		BlockPos signPos = laneStart.relative(forward, -1);
		level.setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState()
			.setValue(StandingSignBlock.ROTATION, RotationSegment.convertToSegment(forward.getOpposite())), 3);
		if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
			String path = entry.style.id.getPath();
			SignText text = new SignText()
				.setMessage(0, Component.literal(entry.style.id.getNamespace()))
				.setMessage(1, Component.literal(path.substring(0, Math.min(15, path.length()))))
				.setMessage(2, Component.literal(path.length() > 15 ? path.substring(15, Math.min(30, path.length())) : ""))
				.setMessage(3, Component.literal(entry.size.id().getPath()));
			sign.setText(text, true);
			sign.setChanged();
			level.sendBlockUpdated(signPos, sign.getBlockState(), sign.getBlockState(), 3);
		}

		// The station, two blocks to the side and pointed at the lane like a placed station item would be.
		BlockPos target = laneStart.relative(forward, STATION_INDEX);
		BlockPos stationPos = target.relative(right, 2);
		job.keepLoaded(stationPos);
		level.setBlock(stationPos, AllBlocks.TRACK_STATION.getDefaultState(), 3);
		if (!(level.getBlockEntity(stationPos) instanceof StationBlockEntity station)) {
			job.failures.add(entry.name() + " (station could not be placed)");
			return;
		}
		CompoundTag tag = station.saveWithoutMetadata(level.registryAccess());
		tag.put("TargetTrack", NbtUtils.writeBlockPos(target.subtract(stationPos)));
		tag.putBoolean("TargetDirection", forward.getAxisDirection() == AxisDirection.POSITIVE);
		station.loadWithComponents(tag, level.registryAccess());
		station.notifyUpdate();

		// The train. Its front bogey stands right behind the station's track block.
		int bogeys = entry.block.allowsSingleBogeyCarriage() ? 1 : 2;
		int lastIndex = Math.max(1, (bogeys - 1) * BOGEY_SPACING);
		BlockState bogeyState = entry.block.defaultBlockState();
		if (bogeyState.hasProperty(BlockStateProperties.HORIZONTAL_AXIS))
			bogeyState = bogeyState.setValue(BlockStateProperties.HORIZONTAL_AXIS, forward.getAxis());
		for (int n = 0; n < bogeys; n++) {
			BlockPos bogeyPos = target.relative(forward, 1 + n * BOGEY_SPACING).above();
			level.setBlock(bogeyPos, bogeyState, 3);
			if (level.getBlockEntity(bogeyPos) instanceof AbstractBogeyBlockEntity bogey) {
				bogey.setBogeyStyle(entry.style);
				bogey.setChanged();
				level.sendBlockUpdated(bogeyPos, bogeyState, bogeyState, 3);
			}
		}
		for (int i = 0; i <= lastIndex; i++)
			level.setBlock(target.relative(forward, 1 + i).above(2), AllBlocks.RAILWAY_CASING.getDefaultState(), 3);
		level.setBlock(target.relative(forward, 1).above(3), AllBlocks.TRAIN_CONTROLS.getDefaultState()
			.setValue(ControlsBlock.FACING, forward), 3);
		level.setBlock(target.relative(forward, 2).above(3), MoreSteamBlocks.FIREBOX.get().defaultBlockState()
			.setValue(FireboxBlock.FACING, right.getOpposite()), 3);

		// One sheet of glue around all of it, bogeys included.
		level.addFreshEntity(new SuperGlueEntity(level, SuperGlueEntity.span(target.relative(forward, 1).above(),
			target.relative(forward, 1 + lastIndex).above(3))));

		job.lanes.add(new Lane(entry, stationPos));
	}

	private static void onServerTick(ServerTickEvent.Post event) {
		for (Iterator<Job> iterator = JOBS.iterator(); iterator.hasNext();) {
			Job job = iterator.next();
			job.age++;
			if (job.age == ASSEMBLE_AFTER)
				assemble(job);
			if (job.age >= REPORT_AFTER) {
				report(job);
				iterator.remove();
			}
		}
	}

	private static void assemble(Job job) {
		for (Lane lane : job.lanes) {
			if (!(job.level.getBlockEntity(lane.station) instanceof StationBlockEntity station))
				continue;
			try {
				station.assemble(job.owner);
			} catch (RuntimeException e) {
				LOGGER.error("Test train {}: assembly threw", lane.entry.name(), e);
			}
		}
	}

	private static void report(Job job) {
		int assembled = 0;
		for (Lane lane : job.lanes) {
			String problem = null;
			if (!(job.level.getBlockEntity(lane.station) instanceof StationBlockEntity station)) {
				problem = "station is gone";
			} else {
				GlobalStation globalStation = station.getStation();
				if (globalStation == null) {
					problem = "station never connected to its track";
				} else if (globalStation.getPresentTrain() == null) {
					CompoundTag tag = station.saveWithoutMetadata(job.level.registryAccess());
					problem = "not assembled" + (tag.contains("LastException") ? ": " + tag.get("LastException") : "");
				}
			}

			if (problem == null) {
				assembled++;
				LOGGER.info("Test train {}: assembled", lane.entry.name());
			} else {
				LOGGER.warn("Test train {}: {}", lane.entry.name(), problem);
				job.failures.add(lane.entry.name() + " (" + problem + ")");
			}
		}

		for (ChunkPos chunk : job.forcedChunks)
			job.level.setChunkForced(chunk.x, chunk.z, false);

		int total = job.total;
		int done = assembled;
		LOGGER.info("Test trains: {} of {} assembled, {} failed", done, total, job.failures.size());
		job.source.sendSuccess(() -> Component.literal("Test trains: " + done + " of " + total + " assembled."), true);
		for (String failure : job.failures)
			job.source.sendFailure(Component.literal("Failed: " + failure));
	}
}
