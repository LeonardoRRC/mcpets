package fr.nocsy.mcpets.hyfe;

import fr.nocsy.mcpets.data.Pet;
import fr.nocsy.mcpets.data.config.GlobalConfig;
import fr.nocsy.mcpets.utils.PetAnnouncement;
import fr.nocsy.mcpets.data.livingpets.PetLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Niveles automaticos (fork de HyfeCraft): las mascotas sin "Levels" en su YAML reciben
 * niveles.cantidad niveles. El nivel N pide xp-base * (N-1) * N / 2 de experiencia (100, 300, 600...
 * con xp-base 100) y pega combate.dano-base + (N-1) * combate.dano-por-nivel, guardado como
 * DamageModifier sobre el daño base. Una mascota con Levels propios en su YAML no se toca.
 */
public final class HyfeNiveles {

    private HyfeNiveles() {
    }

    public static List<PetLevel> generar(final Pet pet) {
        final List<PetLevel> niveles = new ArrayList<>();
        final int cantidad = Math.max(1, HyfeConfig.integer("niveles.cantidad"));
        final double xpBase = Math.max(1, HyfeConfig.number("niveles.xp-base"));
        final double base = Math.max(0.1, HyfeConfig.number("combate.dano-base"));
        final double porNivel = HyfeConfig.number("combate.dano-por-nivel");
        final String anuncio = HyfeConfig.text("niveles.anuncio");
        final PetAnnouncement tipo = PetAnnouncement.get(HyfeConfig.text("niveles.anuncio-tipo"));

        for (int n = 1; n <= cantidad; n++) {
            final double umbral = xpBase * (n - 1) * n / 2.0;
            final double modificador = (base + (n - 1) * porNivel) / base;
            niveles.add(new PetLevel(pet,
                    "hyfe-" + n,
                    null,
                    0,
                    true,
                    100,
                    1,
                    1,
                    modificador,
                    1,
                    GlobalConfig.getInstance().getDefaultRespawnCooldown(),
                    0,
                    0,
                    String.valueOf(n),
                    umbral,
                    n == 1 || anuncio.isEmpty() ? null : anuncio.replace("%nivel%", String.valueOf(n)),
                    tipo,
                    null));
        }
        return niveles;
    }
}
