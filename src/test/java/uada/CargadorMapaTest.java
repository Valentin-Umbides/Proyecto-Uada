package uada;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CargadorMapaTest {

    private static final String MAPA_GRANDE = "mapas/mapa_grande.txt";
    private static final String MAPA_SIN_SOLUCION = "mapas/mapa_grande_sin_solucion.txt";

    @TempDir
    Path tmp;

    private String archivo(String contenido) throws IOException {
        Path p = tmp.resolve("mapa.txt");
        Files.writeString(p, contenido, StandardCharsets.UTF_8);
        return p.toString();
    }

    private static Set<String> codigos(List<Estacion> estaciones) {
        return estaciones.stream().map(Estacion::getCodigo).collect(Collectors.toSet());
    }

    /** Cantidad de conexiones que llegan a 'destino' desde cualquier otro lugar. */
    private static long entrantes(Mapa m, Estacion destino) {
        return m.getEstaciones().stream()
                .flatMap(e -> m.adyacentes(e).stream().filter(c -> c.otroExtremo(e) == destino))
                .count();
    }

    // ---------------------------------------------------------------- mapas de la cátedra

    @Test
    void mapaGrande() throws IOException {
        CargadorMapa.Resultado r = CargadorMapa.cargar(MAPA_GRANDE, null);
        Mapa m = r.mapa;
        assertTrue(m.isDirigido());
        assertEquals(20, m.size());
        assertEquals(32, m.getCantConexiones());
        assertEquals("Aula 348", r.origen);
        assertTrue(m.isUsarCoordenadas());
        assertEquals(Set.of("Aula 613", "Molinete Lima 3", "Starbucks", "Auditorio"),
                codigos(m.getEstaciones().stream().filter(Estacion::isObligatoria).toList()));
        assertEquals(Set.of("Zona obras"),
                codigos(m.getEstaciones().stream().filter(Estacion::isProhibida).toList()));
        assertEquals(Set.of("Starbucks", "Patio", "Molinete Lima 3"),
                codigos(m.getEstaciones().stream().filter(e -> !e.getCierres().isEmpty()).toList()));
    }

    @Test
    void mapaGrandeSoloSeVuelveAl348DesdeEl402() throws IOException {
        Mapa m = CargadorMapa.cargar(MAPA_GRANDE, null).mapa;
        Estacion a402 = m.get("Aula 402");
        List<Conexion> salidas = m.adyacentes(a402);
        assertEquals(1, salidas.size());
        assertEquals("Aula 348", salidas.get(0).otroExtremo(a402).getCodigo());
        assertEquals(EnumSet.of(Modo.CAMINANDO, Modo.PATAS_ARRIBA), salidas.get(0).getModos());
        assertEquals(1, entrantes(m, m.get("Aula 348")));
    }

    @Test
    void mapaSinSolucionNoTieneConexionesQueEntrenAl348() throws IOException {
        Mapa m = CargadorMapa.cargar(MAPA_SIN_SOLUCION, null).mapa;
        assertTrue(m.isDirigido());
        assertEquals(31, m.getCantConexiones());
        assertEquals(0, entrantes(m, m.get("Aula 348")));
    }

    @Test
    void forzarNoDirigido() throws IOException {
        Mapa m = CargadorMapa.cargar(MAPA_SIN_SOLUCION, false).mapa;
        assertFalse(m.isDirigido());
        assertEquals(31, m.getCantConexiones());
        Estacion a348 = m.get("Aula 348");
        assertEquals(Set.of("Pasillo P3", "Aula 501", "Aula 402"),
                m.adyacentes(a348).stream().map(c -> c.otroExtremo(a348).getCodigo()).collect(Collectors.toSet()));
    }

    // ---------------------------------------------------------------- formato del archivo

    @Test
    void toleraBomCrlfLineasVaciasComentariosYEspacios() throws IOException {
        String f = archivo("﻿# comentario\r\n"
                + "\r\n"
                + "LUGAR; Aula 348 ;348;0;0;0;0\r\n"
                + "LUGAR;A;0;10;0;1;0\r\n"
                + "ARISTA; Aula 348 ; A ; 10 ; CS \r\n"
                + "INICIO;Aula 348\r\n");
        CargadorMapa.Resultado r = CargadorMapa.cargar(f, null);
        assertEquals(2, r.mapa.size());
        assertEquals("Aula 348", r.origen);
        Conexion c = r.mapa.adyacentes(r.mapa.get("Aula 348")).get(0);
        assertEquals(10, c.getSegundos());
        assertEquals(EnumSet.of(Modo.CAMINANDO, Modo.SALTANDO), c.getModos());
    }

    @Test
    void lineaGrafoNoDirigido() throws IOException {
        String f = archivo("GRAFO;NO_DIRIGIDO\nLUGAR;Aula 348;348;0;0;0;0\nLUGAR;A;0;1;0;0;0\n"
                + "ARISTA;Aula 348;A;10;C\nINICIO;Aula 348\n");
        Mapa m = CargadorMapa.cargar(f, null).mapa;
        assertFalse(m.isDirigido());
        assertEquals(1, m.adyacentes(m.get("A")).size());
    }

    @Test
    void formatoAlternativoDeLugar() throws IOException {
        String f = archivo("LUGAR;A348;Aula 348;348;NORMAL;0;0\n"
                + "LUGAR;SB;Starbucks;-1;OBLIGATORIO;5;5\n"
                + "LUGAR;OBRA;Zona obras;-1;PROHIBIDO;9;9\n"
                + "CONEXION;A348;SB;30;C,S\n"
                + "ORIGEN;A348\n");
        CargadorMapa.Resultado r = CargadorMapa.cargar(f, null);
        assertEquals("A348", r.origen);
        assertEquals("Starbucks", r.mapa.get("SB").getNombre());
        assertTrue(r.mapa.get("SB").isObligatoria());
        assertTrue(r.mapa.get("OBRA").isProhibida());
        assertEquals(348, r.mapa.get("A348").getNumeroAula());
    }

    @Test
    void sinInicioUsaElAula348() throws IOException {
        String f = archivo("LUGAR;Aula 348;348;0;0;0;0\n");
        assertEquals("Aula 348", CargadorMapa.cargar(f, null).origen);
    }

    // ---------------------------------------------------------------- errores con número de línea

    private void assertErrorEnLinea(String contenido, int linea, String fragmento) throws IOException {
        String f = archivo(contenido);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> CargadorMapa.cargar(f, null));
        assertTrue(ex.getMessage().startsWith("Línea " + linea + ":"), ex.getMessage());
        assertTrue(ex.getMessage().contains(fragmento), ex.getMessage());
    }

    @Test
    void errorLugarInexistente() throws IOException {
        assertErrorEnLinea("LUGAR;Aula 348;348;0;0;0;0\nARISTA;Aula 348;Inexistente;10;C\n",
                2, "Inexistente");
    }

    @Test
    void errorModoDesconocido() throws IOException {
        assertErrorEnLinea("LUGAR;Aula 348;348;0;0;0;0\nLUGAR;A;0;1;0;0;0\nARISTA;Aula 348;A;10;X\n",
                3, "modo desconocido");
    }

    @Test
    void errorNumeroInvalido() throws IOException {
        assertErrorEnLinea("LUGAR;Aula 348;348;0;0;0;0\nLUGAR;A;0;1;0;0;0\nARISTA;Aula 348;A;diez;C\n",
                3, "número inválido");
    }

    @Test
    void errorInstruccionDesconocida() throws IOException {
        assertErrorEnLinea("LUGAR;Aula 348;348;0;0;0;0\nPUERTA;A;B\n", 2, "instrucción desconocida");
    }

    @Test
    void errorSinInicioNiAula348() throws IOException {
        String f = archivo("LUGAR;A;0;0;0;0;0\n");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> CargadorMapa.cargar(f, null));
        assertTrue(ex.getMessage().contains("Falta la línea INICIO"), ex.getMessage());
    }
}
