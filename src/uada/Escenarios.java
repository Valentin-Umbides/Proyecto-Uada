package uada;

import java.util.*;

import static uada.Modo.*;

/** Mapas de prueba: UADE (base e imposible) y grafos aleatorios para medir tiempos. */
public class Escenarios {

    /**
     * Mapa de UADE propuesto por el grupo (el enunciado no trae uno).
     * Tiempos en segundos de mapa (caminando). Coordenadas en metros (x, y) para la distancia recta.
     */
    public static Mapa uade() {
        Mapa m = new Mapa();
        //                 código       nombre                        x     y    nº aula
        m.agregarEstacion("A348",      "Aula 348 (inicio y fin)",      40,   30,  348);
        m.agregarEstacion("PASILLO3",  "Pasillo 3er piso",             60,   30,   -1);
        m.agregarEstacion("ESCALERA",  "Escalera de emergencia",       10,   40,   -1);
        m.agregarEstacion("ASCENSOR",  "Ascensores Lima",              70,   50,   -1);
        m.agregarEstacion("A613",      "Aula 613",                     90,   70,  613);
        m.agregarEstacion("A612",      "Aula 612",                     70,   80,  612);
        m.agregarEstacion("A633",      "Aula 633",                     30,   80,  633);
        m.agregarEstacion("A201",      "Aula 201",                     90,   20,  201);
        m.agregarEstacion("PATIO",     "Patio central",                40,   60,   -1);
        m.agregarEstacion("STARBUCKS", "Starbucks del patio",          30,   65,   -1);
        m.agregarEstacion("LIMA3",     "Molinete Lima 3",              60,  100,   -1);
        m.agregarEstacion("BIBLIO",    "Biblioteca",                  100,  100,   -1);
        m.agregarEstacion("OBRA",      "Zona en obra (prohibida)",      0,   90,   -1);

        // Lugares obligatorios de la misión
        for (String c : new String[]{"STARBUCKS", "LIMA3", "A613", "BIBLIO"}) m.get(c).setObligatoria(true);
        // Zona prohibida
        m.get("OBRA").setProhibida(true);
        // Obstáculos dinámicos: Starbucks cerrado por recepción de mercadería
        m.get("STARBUCKS").agregarCierre(new Ventana(2400, 3300));
        // Biblioteca cerrada un rato por limpieza
        m.get("BIBLIO").agregarCierre(new Ventana(4200, 4800));

        // Conexiones (única por par de lugares)          seg   modos permitidos
        m.conectar("A348",     "PASILLO3",   300, CAMINANDO, SALTANDO, PATAS_ARRIBA);
        m.conectar("A348",     "ESCALERA",   420, CAMINANDO, PATAS_ARRIBA);
        m.conectar("PASILLO3", "ASCENSOR",   240, CAMINANDO, SALTANDO);
        m.conectar("PASILLO3", "A201",       420, CAMINANDO, SALTANDO);
        m.conectar("ASCENSOR", "A613",       600, CAMINANDO, PATAS_ARRIBA);
        m.conectar("ASCENSOR", "A612",       660, CAMINANDO, SALTANDO, PATAS_ARRIBA);
        m.conectar("ASCENSOR", "PATIO",      480, CAMINANDO, PATAS_ARRIBA);
        m.conectar("ASCENSOR", "A201",       360, CAMINANDO, SALTANDO);
        m.conectar("ESCALERA", "A633",      1020, CAMINANDO, SALTANDO);
        m.conectar("ESCALERA", "PATIO",      720, CAMINANDO, SALTANDO, PATAS_ARRIBA);
        m.conectar("ESCALERA", "OBRA",       240, CAMINANDO, SALTANDO, PATAS_ARRIBA);
        m.conectar("A613",     "A612",       150, CAMINANDO, SALTANDO);   // ejemplo del enunciado
        m.conectar("A612",     "A633",       240, CAMINANDO, SALTANDO, PATAS_ARRIBA);
        m.conectar("A613",     "A633",       300, CAMINANDO, SALTANDO, PATAS_ARRIBA);
        m.conectar("A633",     "OBRA",       180, CAMINANDO, SALTANDO, PATAS_ARRIBA);
        m.conectar("PATIO",    "STARBUCKS",  120, CAMINANDO, SALTANDO, PATAS_ARRIBA);
        m.conectar("PATIO",    "LIMA3",      360, CAMINANDO, SALTANDO);
        m.conectar("PATIO",    "BIBLIO",     480, CAMINANDO, SALTANDO, PATAS_ARRIBA);
        m.conectar("STARBUCKS","LIMA3",      300, CAMINANDO, PATAS_ARRIBA);
        m.conectar("LIMA3",    "BIBLIO",     540, CAMINANDO, SALTANDO);
        m.conectar("BIBLIO",   "A201",       480, CAMINANDO, SALTANDO, PATAS_ARRIBA);
        m.conectar("OBRA",     "BIBLIO",     150, CAMINANDO, SALTANDO, PATAS_ARRIBA);
        return m;
    }

    /** Mismo mapa, pero el Starbucks cierra todo el día por inventario: no hay solución. */
    public static Mapa uadeImposible() {
        Mapa m = uade();
        m.get("STARBUCKS").agregarCierre(new Ventana(0, 100_000));
        return m;
    }

    /** Versión reducida (6 lugares) para mostrar el árbol de exploración completo en la defensa. */
    public static Mapa uadeChico() {
        Mapa m = new Mapa();
        m.agregarEstacion("A348",      "Aula 348 (inicio y fin)", 40,  30, 348);
        m.agregarEstacion("PASILLO3",  "Pasillo 3er piso",        60,  30,  -1);
        m.agregarEstacion("A613",      "Aula 613",                90,  70, 613);
        m.agregarEstacion("PATIO",     "Patio central",           40,  60,  -1);
        m.agregarEstacion("STARBUCKS", "Starbucks del patio",     30,  65,  -1);
        m.agregarEstacion("OBRA",      "Zona en obra (prohibida)", 0,  90,  -1);
        m.get("A613").setObligatoria(true);
        m.get("STARBUCKS").setObligatoria(true);
        m.get("OBRA").setProhibida(true);
        m.get("STARBUCKS").agregarCierre(new Ventana(1500, 2100));
        m.conectar("A348",      "PASILLO3",  600, CAMINANDO, SALTANDO);
        m.conectar("A348",      "PATIO",     900, CAMINANDO, PATAS_ARRIBA);
        m.conectar("A348",      "A613",      1200, CAMINANDO, PATAS_ARRIBA);
        m.conectar("PASILLO3",  "A613",      900, CAMINANDO, SALTANDO);
        m.conectar("PASILLO3",  "STARBUCKS", 1200, CAMINANDO);
        m.conectar("A613",      "STARBUCKS", 1500, CAMINANDO, SALTANDO);
        m.conectar("A613",      "OBRA",      300, CAMINANDO);
        m.conectar("PATIO",     "STARBUCKS", 300, CAMINANDO, SALTANDO);
        m.conectar("PATIO",     "OBRA",      300, CAMINANDO);
        return m;
    }

    /**
     * Grafo aleatorio de n lugares para las mediciones de complejidad práctica.
     * Siempre contiene un ciclo que pasa por todos los nodos (garantiza al menos una ruta),
     * más aristas extra con probabilidad 'densidad'.
     */
    public static Mapa aleatorio(int n, double densidad, long semilla) {
        Random r = new Random(semilla);
        Mapa m = new Mapa();
        for (int i = 0; i < n; i++) {
            int aula = (i == 0) ? 348 : (r.nextBoolean() ? 100 + r.nextInt(600) : -1);
            m.agregarEstacion("N" + i, "Lugar " + i, r.nextDouble() * 100, r.nextDouble() * 100, aula);
        }
        boolean[][] unido = new boolean[n][n];
        List<Integer> orden = new ArrayList<>();
        for (int i = 1; i < n; i++) orden.add(i);
        Collections.shuffle(orden, r);
        orden.add(0, 0);
        for (int i = 0; i < n; i++) {
            int a = orden.get(i), b = orden.get((i + 1) % n);
            unir(m, r, a, b, unido, true);
        }
        for (int a = 0; a < n; a++)
            for (int b = a + 1; b < n; b++)
                if (!unido[a][b] && r.nextDouble() < densidad) unir(m, r, a, b, unido, false);

        // ~ la mitad de los lugares son obligatorios
        for (int i = 1; i < n; i++) if (r.nextDouble() < 0.5) m.get("N" + i).setObligatoria(true);
        // una ventana de cierre en un lugar al azar
        m.get("N" + (1 + r.nextInt(n - 1))).agregarCierre(new Ventana(600, 900));
        return m;
    }

    private static void unir(Mapa m, Random r, int a, int b, boolean[][] unido, boolean ciclo) {
        unido[a][b] = unido[b][a] = true;
        int seg = 60 + r.nextInt(240);
        List<Modo> modos = new ArrayList<>(List.of(CAMINANDO));   // caminar siempre permitido
        if (r.nextBoolean()) modos.add(SALTANDO);
        if (r.nextBoolean()) modos.add(PATAS_ARRIBA);
        m.conectar("N" + a, "N" + b, seg, modos.toArray(new Modo[0]));
    }
}
