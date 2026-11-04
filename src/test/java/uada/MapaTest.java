package uada;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static uada.Modo.*;

class MapaTest {

    private static Mapa conLugares(boolean dirigido, String... codigos) {
        Mapa m = new Mapa(dirigido);
        for (String c : codigos) m.agregarEstacion(c, c, 0, 0, -1);
        return m;
    }

    @Test
    void dirigidoSoloAgregaElSentidoIndicado() {
        Mapa m = conLugares(true, "A", "B");
        m.conectar("A", "B", 10, CAMINANDO);
        assertEquals(1, m.adyacentes(m.get("A")).size());
        assertTrue(m.adyacentes(m.get("B")).isEmpty());
    }

    @Test
    void dirigidoPermiteIdaYVueltaComoConexionesDistintas() {
        Mapa m = conLugares(true, "A", "B");
        m.conectar("A", "B", 10, CAMINANDO);
        m.conectar("B", "A", 20, SALTANDO);
        assertEquals(2, m.getCantConexiones());
        assertSame(m.get("A"), m.adyacentes(m.get("B")).get(0).otroExtremo(m.get("B")));
    }

    @Test
    void noDirigidoAgregaLosDosSentidos() {
        Mapa m = conLugares(false, "A", "B");
        m.conectar("A", "B", 10, CAMINANDO);
        assertEquals(1, m.getCantConexiones());
        assertSame(m.get("A"), m.adyacentes(m.get("B")).get(0).otroExtremo(m.get("B")));
    }

    @Test
    void rechazaDatosInvalidos() {
        Mapa m = conLugares(false, "A", "B");
        m.conectar("A", "B", 10, CAMINANDO);
        assertThrows(IllegalArgumentException.class, () -> m.agregarEstacion("A", "A", 0, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> m.conectar("A", "A", 10, CAMINANDO));
        assertThrows(IllegalArgumentException.class, () -> m.conectar("A", "B", 10, CAMINANDO));
        assertThrows(IllegalArgumentException.class, () -> m.conectar("B", "A", 10, CAMINANDO));
        assertThrows(IllegalArgumentException.class, () -> m.get("Z"));
    }

    @Test
    void rechazaConexionSinTiempoOSinModos() {
        Mapa m = conLugares(true, "A", "B");
        assertThrows(IllegalArgumentException.class, () -> m.conectar("A", "B", 0, CAMINANDO));
        assertThrows(IllegalArgumentException.class, () -> m.conectar("A", "B", 10));
    }
}
