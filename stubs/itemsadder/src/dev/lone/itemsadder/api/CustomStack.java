package dev.lone.itemsadder.api;

import org.bukkit.inventory.ItemStack;

/**
 * Solo para compilar: el repositorio de ItemsAdder ya no publica api-itemsadder 4.0.10. En el
 * servidor se usa la clase real del plugin ItemsAdder (la dependencia es "provided").
 */
public class CustomStack {

    public static CustomStack getInstance(String namespacedId) {
        throw new UnsupportedOperationException();
    }

    public static CustomStack byItemStack(ItemStack itemStack) {
        throw new UnsupportedOperationException();
    }

    public ItemStack getItemStack() {
        throw new UnsupportedOperationException();
    }

    public String getNamespacedID() {
        throw new UnsupportedOperationException();
    }
}
