package com.example;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collection; // Importación añadida

public class TablaDeSimbolos {
    // Usamos LinkedHashMap para mantener el orden de inserción, útil para imprimir la tabla.
    private Map<String, Simbolo> tabla;
    // Podríamos añadir una pila de tablas para manejar alcances anidados en el futuro.
    // private Stack<Map<String, Simbolo>> alcances;
    private String alcanceActual; // Simple manejo de alcance por ahora

    public TablaDeSimbolos() {
        this.tabla = new LinkedHashMap<>();
        this.alcanceActual = "global"; // Alcance por defecto
    }

    public void setAlcanceActual(String alcance) {
        this.alcanceActual = alcance;
    }

    public String getAlcanceActual() {
        return this.alcanceActual;
    }

    /**
     * Agrega un símbolo a la tabla.
     * Verifica si ya existe un símbolo con el mismo nombre en el alcance actual.
     * @param simbolo El símbolo a agregar.
     * @return true si se agregó exitosamente, false si ya existía (error de redeclaración).
     */
    public boolean agregar(Simbolo simbolo) {
        // Para un manejo de alcance más complejo, la clave podría ser "nombre@alcance"
        // o se buscaría solo en la tabla del tope de la pila de alcances.
        // Por ahora, asumimos un solo espacio de nombres global o que el alcance se maneja en el nombre del símbolo.
        String nombreKey = simbolo.getNombre() + "@" + simbolo.getAlcance(); // Clave única por nombre y alcance

        if (tabla.containsKey(nombreKey)) {
            // Error: Símbolo ya definido en este alcance.
            // El SyntaxAnalyzer se encargará de reportar este error usando esta info.
            return false;
        }
        tabla.put(nombreKey, simbolo);
        return true;
    }

    /**
     * Busca un símbolo por nombre en el alcance actual y luego en alcances superiores si se implementa.
     * @param nombre El nombre del símbolo a buscar.
     * @param alcance El alcance en el que buscar.
     * @return El Simbolo encontrado, o null si no existe.
     */
    public Simbolo buscar(String nombre, String alcance) {
        // Búsqueda simple por ahora:
        return tabla.get(nombre + "@" + alcance);
    }

    /**
     * Busca un símbolo por nombre. Primero en el alcance actual, luego en global si no se encuentra.
     * (Simplificado para un sistema de dos niveles: local y global)
     * @param nombre El nombre del símbolo a buscar.
     * @param alcancePrioritario El alcance actual/prioritario (e.g., nombre de una función).
     * @return El Simbolo encontrado, o null si no existe.
     */
    public Simbolo buscarConPrioridad(String nombre, String alcancePrioritario) {
        Simbolo s = buscar(nombre, alcancePrioritario);
        if (s == null && !alcancePrioritario.equals("global")) {
            // Si no se encuentra en el alcance prioritario (y no es ya el global), buscar en global.
            s = buscar(nombre, "global");
        }
        return s;
    }


    public void imprimirTabla() {
        System.out.println("\n--- Tabla de Símbolos ---");
        if (tabla.isEmpty()) {
            System.out.println("La tabla de símbolos está vacía.");
            return;
        }
        System.out.println(Simbolo.getHeader());
        for (Simbolo simbolo : tabla.values()) {
            System.out.println(simbolo.toString());
        }
    }

    public Collection<Simbolo> getSimbolosAsCollection() {
        return tabla.values();
    }
}
