package com.example.ast;

import com.example.Token;
import com.example.TokenType;

public class LiteralNode implements NodoAST { // Podría ser también un NodoExpresion
    private Token token; // El token NUMBER, STRING, o KEYWORD (Verdadero/Falso)

    // Este método no es ideal aquí si el token puede ser nulo durante la construcción.
    // Se asume que el token ya fue validado por el parser antes de crear el LiteralNode.
    // Sin embargo, la traza de error indica que `this.token.getLexeme()` falla en `isLiteralType`
    // que se llama desde el constructor. Esto sugiere que `token` es nulo *dentro* del constructor
    // antes de ser asignado a `this.token`, o `isLiteralType` se llamó con un token nulo.

    // Revisando el constructor original:
    // public LiteralNode(Token token) {
    //    if (token == null || !isLiteralType(token.getType())) { // Error aquí si token es nulo
    //    }
    //    this.token = token;
    // }
    // El problema es que isLiteralType(token.getType()) llama a this.token.getLexeme() si type es KEYWORD.
    // Pero this.token aún no ha sido asignado.

    // Corregimos isLiteralType para que use el parámetro token, no this.token
    private static boolean isLiteralType(Token localToken) {
        if (localToken == null) return false;
        TokenType type = localToken.getType();
        return type == TokenType.NUMBER ||
               type == TokenType.STRING ||
               (type == TokenType.KEYWORD &&
                (localToken.getLexeme().equalsIgnoreCase("verdadero") || localToken.getLexeme().equalsIgnoreCase("falso")));
    }

    // Constructor corregido para usar la versión estática de isLiteralType
    // public LiteralNode(Token token) { // Ya está arriba, solo referenciando
    //     if (token == null || !isLiteralType(token)) { // Ahora isLiteralType es estático y usa el token pasado
    //         // Considerar lanzar una excepción o manejar el error
    //         // Por ahora, si SyntaxAnalyzer asegura un token válido, esto es una salvaguarda.
    //         // Si el token es inválido, this.token podría quedar nulo o incorrecto.
    //         // Mejor lanzar excepción si el token no es válido:
    //         throw new IllegalArgumentException("Token inválido para LiteralNode: " + (token != null ? token.toString() : "null"));
    //     }
    //     this.token = token;
    // }
    // La versión actual del constructor ya es:
    // public LiteralNode(Token token) {
    //     this.token = token;
    // }
    // Esto implica que SyntaxAnalyzer DEBE garantizar que el token es válido.
    // El error original en la línea 20 de LiteralNode.java es:
    // (type == TokenType.KEYWORD && (token.getLexeme().equalsIgnoreCase("verdadero") || token.getLexeme().equalsIgnoreCase("falso")));
    // Aquí, `token` se refiere a `this.token`. Si el constructor es llamado con un token nulo, y luego se llama a `isLiteralType`
    // (que no debería ser llamada desde el constructor si este puede recibir un token nulo y no lo ha asignado aún),
    // o si `isLiteralType` es llamada con un token nulo (que no parece ser el caso por la firma `isLiteralType(TokenType type)`).

    // El problema está en la lógica original de `isLiteralType(TokenType type)` que usaba `this.token`
    // cuando debería haber usado el token correspondiente al `type` pasado.
    // La corrección es hacer que `isLiteralType` tome el `Token` completo.

    // Re-revisando el código original de LiteralNode.java:
    // private boolean isLiteralType(TokenType type) {
    //     return type == TokenType.NUMBER ||
    //            type == TokenType.STRING ||
    //            (type == TokenType.KEYWORD &&
    //             (token.getLexeme().equalsIgnoreCase("verdadero") || token.getLexeme().equalsIgnoreCase("falso")));
    // }
    // El `token.getLexeme()` aquí es `this.token.getLexeme()`.
    // Si el constructor es `public LiteralNode(Token tokenParam) { this.token = tokenParam; }`
    // y `parseFactor` llama `new LiteralNode(factorToken)` donde `factorToken` es nulo, entonces `this.token` será nulo.
    // Luego, si CUALQUIER método en `LiteralNode` (como `getValor` o `aRepresentacionTextual`) intenta acceder a `this.token.getLexeme()`, fallará.

    // La traza dice: `LiteralNode.isLiteralType(LiteralNode.java:20)`
    // Línea 20 es: `(token.getLexeme().equalsIgnoreCase("verdadero") || token.getLexeme().equalsIgnoreCase("falso")));`
    // Y el constructor es:
    // `public LiteralNode(Token token) { if (token == null || !isLiteralType(token.getType())) { ... } this.token = token; }`
    // Aquí está el problema: `isLiteralType` se llama ANTES de que `this.token` sea asignado.
    // Y `isLiteralType` usa `this.token` (implícitamente).

    // Solución: `isLiteralType` debe tomar el `Token` como parámetro.

    // El constructor ahora solo asigna el token. La validación debe ocurrir ANTES en SyntaxAnalyzer.
    // public LiteralNode(Token token) {
    //     this.token = token;
    // }
    // No, el constructor original es el que causaba el problema.
    // Lo restauramos y usamos la versión estática de isLiteralType.
    public LiteralNode(Token tokenParam) { // Renombrado para evitar confusión con this.token
        if (tokenParam == null || !isLiteralType(tokenParam)) {
            throw new IllegalArgumentException("Token inválido para LiteralNode: " + (tokenParam != null ? tokenParam.toString() : "null"));
        }
        this.token = tokenParam;
    }


    public Object getValor() {
        if (token == null) return null; // No debería pasar si el constructor valida.
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
                if (lexeme.startsWith("'") && lexeme.endsWith("'")) { // PSeInt usa comillas simples para caracteres
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
        if (token == null) return TokenType.ERROR; // Salvaguarda
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
        if (token == null) { // Salvaguarda
             return indentacion + (esUltimo ? "└── " : "├── ") + "Literal (Token NULO)\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(indentacion);
        sb.append(esUltimo ? "└── " : "├── ");
        String tipoStr = "";
        if (token.getType() == TokenType.STRING) tipoStr = "Cadena";
        else if (token.getType() == TokenType.NUMBER) tipoStr = "Numero";
        else if (token.getType() == TokenType.KEYWORD && (token.getLexeme().equalsIgnoreCase("verdadero") || token.getLexeme().equalsIgnoreCase("falso"))) tipoStr = "Logico";
        else tipoStr = token.getType().toString();


        sb.append("Literal ").append(tipoStr).append(": ").append(token.getLexeme());
        sb.append(" (L:").append(getLinea()).append(", C:").append(getColumna()).append(")\n");
        return sb.toString();
    }
}
