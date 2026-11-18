package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class MaterialGameTests {
    @GameTest(template = "machine_test")
    public static void netherBrickLavaConvertsGoldIntoDemonIngots(GameTestHelper test) {
        var lava = new BlockPos(3, 2, 3);
        var openLava = new BlockPos(6, 2, 3);
        for (var pos : List.of(lava, openLava)) {
            test.setBlock(pos.below(), Blocks.STONE);
            for (var side : Direction.Plane.HORIZONTAL) {
                var wall = pos.equals(openLava) && side == Direction.NORTH ? Blocks.STONE : Blocks.NETHER_BRICKS;
                test.setBlock(pos.relative(side), wall);
            }
            test.setBlock(pos.relative(Direction.SOUTH), Blocks.NETHER_BRICK_FENCE);
            test.setBlock(pos, Blocks.LAVA);
        }
        var gold = test.spawnItem(Items.GOLD_INGOT, 3.5f, 3.5f, 3.5f);
        gold.setItem(new ItemStack(Items.GOLD_INGOT, 9));
        gold.setDeltaMovement(0, -0.12, 0);
        var unmatchedGold = test.spawnItem(Items.GOLD_INGOT, 6.5f, 3.5f, 3.5f);
        unmatchedGold.setDeltaMovement(0, -0.12, 0);
        test.runAfterDelay(28, () -> {
            var demon = ExtrautilitiesrebirthModItems.DEMON_INGOT.get();
            var ingots = test.getEntities(EntityType.ITEM, lava, 2);
            test.assertTrue(ingots.size() == 1 && ingots.get(0).getItem().is(demon)
                    && ingots.get(0).getItem().getCount() == 9, "All nine gold ingots should turn into lava-resistant demon ingots. Found " + ingots.stream().map(i -> i.getItem().toString()).toList()
                            + " at " + gold.position() + ", alive=" + gold.isAlive() + ", expected " + test.absolutePos(lava));
            test.assertTrue(test.getEntities(EntityType.ITEM, openLava, 2).stream().noneMatch(item -> item.getItem().is(demon)),
                    "A pool with a stone wall must not convert gold");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void bothRedstoneOresCanDropCrystalsThroughNormalLoot(GameTestHelper test) {
        var level = test.getLevel();
        var player = test.makeMockSurvivalPlayer();
        var pos = test.absolutePos(new BlockPos(3, 2, 3));
        var crystal = ExtrautilitiesrebirthModItems.REDSTONE_CRYSTAL.get();
        var ores = List.of(Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DIAMOND_ORE);
        for (var ore : ores) {
            int crystals = 0;
            for (int attempt = 0; attempt < 192; attempt++) {
                var drops = Block.getDrops(ore.defaultBlockState(), level, pos, null, player, new ItemStack(Items.IRON_PICKAXE));
                crystals += drops.stream().filter(stack -> stack.is(crystal)).mapToInt(ItemStack::getCount).sum();
                test.assertTrue(Block.getDrops(ore.defaultBlockState(), level, pos, null, player, ItemStack.EMPTY)
                        .stream().noneMatch(stack -> stack.is(crystal)), "Breaking ore without a tool should grant no crystal");
            }
            test.assertTrue(ore == Blocks.DIAMOND_ORE ? crystals == 0 : crystals > 0,
                    "Only the two types of redstone ore should drop redstone crystals: " + ore.getName().getString());
        }
        test.succeed();
    }
}
