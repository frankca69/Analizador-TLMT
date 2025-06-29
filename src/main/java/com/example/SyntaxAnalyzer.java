package com.example;

import java.util.List;
import java.util.ArrayList; // For storing syntax errors or other info

public class SyntaxAnalyzer {

    private List<Token> tokens;
    private int currentTokenIndex;
    private Token currentToken;
    private List<String> syntaxLog; // To log rules and errors
    private TablaDeSimbolos tablaDeSimbolos;

    public SyntaxAnalyzer(List<Token> tokens) {
        this.tokens = tokens;
        this.currentTokenIndex = 0;
        this.currentToken = tokens.isEmpty() ? null : tokens.get(0);
        this.syntaxLog = new ArrayList<>();
        this.tablaDeSimbolos = new TablaDeSimbolos(); // Instanciar la tabla de símbolos
    }

    public TablaDeSimbolos getTablaDeSimbolos() {
        return tablaDeSimbolos;
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
        System.err.println(logMessage);
    }

    private void logRule(String ruleMessage) {
        String logMessage = "Regla/Acción Sintáctica: " + ruleMessage;
        syntaxLog.add(logMessage);
        System.out.println(logMessage);
    }

    public List<String> getSyntaxLog() {
        return syntaxLog;
    }

    public void parse() {
        logRule("Iniciando análisis sintáctico...");
        if (tokens.isEmpty() || currentToken == null) {
            logRule("No hay tokens para analizar o se llegó al final inesperadamente.");
            return;
        }
        parsePrograma();

        if (currentToken != null) {
            reportError("Se esperaba FinDeArchivo pero se encontraron tokens adicionales a partir de: " + currentToken.getLexeme());
        }
        logRule("Análisis sintáctico completado.");
    }

    private void parsePrograma() {
        logRule("Analizando <programa>");
        expectKeyword("Proceso", "Se esperaba la palabra reservada 'Proceso'");

        Token nombreProcesoToken = currentToken;
        if (nombreProcesoToken != null && nombreProcesoToken.getType() == TokenType.IDENTIFIER) {
            Simbolo procSimbolo = new Simbolo(
                nombreProcesoToken.getLexeme(),
                "N/A",
                "nombre_proceso",
                "global",
                nombreProcesoToken.getLineNumber(),
                null);
            if (!tablaDeSimbolos.agregar(procSimbolo)) {
                reportError("Error interno al agregar nombre de proceso a tabla de símbolos.");
            }
            tablaDeSimbolos.setAlcanceActual("global");
            logRule("Nombre del proceso '" + nombreProcesoToken.getLexeme() + "' agregado a la tabla de símbolos. Alcance actual: " + tablaDeSimbolos.getAlcanceActual());
        }
        expect(TokenType.IDENTIFIER, "Se esperaba un IDENTIFICADOR para el nombre del proceso");

        parseBloqueSentencias();
        expectKeyword("FinProceso", "Se esperaba la palabra reservada 'FinProceso'");
        logRule("Fin <programa>");
    }

    private void parseBloqueSentencias() {
        logRule("Analizando <bloque_sentencias>");
        while (currentToken != null && canStartSentencia(currentToken) && !isEndOfBlockMarker(currentToken)) {
            parseSentencia();
        }
        logRule("Fin <bloque_sentencias>");
    }

    private boolean canStartSentencia(Token token) {
        if (token == null) return false;
        if (token.getType() == TokenType.KEYWORD) {
            String lexeme = token.getLexeme().toLowerCase();
            switch (lexeme) {
                case "definir":
                case "leer":
                case "escribir":
                case "si":
                    return true;
            }
        } else if (token.getType() == TokenType.IDENTIFIER) {
            return true;
        }
        return false;
    }

    private boolean isEndOfBlockMarker(Token token) {
        if (token == null) return true;
        if (token.getType() == TokenType.KEYWORD) {
            String lexeme = token.getLexeme().toLowerCase();
            switch (lexeme) {
                case "finproceso":
                case "finsi":
                case "sino":
                    return true;
            }
        }
        return false;
    }

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
                    advance();
            }
        } else if (currentToken.getType() == TokenType.IDENTIFIER) {
            parseSentenciaAsignacion();
        } else {
            reportError("Sentencia inválida. Se encontró: " + currentToken.getLexeme());
            advance();
        }
        logRule("Fin <sentencia>");
    }

    private void parseSentenciaDefinir() {
        logRule("Analizando <sent_definir>");
        expectKeyword("Definir", "Se esperaba 'Definir'");
        List<Token> variablesDefinidas = parseListaVariables();
        expectKeyword("Como", "Se esperaba 'Como' en la definición");
        Token tipoDatoToken = parseTipoDato();

        if (tipoDatoToken != null) {
            for (Token varToken : variablesDefinidas) {
                Simbolo varSimbolo = new Simbolo(
                    varToken.getLexeme(),
                    tipoDatoToken.getLexeme(),
                    "variable",
                    tablaDeSimbolos.getAlcanceActual(),
                    varToken.getLineNumber(),
                    null
                );
                if (!tablaDeSimbolos.agregar(varSimbolo)) {
                    reportError("Variable '" + varToken.getLexeme() + "' ya definida en el alcance '" + tablaDeSimbolos.getAlcanceActual() + "'. Línea: " + varToken.getLineNumber());
                } else {
                    logRule("Variable '" + varToken.getLexeme() + "' ("+tipoDatoToken.getLexeme()+") agregada a la tabla de símbolos.");
                }
            }
        }
        expect(TokenType.DELIMITER, "Se esperaba ';' al final de la sentencia Definir");
        logRule("Fin <sent_definir>");
    }

    private List<Token> parseListaVariables() {
        logRule("Analizando <lista_variables>");
        List<Token> variables = new ArrayList<>();

        if (currentToken != null && currentToken.getType() == TokenType.IDENTIFIER) {
            variables.add(currentToken);
            advance();
        } else {
            expect(TokenType.IDENTIFIER, "Se esperaba un IDENTIFICADOR de variable inicial");
        }

        while (currentToken != null && currentToken.getType() == TokenType.DELIMITER && currentToken.getLexeme().equals(",")) {
            match(TokenType.DELIMITER);
            if (currentToken != null && currentToken.getType() == TokenType.IDENTIFIER) {
                variables.add(currentToken);
                advance();
            } else {
                expect(TokenType.IDENTIFIER, "Se esperaba un IDENTIFICADOR de variable después de la coma");
            }
        }
        logRule("Fin <lista_variables>");
        return variables;
    }

    private Token parseTipoDato() {
        logRule("Analizando <tipo_dato>");
        Token tipoToken = null;
        if (currentToken != null && currentToken.getType() == TokenType.KEYWORD) {
            String lexeme = currentToken.getLexeme().toLowerCase();
            switch (lexeme) {
                case "entero":
                case "real":
                case "caracter":
                case "logico":
                case "cadena":
                    logRule("Tipo de dato reconocido: " + currentToken.getLexeme());
                    tipoToken = currentToken;
                    advance();
                    break;
                default:
                     reportError("Tipo de dato keyword no reconocido: " + currentToken.getLexeme());
            }
        } else {
            reportError("Se esperaba una palabra clave de tipo de dato (Entero, Real, etc.). Encontrado: " +
                        (currentToken != null ? currentToken.getType() + " ('" + currentToken.getLexeme() + "')" : "FIN DE ARCHIVO"));
        }
        logRule("Fin <tipo_dato>");
        return tipoToken;
    }

    private void parseSentenciaLeer() {
        logRule("Analizando <sent_leer>");
        expectKeyword("Leer", "Se esperaba 'Leer'");
        List<Token> variablesLeidas = parseListaVariables();
        for (Token varToken : variablesLeidas) {
            Simbolo s = tablaDeSimbolos.buscarConPrioridad(varToken.getLexeme(), tablaDeSimbolos.getAlcanceActual());
            if (s == null) {
                reportError("Variable '" + varToken.getLexeme() + "' no ha sido definida. Línea: " + varToken.getLineNumber());
            }
        }
        expect(TokenType.DELIMITER, "Se esperaba ';' al final de la sentencia Leer");
        logRule("Fin <sent_leer>");
    }

    private void parseSentenciaEscribir() {
        logRule("Analizando <sent_escribir>");
        expectKeyword("Escribir", "Se esperaba 'Escribir'");
        parseListaExpresiones();
        expect(TokenType.DELIMITER, "Se esperaba ';' al final de la sentencia Escribir");
        logRule("Fin <sent_escribir>");
    }

    private void parseListaExpresiones() {
        logRule("Analizando <lista_expresiones>");
        parseExpresion();
        while (currentToken != null && currentToken.getType() == TokenType.DELIMITER && currentToken.getLexeme().equals(",")) {
            match(TokenType.DELIMITER);
            parseExpresion();
        }
        logRule("Fin <lista_expresiones>");
    }

    private void parseSentenciaAsignacion() {
        logRule("Analizando <sent_asignacion>");
        Token variableAsignada = currentToken;
        expect(TokenType.IDENTIFIER, "Se esperaba un IDENTIFICADOR para la asignación");

        if (variableAsignada != null && variableAsignada.getType() == TokenType.IDENTIFIER) {
            Simbolo s = tablaDeSimbolos.buscarConPrioridad(variableAsignada.getLexeme(), tablaDeSimbolos.getAlcanceActual());
            if (s == null) {
                reportError("Variable '" + variableAsignada.getLexeme() + "' no ha sido definida. Línea: " + variableAsignada.getLineNumber());
            }
        }

        if (currentToken != null && currentToken.getType() == TokenType.OPERATOR &&
            (currentToken.getLexeme().equals("<-") || currentToken.getLexeme().equals("="))) {
            logRule("Operador de asignación reconocido: " + currentToken.getLexeme());
            advance();
        } else {
            reportError("Se esperaba '<-' o '=' para la asignación. Encontrado: " +
                        (currentToken != null ? currentToken.getLexeme() : "FIN DE ARCHIVO"));
        }
        parseExpresion();
        expect(TokenType.DELIMITER, "Se esperaba ';' al final de la sentencia de asignación");
        logRule("Fin <sent_asignacion>");
    }

    private void parseExpresion() {
        logRule("Analizando <expresion>");
        parseExpresionSimple();
        while (currentToken != null && currentToken.getType() == TokenType.OPERATOR &&
               isRelationalOrLogicalOperator(currentToken.getLexeme())) {
            logRule("Operador relacional/lógico reconocido: " + currentToken.getLexeme());
            advance();
            parseExpresionSimple();
        }
        logRule("Fin <expresion>");
    }

    private boolean isRelationalOrLogicalOperator(String lexeme) {
        switch (lexeme.toLowerCase()) {
            case ">": case "<": case ">=": case "<=": case "==":
            case "=": case "<>": case "y": case "o":
                return true;
            default:
                return false;
        }
    }

    private void parseExpresionSimple() {
        logRule("Analizando <expresion_simple>");
        parseTermino();
        while (currentToken != null && currentToken.getType() == TokenType.OPERATOR &&
               (currentToken.getLexeme().equals("+") || currentToken.getLexeme().equals("-"))) {
            logRule("Operador aditivo/sustractivo reconocido: " + currentToken.getLexeme());
            advance();
            parseTermino();
        }
        logRule("Fin <expresion_simple>");
    }

    private void parseTermino() {
        logRule("Analizando <termino>");
        parseFactor();
        while (currentToken != null && currentToken.getType() == TokenType.OPERATOR &&
               (currentToken.getLexeme().equals("*") || currentToken.getLexeme().equals("/"))) {
            logRule("Operador multiplicativo/divisivo reconocido: " + currentToken.getLexeme());
            advance();
            parseFactor();
        }
        logRule("Fin <termino>");
    }

    private void parseFactor() {
        logRule("Analizando <factor>");
        if (currentToken == null) {
            reportError("Se esperaba un factor (identificador, número, cadena, etc.), pero se encontró fin de archivo.");
            return;
        }
        switch (currentToken.getType()) {
            case IDENTIFIER:
                Token idToken = currentToken;
                Simbolo s = tablaDeSimbolos.buscarConPrioridad(idToken.getLexeme(), tablaDeSimbolos.getAlcanceActual());
                if (s == null) {
                    reportError("Variable '" + idToken.getLexeme() + "' no ha sido definida. Línea: " + idToken.getLineNumber());
                }
                logRule("Factor reconocido: " + idToken.getType() + " ('" + idToken.getLexeme() + "')");
                advance();
                break;
            case NUMBER:
            case STRING:
                logRule("Factor reconocido: " + currentToken.getType() + " ('" + currentToken.getLexeme() + "')");
                advance();
                break;
            case KEYWORD:
                 if (currentToken.getLexeme().equalsIgnoreCase("verdadero") || currentToken.getLexeme().equalsIgnoreCase("falso")) {
                    logRule("Factor reconocido (valor lógico): " + currentToken.getLexeme());
                    advance();
                 } else {
                    reportError("Palabra clave inesperada como factor: " + currentToken.getLexeme());
                 }
                 break;
            case DELIMITER:
                if (currentToken.getLexeme().equals("(")) {
                    match(TokenType.DELIMITER);
                    parseExpresion();
                    expect(TokenType.DELIMITER, "Se esperaba ')' para cerrar la expresión");
                } else {
                    reportError("Factor inesperado: Se encontró el delimitador '" + currentToken.getLexeme() + "'");
                }
                break;
            default:
                reportError("Factor inesperado: " + currentToken.getType() + " ('" + currentToken.getLexeme() + "')");
                break;
        }
        logRule("Fin <factor>");
    }

    private void parseSentenciaSi() {
        logRule("Analizando <sent_si>");
        expectKeyword("Si", "Se esperaba 'Si'");
        parseExpresion();
        expectKeyword("Entonces", "Se esperaba 'Entonces' después de la condición del Si");
        parseBloqueSentencias();

        if (currentToken != null && currentToken.getType() == TokenType.KEYWORD && currentToken.getLexeme().equalsIgnoreCase("SiNo")) {
            matchKeyword("SiNo");
            parseBloqueSentencias();
        }

        expectKeyword("FinSi", "Se esperaba 'FinSi' para cerrar la estructura Si");
        logRule("Fin <sent_si>");
    }
}
