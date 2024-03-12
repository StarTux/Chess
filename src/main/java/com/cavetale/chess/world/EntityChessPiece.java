package com.cavetale.chess.world;

import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import static com.cavetale.mytems.MytemsPlugin.namespacedKey;

public interface EntityChessPiece extends WorldChessPiece {
    List<Entity> getEntities();

    @Override
    default void remove() {
        for (var entity : getEntities()) {
            entity.remove();
        }
    }

    static void markAsChessPiece(Entity entity) {
        entity.getPersistentDataContainer().set(namespacedKey("ChessPiece"), PersistentDataType.BOOLEAN, true);
    }

    static boolean isChessPiece(Entity entity) {
        return entity.getPersistentDataContainer().has(namespacedKey("ChessPiece"));
    }
}
