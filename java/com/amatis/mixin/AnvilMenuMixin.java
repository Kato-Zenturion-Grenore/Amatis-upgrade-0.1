package com.amatis.mixin;

import com.amatis.events.AnvilLogic;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Anade las recetas de yunque del mod. Si los ingredientes no coinciden con ninguna,
 * no hace nada y el yunque vanilla funciona igual que siempre.
 * (Minecraft 26.x no esta ofuscado, asi que los nombres son los oficiales de Mojang.)
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow
    private int repairItemCountCost;

    @Shadow
    @Final
    private DataSlot cost;

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void amatis$createResult(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack left = menu.getSlot(0).getItem();
        ItemStack right = menu.getSlot(1).getItem();

        AnvilLogic.Result result = AnvilLogic.compute(left, right);
        if (result == null) {
            return;
        }

        menu.getSlot(2).set(result.output());
        this.cost.set(result.xpCost());
        this.repairItemCountCost = result.materialCost();
        menu.broadcastChanges();
        ci.cancel();
    }
}
