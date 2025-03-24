package io.github.arkosammy12.publicenderchest.serialization

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.text.TextCodecs
import io.github.arkosammy12.publicenderchest.util.ducks.ItemStackDuck
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional

class SerializedItemStack(val slotIndex: Int, inputItemStack: ItemStack) {

    val inserterName: Text? = (inputItemStack as ItemStackDuck).`publicenderchest$getInserterName`()
    val insertedTime: LocalDateTime? = (inputItemStack as ItemStackDuck).`publicenderchest$getInsertedTime`()

    val itemStack: ItemStack

    init {
        if (inputItemStack.isEmpty) {
            throw IllegalArgumentException("Cannot serialize empty item stack!")
        }
        itemStack = inputItemStack
    }

    companion object {

        val CODEC: Codec<SerializedItemStack> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("slot_index").forGetter(SerializedItemStack::slotIndex),
                ItemStack.CODEC.fieldOf("item_stack").forGetter(SerializedItemStack::itemStack),
                TextCodecs.CODEC.optionalFieldOf("inserter_name").forGetter { serializedStack -> Optional.ofNullable(serializedStack.inserterName) },
                Codec.LONG.optionalFieldOf("inserted_time").forGetter { serializedStack -> Optional.ofNullable(serializedStack.insertedTime?.toEpochSecond(ZoneOffset.UTC)) }
            ).apply(instance) { slotIndex, itemStack, inserterName, insertedTime ->
                (itemStack as ItemStackDuck).`publicenderchest$setInserterName`(inserterName.orElse(null))
                if (insertedTime.isPresent) {
                    (itemStack as ItemStackDuck).`publicenderchest$setInsertedTime`(LocalDateTime.ofEpochSecond(insertedTime.get(), 0, ZoneOffset.UTC))
                }
                SerializedItemStack(slotIndex, itemStack)
            }
        }

    }

}
