package uada;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MisionUadaTest {

    private static final double EPS = 1e-9;
    private static final String MINI = "src/test/resources/mapas/";
    private static final String MAPA_GRANDE = "mapas/mapa_grande.txt";
    private static final String MAPA_SIN_SOLUCION = "mapas/mapa_grande_sin_solucion.txt";

    /** Carga el mapa, corre el backtracking sin log y valida todas las rutas devueltas. */
    private static MisionUada resolver(String archivo, Boolean dirigido, boolean usarCota) throws IOException {
        CargadorMapa.Resultado r = CargadorMapa.cargar(archivo, dirigido);
        return resolver(r.mapa, r.origen, usarCota);
    }

    private static MisionUada resolver(Mapa mapa, String origen, boolean usarCota) {
        MisionUada u = new MisionUada(mapa, origen, usarCota, null, 0);
        u.resolver();
        for (Ruta ruta : u.getMejores()) ValidadorRuta.validarSolucion(mapa, origen, ruta);
        if (u.getMejores().isEmpty()) ValidadorRuta.validarTruncada(mapa, origen, u.getMejorParcial());
        return u;
    }

    private static List<String> modos(Ruta r) {
        return r.pasos.stream().map(m -> m.modo.name()).toList();
    }

    // ---------------------------------------------------------------- mapas chicos

    @Test
    void idaYVueltaSaltandoLosDosTramos() throws IOException {
        MisionUada u = resolver(MINI + "mini_ida_vuelta.txt", null, true);
        assertEquals(1, u.getMejores().size());   // una sola secuencia de lugares posible
        Ruta r = u.getMejores().get(0);
        assertEquals(10.0, r.tiempoTotal, EPS);
        assertEquals(List.of("SALTANDO", "SALTANDO"), modos(r));
        assertEquals(100 - 2 * (10 / 60.0 * 2.2), r.pasos.get(1).bateriaRestante, EPS);
    }

    @Test
    void ventanaCerradaObligaACaminarElPrimerTramo() throws IOException {
        MisionUada u = resolver(MINI + "mini_ventana.txt", null, true);
        Ruta r = u.getMejores().get(0);
        assertEquals(15.0, r.tiempoTotal, EPS);   // caminando llega en t=10, vuelve saltando en 5
        assertEquals(List.of("CAMINANDO", "SALTANDO"), modos(r));
        assertTrue(u.getPodas().get("ventana temporal") >= 1);
    }

    @Test
    void evitaLaZonaProhibidaAunqueSeaMasCorta() throws IOException {
        MisionUada u = resolver(MINI + "mini_prohibido.txt", null, true);
        Ruta r = u.getMejores().get(0);
        assertEquals("Aula 348 > B > A > Aula 348", r.secuencia());
        assertEquals(60.0, r.tiempoTotal, EPS);
        assertTrue(u.getPodas().get("zona prohibida") >= 1);
    }

    @Test
    void noAceptaVueltaAnticipada() throws IOException {
        MisionUada u = resolver(MINI + "mini_dos_obligatorios.txt", null, true);
        assertEquals(1, u.getMejores().size());
        assertEquals("Aula 348 > A > B > Aula 348", u.getMejores().get(0).secuencia());
        assertEquals(15.0, u.getMejores().get(0).tiempoTotal, EPS);
        assertTrue(u.getPodas().get("vuelta anticipada") >= 1);
    }

    @Test
    void sinBateriaDevuelveLaRutaTruncada() throws IOException {
        for (boolean cota : new boolean[]{true, false}) {
            MisionUada u = resolver(MINI + "mini_bateria.txt", null, cota);
            assertTrue(u.getMejores().isEmpty());
            assertEquals("A", u.getMejorParcial().ultima().getCodigo());
            assertEquals(50.0, u.getMejorParcialDistancia(), EPS);   // A está en (30, 40)
            assertEquals(1, u.getMejorParcialObligatorias());
            assertTrue(u.getPodas().get("batería") >= 1);           // A -> 348 quedaría en -20
        }
    }

    @Test
    void bateriaJustaLlegaConCero() throws IOException {
        MisionUada u = resolver(MINI + "mini_bateria_justa.txt", null, true);
        assertEquals(1, u.getMejores().size());
        Ruta r = u.getMejores().get(0);
        assertEquals(0.0, r.pasos.get(r.pasos.size() - 1).bateriaRestante, EPS);
    }

    // ---------------------------------------------------------------- mapas de la cátedra

    @Test
    void mapaGrandeTresRutasDe417ConLugaresDistintos() throws IOException {
        MisionUada u = resolver(MAPA_GRANDE, null, true);
        List<Ruta> mejores = u.getMejores();
        assertEquals(3, mejores.size());
        Set<String> secuencias = new HashSet<>();
        for (Ruta r : mejores) {
            assertEquals(417.5, r.tiempoTotal, EPS);
            assertTrue(secuencias.add(r.secuencia()), "secuencia repetida: " + r.secuencia());
            // las ventanas obligan a llegar al Molinete recién en t=247.5 (cerrado en [200, 240])
            Movimiento aMolinete = r.pasos.stream()
                    .filter(m -> m.destino.getCodigo().equals("Molinete Lima 3")).findFirst().orElseThrow();
            assertEquals(247.5, aMolinete.tiempoAcumulado, EPS);
        }
    }

    @Test
    void mapaSinSolucionTerminaEnElAula613() throws IOException {
        MisionUada u = resolver(MAPA_SIN_SOLUCION, null, true);
        assertTrue(u.getMejores().isEmpty());
        assertEquals("Aula 613", u.getMejorParcial().ultima().getCodigo());
        assertEquals(Math.sqrt(9700), u.getMejorParcialDistancia(), EPS);
        assertEquals(4, u.getMejorParcialObligatorias());
    }

    @Test
    void mapaSinSolucionLeidoComoNoDirigidoTieneRutasDe285() throws IOException {
        MisionUada u = resolver(MAPA_SIN_SOLUCION, false, true);
        assertEquals(3, u.getMejores().size());
        for (Ruta r : u.getMejores()) assertEquals(285.0, r.tiempoTotal, EPS);
    }

    // ---------------------------------------------------------------- la poda por cota no descarta soluciones

    @Test
    void conCotaYSinCotaDanElMismoTop3() {
        for (Mapa m : List.of(Escenarios.uade(), Escenarios.uadeChico())) {
            MisionUada con = resolver(m, "A348", true);
            MisionUada sin = resolver(m, "A348", false);
            assertEquals(secuencias(sin.getMejores()), secuencias(con.getMejores()));
            for (int i = 0; i < con.getMejores().size(); i++)
                assertEquals(sin.getMejores().get(i).tiempoTotal, con.getMejores().get(i).tiempoTotal, EPS);
            assertTrue(con.getNodosAnalizados() < sin.getNodosAnalizados(), "la cota no podó nada");
        }
    }

    @Test
    void escenarioImposibleNoTieneSolucion() {
        MisionUada u = resolver(Escenarios.uadeImposible(), "A348", true);
        assertTrue(u.getMejores().isEmpty());
        assertFalse(u.getMejorParcial().pasos.isEmpty());
    }

    private static List<String> secuencias(List<Ruta> rutas) {
        return rutas.stream().map(Ruta::secuencia).toList();
    }
}
