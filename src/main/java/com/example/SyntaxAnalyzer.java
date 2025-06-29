package com.example;

import java.util.List;
import java.util.ArrayList; // For storing syntax errors or other info

public class SyntaxAnalyzer {

    private List<Token> tokens;
    private int currentTokenIndex;
    private Token currentToken;
    private List<String> syntaxLog; // To log rules and errors

    public SyntaxAnalyzer(List<Token> tokens) {
        this.tokens = tokens;
        this.currentTokenIndex = 0;
        this.currentToken = tokens.isEmpty() ? null : tokens.get(0);
        this.syntaxLog = new ArrayList<>();
    }

    private void advance() {
        currentTokenIndex++;
        if (currentTokenIndex < tokens.size()) {
            currentToken = tokens.get(currentTokenIndex);
        } else {
            currentToken = null; // End of tokens
        }
    }

    private boolean match(TokenType expectedType) {
        if (currentToken != null && currentToken.getType() == expectedType) {
            logRule("Consumido token: " + currentToken.getType() + " ('" + currentToken.getLexeme() + "')");
            advance();
            return true;
        }
        return false;
    }

    private boolean matchKeyword(String keywordText) {
        if (currentToken != null && currentToken.getType() == TokenType.KEYWORD && currentToken.getLexeme().equalsIgnoreCase(keywordText)) {
            logRule("Consumido keyword: " + currentToken.getLexeme());
            advance();
            return true;
        }
        return false;
    }

    private void expect(TokenType expectedType, String errorMessage) {
        if (!match(expectedType)) {
            reportError(errorMessage + ". Se encontró: " + (currentToken != null ? currentToken.getType() + " ('" + currentToken.getLexeme() + "')" : "FIN DE ARCHIVO"));
            // Basic error recovery: advance to next token to allow further parsing attempts,
            // but this can lead to cascaded errors. More sophisticated recovery is complex.
            // For now, we might just stop or try to find a semicolon or known statement keyword.
            // For this initial design, we'll let it try to continue, but errors might cascade.
        }
    }

    private void expectKeyword(String keywordText, String errorMessage) {
         if (currentToken != null && currentToken.getType() == TokenType.KEYWORD && currentToken.getLexeme().equalsIgnoreCase(keywordText)) {
            logRule("Consumido keyword: " + currentToken.getLexeme());
            advance();
        } else {
            reportError(errorMessage + ". Se encontró: " + (currentToken != null ? currentToken.getType() + " ('" + currentToken.getLexeme() + "')" : "FIN DE ARCHIVO"));
        }
    }


    private void reportError(String message) {
        String errorLine = (currentToken != null) ? String.valueOf(currentToken.getLineNumber()) : "N/A";
        String logMessage = "Error Sintáctico (Línea " + errorLine + "): " + message;
        syntaxLog.add(logMessage);
        System.err.println(logMessage); // Also print to stderr for immediate visibility
        // Potentially throw an exception or set an error flag
    }

    private void logRule(String ruleMessage) {
        String logMessage = "Regla/Acción Sintáctica: " + ruleMessage;
        syntaxLog.add(logMessage);
        System.out.println(logMessage); // Print to stdout for visibility of rules
    }

    public List<String> getSyntaxLog() {
        return syntaxLog;
    }

    // Main parsing method to be called
    public void parse() {
        logRule("Iniciando análisis sintáctico...");
        if (tokens.isEmpty() || currentToken == null) { // Check currentToken as well
            logRule("No hay tokens para analizar o se llegó al final inesperadamente.");
            return;
        }
        parsePrograma();

        if (currentToken != null) {
            reportError("Se esperaba FinDeArchivo pero se encontraron tokens adicionales a partir de: " + currentToken.getLexeme());
        }
        logRule("Análisis sintáctico completado.");
    }

    // <programa> ::= "Proceso" IDENTIFICADOR <bloque_sentencias> "FinProceso"
    private void parsePrograma() {
        logRule("Analizando <programa>");
        expectKeyword("Proceso", "Se esperaba la palabra reservada 'Proceso'");
        expect(TokenType.IDENTIFIER, "Se esperaba un IDENTIFICADOR para el nombre del proceso");
        parseBloqueSentencias();
        expectKeyword("FinProceso", "Se esperaba la palabra reservada 'FinProceso'");
        logRule("Fin <programa>");
    }

    // <bloque_sentencias> ::= <sentencia>*
    // This will try to parse sentences until a keyword that cannot start a sentence is found,
    // or until a token indicating end of block (like FinProceso, FinSi, SiNo etc.) is found.
    private void parseBloqueSentencias() {
        logRule("Analizando <bloque_sentencias>");
        // Loop while the current token can start a known sentence or is not an end-of-block marker
        while (currentToken != null && canStartSentencia(currentToken) && !isEndOfBlockMarker(currentToken)) {
            parseSentencia();
        }
        logRule("Fin <bloque_sentencias>");
    }

    private boolean canStartSentencia(Token token) {
        if (token == null) return false;
        if (token.getType() == TokenType.KEYWORD) {
            String lexeme = token.getLexeme().toLowerCase(); // PSeInt keywords are case-insensitive
            switch (lexeme) {
                case "definir":
                case "leer":
                case "escribir":
                case "si":
                // case "mientras": // etc.
                    return true;
            }
        } else if (token.getType() == TokenType.IDENTIFIER) {
            // Could be an assignment or procedure call (not implemented yet)
            return true;
        }
        return false;
    }

    private boolean isEndOfBlockMarker(Token token) {
        if (token == null) return true; // End of file is an end of block
        if (token.getType() == TokenType.KEYWORD) {
            String lexeme = token.getLexeme().toLowerCase();
            switch (lexeme) {
                case "finproceso":
                case "finsi":
                case "sino":
                // case "finmientras":
                // case "hastaque":
                // case "finsegun":
                    return true;
            }
        }
        return false;
    }

    // <sentencia> ::= <sent_definir> | <sent_leer> | <sent_escribir> | <sent_asignacion> | <sent_si>
    private void parseSentencia() {
        logRule("Analizando <sentencia>");
        if (currentToken == null) {
            reportError("Se esperaba una sentencia, pero se encontró el fin de archivo.");
            return;
        }

        if (currentToken.getType() == TokenType.KEYWORD) {
            String keyword = currentToken.getLexeme().toLowerCase();
            switch (keyword) {
                case "definir":
                    parseSentenciaDefinir();
                    break;
                case "leer":
                    parseSentenciaLeer();
                    break;
                case "escribir":
                    parseSentenciaEscribir();
                    break;
                case "si":
                    parseSentenciaSi();
                    break;
                default:
                    reportError("Palabra clave no reconocida o no esperada para iniciar una sentencia: " + currentToken.getLexeme());
                    advance(); // Consume the unexpected token to try to recover
            }
        } else if (currentToken.getType() == TokenType.IDENTIFIER) {
            // Could be an assignment. A lookahead might be needed if procedure calls were allowed without a keyword.
            // For now, assume IDENTIFIER at start of statement is assignment.
            parseSentenciaAsignacion();
        } else {
            reportError("Sentencia inválida. Se encontró: " + currentToken.getLexeme());
            advance(); // Consume to prevent infinite loop on unhandled token
        }
        logRule("Fin <sentencia>");
    }

    // <sent_definir> ::= "Definir" <lista_variables> "Como" <tipo_dato> ";"
    private void parseSentenciaDefinir() {
        logRule("Analizando <sent_definir>");
        expectKeyword("Definir", "Se esperaba 'Definir'");
        parseListaVariables();
        expectKeyword("Como", "Se esperaba 'Como' en la definición");
        parseTipoDato();
        expect(TokenType.DELIMITER, "Se esperaba ';' al final de la sentencia Definir"); // PSeInt uses ;
        logRule("Fin <sent_definir>");
    }

    // <lista_variables> ::= IDENTIFICADOR ("," IDENTIFICADOR)*
    private void parseListaVariables() {
        logRule("Analizando <lista_variables>");
        expect(TokenType.IDENTIFIER, "Se esperaba un IDENTIFICADOR de variable");
        while (currentToken != null && currentToken.getType() == TokenType.DELIMITER && currentToken.getLexeme().equals(",")) {
            match(TokenType.DELIMITER); // Consume ","
            expect(TokenType.IDENTIFIER, "Se esperaba un IDENTIFICADOR de variable después de la coma");
        }
        logRule("Fin <lista_variables>");
    }

    // <tipo_dato> ::= "Entero" | "Real" | "Caracter" | "Logico" | "Cadena"
    private void parseTipoDato() {
        logRule("Analizando <tipo_dato>");
        if (currentToken != null && currentToken.getType() == TokenType.KEYWORD) {
            String lexeme = currentToken.getLexeme().toLowerCase();
            switch (lexeme) {
                case "entero":
                case "real":
                case "caracter":
                case "logico":
                case "cadena":
                    logRule("Tipo de dato reconocido: " + currentToken.getLexeme());
                    advance(); // Consume type keyword
                    break;
                default:
                    // PSeInt is also flexible allowing IDENTIFIER as type (e.g. for arrays/custom not handled yet)
                    // For now, strictly keywords.
                     reportError("Tipo de dato no reconocido: " + currentToken.getLexeme());
            }
        } else if (currentToken != null && currentToken.getType() == TokenType.IDENTIFIER) {
            // PSeInt allows any identifier as a type, often for arrays or user-defined (not supported yet)
            // For simplicity, we can allow IDENTIFIER here but log it.
            // Or be strict and only allow keyword types for now.
            // Let's be strict for now as per BNF.
            // To be more PSeInt-like, one might just consume IDENTIFIER here.
            reportError("Se esperaba una palabra clave de tipo de dato (Entero, Real, etc.), pero se encontró IDENTIFICADOR: " + currentToken.getLexeme());
            // advance(); // Optionally consume it if we want to be more lenient or if IDENTIFIER types are valid.
        }
        else {
            reportError("Se esperaba un tipo de dato (Entero, Real, etc.)");
        }
        logRule("Fin <tipo_dato>");
    }

    // <sent_leer> ::= "Leer" <lista_variables> ";"
    private void parseSentenciaLeer() {
        logRule("Analizando <sent_leer>");
        expectKeyword("Leer", "Se esperaba 'Leer'");
        parseListaVariables();
        expect(TokenType.DELIMITER, "Se esperaba ';' al final de la sentencia Leer");
        logRule("Fin <sent_leer>");
    }

    // <sent_escribir> ::= "Escribir" <lista_expresiones> ";"
    private void parseSentenciaEscribir() {
        logRule("Analizando <sent_escribir>");
        expectKeyword("Escribir", "Se esperaba 'Escribir'");
        parseListaExpresiones();
        expect(TokenType.DELIMITER, "Se esperaba ';' al final de la sentencia Escribir");
        logRule("Fin <sent_escribir>");
    }

    // <lista_expresiones> ::= <expresion> ("," <expresion>)*
    private void parseListaExpresiones() {
        logRule("Analizando <lista_expresiones>");
        parseExpresion(); // Parse the first expression
        while (currentToken != null && currentToken.getType() == TokenType.DELIMITER && currentToken.getLexeme().equals(",")) {
            match(TokenType.DELIMITER); // Consume ","
            parseExpresion(); // Parse subsequent expressions
        }
        logRule("Fin <lista_expresiones>");
    }

    // <sent_asignacion> ::= IDENTIFICADOR ( "<-" | "=" ) <expresion> ";"
    private void parseSentenciaAsignacion() {
        logRule("Analizando <sent_asignacion>");
        expect(TokenType.IDENTIFIER, "Se esperaba un IDENTIFICADOR para la asignación");

        if (currentToken != null && currentToken.getType() == TokenType.OPERATOR &&
            (currentToken.getLexeme().equals("<-") || currentToken.getLexeme().equals("="))) {
            logRule("Operador de asignación reconocido: " + currentToken.getLexeme());
            advance(); // Consume "<-" or "="
        } else {
            reportError("Se esperaba '<-' o '=' para la asignación. Encontrado: " +
                        (currentToken != null ? currentToken.getLexeme() : "FIN DE ARCHIVO"));
        }
        parseExpresion();
        expect(TokenType.DELIMITER, "Se esperaba ';' al final de la sentencia de asignación");
        logRule("Fin <sent_asignacion>");
    }

    // --- Expresiones (muy simplificado por ahora) ---
    // <expresion> ::= <expresion_simple> ( (<op_relacional> | <op_logico>) <expresion_simple> )*
    // For now, an expression will just be a single factor to keep it simple.
    // This needs proper expansion for operators and precedence.
    private void parseExpresion() {
        logRule("Analizando <expresion>");
        parseExpresionSimple();

        // Loop for relational and logical operators (simplified)
        while (currentToken != null && currentToken.getType() == TokenType.OPERATOR &&
               isRelationalOrLogicalOperator(currentToken.getLexeme())) {
            logRule("Operador relacional/lógico reconocido: " + currentToken.getLexeme());
            advance(); // Consume the operator
            parseExpresionSimple(); // Parse the right-hand side operand
        }
        logRule("Fin <expresion>");
    }

    private boolean isRelationalOrLogicalOperator(String lexeme) {
        // TODO: This should be more robust, perhaps checking specific lexemes from the grammar
        // For now, any operator not covered by arithmetic might be considered (this is too broad)
        // Let's list them explicitly as per our simplified grammar:
        switch (lexeme.toLowerCase()) { // PSeInt operators can be case insensitive like Y, O, NO
            case ">":
            case "<":
            case ">=":
            case "<=":
            case "==": // Standard PSeInt equality
            case "=":  // Also used for equality in PSeInt expressions
            case "<>":
            case "y":  // Logical AND
            case "o":  // Logical OR
            // case "no": // Unary, handled in factor
                return true;
            default:
                return false;
        }
    }

    // <expresion_simple> ::= <termino> ( ("+" | "-") <termino> )*
    private void parseExpresionSimple() {
        logRule("Analizando <expresion_simple>");
        parseTermino();
        while (currentToken != null && currentToken.getType() == TokenType.OPERATOR &&
               (currentToken.getLexeme().equals("+") || currentToken.getLexeme().equals("-"))) {
            logRule("Operador aditivo/sustractivo reconocido: " + currentToken.getLexeme());
            advance(); // Consume "+" or "-"
            parseTermino();
        }
        logRule("Fin <expresion_simple>");
    }

    // <termino> ::= <factor> ( ("*" | "/") <factor> )*
    private void parseTermino() {
        logRule("Analizando <termino>");
        parseFactor();
        while (currentToken != null && currentToken.getType() == TokenType.OPERATOR &&
               (currentToken.getLexeme().equals("*") || currentToken.getLexeme().equals("/"))) {
            logRule("Operador multiplicativo/divisivo reconocido: " + currentToken.getLexeme());
            advance(); // Consume "*" or "/"
            parseFactor();
        }
        logRule("Fin <termino>");
    }

    // <factor> ::= IDENTIFICADOR | NUMERO_ENTERO | NUMERO_REAL | CADENA_LITERAL | "(" <expresion> ")" | ...
    private void parseFactor() {
        logRule("Analizando <factor>");
        if (currentToken == null) {
            reportError("Se esperaba un factor (identificador, número, cadena, etc.), pero se encontró fin de archivo.");
            return;
        }
        switch (currentToken.getType()) {
            case IDENTIFIER:
            case NUMBER: // Covers Entero y Real based on Lexer
            case STRING:
                logRule("Factor reconocido: " + currentToken.getType() + " ('" + currentToken.getLexeme() + "')");
                advance(); // Consume the token
                break;
            case DELIMITER:
                if (currentToken.getLexeme().equals("(")) {
                    match(TokenType.DELIMITER); // Consume "("
                    parseExpresion(); // Recursive call for parenthesized expression
                    expect(TokenType.DELIMITER, "Se esperaba ')' para cerrar la expresión"); // Expect ")"
                } else {
                    reportError("Factor inesperado: Se encontró el delimitador '" + currentToken.getLexeme() + "'");
                }
                break;
            // TODO: Add TRUE, FALSE, unary operators like NOT, -, +
            default:
                reportError("Factor inesperado: " + currentToken.getType() + " ('" + currentToken.getLexeme() + "')");
                // advance(); // Optionally consume to try to recover, but might hide other errors
        }
        logRule("Fin <factor> (simplificado)");
    }

    // <sent_si> ::= "Si" <expresion> "Entonces" <bloque_sentencias> ("SiNo" <bloque_sentencias>)? "FinSi"
    private void parseSentenciaSi() {
        logRule("Analizando <sent_si>");
        expectKeyword("Si", "Se esperaba 'Si'");
        parseExpresion(); // Condition
        expectKeyword("Entonces", "Se esperaba 'Entonces' después de la condición del Si");
        parseBloqueSentencias(); // Block for "Entonces"

        if (currentToken != null && currentToken.getType() == TokenType.KEYWORD && currentToken.getLexeme().equalsIgnoreCase("SiNo")) {
            matchKeyword("SiNo"); // Consume "SiNo"
            parseBloqueSentencias(); // Block for "SiNo"
        }

        expectKeyword("FinSi", "Se esperaba 'FinSi' para cerrar la estructura Si");
        logRule("Fin <sent_si>");
    }
}
