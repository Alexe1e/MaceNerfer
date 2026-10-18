package com.macenerf;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side mace changes:
 * - 5% less damage (see LivingEntityMixin)
 * - Wind Burst is removed from every mace (other enchantments stay)
 * - 25 second cooldown after every attack with the mace (on players, shields, anything)
 */
public class MaceNerfMod implements ModInitializer {
	public static final String MOD_ID = "macenerf";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Damage multiplier for the mace (0.95 = 5% weaker). */
	public static final float DAMAGE_MULTIPLIER = 0.95f;
	/** Cooldown after an attack in ticks (20 ticks = 1 second, 500 = 25 seconds). */
	public static final int COOLDOWN_TICKS = 25 * 20;
	/** Check inventories for Wind Burst every N ticks. */
	private static final int CHECK_INTERVAL = 10;

	@Override
	public void onInitialize() {
		// Cooldown: block attacks while the mace is on cooldown, start the cooldown on attack.
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}
			ItemStack held = player.getItemInHand(hand);
			if (!held.is(Items.MACE)) {
				return InteractionResult.PASS;
			}
			if (player.getCooldowns().isOnCooldown(held)) {
				return InteractionResult.FAIL;
			}
			player.getCooldowns().addCooldown(held, COOLDOWN_TICKS);
			return InteractionResult.PASS;
		});

		// Wind Burst: remove it from every mace a player carries.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_INTERVAL != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Inventory inventory = player.getInventory();
				for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
					removeWindBurst(inventory.getItem(slot));
				}
				removeWindBurst(player.containerMenu.getCarried());
			}
		});

		LOGGER.info("Mace Nerf loaded: -5% damage, no Wind Burst, 25s cooldown");
	}

	private static void removeWindBurst(ItemStack stack) {
		if (stack.isEmpty() || !stack.is(Items.MACE)) {
			return;
		}
		ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
		if (enchantments == null) {
			return;
		}
		boolean hasWindBurst = enchantments.keySet().stream().anyMatch(holder -> holder.is(Enchantments.WIND_BURST));
		if (hasWindBurst) {
			EnchantmentHelper.updateEnchantments(stack,
					mutable -> mutable.removeIf(holder -> holder.is(Enchantments.WIND_BURST)));
		}
	}
}
