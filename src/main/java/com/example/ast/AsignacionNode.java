package com.example.ast;

import com.example.Token; // Para el operador de asignación

public class AsignacionNode implements NodoAST { // Es un tipo de NodoSentencia
    private IdentificadorNode identificador;
    private Token operadorAsignacion; // "<-" o "="
    private NodoAST expresion;

    public AsignacionNode(IdentificadorNode identificador, Token operadorAsignacion, NodoAST expresion) {
        this.identificador = identificador;
        this.operadorAsignacion = operadorAsignacion;
        this.expresion = expresion;
    }

    public IdentificadorNode getIdentificador() {
        return identificador;
    }

    public Token getOperadorAsignacion() {
        return operadorAsignacion;
    }

    public NodoAST getExpresion() {
        return expresion;
    }

    @Override
    public String aRepresentacionTextual(String indentacion, boolean esUltimo) {
        StringBuilder sb = new StringBuilder();
        sb.append(indentacion);
        sb.append(esUltimo ? "└── " : "├── ");
        sb.append("Asignacion: ").append(operadorAsignacion.getLexeme());
        sb.append(" (L:").append(operadorAsignacion.getLineNumber()).append(")\n");

        String indentacionHijos = indentacion + (esUltimo ? "    " : "│   ");
        sb.append(identificador.aRepresentacionTextual(indentacionHijos, false));
        sb.append(expresion.aRepresentacionTextual(indentacionHijos, true));
        return sb.toString();
    }
}
