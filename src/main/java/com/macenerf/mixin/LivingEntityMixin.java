package com.macenerf.mixin;

import com.macenerf.MaceNerfMod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	/** Reduces the damage of attacks made with a mace by 5%. */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float macenerf$reduceMaceDamage(float amount, ServerLevel level, DamageSource source, float originalAmount) {
		ItemStack weapon = source.getWeaponItem();
		if (weapon != null && weapon.is(Items.MACE)) {
			return amount * MaceNerfMod.DAMAGE_MULTIPLIER;
		}
		return amount;
	}
}
