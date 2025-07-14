package com.example.ast;

import com.example.Token;

public class IdentificadorNode implements NodoAST { // Podría ser también un NodoExpresion
    private Token token; // El token IDENTIFIER

    public IdentificadorNode(Token token) {
        if (token == null || token.getType() != com.example.TokenType.IDENTIFIER) {
            // Considerar lanzar una excepción si el token no es un identificador,
            // aunque el parser debería asegurar esto.
            // Por ahora, permitimos la creación, pero el nombre podría ser incorrecto.
        }
        this.token = token;
    }

    public String getNombre() {
        return token != null ? token.getLexeme() : "<identificador_desconocido>";
    }

    public Token getToken() {
        return token;
    }

    public int getLinea() {
        return token != null ? token.getLineNumber() : 0;
    }

    public int getColumna() {
        return token != null ? token.getColumnNumber() : 0;
    }


    @Override
    public String aRepresentacionTextual(String indentacion, boolean esUltimo) {
        StringBuilder sb = new StringBuilder();
        sb.append(indentacion);
        sb.append(esUltimo ? "└── " : "├── ");
        sb.append("Identificador: ").append(getNombre());
        sb.append(" (L:").append(getLinea()).append(", C:").append(getColumna()).append(")\n");
        return sb.toString();
    }
}
