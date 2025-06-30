package com.example.ast;

import java.util.List;
import com.example.Token; // Para el token "Leer"

public class LeerNode implements NodoAST { // Es un tipo de NodoSentencia
    private Token tokenLeer;
    private List<IdentificadorNode> variables;

    public LeerNode(Token tokenLeer, List<IdentificadorNode> variables) {
        this.tokenLeer = tokenLeer;
        this.variables = variables;
    }

    public List<IdentificadorNode> getVariables() {
        return variables;
    }

    @Override
    public String aRepresentacionTextual(String indentacion, boolean esUltimo) {
        StringBuilder sb = new StringBuilder();
        sb.append(indentacion);
        sb.append(esUltimo ? "└── " : "├── ");
        sb.append("Leer");
        sb.append(" (L:").append(tokenLeer.getLineNumber()).append(")\n");

        String indentacionHijos = indentacion + (esUltimo ? "    " : "│   ");
        for (int i = 0; i < variables.size(); i++) {
            sb.append(variables.get(i).aRepresentacionTextual(indentacionHijos, i == variables.size() - 1));
        }
        return sb.toString();
    }
}
