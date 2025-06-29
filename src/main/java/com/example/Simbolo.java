package com.example;

public class Simbolo {
    private String nombre;
    private String tipo; // E.g., "Entero", "Real", "Cadena", "Logico", "Proceso"
    private String categoria; // E.g., "nombre_proceso", "variable", "parametro_funcion", "nombre_funcion"
    private String alcance; // E.g., "global", nombre de la funcion/procedimiento
    private int linea; // Línea donde fue definido
    private Object valor; // Valor actual o inicial, puede ser String, Integer, Double, Boolean, o null

    public Simbolo(String nombre, String tipo, String categoria, String alcance, int linea, Object valor) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.categoria = categoria;
        this.alcance = alcance;
        this.linea = linea;
        this.valor = valor;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getAlcance() {
        return alcance;
    }

    public int getLinea() {
        return linea;
    }

    public Object getValor() {
        return valor;
    }

    public void setValor(Object valor) {
        this.valor = valor;
    }

    @Override
    public String toString() {
        return String.format("| %-20s | %-10s | %-15s | %-10s | %-5d | %-20s |",
                nombre,
                tipo != null ? tipo : "N/A",
                categoria != null ? categoria : "N/A",
                alcance != null ? alcance : "N/A",
                linea,
                valor != null ? valor.toString() : "<indefinido>");
    }

    public static String getHeader() {
        return String.format("| %-20s | %-10s | %-15s | %-10s | %-5s | %-20s |",
                "NOMBRE", "TIPO", "CATEGORIA", "ALCANCE", "LINEA", "VALOR") +
               "\n" + String.format("|-%-20s-|-%-10s-|-%-15s-|-%-10s-|-%-5s-|-%-20s-|",
                "--------------------", "----------", "---------------", "----------", "-----", "--------------------");
    }
}
