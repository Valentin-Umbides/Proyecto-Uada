package uada;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Re-simula una ruta paso a paso usando solo las reglas del enunciado (Modo y Estacion), sin pasar
 * por MisionUada, y falla si viola alguna restricción o si los valores informados no coinciden.
 */
final class ValidadorRuta {

    private static final double EPS = 1e-9;

    private ValidadorRuta() { }

    /** Valida una solución completa: sale y vuelve al origen con todos los obligatorios. */
    static void validarSolucion(Mapa mapa, String codigoOrigen, Ruta r) {
        Estacion origen = mapa.get(codigoOrigen);
        assertFalse(r.pasos.isEmpty(), "la solución no tiene movimientos");
        assertSame(origen, r.ultima(), "la ruta no termina en " + codigoOrigen);
        Set<Estacion> visitados = simular(mapa, origen, r, true);
        for (Estacion e : mapa.getEstaciones())
            if (e.isObligatoria()) assertTrue(visitados.contains(e), "no visita el obligatorio " + e);
    }

    /** Valida una ruta truncada: cada paso es legal, pero no tiene por qué volver al origen. */
    static void validarTruncada(Mapa mapa, String codigoOrigen, Ruta r) {
        simular(mapa, mapa.get(codigoOrigen), r, false);
    }

    private static Set<Estacion> simular(Mapa mapa, Estacion origen, Ruta r, boolean cierraEnOrigen) {
        Set<Estacion> visitados = new HashSet<>();
        visitados.add(origen);
        Estacion actual = origen;
        double t = 0, bat = MisionUada.BATERIA_INICIAL;

        for (int i = 0; i < r.pasos.size(); i++) {
            Movimiento m = r.pasos.get(i);
            String paso = "paso " + (i + 1) + " (" + m.origen + " -> " + m.destino + "): ";
            boolean esCierre = cierraEnOrigen && i == r.pasos.size() - 1;

            assertSame(actual, m.origen, paso + "no sale de donde terminó el paso anterior");
            Conexion c = conexion(mapa, m.origen, m.destino);
            assertNotNull(c, paso + "no existe la conexión");
            assertTrue(c.getModos().contains(m.modo), paso + "el modo " + m.modo + " no está permitido");
            assertFalse(m.destino.isProhibida(), paso + "entra a una zona prohibida");
            if (esCierre) assertSame(origen, m.destino, paso + "el último paso no vuelve al origen");
            else assertTrue(visitados.add(m.destino), paso + "repite un lugar");

            t += m.modo.tiempo(c.getSegundos());
            bat -= m.modo.consumo(c.getSegundos());
            if (esCierre) assertTrue(bat >= -EPS, paso + "llega al destino con batería " + bat);
            else assertTrue(bat > EPS, paso + "se queda sin batería (" + bat + ")");
            if (m.espera > EPS) {
                // solo se espera si el lugar está cerrado al llegar, y lo mínimo para poder entrar
                assertTrue(m.destino.cerradaEn(t), paso + "espera sin que el lugar esté cerrado");
                double entrada = m.destino.primerInstanteAbierto(t, MisionUada.MARGEN_ESPERA);
                assertEquals(entrada - t, m.espera, EPS, paso + "espera");
                t = entrada;   // esperar consume tiempo, no batería
            }
            assertFalse(m.destino.cerradaEn(t), paso + "entra en t=" + t + " con el lugar cerrado");

            double recarga = esCierre ? 0 : Math.min(m.destino.recarga(), MisionUada.BATERIA_MAXIMA - bat);
            bat += recarga;

            assertEquals(t, m.tiempoAcumulado, EPS, paso + "tiempo acumulado");
            assertEquals(recarga, m.recarga, EPS, paso + "recarga");
            assertEquals(bat, m.bateriaRestante, EPS, paso + "batería restante");
            actual = m.destino;
        }
        assertEquals(t, r.tiempoTotal, EPS, "tiempo total");
        return visitados;
    }

    private static Conexion conexion(Mapa mapa, Estacion a, Estacion b) {
        for (Conexion c : mapa.adyacentes(a)) if (c.otroExtremo(a) == b) return c;
        return null;
    }
}
