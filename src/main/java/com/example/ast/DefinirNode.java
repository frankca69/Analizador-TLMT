package com.example.ast;

import java.util.List;
import com.example.Token; // Para el token "Definir"

public class DefinirNode implements NodoAST { // Es un tipo de NodoSentencia
    private Token tokenDefinir; // El token "Definir"
    private List<IdentificadorNode> variables;
    private Token tipoDato; // Token del tipo de dato (e.g., "Entero", "Real")

    public DefinirNode(Token tokenDefinir, List<IdentificadorNode> variables, Token tipoDato) {
        this.tokenDefinir = tokenDefinir;
        this.variables = variables;
        this.tipoDato = tipoDato;
    }

    public List<IdentificadorNode> getVariables() {
        return variables;
    }

    public Token getTipoDatoToken() {
        return tipoDato;
    }

    public String getTipoDatoNombre() {
        return tipoDato.getLexeme();
    }


    @Override
    public String aRepresentacionTextual(String indentacion, boolean esUltimo) {
        StringBuilder sb = new StringBuilder();
        sb.append(indentacion);
        sb.append(esUltimo ? "└── " : "├── ");
        sb.append("Definir (Tipo: ").append(tipoDato.getLexeme()).append(")");
        sb.append(" (L:").append(tokenDefinir.getLineNumber()).append(")\n");

        String indentacionHijos = indentacion + (esUltimo ? "    " : "│   ");
        for (int i = 0; i < variables.size(); i++) {
            sb.append(variables.get(i).aRepresentacionTextual(indentacionHijos, i == variables.size() - 1));
        }
        return sb.toString();
    }
}
