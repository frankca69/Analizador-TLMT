package com.example;

public class Simbolo {
    private String nombre;
    private String tipo; // E.g., "Entero", "Real", "Cadena", "Logico", "Proceso"
    private String categoria; // E.g., "nombre_proceso", "variable", "parametro_funcion", "nombre_funcion"
    private String alcance; // E.g., "global", nombre de la funcion/procedimiento
    private int linea; // Línea donde fue definido
    private Object valor; // Valor actual o inicial, puede ser String, Integer, Double, Boolean, o null
    private int sizeInBytes; // Tamaño en bytes del tipo de dato

    public Simbolo(String nombre, String tipo, String categoria, String alcance, int linea, Object valor, int sizeInBytes) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.categoria = categoria;
        this.alcance = alcance;
        this.linea = linea;
        this.valor = valor;
        this.sizeInBytes = sizeInBytes;
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

    public int getSizeInBytes() {
        return sizeInBytes;
    }

    @Override
    public String toString() {
        return String.format("| %-20s | %-10s | %-15s | %-10s | %-5d | %-15s | %-15s |",
                nombre,
                tipo != null ? tipo : "N/A",
                categoria != null ? categoria : "N/A",
                alcance != null ? alcance : "N/A",
                linea,
                valor != null ? valor.toString() : "<indefinido>",
                sizeInBytes);
    }

    public static String getHeader() {
        return String.format("| %-20s | %-10s | %-15s | %-10s | %-5s | %-15s | %-15s |",
                "NOMBRE", "TIPO", "CATEGORIA", "ALCANCE", "LINEA", "VALOR", "TAMAÑO (BYTES)") +
               "\n" + String.format("|-%-20s-|-%-10s-|-%-15s-|-%-10s-|-%-5s-|-%-15s-|-%-15s-|",
                "--------------------", "----------", "---------------", "----------", "-----", "---------------", "---------------");
    }
}
