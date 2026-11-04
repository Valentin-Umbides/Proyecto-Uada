package uada;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstacionTest {

    private static final double EPS = 1e-9;

    private static Estacion aula(int numero) {
        return new Estacion(0, "Aula " + numero, "Aula " + numero, 0, 0, numero);
    }

    @Test
    void recargaEsSumaDeDigitosSobreCinco() {
        assertEquals(2.0, aula(613).recarga(), EPS);   // ejemplo del enunciado: (6+1+3)/5
        assertEquals(3.0, aula(348).recarga(), EPS);
        assertEquals(1.2, aula(501).recarga(), EPS);
    }

    @Test
    void lugarQueNoEsAulaNoRecarga() {
        Estacion patio = new Estacion(0, "Patio", "Patio", 0, 0, -1);
        assertFalse(patio.esAula());
        assertEquals(0.0, patio.recarga(), EPS);
    }

    @Test
    void ventanaCerradaIncluyeLosBordes() {
        Estacion starbucks = new Estacion(0, "Starbucks", "Starbucks", 0, 0, -1);
        starbucks.agregarCierre(new Ventana(100, 130));
        assertTrue(starbucks.cerradaEn(100));
        assertTrue(starbucks.cerradaEn(115));
        assertTrue(starbucks.cerradaEn(130));
        assertFalse(starbucks.cerradaEn(99.5));
        assertFalse(starbucks.cerradaEn(130.5));
    }

    @Test
    void conVariasVentanasAlcanzaConQueUnaEsteCerrada() {
        Estacion patio = new Estacion(0, "Patio", "Patio", 0, 0, -1);
        patio.agregarCierre(new Ventana(10, 20));
        patio.agregarCierre(new Ventana(50, 60));
        assertTrue(patio.cerradaEn(55));
        assertFalse(patio.cerradaEn(30));
    }

    @Test
    void distanciaRectaEuclidea() {
        Estacion origen = new Estacion(0, "Aula 348", "Aula 348", 0, 0, 348);
        Estacion a613 = new Estacion(1, "Aula 613", "Aula 613", 90, -40, 613);
        assertEquals(Math.sqrt(9700), a613.distanciaRecta(origen), EPS);
    }
}
