package com.example;

public class ErrorCompilacion {

    public enum TipoError {
        LEXICO,
        SINTACTICO,
        SEMANTICO // Para el futuro
    }

    private TipoError tipoError;
    private String mensaje;
    private int linea;
    private int columna;
    private String lexemaProblematico; // Opcional, puede ser null

    public ErrorCompilacion(TipoError tipoError, String mensaje, int linea, int columna, String lexemaProblematico) {
        this.tipoError = tipoError;
        this.mensaje = mensaje;
        this.linea = linea;
        this.columna = columna;
        this.lexemaProblematico = lexemaProblematico;
    }

    public ErrorCompilacion(TipoError tipoError, String mensaje, int linea, int columna) {
        this(tipoError, mensaje, linea, columna, null);
    }

    public TipoError getTipoError() {
        return tipoError;
    }

    public String getMensaje() {
        return mensaje;
    }

    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }

    public String getLexemaProblematico() {
        return lexemaProblematico;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Error ").append(tipoError).append(": ");
        sb.append("[Línea ").append(linea);
        if (columna > 0) { // Columna 0 o negativa puede indicar que no es aplicable o no se pudo determinar
            sb.append(", Columna ").append(columna);
        }
        sb.append("] ");
        sb.append(mensaje);
        if (lexemaProblematico != null && !lexemaProblematico.isEmpty()) {
            sb.append(" Cerca de: '").append(lexemaProblematico).append("'");
        }
        return sb.toString();
    }
}
