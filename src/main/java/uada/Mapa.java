package uada;

import java.util.*;

/**
 * Grafo (lista de adyacencia) con a lo sumo una conexión directa por par de lugares.
 * Puede ser dirigido (a -> b) o no dirigido (a <-> b).
 */
public class Mapa {
    private final boolean dirigido;
    private final List<Estacion> estaciones = new ArrayList<>();
    private final Map<String, Estacion> porCodigo = new HashMap<>();
    private final List<List<Conexion>> ady = new ArrayList<>();
    private int cantConexiones = 0;
    private boolean usarCoordenadas = true;   // false si el mapa no trae coordenadas x, y

    public Mapa() { this(false); }
    public Mapa(boolean dirigido) { this.dirigido = dirigido; }
    public boolean isDirigido() { return dirigido; }

    public void setUsarCoordenadas(boolean b) { usarCoordenadas = b; }
    public boolean isUsarCoordenadas() { return usarCoordenadas; }

    public Estacion agregarEstacion(String codigo, String nombre, double x, double y, int numeroAula) {
        if (porCodigo.containsKey(codigo)) throw new IllegalArgumentException("Lugar repetido: " + codigo);
        Estacion e = new Estacion(estaciones.size(), codigo, nombre, x, y, numeroAula);
        estaciones.add(e);
        porCodigo.put(codigo, e);
        ady.add(new ArrayList<>());
        return e;
    }

    public void conectar(String c1, String c2, int segundos, Modo... modos) {
        Estacion a = get(c1), b = get(c2);
        if (a == b) throw new IllegalArgumentException("Conexión de un lugar consigo mismo: " + c1);
        if (segundos <= 0) throw new IllegalArgumentException("El tiempo debe ser positivo: " + c1 + "-" + c2);
        if (modos.length == 0) throw new IllegalArgumentException("Conexión sin modos permitidos: " + c1 + "-" + c2);
        for (Conexion c : ady.get(a.getId()))
            if (c.otroExtremo(a) == b)
                throw new IllegalArgumentException("Ya existe una conexión directa " + c1 + (dirigido ? " -> " : " - ") + c2);
        Conexion c = new Conexion(a, b, segundos, EnumSet.copyOf(Arrays.asList(modos)));
        ady.get(a.getId()).add(c);
        if (!dirigido) ady.get(b.getId()).add(c);   // no dirigido: también se recorre de b hacia a
        cantConexiones++;
    }

    public Estacion get(String codigo) {
        Estacion e = porCodigo.get(codigo);
        if (e == null) throw new IllegalArgumentException("Estación inexistente: " + codigo);
        return e;
    }

    public List<Conexion> adyacentes(Estacion e) { return ady.get(e.getId()); }
    public List<Estacion> getEstaciones() { return estaciones; }
    public int size() { return estaciones.size(); }
    public int getCantConexiones() { return cantConexiones; }
}
