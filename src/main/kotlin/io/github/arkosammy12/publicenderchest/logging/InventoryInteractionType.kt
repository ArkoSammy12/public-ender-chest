package io.github.arkosammy12.publicenderchest.logging

import net.minecraft.util.StringIdentifiable

enum class InventoryInteractionType(val string: String) : StringIdentifiable {
    ITEM_REMOVE("item_remove"),
    ITEM_INSERT("item_insert");

    override fun asString(): String = this.string

}