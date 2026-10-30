package fr.nocsy.mcpets.hyfe;

import fr.nocsy.mcpets.MCPets;
import fr.nocsy.mcpets.data.Pet;
import fr.nocsy.mcpets.data.livingpets.PetStats;
import fr.nocsy.mcpets.utils.PathFindingUtils;
import fr.nocsy.mcpets.utils.Utils;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Combate de las mascotas (fork de HyfeCraft): cuando el dueño golpea a un jugador o a un mob, sus
 * mascotas activas van a por el. El daño es combate.dano-base mas combate.dano-por-nivel por cada
 * nivel por encima del 1 (va en el DamageModifier del nivel, ver HyfeNiveles), y la mascota gana XP
 * por el daño que hace y por rematar.
 *
 * <p>Solo se ataca si el golpe del dueño no fue cancelado: donde el dueño no puede pegar (spawn,
 * claims, PvP apagado), la mascota tampoco.</p>
 */
public final class HyfeCombate implements Listener {

    private static final class Objetivo {
        LivingEntity entidad;
        long hasta;
        long proximoGolpe;
    }

    private static final Map<Pet, Objetivo> OBJETIVOS = new WeakHashMap<>();

    // ---- el dueño golpea: sus mascotas toman el objetivo ----

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGolpeDueno(final EntityDamageByEntityEvent e) {
        if (!HyfeConfig.bool("combate.activado")) return;
        final Player dueno = atacante(e.getDamager());
        if (dueno == null || !(e.getEntity() instanceof LivingEntity victima)) return;
        if (!valido(dueno, victima)) return;

        for (final Pet pet : Pet.getActivePetsForOwner(dueno.getUniqueId())) {
            if (pet.isStillHere()) {
                final Objetivo o = OBJETIVOS.computeIfAbsent(pet, k -> new Objetivo());
                o.entidad = victima;
                o.hasta = System.currentTimeMillis() + HyfeConfig.integer("combate.seguir-segundos") * 1000L;
            }
        }
    }

    private static Player atacante(final Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile pr && pr.getShooter() instanceof Player p) return p;
        return null;
    }

    private static boolean valido(final Player dueno, final LivingEntity victima) {
        if (victima.equals(dueno) || victima instanceof ArmorStand) return false;
        if (Pet.getFromEntity(victima) != null) return false;
        // NPCs de Citizens y similares
        if (victima.hasMetadata("NPC")) return false;
        if (victima instanceof Player) return HyfeConfig.bool("combate.atacar-jugadores");
        return HyfeConfig.bool("combate.atacar-mobs");
    }

    // ---- cada medio segundo, desde la IA de la mascota ----

    /**
     * Persigue y golpea al objetivo. Devuelve true si la mascota esta en combate (entonces la IA no
     * la hace seguir al dueño en ese ciclo).
     */
    public static boolean tick(final Pet pet, final Player dueno, final ActiveMob activeMob) {
        final Objetivo o = OBJETIVOS.get(pet);
        if (o == null) return false;

        final LivingEntity t = o.entidad;
        final Entity petEnt = activeMob.getEntity().getBukkitEntity();
        final long ahora = System.currentTimeMillis();
        if (t == null || !t.isValid() || t.isDead() || ahora > o.hasta
                || !t.getWorld().equals(petEnt.getWorld())
                || Utils.distance(t.getLocation(), dueno.getLocation()) > HyfeConfig.number("combate.distancia-max-dueno")
                || (t instanceof Player tp && !tp.isOnline())) {
            OBJETIVOS.remove(pet);
            return false;
        }

        final Location destino = t.getLocation();
        if (petEnt instanceof Mob mob) mob.lookAt(t);

        if (Utils.distance(petEnt.getLocation(), destino) > HyfeConfig.number("combate.alcance")) {
            PathFindingUtils.moveTo(activeMob.getEntity(),
                    new AbstractLocation(activeMob.getEntity().getWorld(), destino.getX(), destino.getY(), destino.getZ()),
                    HyfeConfig.number("combate.velocidad"));
            return true;
        }

        if (ahora >= o.proximoGolpe) {
            o.proximoGolpe = ahora + HyfeConfig.integer("combate.cooldown-ticks") * 50L;
            o.hasta = Math.max(o.hasta, ahora + 3000L);
            final String skill = HyfeConfig.text("combate.skill-ataque");
            if (!skill.isEmpty()) {
                try {
                    MCPets.getMythicMobs().getAPIHelper().castSkill(petEnt, skill);
                } catch (final Exception ignored) {
                    // skill inexistente: solo se pierde la animacion
                }
            }
            animar(petEnt);
            petEnt.getWorld().playSound(petEnt.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.1f);
            t.damage(dano(pet), petEnt);
        }
        return true;
    }

    /** Animacion de ataque de ModelEngine (combate.animaciones), si el modelo tiene una configurada. */
    private static void animar(final Entity petEnt) {
        try {
            final com.ticxo.modelengine.api.model.ModeledEntity me =
                    com.ticxo.modelengine.api.ModelEngineAPI.getModeledEntity(petEnt.getUniqueId());
            if (me == null) return;
            me.getModels().forEach((id, modelo) -> {
                final String anim = HyfeConfig.animacion(id);
                if (!anim.isEmpty()) modelo.getAnimationHandler().playAnimation(anim, 0.1, 0.1, 1.0, true);
            });
        } catch (final Throwable ignored) {
            // otra version de ModelEngine: se pega sin animacion
        }
    }

    /** Daño del golpe de la mascota segun su nivel. */
    public static double dano(final Pet pet) {
        final PetStats stats = pet.getPetStats();
        final double base = HyfeConfig.number("combate.dano-base");
        return stats != null ? base * stats.getDamageModifier() : base;
    }

    /** ¿Esta la mascota peleando ahora? (para no teletransportarla por "atascada"). */
    public static boolean enCombate(final Pet pet) {
        return OBJETIVOS.containsKey(pet);
    }

    // ---- experiencia por pelear ----

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGolpeMascota(final EntityDamageByEntityEvent e) {
        final Pet pet = Pet.getFromEntity(e.getDamager());
        if (pet == null || pet.getPetStats() == null) return;
        final double xp = e.getFinalDamage() * HyfeConfig.number("combate.xp-por-dano");
        if (xp > 0) pet.getPetStats().addExperience(xp);
    }

    @EventHandler
    public void onMuerte(final EntityDeathEvent e) {
        if (!(e.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent ultimo)) return;
        final Pet pet = Pet.getFromEntity(ultimo.getDamager());
        if (pet == null || pet.getPetStats() == null) return;
        final double xp = HyfeConfig.number(e.getEntity() instanceof Player ? "combate.xp-kill-jugador" : "combate.xp-kill-mob");
        if (xp > 0) pet.getPetStats().addExperience(xp);
        OBJETIVOS.remove(pet);
    }
}
