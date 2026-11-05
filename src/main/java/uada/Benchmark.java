package uada;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Complejidad práctica: mide el tiempo (ms) y los nodos analizados para distintos tamaños n.
 * Para cada n promedia varias semillas. Compara backtracking con poda por cota vs. sin cota
 * (el "sin cota" solo hasta N_MAX_SIN_COTA porque crece demasiado) y verifica que ambos
 * devuelvan las mismas 3 mejores rutas.
 * Salida: complejidad_practica.csv
 *
 * Uso: java -cp target/classes uada.Benchmark [nMin nMax [semillas]]   (por defecto 5 18 5)
 * Las semillas son fijas (n*100 + s), así que dos corridas miden los mismos mapas.
 */
public class Benchmark {
    static final int N_MIN = 5, N_MAX = 18, N_MAX_SIN_COTA = 11, SEMILLAS = 5;
    static final double DENSIDAD = 0.35;

    public static void main(String[] args) throws IOException {
        int[] p = parametros(args);
        int nMin = p[0], nMax = p[1], semillas = p[2];
        // calentamiento de la JVM (JIT) para que las primeras mediciones no salgan infladas
        for (int i = 0; i < 20; i++) correr(Escenarios.aleatorio(9, DENSIDAD, 1000 + i), true);

        try (PrintWriter csv = new PrintWriter(new FileWriter("complejidad_practica.csv", StandardCharsets.UTF_8))) {
            csv.println("n,ms_con_cota,nodos_con_cota,ms_sin_cota,nodos_sin_cota");
            System.out.printf(Locale.ROOT, "%4s %14s %16s %14s %16s %s%n", "n", "ms con cota", "nodos con cota",
                    "ms sin cota", "nodos sin cota", "verificación");
            for (int n = nMin; n <= nMax; n++) {
                double msC = 0, msS = 0; long nodC = 0, nodS = 0; boolean ok = true;
                for (int s = 0; s < semillas; s++) {
                    long semilla = n * 100L + s;
                    Resultado c = correr(Escenarios.aleatorio(n, DENSIDAD, semilla), true);
                    msC += c.ms; nodC += c.nodos;
                    if (n <= N_MAX_SIN_COTA) {
                        Resultado sc = correr(Escenarios.aleatorio(n, DENSIDAD, semilla), false);
                        msS += sc.ms; nodS += sc.nodos;
                        ok &= mismoTop3(c.mejores, sc.mejores);
                    }
                }
                boolean conSin = n <= N_MAX_SIN_COTA;
                csv.printf(java.util.Locale.US, "%d,%.3f,%d,%s,%s%n", n, msC / semillas, nodC / semillas,
                        conSin ? String.format(java.util.Locale.US, "%.3f", msS / semillas) : "",
                        conSin ? String.valueOf(nodS / semillas) : "");
                System.out.printf(Locale.ROOT, "%4d %14.3f %16d %14s %16s %s%n", n, msC / semillas, nodC / semillas,
                        conSin ? String.format(Locale.ROOT, "%.3f", msS / semillas) : "-",
                        conSin ? String.valueOf(nodS / semillas) : "-",
                        conSin ? (ok ? "OK (mismo top-3)" : "DIFERENTE!") : "");
            }
        }
        System.out.println("Resultados en complejidad_practica.csv");
    }

    /** nMin, nMax y semillas desde los argumentos (opcionales). */
    static int[] parametros(String[] args) {
        int nMin = args.length > 0 ? Integer.parseInt(args[0]) : N_MIN;
        int nMax = args.length > 1 ? Integer.parseInt(args[1]) : N_MAX;
        int semillas = args.length > 2 ? Integer.parseInt(args[2]) : SEMILLAS;
        if (nMin < 4 || nMax < nMin || semillas < 1)
            throw new IllegalArgumentException("Uso: Benchmark [nMin nMax [semillas]] con 4 <= nMin <= nMax y semillas >= 1");
        return new int[]{nMin, nMax, semillas};
    }

    record Resultado(double ms, long nodos, List<Ruta> mejores) {}

    static Resultado correr(Mapa m, boolean cota) {
        MisionUada u = new MisionUada(m, "N0", cota, null, 0);
        long t0 = System.nanoTime();
        u.resolver();
        double ms = (System.nanoTime() - t0) / 1e6;
        return new Resultado(ms, u.getNodosAnalizados(), u.getMejores());
    }

    /** Mismas rutas (secuencia de lugares) con los mismos tiempos y en el mismo orden. */
    static boolean mismoTop3(List<Ruta> a, List<Ruta> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (Math.abs(a.get(i).tiempoTotal - b.get(i).tiempoTotal) > 1e-6) return false;
            if (!a.get(i).secuencia().equals(b.get(i).secuencia())) return false;
        }
        return true;
    }
}
