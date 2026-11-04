package uada;

import java.util.ArrayList;
import java.util.List;

/** Un lugar de UADE (vértice del grafo). */
public class Estacion {
    private final int id;
    private final String codigo;
    private final String nombre;
    private final double x, y;          // coordenadas (metros) para la distancia recta
    private final int numeroAula;       // -1 si no es aula
    private boolean obligatoria;
    private boolean prohibida;
    private final List<Ventana> cierres = new ArrayList<>();

    public Estacion(int id, String codigo, String nombre, double x, double y, int numeroAula) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.x = x;
        this.y = y;
        this.numeroAula = numeroAula;
    }

    public boolean esAula() { return numeroAula > 0; }

    /** Recarga = (suma de dígitos del número de aula) / 5. 0 si no es aula. */
    public double recarga() {
        if (!esAula()) return 0;
        int n = numeroAula, suma = 0;
        while (n > 0) { suma += n % 10; n /= 10; }
        return suma / 5.0;
    }

    /** true si la estación está cerrada en el instante t (obstáculo dinámico). */
    public boolean cerradaEn(double t) {
        for (Ventana v : cierres) if (v.contiene(t)) return true;
        return false;
    }

    public double distanciaRecta(Estacion otra) {
        return Math.hypot(x - otra.x, y - otra.y);
    }

    public void agregarCierre(Ventana v) { cierres.add(v); }
    public void setObligatoria(boolean b) { obligatoria = b; }
    public void setProhibida(boolean b) { prohibida = b; }

    public int getId() { return id; }
    public int getNumeroAula() { return numeroAula; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public boolean isObligatoria() { return obligatoria; }
    public boolean isProhibida() { return prohibida; }
    public List<Ventana> getCierres() { return cierres; }

    @Override
    public String toString() { return codigo; }
}
