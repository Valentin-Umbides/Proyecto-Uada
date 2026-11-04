package uada;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lee un mapa desde un archivo de texto, así el algoritmo es independiente de los datos.
 * Una instrucción por línea, campos separados por ';', las líneas con '#' son comentarios.
 * El orden de las líneas no importa (las conexiones y ventanas se procesan al final).
 *
 * Instrucciones aceptadas (dos formatos equivalentes):
 *
 *   INICIO;lugar                      (o ORIGEN;lugar). Si falta, se usa el aula 348.
 *   GRAFO;DIRIGIDO | NO_DIRIGIDO      opcional. Por defecto DIRIGIDO.
 *
 *   Lugares:
 *     LUGAR;nombre;aula;x;y;obligatorio;prohibido     aula = 0 si no es aula; obligatorio/prohibido = 0 o 1
 *     LUGAR;codigo;nombre;aula;tipo[;x;y]             aula = -1 si no es aula; tipo = NORMAL|OBLIGATORIO|PROHIBIDO
 *
 *   Conexiones:
 *     ARISTA;a;b;segundos;modos     (o CONEXION;...)  modos: C, S, P  ("CSP" o "C,S,P")
 *       En grafo DIRIGIDO la conexión va solo de a hacia b; en NO_DIRIGIDO va en los dos sentidos.
 *
 *   Ventanas de cierre:
 *     VENTANA;lugar;desde;hasta     (o CIERRE;...)   cerrado en [desde, hasta] segundos
 *
 * Los errores se informan con el número de línea.
 */
public class CargadorMapa {

    public static class Resultado {
        public final Mapa mapa;
        public final String origen;
        Resultado(Mapa mapa, String origen) { this.mapa = mapa; this.origen = origen; }
    }

    /** @param dirigidoForzado null = lo que diga el archivo (por defecto dirigido); true/false = forzar. */
    public static Resultado cargar(String archivo, Boolean dirigidoForzado) throws IOException {
        List<String> lineas = Files.readAllLines(Path.of(archivo), StandardCharsets.UTF_8);
        String origen = null;
        Boolean dirigidoArchivo = null;
        List<String[]> lugares = new ArrayList<>(), conexiones = new ArrayList<>(), ventanas = new ArrayList<>();
        List<Integer> nroLug = new ArrayList<>(), nroCon = new ArrayList<>(), nroVen = new ArrayList<>();

        for (int i = 0; i < lineas.size(); i++) {
            String l = lineas.get(i).replace("﻿", "").strip();   // BOM de Windows
            if (l.isEmpty() || l.startsWith("#")) continue;
            String[] p = l.split(";", -1);
            for (int k = 0; k < p.length; k++) p[k] = p[k].strip();
            int nro = i + 1;
            switch (p[0].toUpperCase(Locale.ROOT)) {
                case "INICIO", "ORIGEN" -> {
                    if (p.length < 2) throw error(nro, "falta el lugar de inicio", l);
                    origen = p[1];
                }
                case "GRAFO" -> {
                    if (p.length < 2) throw error(nro, "usar GRAFO;DIRIGIDO o GRAFO;NO_DIRIGIDO", l);
                    String t = p[1].toUpperCase(Locale.ROOT).replace(" ", "_");
                    if (t.equals("DIRIGIDO")) dirigidoArchivo = true;
                    else if (t.equals("NO_DIRIGIDO")) dirigidoArchivo = false;
                    else throw error(nro, "usar GRAFO;DIRIGIDO o GRAFO;NO_DIRIGIDO", l);
                }
                case "LUGAR" -> { lugares.add(p); nroLug.add(nro); }
                case "ARISTA", "CONEXION" -> { conexiones.add(p); nroCon.add(nro); }
                case "VENTANA", "CIERRE" -> { ventanas.add(p); nroVen.add(nro); }
                default -> throw error(nro, "instrucción desconocida '" + p[0] + "'", l);
            }
        }

        boolean dirigido = dirigidoForzado != null ? dirigidoForzado
                         : dirigidoArchivo != null ? dirigidoArchivo : true;
        Mapa m = new Mapa(dirigido);
        boolean todasConCoordenadas = true;

        for (int i = 0; i < lugares.size(); i++) {
            String[] p = lugares.get(i);
            try {
                if (esFormatoNumerico(p)) {
                    // LUGAR;nombre;aula;x;y;obligatorio;prohibido
                    int aula = Integer.parseInt(p[2]);
                    Estacion e = m.agregarEstacion(p[1], p[1], num(p[3]), num(p[4]), aula > 0 ? aula : -1);
                    e.setObligatoria(p[5].equals("1"));
                    e.setProhibida(p[6].equals("1"));
                } else {
                    // LUGAR;codigo;nombre;aula;tipo[;x;y]
                    if (p.length < 5) throw new IllegalArgumentException("faltan campos");
                    double x = 0, y = 0;
                    if (p.length >= 7 && !p[5].isEmpty() && !p[6].isEmpty()) { x = num(p[5]); y = num(p[6]); }
                    else todasConCoordenadas = false;
                    int aula = Integer.parseInt(p[3]);
                    Estacion e = m.agregarEstacion(p[1], p[2], x, y, aula > 0 ? aula : -1);
                    switch (p[4].toUpperCase(Locale.ROOT)) {
                        case "OBLIGATORIO" -> e.setObligatoria(true);
                        case "PROHIBIDO" -> e.setProhibida(true);
                        case "NORMAL" -> { }
                        default -> throw new IllegalArgumentException("tipo desconocido '" + p[4]
                                + "' (usar NORMAL, OBLIGATORIO o PROHIBIDO)");
                    }
                }
            } catch (RuntimeException ex) {
                throw error(nroLug.get(i), mensaje(ex), String.join(";", p));
            }
        }

        for (int i = 0; i < conexiones.size(); i++) {
            String[] p = conexiones.get(i);
            try {
                if (p.length < 5) throw new IllegalArgumentException("faltan campos (ARISTA;a;b;segundos;modos)");
                List<Modo> modos = new ArrayList<>();
                for (char c : p[4].toUpperCase(Locale.ROOT).toCharArray()) {
                    switch (c) {
                        case 'C' -> modos.add(Modo.CAMINANDO);
                        case 'S' -> modos.add(Modo.SALTANDO);
                        case 'P' -> modos.add(Modo.PATAS_ARRIBA);
                        case ',', ' ' -> { }
                        default -> throw new IllegalArgumentException("modo desconocido '" + c + "' (usar C, S o P)");
                    }
                }
                m.conectar(p[1], p[2], Integer.parseInt(p[3]), modos.toArray(new Modo[0]));
            } catch (RuntimeException ex) {
                throw error(nroCon.get(i), mensaje(ex), String.join(";", p));
            }
        }

        for (int i = 0; i < ventanas.size(); i++) {
            String[] p = ventanas.get(i);
            try {
                if (p.length < 4) throw new IllegalArgumentException("faltan campos (VENTANA;lugar;desde;hasta)");
                m.get(p[1]).agregarCierre(new Ventana(num(p[2]), num(p[3])));
            } catch (RuntimeException ex) {
                throw error(nroVen.get(i), mensaje(ex), String.join(";", p));
            }
        }

        if (origen == null) {   // el enunciado fija el aula 348
            for (Estacion e : m.getEstaciones()) if (e.getNumeroAula() == 348) origen = e.getCodigo();
            if (origen == null) throw new IllegalArgumentException("Falta la línea INICIO;lugar");
        }
        m.get(origen);           // valida que exista
        m.setUsarCoordenadas(todasConCoordenadas);
        return new Resultado(m, origen);
    }

    /** LUGAR;nombre;aula;x;y;obligatorio;prohibido -> 7 campos, el 2do y los dos últimos numéricos. */
    private static boolean esFormatoNumerico(String[] p) {
        return p.length == 7 && p[2].matches("-?\\d+") && p[5].matches("[01]") && p[6].matches("[01]");
    }

    private static IllegalArgumentException error(int nro, String msg, String linea) {
        return new IllegalArgumentException("Línea " + nro + ": " + msg + "  ->  " + linea);
    }

    private static String mensaje(RuntimeException ex) {
        if (ex instanceof NumberFormatException) return "número inválido (" + ex.getMessage() + ")";
        return ex.getMessage();
    }

    private static double num(String s) {
        return Double.parseDouble(s.replace(',', '.'));
    }
}
