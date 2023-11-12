package supercoder79.survivalgames.game;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.WorldBorderInitializeS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldBorderInterpolateSizeS2CPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;
import supercoder79.survivalgames.SurvivalGames;
import supercoder79.survivalgames.game.config.SurvivalGamesConfig;
import supercoder79.survivalgames.game.logic.ActiveLogic;
import supercoder79.survivalgames.game.logic.SpawnerLogic;
import supercoder79.survivalgames.game.map.SurvivalGamesMap;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerSet;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.block.BlockPlaceEvent;
import xyz.nucleoid.stimuli.event.entity.EntityDeathEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class SurvivalGamesActive {
    private final GameSpace space;
    private final SurvivalGamesMap map;
    private final SurvivalGamesConfig config;

    private final PlayerSet participants;

    private final SurvivalGamesSpawnLogic spawnLogic;
    private final SurvivalGamesBar bar;

    private long startTime;
    private long shrinkStartTime;
    private boolean borderShrinkStarted = false;
    private long gameCloseTick = Long.MAX_VALUE;
    private boolean finished = false;
    private final ServerWorld world;
    private final GenerationTracker tracker;
    private final Set<ActiveLogic> logics = new HashSet<>();

    private SurvivalGamesActive(GameSpace space, SurvivalGamesMap map, SurvivalGamesConfig config, PlayerSet participants, GlobalWidgets widgets, ServerWorld world, GenerationTracker tracker) {
        this.space = space;
        this.map = map;
        this.config = config;
        this.participants = participants;
        this.world = world;
        this.tracker = tracker;

        this.spawnLogic = new SurvivalGamesSpawnLogic(space, config);
        this.bar = SurvivalGamesBar.create(widgets);
    }

    public static void open(GameSpace space, SurvivalGamesMap map, SurvivalGamesConfig config, ServerWorld world, GenerationTracker tracker) {
        space.setActivity(game -> {
            GlobalWidgets widgets = GlobalWidgets.addTo(game);
            SurvivalGamesActive active = new SurvivalGamesActive(space, map, config, space.getPlayers(), widgets, world, tracker);

            game.setRule(GameRuleType.CRAFTING, ActionResult.PASS);
            game.setRule(GameRuleType.PORTALS, ActionResult.FAIL);
            game.setRule(GameRuleType.PVP, ActionResult.PASS);
            game.setRule(GameRuleType.BLOCK_DROPS, ActionResult.PASS);
            game.setRule(GameRuleType.FALL_DAMAGE, ActionResult.PASS);
            game.setRule(GameRuleType.HUNGER, ActionResult.FAIL);
            game.setRule(GameRuleType.SATURATED_REGENERATION, ActionResult.FAIL);
            game.setRule(GameRuleType.UNSTABLE_TNT, ActionResult.PASS);
            game.setRule(GameRuleType.THROW_ITEMS, ActionResult.SUCCESS);
            game.setRule(SurvivalGames.DISABLE_SPAWNERS, ActionResult.SUCCESS);

            game.listen(GameActivityEvents.CREATE, active::open);
            game.listen(GameActivityEvents.DESTROY, active::close);

            game.listen(GamePlayerEvents.JOIN, (player -> active.spawnSpectator(player, world)));
            game.listen(GamePlayerEvents.ADD, (player -> active.addPlayer(player, world)));

            game.listen(GameActivityEvents.TICK, active::tick);

            game.listen(BlockBreakEvent.EVENT, active::onBreakBlock);

            game.listen(PlayerDeathEvent.EVENT, active::onPlayerDeath);
            game.listen(EntityDeathEvent.EVENT, active::onEntityDeath);
            game.listen(BlockPlaceEvent.BEFORE, active::onUseBlock);
        });
    }

    private void open() {
        // World border stuff
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(config.borderConfig.startSize);
        world.getWorldBorder().setDamagePerBlock(0.5);
        startTime = world.getTime();

        int index = 0;

        Random random = Random.create();

        double radius = (config.borderConfig.startSize / 2.0);

        double maxSpawnDistance = radius * this.config.noiseGenerator.maxSpawnDistFactor();
        double minSpawnDistance = radius * this.config.noiseGenerator.minSpawnDistFactor();

        for (ServerPlayerEntity player : this.participants) {
            player.networkHandler.sendPacket(new WorldBorderInitializeS2CPacket(world.getWorldBorder()));

            double theta = ((double) index++ / this.participants.size()) * 2 * Math.PI;

            int spawnDistance = (int) MathHelper.lerp(random.nextDouble(), minSpawnDistance, maxSpawnDistance);

            int x = MathHelper.floor(Math.cos(theta) * spawnDistance);
            int z = MathHelper.floor(Math.sin(theta) * spawnDistance);

            this.spawnLogic.resetPlayer(player, GameMode.SURVIVAL);
            this.spawnLogic.spawnPlayerAt(player, x, z, player.getServerWorld());

            for (ItemStack stack : config.kit) {
                player.getInventory().insertStack(stack.copy());
            }
        }
    }

    private void close(GameCloseReason gameCloseReason) {
        // this should hopefully fix players returning as survival mode to the lobby
        for (ServerPlayerEntity player : this.participants) {
            player.interactionManager.changeGameMode(GameMode.SURVIVAL);
        }
    }

    private void addPlayer(ServerPlayerEntity player, ServerWorld world) {
        if (!this.participants.contains(PlayerRef.of(player))) {
            player.networkHandler.sendPacket(new WorldBorderInitializeS2CPacket(player.getWorld().getWorldBorder()));
            this.spawnSpectator(player, world);
        }
    }

    private void tick() {
        if (!this.borderShrinkStarted) {
            long totalSafeTime = config.borderConfig.safeSecs * 20L;
            this.bar.tickSafe(totalSafeTime - (world.getTime() - startTime), totalSafeTime);

            if ((world.getTime() - startTime) > totalSafeTime) {
                this.bar.setActive();
                this.borderShrinkStarted = true;
                this.shrinkStartTime = world.getTime();
                this.participants.sendMessage(Text.literal("The worldborder has started shrinking!").formatted(Formatting.RED));

                world.getWorldBorder().interpolateSize(config.borderConfig.startSize, config.borderConfig.endSize, 1000L * config.borderConfig.shrinkSecs);
                for (ServerPlayerEntity player : this.participants) {
                    player.networkHandler.sendPacket(new WorldBorderInterpolateSizeS2CPacket(world.getWorldBorder()));
                }
            }
        } else {
            long totalShrinkTime = config.borderConfig.shrinkSecs * 20L;

            if ((world.getTime() - shrinkStartTime) > totalShrinkTime || world.getWorldBorder().getSize() == this.config.borderConfig.endSize) {
                if (!this.finished) {
                    this.participants.sendMessage(Text.literal("Last one standing wins!").formatted(Formatting.BLUE));
                    world.getWorldBorder().setDamagePerBlock(2.5);
                    world.getWorldBorder().setSafeZone(0.125);
                    this.bar.setFinished();

                    this.finished = true;
                }
            } else {
                this.bar.tickActive(totalShrinkTime - (world.getTime() - shrinkStartTime), totalShrinkTime);
            }
        }

        long time = this.world.getTime();

        if (time > this.gameCloseTick) {
            this.space.close(GameCloseReason.FINISHED);
        }

        if (time % 20 == 0) {
            this.tracker.iterateRedstoneTracked(this::tickMobSpawners);
        }

        for (var logic : List.copyOf(this.logics)) {
            logic.tick(time);
        }
    }

    private boolean tickMobSpawners(BlockPos pos) {
        if (this.world.isReceivingRedstonePower(pos)) {
            addLogic(new SpawnerLogic(this, pos));
            TargetPredicate pred = TargetPredicate.DEFAULT;
            pred.setPredicate(p -> p instanceof ServerPlayerEntity player && this.participants.contains(player) && player.interactionManager.getGameMode() == GameMode.SURVIVAL);

            PlayerEntity player = this.world.getClosestPlayer(pred, pos.getX(), pos.getY(), pos.getZ());

            if (player != null) {
                this.participants.sendMessage(Text.literal(player.getNameForScoreboard() + " triggered a spawner!").formatted(Formatting.GOLD));
            } else {
                this.participants.sendMessage(Text.literal("A spawner has been triggered!").formatted(Formatting.GOLD));
            }

            return true;
        }
        return false;
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        this.eliminatePlayer(player);
        return ActionResult.FAIL;
    }

    private ActionResult onEntityDeath(Entity entity, DamageSource source) {
        for (ActiveLogic logic : this.logics) {
            ActionResult res = logic.onEntityDeath(entity, source);

            if (res != ActionResult.PASS) {
                return res;
            }
        }


        return ActionResult.PASS;
    }

    private void eliminatePlayer(ServerPlayerEntity player) {
        Text message = player.getDisplayName().copy().append(" has been eliminated!")
                .formatted(Formatting.RED);

        PlayerSet players = this.space.getPlayers();
        players.sendMessage(message);
        players.forEach(p -> p.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1));

        ItemScatterer.spawn(player.getWorld(), player.getBlockPos(), player.getInventory());

        this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);

        int survival = 0;
        for (ServerPlayerEntity participant : this.participants) {
            if (participant.interactionManager.getGameMode().isSurvivalLike()) {
                survival++;
            }
        }

        if (survival == 1) {
            for (ServerPlayerEntity participant : this.participants) {
                if (participant.interactionManager.getGameMode().isSurvivalLike()) {
                    players.sendMessage(Text.literal(participant.getNameForScoreboard() + " won!").formatted(Formatting.GOLD));
                    this.gameCloseTick = this.space.getTime() + (20 * 10);
                    break;
                }
            }
        }
    }

    private void spawnSpectator(ServerPlayerEntity player, ServerWorld world) {
        this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
        this.spawnLogic.spawnPlayerAtCenter(player, world);
    }

    private ActionResult onBreakBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        if (state.isIn(BlockTags.LOGS) && !player.isSneaking()) {
            Set<BlockPos> logs = new HashSet<>();
            logs.add(pos);

            findLogs(world, pos, logs);

            for (BlockPos log : logs) {
                BlockState logState = world.getBlockState(log);
                world.breakBlock(log, false);

                world.spawnEntity(new ItemEntity(world, log.getX(), log.getY(), log.getZ(), new ItemStack(logState.getBlock())));
            }

            return ActionResult.FAIL;
        }

        if (state.isOf(Blocks.SPAWNER)) {
            return ActionResult.FAIL;
        }

        if (state.isOf(Blocks.IRON_ORE)) {
            world.spawnEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.IRON_INGOT)));
            world.breakBlock(pos, false);

            return ActionResult.FAIL;
        }

        if (state.isOf(Blocks.RAW_IRON_BLOCK)) {
            world.spawnEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.IRON_INGOT, 9)));
            world.breakBlock(pos, false);

            return ActionResult.FAIL;
        }

        if (state.isOf(Blocks.COAL_ORE)) {
            world.spawnEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.COAL)));
            world.breakBlock(pos, false);

            return ActionResult.FAIL;
        }

        if (state.isOf(Blocks.ENCHANTING_TABLE)) {
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    private void findLogs(ServerWorld world, BlockPos pos, Set<BlockPos> logs) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -1; y <= 1; y++) {
                    BlockPos local = pos.add(x, y, z);
                    BlockState state = world.getBlockState(local);

                    if (!logs.contains(local)) {
                        if (state.isIn(BlockTags.LOGS)) {
                            logs.add(local);
                            findLogs(world, local, logs);
                        }
                    }
                }
            }
        }
    }

    private ActionResult onUseBlock(ServerPlayerEntity playerEntity, ServerWorld world, BlockPos pos, BlockState state, ItemUsageContext itemUsageContext) {
        if (pos.getY() >= 100) {
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    public ServerWorld getWorld() {
        return world;
    }

    public void addLogic(ActiveLogic logic) {
        this.logics.add(logic);
    }

    public void destroyLogic(ActiveLogic logic) {
        this.logics.remove(logic);
    }
}
