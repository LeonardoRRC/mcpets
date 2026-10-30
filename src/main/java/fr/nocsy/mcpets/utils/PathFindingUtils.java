package fr.nocsy.mcpets.utils;

import fr.nocsy.mcpets.MCPets;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.adapters.AbstractLocation;

import java.util.HashMap;
import java.util.UUID;

public class PathFindingUtils {

    private static HashMap<UUID, AbstractLocation> registry = new HashMap<>();

    /**
     * Move the entity to the specified location
     */
    public static void moveTo(AbstractEntity entity, AbstractLocation destination) {
        moveTo(entity, destination, fr.nocsy.mcpets.hyfe.HyfeConfig.number("seguir.velocidad"));
    }

    /**
     * Fork HyfeCraft: con el pathfinder de Paper se puede dar velocidad (multiplicador sobre el
     * MovementSpeed del mob); el de MythicMobs iba siempre a velocidad 1 y las mascotas iban lentas.
     */
    public static void moveTo(AbstractEntity entity, AbstractLocation destination, double velocidad) {
        if (entity.getBukkitEntity() instanceof org.bukkit.entity.Mob mob && velocidad > 0) {
            mob.getPathfinder().moveTo(new org.bukkit.Location(mob.getWorld(),
                    destination.getX(), destination.getY(), destination.getZ()), velocidad);
            return;
        }
        MCPets.getMythicMobs().getVolatileCodeHandler().getAIHandler().navigateToLocation(entity, destination, 1);
    }

    /**
     * Stop the entity at its location
     */
    public static void stop(AbstractEntity entity, UUID owner) {
        if (registry.get(owner) != null) {
            AbstractLocation loc = registry.get(owner);
            if (loc.getBlockX() == entity.getLocation().getBlockX() &&
                    loc.getBlockY() == entity.getLocation().getBlockY() &&
                    loc.getBlockZ() == entity.getLocation().getBlockZ()) {
                return;
            }
        }

        if (entity.getBukkitEntity() instanceof org.bukkit.entity.Mob mob) {
            mob.getPathfinder().stopPathfinding();
        } else {
            moveTo(entity, entity.getLocation());
        }
        registry.put(owner, entity.getLocation());
    }
}
