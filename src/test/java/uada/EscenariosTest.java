package uada;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EscenariosTest {

    /** Descripción completa del mapa (lugares, obligatorios, cierres y conexiones) para compararlo. */
    private static List<String> describir(Mapa m) {
        List<String> d = new ArrayList<>();
        for (Estacion e : m.getEstaciones()) {
            d.add(e.getCodigo() + " aula=" + e.getNumeroAula() + " oblig=" + e.isObligatoria()
                    + " cierres=" + e.getCierres());
            for (Conexion c : m.adyacentes(e))
                d.add("  " + e.getCodigo() + "->" + c.otroExtremo(e).getCodigo() + " " + c.getSegundos() + "s " + c.getModos());
        }
        return d;
    }

    @Test
    void mismaSemillaMismoMapa() {
        assertEquals(describir(Escenarios.aleatorio(12, 0.35, 42)), describir(Escenarios.aleatorio(12, 0.35, 42)));
        assertNotEquals(describir(Escenarios.aleatorio(12, 0.35, 42)), describir(Escenarios.aleatorio(12, 0.35, 43)));
    }

    @Test
    void mapaAleatorioTieneNLugaresYElOrigenEsElAula348() {
        for (int n = 5; n <= 15; n++) {
            Mapa m = Escenarios.aleatorio(n, 0.35, n);
            assertEquals(n, m.size());
            assertEquals(348, m.get("N0").getNumeroAula());
            assertFalse(m.get("N0").isObligatoria());
        }
    }

    @Test
    void mapaAleatorioSiempreTieneUnCicloPorTodosLosLugares() {
        // cada lugar tiene al menos 2 conexiones (las del ciclo), así que siempre se puede volver al origen
        for (long semilla = 0; semilla < 20; semilla++) {
            Mapa m = Escenarios.aleatorio(10, 0.0, semilla);   // densidad 0: solo el ciclo
            assertEquals(10, m.getCantConexiones());
            for (Estacion e : m.getEstaciones()) assertEquals(2, m.adyacentes(e).size(), e.getCodigo());
        }
    }

    @Test
    void escenariosDeEjemploSeResuelven() {
        MisionUada base = new MisionUada(Escenarios.uade(), "A348", true, null, 0);
        base.resolver();
        assertEquals(3, base.getMejores().size());

        MisionUada chico = new MisionUada(Escenarios.uadeChico(), "A348", true, null, 0);
        chico.resolver();
        assertFalse(chico.getMejores().isEmpty());
    }
}
