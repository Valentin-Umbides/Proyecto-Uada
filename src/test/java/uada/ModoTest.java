package uada;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModoTest {

    private static final double EPS = 1e-9;

    @Test
    void tiempoSegunModo() {
        assertEquals(27.5, Modo.SALTANDO.tiempo(55), EPS);
        assertEquals(55.0, Modo.CAMINANDO.tiempo(55), EPS);
        assertEquals(110.0, Modo.PATAS_ARRIBA.tiempo(55), EPS);
    }

    @Test
    void consumoRelativoACaminar() {
        assertEquals(2.0, Modo.CAMINANDO.consumo(120), EPS);   // 2 minutos -> 2 puntos
        assertEquals(2.2, Modo.SALTANDO.consumo(60), EPS);     // 2.2 veces caminar
        assertEquals(0.45, Modo.PATAS_ARRIBA.consumo(60), EPS); // 55% menos que caminar
    }

    @Test
    void sePruebaPrimeroElModoMasRapido() {
        // EnumSet itera en orden de declaración: es el orden en que el backtracking prueba los modos
        assertEquals(List.of(Modo.SALTANDO, Modo.CAMINANDO, Modo.PATAS_ARRIBA),
                List.copyOf(EnumSet.allOf(Modo.class)));
    }
}
