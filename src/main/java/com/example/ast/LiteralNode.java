package com.example.ast;

import com.example.Token;
import com.example.TokenType;

public class LiteralNode implements NodoAST { // Podría ser también un NodoExpresion
    private Token token; // El token NUMBER, STRING, o KEYWORD (Verdadero/Falso)

    public LiteralNode(Token token) {
        if (token == null || !isLiteralType(token.getType())) {
            // Considerar lanzar una excepción
        }
        this.token = token;
    }

    private boolean isLiteralType(TokenType type) {
        return type == TokenType.NUMBER ||
               type == TokenType.STRING ||
               (type == TokenType.KEYWORD &&
                (token.getLexeme().equalsIgnoreCase("verdadero") || token.getLexeme().equalsIgnoreCase("falso")));
    }

    public Object getValor() {
        if (token == null) return null;
        // Convertir el lexema al tipo apropiado
        switch (token.getType()) {
            case NUMBER:
                try {
                    if (token.getLexeme().contains(".")) {
                        return Double.parseDouble(token.getLexeme());
                    } else {
                        return Integer.parseInt(token.getLexeme());
                    }
                } catch (NumberFormatException e) {
                    return token.getLexeme(); // Devolver como string si falla el parseo (no debería pasar con lexer bueno)
                }
            case STRING:
                // Quitar comillas externas si están incluidas en el lexema del token
                String lexeme = token.getLexeme();
                if (lexeme.startsWith("\"") && lexeme.endsWith("\"")) {
                    return lexeme.substring(1, lexeme.length() - 1);
                }
                if (lexeme.startsWith("'") && lexeme.endsWith("'")) {
                    return lexeme.substring(1, lexeme.length() - 1);
                }
                return lexeme;
            case KEYWORD: // Verdadero o Falso
                return Boolean.parseBoolean(token.getLexeme().toLowerCase());
            default:
                return token.getLexeme(); // Como fallback
        }
    }

    public TokenType getTipoLiteral() {
        return token.getType();
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
        String tipoStr = "";
        if (token.getType() == TokenType.STRING) tipoStr = "Cadena";
        else if (token.getType() == TokenType.NUMBER) tipoStr = "Numero";
        else if (token.getType() == TokenType.KEYWORD) tipoStr = "Logico";

        sb.append("Literal ").append(tipoStr).append(": ").append(token.getLexeme());
        sb.append(" (L:").append(getLinea()).append(", C:").append(getColumna()).append(")\n");
        return sb.toString();
    }
}
