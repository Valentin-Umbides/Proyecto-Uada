package uada;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BenchmarkTest {

    @Test
    void conCotaYSinCotaDanElMismoTop3EnMapasAleatorios() {
        for (int n = 5; n <= 8; n++) {
            for (int s = 0; s < Benchmark.SEMILLAS; s++) {
                long semilla = n * 100L + s;   // mismas semillas que el benchmark
                Benchmark.Resultado con = Benchmark.correr(Escenarios.aleatorio(n, Benchmark.DENSIDAD, semilla), true);
                Benchmark.Resultado sin = Benchmark.correr(Escenarios.aleatorio(n, Benchmark.DENSIDAD, semilla), false);
                assertTrue(Benchmark.mismoTop3(con.mejores(), sin.mejores()), "n=" + n + " semilla=" + semilla);
                assertTrue(con.nodos() <= sin.nodos(), "n=" + n + " semilla=" + semilla);
            }
        }
    }

    @Test
    void argumentosOpcionales() {
        assertArrayEquals(new int[]{Benchmark.N_MIN, Benchmark.N_MAX, Benchmark.SEMILLAS}, Benchmark.parametros(new String[0]));
        assertArrayEquals(new int[]{5, 9, 2}, Benchmark.parametros(new String[]{"5", "9", "2"}));
        assertArrayEquals(new int[]{6, 10, Benchmark.SEMILLAS}, Benchmark.parametros(new String[]{"6", "10"}));
        assertThrows(IllegalArgumentException.class, () -> Benchmark.parametros(new String[]{"9", "5"}));
        assertThrows(IllegalArgumentException.class, () -> Benchmark.parametros(new String[]{"3", "9"}));
    }
}
