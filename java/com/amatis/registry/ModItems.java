package com.amatis.registry;

import com.amatis.AmatisUpgrade;
import com.amatis.item.AmatisShieldItem;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.List;
import java.util.function.Function;

public final class ModItems {

    // ---------------------------------------------------------------- materiales de crafteo
    public static final Item TEMPLATE = register("amatis_upgrade_template", Item::new,
            lore(new Item.Properties(), "tooltip.amatis_upgrade.template"));
    public static final Item DISCHARGED = register("discharged_resonant_amethyst", Item::new, new Item.Properties());
    public static final Item OVERCHARGED = register("overcharged_resonant_fragment", Item::new, new Item.Properties());

    // ---------------------------------------------------------------- herramientas (netherita x2.9)
    // Dano total de la netherita: espada 8, hacha 10, pico 6, pala 6.5, azada 1. Se multiplica x2.9.
    public static final Item SWORD = register("amatis_sword", Item::new,
            new Item.Properties().sword(AmatisMaterials.TOOL, AmatisMaterials.baseFor(8.0 * AmatisMaterials.MULT), -2.4F));
    public static final Item AXE = register("amatis_axe",
            p -> new AxeItem(AmatisMaterials.TOOL, AmatisMaterials.baseFor(10.0 * AmatisMaterials.MULT), -3.0F, p),
            new Item.Properties());
    public static final Item PICKAXE = register("amatis_pickaxe", Item::new,
            new Item.Properties().pickaxe(AmatisMaterials.TOOL, AmatisMaterials.baseFor(6.0 * AmatisMaterials.MULT), -2.8F));
    public static final Item SHOVEL = register("amatis_shovel",
            p -> new ShovelItem(AmatisMaterials.TOOL, AmatisMaterials.baseFor(6.5 * AmatisMaterials.MULT), -3.0F, p),
            new Item.Properties());
    public static final Item HOE = register("amatis_hoe",
            p -> new HoeItem(AmatisMaterials.TOOL, AmatisMaterials.baseFor(1.0 * AmatisMaterials.MULT), 0.0F, p),
            new Item.Properties());

    // ---------------------------------------------------------------- armas nuevas
    /** Espada pesada: dano = x3 el de un hacha de netherita (10 -> 30). Lenta como un hacha. */
    public static final Item HEAVY_SWORD = register("amatis_heavy_sword", Item::new,
            lore(new Item.Properties(), "tooltip.amatis_upgrade.heavy")
                    .sword(AmatisMaterials.TOOL, AmatisMaterials.baseFor(10.0 * 3.0), -3.0F));
    /** Daga: rapida, mismo dano que una espada de netherita (8). */
    public static final Item DAGGER = register("amatis_dagger", Item::new,
            lore(new Item.Properties(), "tooltip.amatis_upgrade.dagger")
                    .sword(AmatisMaterials.TOOL, AmatisMaterials.baseFor(8.0), -1.0F));
    /** Guadana: dano = x2.9 el de un tridente (9 -> 26.1). */
    public static final Item SCYTHE = register("amatis_scythe", Item::new,
            lore(new Item.Properties(), "tooltip.amatis_upgrade.scythe")
                    .sword(AmatisMaterials.TOOL, AmatisMaterials.baseFor(9.0 * AmatisMaterials.MULT), -2.9F));

    // ---------------------------------------------------------------- escudo
    public static final Item SHIELD = register("amatis_shield", AmatisShieldItem::new,
            lore(new Item.Properties(), "tooltip.amatis_upgrade.shield")
                    .durability((int) Math.round(336 * AmatisMaterials.MULT)));

    // ---------------------------------------------------------------- armadura
    public static final Item HELMET = register("amatis_helmet", Item::new, armor(ArmorType.HELMET));
    public static final Item CHESTPLATE = register("amatis_chestplate", Item::new, armor(ArmorType.CHESTPLATE));
    public static final Item LEGGINGS = register("amatis_leggings", Item::new, armor(ArmorType.LEGGINGS));
    public static final Item BOOTS = register("amatis_boots", Item::new, armor(ArmorType.BOOTS));

    // ---------------------------------------------------------------- pestana creativa propia
    public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(),
            Identifier.fromNamespaceAndPath(AmatisUpgrade.MOD_ID, "amatis_upgrade"));

    public static final CreativeModeTab TAB = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(SCYTHE))
            .title(Component.translatable("creativeTab.amatis_upgrade"))
            .displayItems((params, output) -> {
                // materiales de crafteo
                output.accept(TEMPLATE);
                output.accept(DISCHARGED);
                output.accept(OVERCHARGED);
                // armas
                output.accept(SWORD);
                output.accept(HEAVY_SWORD);
                output.accept(DAGGER);
                output.accept(SCYTHE);
                // herramientas
                output.accept(PICKAXE);
                output.accept(AXE);
                output.accept(SHOVEL);
                output.accept(HOE);
                // escudo y armadura
                output.accept(SHIELD);
                output.accept(HELMET);
                output.accept(CHESTPLATE);
                output.accept(LEGGINGS);
                output.accept(BOOTS);
            })
            .build();

    // ----------------------------------------------------------------

    private static Item.Properties armor(ArmorType type) {
        return new Item.Properties()
                .humanoidArmor(AmatisMaterials.ARMOR, type)
                .durability(type.getDurability(AmatisMaterials.ARMOR_BASE_DURABILITY));
    }

    private static Item.Properties lore(Item.Properties props, String translationKey) {
        return props.component(DataComponents.LORE,
                new ItemLore(List.of(Component.translatable(translationKey).withStyle(ChatFormatting.LIGHT_PURPLE))));
    }

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties props) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(AmatisUpgrade.MOD_ID, name));
        T item = factory.apply(props.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    /** Fuerza la carga de la clase (registra todos los items) y registra la pestana creativa. */
    public static void init() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, TAB);
    }

    private ModItems() {
    }
}
