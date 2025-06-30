package com.example.ast;

import com.example.Token;

public class ExpresionParentizadaNode implements NodoAST {
    private NodoAST expresionInterna;
    private Token parentesisApertura; // Para línea/columna

    public ExpresionParentizadaNode(NodoAST expresionInterna, Token parentesisApertura) {
        this.expresionInterna = expresionInterna;
        this.parentesisApertura = parentesisApertura;
    }

    public NodoAST getExpresionInterna() {
        return expresionInterna;
    }

    @Override
    public String aRepresentacionTextual(String indentacion, boolean esUltimo) {
        StringBuilder sb = new StringBuilder();
        sb.append(indentacion);
        sb.append(esUltimo ? "└── " : "├── ");
        sb.append("ExpresionParentizada");
        if (parentesisApertura != null) {
             sb.append(" (L:").append(parentesisApertura.getLineNumber()).append(", C:").append(parentesisApertura.getColumnNumber()).append(")\n");
        } else {
            sb.append("\n");
        }

        String indentacionHijo = indentacion + (esUltimo ? "    " : "│   ");
        sb.append(expresionInterna.aRepresentacionTextual(indentacionHijo, true));
        return sb.toString();
    }
}
