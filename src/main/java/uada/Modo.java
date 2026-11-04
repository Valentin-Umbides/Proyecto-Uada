package uada;

/**
 * Formas de desplazamiento de Uada.
 * factorTiempo  : multiplica el tiempo indicado en el mapa.
 * factorBateria : multiplica el consumo de caminar (1 punto por minuto de mapa).
 */
public enum Modo {
    // El orden de declaración es el orden en que el backtracking prueba los modos:
    // primero el más rápido, así el top-3 se llena antes y la poda por cota corta más ramas.
    SALTANDO("Saltando", 0.5, 2.2),            // mitad de tiempo, 2.2 veces el consumo de caminar
    CAMINANDO("Caminando", 1.0, 1.0),
    PATAS_ARRIBA("Patas para arriba", 2.0, 0.45); // doble de tiempo, 55% menos consumo

    private final String nombre;
    private final double factorTiempo;
    private final double factorBateria;

    Modo(String nombre, double factorTiempo, double factorBateria) {
        this.nombre = nombre;
        this.factorTiempo = factorTiempo;
        this.factorBateria = factorBateria;
    }

    /** Tiempo (segundos) que tarda en recorrer una conexión de 'segundosMapa'. */
    public double tiempo(int segundosMapa) {
        return segundosMapa * factorTiempo;
    }

    /** Batería consumida: caminando = segundos/60 (1 punto por minuto). */
    public double consumo(int segundosMapa) {
        return (segundosMapa / 60.0) * factorBateria;
    }

    public String getNombre() { return nombre; }
}
