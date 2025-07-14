package com.example.ast;

import com.example.Token; // Para el operador

public class ExpresionBinariaNode implements NodoAST { // Es un tipo de NodoExpresion
    private NodoAST izquierda;
    private Token operador;
    private NodoAST derecha;

    public ExpresionBinariaNode(NodoAST izquierda, Token operador, NodoAST derecha) {
        this.izquierda = izquierda;
        this.operador = operador;
        this.derecha = derecha;
    }

    public NodoAST getIzquierda() {
        return izquierda;
    }

    public Token getOperador() {
        return operador;
    }

    public NodoAST getDerecha() {
        return derecha;
    }

    @Override
    public String aRepresentacionTextual(String indentacion, boolean esUltimo) {
        StringBuilder sb = new StringBuilder();
        sb.append(indentacion);
        sb.append(esUltimo ? "└── " : "├── ");
        sb.append("ExpresionBinaria: ").append(operador.getLexeme());
        sb.append(" (L:").append(operador.getLineNumber()).append(", C:").append(operador.getColumnNumber()).append(")\n");

        String indentacionHijos = indentacion + (esUltimo ? "    " : "│   ");
        sb.append(izquierda.aRepresentacionTextual(indentacionHijos, false)); // Izquierda nunca es el último si hay derecha
        sb.append(derecha.aRepresentacionTextual(indentacionHijos, true));  // Derecha es el último
        return sb.toString();
    }
}
