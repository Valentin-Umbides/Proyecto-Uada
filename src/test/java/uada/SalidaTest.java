package uada;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** Log de exploración, reporte final y línea de comandos. */
class SalidaTest {

    private static final String MINI = "src/test/resources/mapas/";

    private Locale localeOriginal;

    @BeforeEach
    void usarLocaleEspanol() {
        // en una PC configurada en español, String.format usa coma decimal: la salida no debe cambiar
        localeOriginal = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("es-AR"));
    }

    @AfterEach
    void restaurarLocale() {
        Locale.setDefault(localeOriginal);
    }

    /** Resultado de una corrida con log en memoria. */
    private record Corrida(MisionUada mision, String log) {}

    private static Corrida correr(String archivo, boolean espera, long maxLineasLog) throws IOException {
        CargadorMapa.Resultado r = CargadorMapa.cargar(archivo, null);
        StringWriter sw = new StringWriter();
        MisionUada u = new MisionUada(r.mapa, r.origen, true, espera, new PrintWriter(sw, true), maxLineasLog);
        u.resolver();
        return new Corrida(u, sw.toString());
    }

    private static long lineasDeNodos(String log) {
        return log.lines().filter(l -> l.strip().startsWith("[#")).count();
    }

    // ---------------------------------------------------------------- log

    @Test
    void elLogTieneUnaLineaPorNodoAnalizado() throws IOException {
        for (String mapa : new String[]{"mini_ventana.txt", "mini_prohibido.txt", "mini_bateria.txt"}) {
            Corrida c = correr(MINI + mapa, true, 1_000_000);
            assertEquals(c.mision().getNodosAnalizados(), lineasDeNodos(c.log()), mapa);
        }
    }

    @Test
    void elLogMuestraLaEsperaYLaSolucion() throws IOException {
        String log = correr(MINI + "mini_ventana.txt", true, 1_000_000).log();
        assertTrue(log.contains("t=7.0s"), log);
        assertTrue(log.contains("esperó 2.0s: cerrada en [4s, 6s]"), log);
        assertTrue(log.contains("★ SOLUCIÓN"), log);
    }

    @Test
    void sinEsperaElLogMuestraLaPodaPorVentana() throws IOException {
        String log = correr(MINI + "mini_ventana.txt", false, 1_000_000).log();
        assertTrue(log.contains("PODA: ventana temporal (llega en t=5.0s y está cerrada en [4s, 6s])"), log);
        assertFalse(log.contains("esperó"), log);
    }

    @Test
    void elLogLlevaSangriaSegunElNivel() throws IOException {
        String log = correr(MINI + "mini_dos_obligatorios.txt", true, 1_000_000).log();
        assertTrue(log.lines().anyMatch(l -> l.startsWith("[#1] RAÍZ")), log);
        assertTrue(log.lines().anyMatch(l -> l.startsWith("  [#") && l.contains("Aula 348 -> A")), log);
        assertTrue(log.lines().anyMatch(l -> l.startsWith("    [#") && l.contains("A -> B")), log);
    }

    @Test
    void elLogSeTruncaAlMaximoDeLineas() throws IOException {
        String log = correr(MINI + "mini_desempate.txt", true, 5).log();
        assertEquals(6, log.lines().count(), log);   // 5 líneas + el aviso
        assertTrue(log.contains("log truncado"), log);
    }

    // ---------------------------------------------------------------- reporte

    @Test
    void reporteConSolucion() throws IOException {
        String rep = correr("mapas/mapa_grande.txt", true, 0).mision().reporte();
        assertTrue(rep.contains("OBJETIVO PRINCIPAL: 3 MEJORES RUTAS"), rep);
        assertTrue(rep.contains("Ruta #1"), rep);
        assertTrue(rep.contains("Tiempo total: 411.0 s (6 min 51 s)"), rep);
        for (String col : new String[]{"Origen", "Destino", "Movimiento", "Batería", "Recarga", "Espera", "T. acumulado"})
            assertTrue(rep.contains(col), "falta la columna " + col);
        assertTrue(rep.contains("Esperas por lugar cerrado"), rep);
    }

    @Test
    void reporteSinSolucion() throws IOException {
        String rep = correr("mapas/mapa_grande_sin_solucion.txt", true, 0).mision().reporte();
        assertTrue(rep.contains("NO EXISTE SOLUCIÓN"), rep);
        assertTrue(rep.contains("Obligatorios visitados: 4/4"), rep);
        assertTrue(rep.contains("Punto máximo de avance: Aula 613"), rep);
        assertTrue(rep.contains("Distancia recta a Aula 348: 98.5 m"), rep);
    }

    @Test
    void reporteMuestraLaEsperaEnLaTabla() throws IOException {
        String rep = correr(MINI + "mini_ventana.txt", true, 0).mision().reporte();
        assertTrue(rep.lines().anyMatch(l -> l.contains("Aula 348") && l.contains("Saltando")
                && l.contains("2.0 s") && l.contains("7.0 s")), rep);
    }

    // ---------------------------------------------------------------- línea de comandos

    private static String salidaDeMain(String... args) throws IOException {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
        try {
            Main.main(args);
        } finally {
            System.setOut(original);
        }
        return buf.toString(StandardCharsets.UTF_8);
    }

    @Test
    void mainConArchivoInexistente() throws IOException {
        assertTrue(salidaDeMain("no_existe.txt").contains("No se encontró el archivo: no_existe.txt"));
    }

    @Test
    void mainConArchivoMalFormado(@org.junit.jupiter.api.io.TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("roto.txt");
        Files.writeString(f, "LUGAR;Aula 348;348;0;0;0;0\nPUERTA;x\n");
        String out = salidaDeMain(f.toString());
        assertTrue(out.contains("Error en el archivo de mapa -> Línea 2:"), out);
    }

    @Test
    void mainResuelveYAvisaOpcionesDesconocidas() throws IOException {
        Path log = Path.of("logs", "log_mini_ventana.txt");
        try {
            String out = salidaDeMain(MINI + "mini_ventana.txt", "opcionrara");
            assertTrue(out.contains("Opción ignorada: opcionrara"), out);
            assertTrue(out.contains("Ruta #1"), out);
            assertTrue(out.contains("Tiempo total: 12.0 s"), out);
            assertTrue(Files.readString(log, StandardCharsets.UTF_8).contains("★ SOLUCIÓN"));
        } finally {
            Files.deleteIfExists(log);
        }
    }
}
