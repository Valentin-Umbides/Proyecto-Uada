package uada;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Complejidad práctica: mide el tiempo (ms) y los nodos analizados para distintos tamaños n.
 * Para cada n promedia varias semillas. Compara backtracking con poda por cota vs. sin cota
 * (el "sin cota" solo hasta N_MAX_SIN_COTA porque crece demasiado) y verifica que ambos
 * devuelvan las mismas 3 mejores rutas.
 * Salida: complejidad_practica.csv
 */
public class Benchmark {
    static final int N_MIN = 5, N_MAX = 18, N_MAX_SIN_COTA = 11, SEMILLAS = 5;
    static final double DENSIDAD = 0.35;

    public static void main(String[] args) throws IOException {
        // calentamiento de la JVM (JIT) para que las primeras mediciones no salgan infladas
        for (int i = 0; i < 20; i++) correr(Escenarios.aleatorio(9, DENSIDAD, 1000 + i), true);

        try (PrintWriter csv = new PrintWriter(new FileWriter("complejidad_practica.csv", StandardCharsets.UTF_8))) {
            csv.println("n,ms_con_cota,nodos_con_cota,ms_sin_cota,nodos_sin_cota");
            System.out.printf("%4s %14s %16s %14s %16s %s%n", "n", "ms con cota", "nodos con cota",
                    "ms sin cota", "nodos sin cota", "verificación");
            for (int n = N_MIN; n <= N_MAX; n++) {
                double msC = 0, msS = 0; long nodC = 0, nodS = 0; boolean ok = true;
                for (int s = 0; s < SEMILLAS; s++) {
                    long semilla = n * 100L + s;
                    Resultado c = correr(Escenarios.aleatorio(n, DENSIDAD, semilla), true);
                    msC += c.ms; nodC += c.nodos;
                    if (n <= N_MAX_SIN_COTA) {
                        Resultado sc = correr(Escenarios.aleatorio(n, DENSIDAD, semilla), false);
                        msS += sc.ms; nodS += sc.nodos;
                        ok &= mismosTiempos(c.mejores, sc.mejores);
                    }
                }
                boolean conSin = n <= N_MAX_SIN_COTA;
                csv.printf(java.util.Locale.US, "%d,%.3f,%d,%s,%s%n", n, msC / SEMILLAS, nodC / SEMILLAS,
                        conSin ? String.format(java.util.Locale.US, "%.3f", msS / SEMILLAS) : "",
                        conSin ? String.valueOf(nodS / SEMILLAS) : "");
                System.out.printf("%4d %14.3f %16d %14s %16s %s%n", n, msC / SEMILLAS, nodC / SEMILLAS,
                        conSin ? String.format("%.3f", msS / SEMILLAS) : "-",
                        conSin ? String.valueOf(nodS / SEMILLAS) : "-",
                        conSin ? (ok ? "OK (mismo top-3)" : "DIFERENTE!") : "");
            }
        }
        System.out.println("Resultados en complejidad_practica.csv");
    }

    record Resultado(double ms, long nodos, List<Ruta> mejores) {}

    static Resultado correr(Mapa m, boolean cota) {
        MisionUada u = new MisionUada(m, "N0", cota, null, 0);
        long t0 = System.nanoTime();
        u.resolver();
        double ms = (System.nanoTime() - t0) / 1e6;
        return new Resultado(ms, u.getNodosAnalizados(), u.getMejores());
    }

    static boolean mismosTiempos(List<Ruta> a, List<Ruta> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++)
            if (Math.abs(a.get(i).tiempoTotal - b.get(i).tiempoTotal) > 1e-6) return false;
        return true;
    }
}
