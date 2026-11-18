package inzhefop.extrautilitiesrebirth.item;

import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;

@Mod.EventBusSubscriber(modid = ExtrautilitiesrebirthMod.MODID)
public class GoldenLassoItem extends Item {
	private static final String CAPTURED_MOB = "CapturedMob";
	private static final int EMPTY_STACK_SIZE = 64;

	public GoldenLassoItem() {
		super(new Item.Properties().stacksTo(EMPTY_STACK_SIZE));
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return capturedMob(stack) == null ? EMPTY_STACK_SIZE : 1;
	}

	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		ItemStack stack = event.getItemStack();
		if (stack.getItem() instanceof GoldenLassoItem && capturedMob(stack) == null
				&& event.getTarget() instanceof Mob mob) {
			// Handle the lasso before interactions such as trading or mounting.
			InteractionResult result = capture(event.getEntity(), event.getHand(), stack, mob);
			if (result.consumesAction()) {
				event.setCancellationResult(result);
				event.setCanceled(true);
			}
		}
	}

	private static InteractionResult capture(Player player, InteractionHand hand, ItemStack stack, Mob mob) {
		Level level = player.level();
		if (!mob.isAlive() || mob instanceof Enemy || mob.getTarget() != null || mob.isLeashed()
				|| mob.isPassenger() || mob.isVehicle() || !level.mayInteract(player, mob.blockPosition())
				|| mob instanceof AbstractVillager villager && villager.isTrading()
				|| mob instanceof OwnableEntity owned && owned.getOwnerUUID() != null
				&& !owned.getOwnerUUID().equals(player.getUUID())) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}

		CompoundTag saved = new CompoundTag();
		if (!mob.save(saved)) {
			return InteractionResult.PASS;
		}
		ItemStack filled = stack.getCount() == 1 ? stack : stack.split(1);
		filled.addTagElement(CAPTURED_MOB, saved);
		if (filled != stack) {
			player.setItemInHand(hand, filled);
			player.getInventory().placeItemBackInInventory(stack);
		}
		mob.discard();
		level.gameEvent(player, GameEvent.ENTITY_INTERACT, mob.position());
		return InteractionResult.CONSUME;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		ItemStack stack = context.getItemInHand();
		CompoundTag saved = capturedMob(stack);
		Player player = context.getPlayer();
		if (saved == null || player == null) {
			return InteractionResult.PASS;
		}
		Level level = context.getLevel();
		BlockPos clicked = context.getClickedPos();
		BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()
				? clicked : clicked.relative(context.getClickedFace());
		if (!level.isInWorldBounds(pos) || !level.mayInteract(player, pos)
				|| !player.mayUseItemAt(pos, context.getClickedFace(), stack)) {
			return InteractionResult.FAIL;
		}
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}

		Entity entity = EntityType.create(saved, level).orElse(null);
		if (!(entity instanceof Mob mob) || mob instanceof Enemy || !mob.isAlive()) {
			return InteractionResult.FAIL;
		}
		Vec3 feet = Vec3.atBottomCenterOf(pos);
		mob.moveTo(feet.x, feet.y, feet.z, context.getRotation(), 0);
		mob.setDeltaMovement(Vec3.ZERO);
		mob.fallDistance = 0;
		if (!level.getWorldBorder().isWithinBounds(mob.getBoundingBox()) || !level.noCollision(mob)
				|| !level.addFreshEntity(mob)) {
			return InteractionResult.FAIL;
		}
		stack.removeTagKey(CAPTURED_MOB);
		level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
		return InteractionResult.CONSUME;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
		CompoundTag saved = capturedMob(stack);
		if (saved != null) {
			EntityType.by(saved).ifPresent(type -> lines.add(Component.translatable(
					"item.extrautilitiesrebirth.golden_lasso.captured", type.getDescription())
					.withStyle(ChatFormatting.GRAY)));
		}
	}

	@Nullable
	private static CompoundTag capturedMob(ItemStack stack) {
		return stack.getTagElement(CAPTURED_MOB);
	}
}
