package com.amatis.events;

import com.amatis.config.AmatisConfig;
import com.amatis.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Recetas de yunque del mod. Fabric no tiene evento de yunque, asi que AnvilMenuMixin
 * llama a esto cuando cambian los ingredientes.
 */
public final class AnvilLogic {

    public record Result(ItemStack output, int xpCost, int materialCost) {
    }

    /** @return el resultado si los ingredientes forman una receta del mod; si no, null (yunque vanilla). */
    public static Result compute(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) return null;
        AmatisConfig cfg = AmatisConfig.get();

        // 1) plantilla de netherita + fragmento de amatista = plantilla de amatista
        if (left.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE) && left.getCount() == 1
                && right.is(Items.AMETHYST_SHARD)) {
            return new Result(new ItemStack(ModItems.TEMPLATE), cfg.xpTemplate, 1);
        }

        // 2) amatista resonante descargada + aliento de dragon = fragmentos sobrecargados
        if (left.is(ModItems.DISCHARGED) && left.getCount() == 1 && right.is(Items.DRAGON_BREATH)) {
            return new Result(new ItemStack(ModItems.OVERCHARGED, cfg.fragmentsPerCraft), cfg.xpFragments, 1);
        }

        // 3) espada de netherita con Filo V + 3 fragmentos sobrecargados = espada pesada
        if (left.is(Items.NETHERITE_SWORD) && right.is(ModItems.OVERCHARGED)
                && right.getCount() >= cfg.heavySwordFragments
                && sharpnessLevel(left) >= cfg.heavySwordMinSharpness) {
            ItemStack out = new ItemStack(ModItems.HEAVY_SWORD);
            out.set(DataComponents.ENCHANTMENTS, left.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
            return new Result(out, cfg.xpHeavySword, cfg.heavySwordFragments);
        }
        return null;
    }

    static int sharpnessLevel(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            if (holder.is(Enchantments.SHARPNESS)) {
                return enchantments.getLevel(holder);
            }
        }
        return 0;
    }

    private AnvilLogic() {
    }
}
