package io.github.arkosammy12.publicenderchest.util.ducks;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemStackDuck {

    void publicenderchest$setInserterName(Text name);

    @Nullable
    Text publicenderchest$getInserterName();

    void publicenderchest$setInsertedTime(LocalDateTime time);

    @Nullable
    LocalDateTime publicenderchest$getInsertedTime();

    void publicenderchest$addCustomInfoLine(Text text);

    List<Text> publicenderchest$getCustomInfoLines();

}
