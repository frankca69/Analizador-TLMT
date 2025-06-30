package com.example.ast;

import java.util.List;
import com.example.Token; // Para el token "Escribir"

public class EscribirNode implements NodoAST { // Es un tipo de NodoSentencia
    private Token tokenEscribir;
    private List<NodoAST> expresiones; // Lista de expresiones a escribir

    public EscribirNode(Token tokenEscribir, List<NodoAST> expresiones) {
        this.tokenEscribir = tokenEscribir;
        this.expresiones = expresiones;
    }

    public List<NodoAST> getExpresiones() {
        return expresiones;
    }

    @Override
    public String aRepresentacionTextual(String indentacion, boolean esUltimo) {
        StringBuilder sb = new StringBuilder();
        sb.append(indentacion);
        sb.append(esUltimo ? "└── " : "├── ");
        sb.append("Escribir");
        sb.append(" (L:").append(tokenEscribir.getLineNumber()).append(")\n");

        String indentacionHijos = indentacion + (esUltimo ? "    " : "│   ");
        for (int i = 0; i < expresiones.size(); i++) {
            sb.append(expresiones.get(i).aRepresentacionTextual(indentacionHijos, i == expresiones.size() - 1));
        }
        return sb.toString();
    }
}
