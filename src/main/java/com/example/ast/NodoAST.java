package com.example.ast;

// Interfaz base para todos los nodos del Árbol Sintáctico Abstracto (AST)
public interface NodoAST {
    /**
     * Genera una representación textual del nodo y sus hijos,
     * con indentación para mostrar la jerarquía.
     * @param indentacion La cadena de indentación actual (e.g., "  ", "    ", etc.)
     * @param esUltimo Para la conexión de líneas en la visualización de árbol (opcional, estético)
     * @return Una representación en cadena del subárbol AST.
     */
    String aRepresentacionTextual(String indentacion, boolean esUltimo);
}
