package com.amatis.registry;

import com.amatis.AmatisUpgrade;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/**
 * Materiales de amatista: exactamente 2.9 veces las estadisticas de la netherita.
 *
 * Netherita (vanilla)                     Amatista
 *   herramientas: durabilidad 2031      -> 5890
 *   velocidad de mineria 9              -> 26.1
 *   bonus de dano 4                     -> 11.6
 *   armadura: durabilidad (x) 37        -> 107
 *   puntos casco/peto/pantalon/botas 3/8/6/3 -> 9/23/17/9 (redondeado)
 *   dureza 3                            -> 8.7
 *   resistencia al empuje 0.1           -> 0.29
 * Nota: Minecraft limita la armadura efectiva a 30 y la dureza a 20.
 */
public final class AmatisMaterials {
    public static final double MULT = 2.9;

    public static final TagKey<Item> REPAIR_MATERIALS = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(AmatisUpgrade.MOD_ID, "repair_materials"));

    public static final ResourceKey<EquipmentAsset> ARMOR_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(AmatisUpgrade.MOD_ID, "amatis"));

    /** Bonus de dano del material (netherita = 4.0). */
    public static final float DAMAGE_BONUS = (float) (4.0 * MULT);

    public static final ToolMaterial TOOL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            (int) Math.round(2031 * MULT),
            (float) (9.0 * MULT),
            DAMAGE_BONUS,
            15,
            REPAIR_MATERIALS);

    /** Multiplicador de durabilidad de la armadura (netherita = 37). */
    public static final int ARMOR_BASE_DURABILITY = (int) Math.round(37 * MULT);

    public static final ArmorMaterial ARMOR = new ArmorMaterial(
            ARMOR_BASE_DURABILITY,
            Map.of(
                    ArmorType.BOOTS, (int) Math.round(3 * MULT),
                    ArmorType.LEGGINGS, (int) Math.round(6 * MULT),
                    ArmorType.CHESTPLATE, (int) Math.round(8 * MULT),
                    ArmorType.HELMET, (int) Math.round(3 * MULT),
                    ArmorType.BODY, (int) Math.round(11 * MULT)),
            15,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            (float) (3.0 * MULT),
            (float) (0.1 * MULT),
            REPAIR_MATERIALS,
            ARMOR_ASSET);

    /**
     * Valor que hay que pasar como "attackDamage" a Item.Properties.sword/axe/... para que el dano TOTAL
     * (incluida la base de 1 del jugador) sea {@code totalDamage}. El bonus del material se suma aparte.
     */
    public static float baseFor(double totalDamage) {
        return (float) (totalDamage - 1.0 - DAMAGE_BONUS);
    }

    private AmatisMaterials() {
    }
}
