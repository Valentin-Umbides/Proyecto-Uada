package uada;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Ejecuta la misión y guarda el log de exploración.
 *
 * Uso:
 *   java -cp out uada.Main mapas/mapa_uade.txt        -> mapa leído de archivo (el que den los profes)
 *   java -cp out uada.Main mapas/mapa_uade.txt sincota -> mismo, sin poda por cota (para verificar)
 *   java -cp out uada.Main mapa.txt nodirigido         -> fuerza grafo no dirigido (o "dirigido")
 *   java -cp out uada.Main base | imposible | chico    -> mapas de ejemplo cargados en código
 */
public class Main {
    public static void main(String[] args) throws IOException {
        String entrada = args.length > 0 ? args[0] : "base";
        boolean usarCota = true;
        Boolean dirigido = null;   // null = lo que diga el archivo
        for (int i = 1; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "sincota" -> usarCota = false;
                case "dirigido" -> dirigido = true;
                case "nodirigido" -> dirigido = false;
                default -> System.out.println("Opción ignorada: " + args[i]);
            }
        }

        Mapa mapa;
        String origen = "A348";
        String nombre;
        if (entrada.toLowerCase().endsWith(".txt")) {
            try {
                CargadorMapa.Resultado r = CargadorMapa.cargar(entrada, dirigido);
                mapa = r.mapa;
                origen = r.origen;
            } catch (java.nio.file.NoSuchFileException ex) {
                System.out.println("No se encontró el archivo: " + entrada);
                return;
            } catch (IllegalArgumentException ex) {
                System.out.println("Error en el archivo de mapa -> " + ex.getMessage());
                return;
            }
            nombre = entrada.replaceAll(".*[/\\\\]", "").replace(".txt", "");
        } else {
            mapa = switch (entrada) {
                case "imposible" -> Escenarios.uadeImposible();
                case "chico" -> Escenarios.uadeChico();
                default -> Escenarios.uade();
            };
            nombre = entrada;
        }
        String archivoLog = "log_" + nombre + (usarCota ? "" : "_sincota") + ".txt";

        try (PrintWriter log = new PrintWriter(new FileWriter(archivoLog, StandardCharsets.UTF_8))) {
            log.println("LOG DE EXPLORACIÓN - mapa '" + nombre + "'" + (usarCota ? "" : " (sin cota)")
                    + " - grafo " + (mapa.isDirigido() ? "dirigido" : "no dirigido"));
            log.println("Formato: [#nodo] origen -> destino (modo) t=tiempo acumulado bat=batería restante");
            log.println("La sangría indica el nivel en el árbol de exploración.\n");

            long t0 = System.nanoTime();
            MisionUada mision = new MisionUada(mapa, origen, usarCota, log, 2_000_000);
            mision.resolver();
            double ms = (System.nanoTime() - t0) / 1e6;

            String rep = mision.reporte() + String.format("%n Tiempo de ejecución: %.2f ms%n", ms);
            log.println();
            log.println(rep);
            System.out.println("Mapa: " + nombre + " | lugares=" + mapa.size()
                    + " | conexiones=" + mapa.getCantConexiones() + (mapa.isDirigido() ? " (dirigidas)" : " (no dirigidas)")
                    + " | origen=" + origen);
            System.out.println(rep);
            System.out.println("Log completo en: " + archivoLog);
        }
    }
}
