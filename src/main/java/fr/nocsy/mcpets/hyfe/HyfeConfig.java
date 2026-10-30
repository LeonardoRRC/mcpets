package fr.nocsy.mcpets.hyfe;

import fr.nocsy.mcpets.MCPets;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Ajustes del fork de HyfeCraft (plugins/MCPets/hyfe.yml): niveles automaticos, combate de las
 * mascotas y arreglos del seguimiento. Las claves que falten se escriben con su valor por defecto.
 */
public final class HyfeConfig {

    private static YamlConfiguration cfg = new YamlConfiguration();

    private HyfeConfig() {
    }

    public static void load() {
        final File file = new File(MCPets.getInstance().getDataFolder(), "hyfe.yml");
        final YamlConfiguration y = YamlConfiguration.loadConfiguration(file);

        def(y, "niveles.auto", true);
        def(y, "niveles.cantidad", 30);
        def(y, "niveles.xp-base", 50);
        def(y, "niveles.anuncio", "§a¡Tu mascota subió al §enivel %nivel%§a!");
        def(y, "niveles.anuncio-tipo", "TITLE");

        def(y, "combate.activado", true);
        def(y, "combate.dano-base", 3.0);
        def(y, "combate.dano-por-nivel", 0.25);
        def(y, "combate.alcance", 2.5);
        def(y, "combate.cooldown-ticks", 20);
        def(y, "combate.seguir-segundos", 10);
        def(y, "combate.distancia-max-dueno", 16.0);
        def(y, "combate.atacar-jugadores", true);
        def(y, "combate.atacar-mobs", true);
        def(y, "combate.skill-ataque", "");
        def(y, "combate.xp-por-dano", 1.0);
        def(y, "combate.xp-kill-mob", 2.0);
        def(y, "combate.xp-kill-jugador", 10.0);

        def(y, "seguir.atascado-segundos", 3);
        def(y, "seguir.teleport-sin-suelo", true);

        try {
            y.save(file);
        } catch (IOException e) {
            MCPets.getInstance().getLogger().warning("No se pudo guardar hyfe.yml: " + e.getMessage());
        }
        cfg = y;
    }

    private static void def(final YamlConfiguration y, final String path, final Object value) {
        if (!y.contains(path)) y.set(path, value);
    }

    public static boolean bool(final String path) {
        return cfg.getBoolean(path);
    }

    public static int integer(final String path) {
        return cfg.getInt(path);
    }

    public static double number(final String path) {
        return cfg.getDouble(path);
    }

    public static String text(final String path) {
        return cfg.getString(path, "");
    }
}
