package com.example;

import java.util.List;
import java.util.ArrayList;
import com.example.ast.*;

public class SyntaxAnalyzer {

    private List<Token> tokens;
    private int currentTokenIndex;
    private Token currentToken;
    private List<String> syntaxLog;
    private TablaDeSimbolos tablaDeSimbolos;
    private List<ErrorCompilacion> erroresSintacticos;
    private ProgramaNode astRootNode;

    public SyntaxAnalyzer(List<Token> tokens) {
        this.tokens = tokens;
        this.currentTokenIndex = 0;
        this.currentToken = tokens.isEmpty() ? null : tokens.get(0);
        this.syntaxLog = new ArrayList<>();
        this.tablaDeSimbolos = new TablaDeSimbolos();
        this.erroresSintacticos = new ArrayList<>();
        this.astRootNode = null;
    }

    public TablaDeSimbolos getTablaDeSimbolos() { return tablaDeSimbolos; }
    public List<String> getSyntaxLog() { return syntaxLog; }
    public List<ErrorCompilacion> getErroresSintacticos() { return erroresSintacticos; }
    public ProgramaNode getAST() { return this.astRootNode; }


    private void advance() {
        currentTokenIndex++;
        currentToken = (currentTokenIndex < tokens.size()) ? tokens.get(currentTokenIndex) : null;
    }

    private Token match(TokenType expectedType) {
        if (currentToken != null && currentToken.getType() == expectedType) {
            Token matchedToken = currentToken;
            logRule("Consumido token: " + matchedToken.getType() + " ('" + matchedToken.getLexeme() + "')");
            advance();
            return matchedToken;
        }
        return null;
    }

    private Token matchKeyword(String keywordText) {
        if (currentToken != null && currentToken.getType() == TokenType.KEYWORD && currentToken.getLexeme().equalsIgnoreCase(keywordText)) {
            Token matchedToken = currentToken;
            logRule("Consumido keyword: " + matchedToken.getLexeme());
            advance();
            return matchedToken;
        }
        return null;
    }

    private Token expect(TokenType expectedType, String errorMessage) {
        Token matchedToken = match(expectedType);
        if (matchedToken == null && erroresSintacticos.isEmpty()) { // Report error only if not already in error state from this line
            reportError(errorMessage + ". Se encontró: " + (currentToken != null ? currentToken.getType() + " ('" + currentToken.getLexeme() + "')" : "FIN DE ARCHIVO"));
        } else if (matchedToken == null && !erroresSintacticos.isEmpty() && erroresSintacticos.get(erroresSintacticos.size()-1).getLinea() != (currentToken != null ? currentToken.getLineNumber():0) ){
             reportError(errorMessage + ". Se encontró: " + (currentToken != null ? currentToken.getType() + " ('" + currentToken.getLexeme() + "')" : "FIN DE ARCHIVO"));
        }
        return matchedToken;
    }

    private Token expectKeyword(String keywordText, String errorMessage) {
        Token matchedToken = currentToken;
        if (currentToken != null && currentToken.getType() == TokenType.KEYWORD && currentToken.getLexeme().equalsIgnoreCase(keywordText)) {
            logRule("Consumido keyword: " + currentToken.getLexeme());
            advance();
            return matchedToken;
        } else {
            reportError(errorMessage + ". Se encontró: " + (currentToken != null ? currentToken.getType() + " ('" + currentToken.getLexeme() + "')" : "FIN DE ARCHIVO"));
            return null;
        }
    }

    private void reportError(String message) {
        int linea = (currentToken != null) ? currentToken.getLineNumber() : (tokens.isEmpty() || currentTokenIndex >= tokens.size() ? 0 : tokens.get(Math.min(currentTokenIndex, tokens.size()-1)).getLineNumber());
        int columna = (currentToken != null) ? currentToken.getColumnNumber() : 0;
        String lexema = (currentToken != null) ? currentToken.getLexeme() : "EOF";
        ErrorCompilacion error = new ErrorCompilacion( ErrorCompilacion.TipoError.SINTACTICO, message, linea, columna, lexema);
        erroresSintacticos.add(error);
        syntaxLog.add(error.toString());
    }

    private void logRule(String ruleMessage) {
        syntaxLog.add("Regla/Acción Sintáctica: " + ruleMessage);
    }

    public void parse() { // Changed back to void, AST obtained via getAST()
        logRule("Iniciando análisis sintáctico...");
        this.astRootNode = null;
        this.erroresSintacticos.clear();

        boolean isEmptyOrCommentsOnly = tokens.stream().allMatch(t -> t.getType() == TokenType.COMMENT);
        if (tokens.isEmpty() || isEmptyOrCommentsOnly && currentToken == null ) {
             logRule("No hay sentencias válidas para analizar.");
             return;
        }
        if (currentToken == null && !tokens.isEmpty() && !isEmptyOrCommentsOnly){
             reportError("Se llegó al final del archivo inesperadamente antes de iniciar el análisis del programa.");
             return;
        }

        ProgramaNode programaNode = parsePrograma();

        if (currentToken != null) {
            reportError("Se esperaba FinDeArchivo pero se encontraron tokens adicionales a partir de: " + currentToken.getLexeme());
        }

        if (erroresSintacticos.isEmpty() && programaNode != null) {
            this.astRootNode = programaNode;
        }
        logRule("Análisis sintáctico completado.");
    }

    private ProgramaNode parsePrograma() {
        logRule("Analizando <programa>");
        Token tokenProceso = expectKeyword("Proceso", "Se esperaba 'Proceso'");
        if (tokenProceso == null && !erroresSintacticos.isEmpty()) return null;

        IdentificadorNode nombreProcesoNode = null;
        Token nombreProcesoToken = currentToken;
        if (nombreProcesoToken != null && nombreProcesoToken.getType() == TokenType.IDENTIFIER) {
            nombreProcesoNode = new IdentificadorNode(nombreProcesoToken);
            Simbolo procSimbolo = new Simbolo( nombreProcesoToken.getLexeme(), "N/A", "nombre_proceso", "global", nombreProcesoToken.getLineNumber(), null);
            if (!tablaDeSimbolos.agregar(procSimbolo)) reportError("Error interno al agregar nombre de proceso.");
            tablaDeSimbolos.setAlcanceActual("global");
            logRule("Nombre del proceso '" + nombreProcesoToken.getLexeme() + "' agregado. Alcance: global");
        }
        expect(TokenType.IDENTIFIER, "Se esperaba IDENTIFICADOR para nombre de proceso");
        if (nombreProcesoNode == null && nombreProcesoToken != null && nombreProcesoToken.getType() == TokenType.IDENTIFIER) { // If expect advanced currentToken but node wasn't created due to prior error state
             nombreProcesoNode = new IdentificadorNode(nombreProcesoToken); // Create with the token that was expected
        }


        List<NodoAST> sentencias = parseBloqueSentencias();
        expectKeyword("FinProceso", "Se esperaba 'FinProceso'");
        logRule("Fin <programa>");
        if(nombreProcesoNode == null && nombreProcesoToken != null) nombreProcesoNode = new IdentificadorNode(nombreProcesoToken); // Fallback if expect failed but token was an ID
        if(nombreProcesoNode == null) { reportError("Nombre de proceso inválido para AST."); return null;}

        return new ProgramaNode(tokenProceso, nombreProcesoNode, sentencias);
    }

    private List<NodoAST> parseBloqueSentencias() {
        logRule("Analizando <bloque_sentencias>");
        List<NodoAST> sentencias = new ArrayList<>();
        while (currentToken != null && canStartSentencia(currentToken) && !isEndOfBlockMarker(currentToken)) {
            NodoAST sentencia = parseSentencia();
            if (sentencia != null) sentencias.add(sentencia);
            else if (!erroresSintacticos.isEmpty()){ // Error occurred in parseSentencia
                 // Try to advance to recover if parseSentencia didn't or couldn't.
                 // This is a simple recovery, might need more sophistication.
                Token problematicToken = currentToken;
                advance();
                if(currentToken == problematicToken) break; // Avoid infinite loop if advance doesn't move
            } else { // Should not happen if canStartSentencia is true and no error
                break;
            }
        }
        logRule("Fin <bloque_sentencias>");
        return sentencias;
    }

    private boolean canStartSentencia(Token token) {
        if (token == null) return false;
        if (token.getType() == TokenType.KEYWORD) {
            String lexeme = token.getLexeme().toLowerCase();
            switch (lexeme) { case "definir": case "leer": case "escribir": case "si": return true; }
        } else if (token.getType() == TokenType.IDENTIFIER) return true;
        return false;
    }
    private boolean isEndOfBlockMarker(Token token) {
        if (token == null) return true;
        if (token.getType() == TokenType.KEYWORD) {
            String lexeme = token.getLexeme().toLowerCase();
            switch (lexeme) { case "finproceso": case "finsi": case "sino": return true; }
        }
        return false;
    }

    private NodoAST parseSentencia() {
        logRule("Analizando <sentencia>");
        if (currentToken == null) { reportError("Fin de archivo inesperado, se esperaba una sentencia."); return null; }
        Token primerTokenSentencia = currentToken;

        if (primerTokenSentencia.getType() == TokenType.KEYWORD) {
            switch (primerTokenSentencia.getLexeme().toLowerCase()) {
                case "definir": return parseSentenciaDefinir();
                case "leer": return parseSentenciaLeer();
                case "escribir": return parseSentenciaEscribir();
                case "si": return parseSentenciaSi();
                default:
                    reportError("Palabra clave no esperada para iniciar sentencia: " + primerTokenSentencia.getLexeme());
                    advance(); return null;
            }
        } else if (primerTokenSentencia.getType() == TokenType.IDENTIFIER) {
            return parseSentenciaAsignacion();
        } else {
            reportError("Sentencia inválida. Se encontró: " + primerTokenSentencia.getLexeme());
            advance(); return null;
        }
    }

    private DefinirNode parseSentenciaDefinir() {
        logRule("Analizando <sent_definir>");
        Token tokenDefinir = expectKeyword("Definir", "Se esperaba 'Definir'");
        if (tokenDefinir == null && !erroresSintacticos.isEmpty()) return null;

        List<IdentificadorNode> variablesNodes = new ArrayList<>();
        List<Token> variablesTokens = parseListaVariables();
        for(Token t : variablesTokens) variablesNodes.add(new IdentificadorNode(t));

        expectKeyword("Como", "Se esperaba 'Como'");
        Token tipoDatoToken = parseTipoDato();

        if (tipoDatoToken != null && !variablesNodes.isEmpty()) {
            for (IdentificadorNode varNode : variablesNodes) {
                Simbolo s = new Simbolo(varNode.getNombre(), tipoDatoToken.getLexeme(), "variable", tablaDeSimbolos.getAlcanceActual(), varNode.getLinea(), null);
                if (!tablaDeSimbolos.agregar(s)) {
                    reportError("Variable '" + varNode.getNombre() + "' ya definida. Línea: " + varNode.getLinea());
                } else {
                    logRule("Variable '" + varNode.getNombre() + "' ("+tipoDatoToken.getLexeme()+") agregada a tabla.");
                }
            }
        }
        expect(TokenType.DELIMITER, "Se esperaba ';' al final de Definir");
        logRule("Fin <sent_definir>");
        if (tokenDefinir == null || tipoDatoToken == null || variablesNodes.isEmpty() && !variablesTokens.isEmpty() /* check if nodes failed from tokens */) return null;
        return new DefinirNode(tokenDefinir, variablesNodes, tipoDatoToken);
    }

    private List<Token> parseListaVariables() {
        logRule("Analizando <lista_variables>");
        List<Token> variables = new ArrayList<>();
        Token idToken = expect(TokenType.IDENTIFIER, "Se esperaba IDENTIFICADOR de variable inicial");
        if (idToken != null) variables.add(idToken);
        else return variables; // Si el primero falla, no hay lista

        while (currentToken != null && currentToken.getType() == TokenType.DELIMITER && currentToken.getLexeme().equals(",")) {
            match(TokenType.DELIMITER);
            idToken = expect(TokenType.IDENTIFIER, "Se esperaba IDENTIFICADOR de variable después de la coma");
            if (idToken != null) variables.add(idToken);
            else break;
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
                case "entero": case "real": case "caracter": case "logico": case "cadena":
                    tipoToken = currentToken;
                    logRule("Tipo de dato reconocido: " + tipoToken.getLexeme());
                    advance();
                    break;
                default: reportError("Tipo de dato keyword no reconocido: " + lexeme);
            }
        } else {
            reportError("Se esperaba una palabra clave de tipo de dato. Encontrado: " + (currentToken != null ? currentToken.getType() + " ('" + currentToken.getLexeme() + "')" : "FIN DE ARCHIVO"));
        }
        logRule("Fin <tipo_dato>");
        return tipoToken;
    }

    private LeerNode parseSentenciaLeer() {
        logRule("Analizando <sent_leer>");
        Token tokenLeer = expectKeyword("Leer", "Se esperaba 'Leer'");
        if (tokenLeer == null && !erroresSintacticos.isEmpty()) return null;

        List<IdentificadorNode> variablesNodes = new ArrayList<>();
        List<Token> varTokens = parseListaVariables();
        for(Token vt : varTokens) {
            IdentificadorNode idNode = new IdentificadorNode(vt);
            variablesNodes.add(idNode);
            if (tablaDeSimbolos.buscarConPrioridad(vt.getLexeme(), tablaDeSimbolos.getAlcanceActual()) == null) {
                reportError("Variable '" + vt.getLexeme() + "' no definida (en Leer). Línea: " + vt.getLineNumber());
            }
        }
        expect(TokenType.DELIMITER, "Se esperaba ';' al final de Leer");
        logRule("Fin <sent_leer>");
        return new LeerNode(tokenLeer, variablesNodes);
    }

    private EscribirNode parseSentenciaEscribir() {
        logRule("Analizando <sent_escribir>");
        Token tokenEscribir = expectKeyword("Escribir", "Se esperaba 'Escribir'");
        if (tokenEscribir == null && !erroresSintacticos.isEmpty()) return null;
        List<NodoAST> expresiones = parseListaExpresiones();
        expect(TokenType.DELIMITER, "Se esperaba ';' al final de Escribir");
        logRule("Fin <sent_escribir>");
        return new EscribirNode(tokenEscribir, expresiones);
    }

    private List<NodoAST> parseListaExpresiones() {
        logRule("Analizando <lista_expresiones>");
        List<NodoAST> expresiones = new ArrayList<>();
        NodoAST expr = parseExpresion();
        if (expr != null) expresiones.add(expr);
        else { return expresiones; }

        while (currentToken != null && currentToken.getType() == TokenType.DELIMITER && currentToken.getLexeme().equals(",")) {
            match(TokenType.DELIMITER);
            expr = parseExpresion();
            if (expr != null) expresiones.add(expr);
            else break;
        }
        logRule("Fin <lista_expresiones>");
        return expresiones;
    }

    private AsignacionNode parseSentenciaAsignacion() {
        logRule("Analizando <sent_asignacion>");
        Token idToken = currentToken;
        IdentificadorNode variableNode = null;

        if (idToken != null && idToken.getType() == TokenType.IDENTIFIER) {
            variableNode = new IdentificadorNode(idToken);
             if (tablaDeSimbolos.buscarConPrioridad(idToken.getLexeme(), tablaDeSimbolos.getAlcanceActual()) == null) {
                reportError("Variable '" + idToken.getLexeme() + "' no definida. Línea: " + idToken.getLineNumber());
            }
        }
        expect(TokenType.IDENTIFIER, "Se esperaba IDENTIFICADOR para asignación");
        // If expect did not advance due to error, idToken might still be the erroneous token.
        // If expect advanced, idToken holds the correct one.
        if (variableNode == null && idToken !=null && idToken.getType() == TokenType.IDENTIFIER) {
            //This happens if expect() reported error but currentToken was ID so it consumed it.
            //Or if it was already an ID and expect consumed it.
            //The original variableAsignadaToken = currentToken; then expect; was better for node construction.
            //Let's stick to variableNode = new IdentificadorNode(idToken); done *before* expect if idToken is IDENTIFIER.
            //The current idToken is after expect, so it's the operator. This logic is flawed.
            //Reverting to capturing idToken *before* expect.
        }
        // Corrected logic for variableNode:
        // Token variableTokenForNode = currentToken; // Token before expect consumes it
        // expect(TokenType.IDENTIFIER, "Se esperaba IDENTIFICADOR para asignación");
        // if(variableTokenForNode != null && variableTokenForNode.getType() == TokenType.IDENTIFIER) variableNode = new IdentificadorNode(variableTokenForNode);
        // This needs careful handling of when `expect` consumes. Let's assume `idToken` is the correct one if `expect` didn't error out.
        // The `variableNode` above is constructed with `currentToken` *before* `expect`. That's correct.

        Token opAsignacionToken = currentToken;
        if (currentToken != null && currentToken.getType() == TokenType.OPERATOR && (currentToken.getLexeme().equals("<-") || currentToken.getLexeme().equals("="))) {
            advance();
        } else {
            reportError("Se esperaba '<-' o '=' para asignación. Encontrado: " + (currentToken != null ? currentToken.getLexeme() : "EOF"));
            opAsignacionToken = null;
        }

        NodoAST exprNode = parseExpresion();
        expect(TokenType.DELIMITER, "Se esperaba ';' al final de asignación");
        logRule("Fin <sent_asignacion>");

        if (variableNode == null || opAsignacionToken == null || exprNode == null) return null;
        return new AsignacionNode(variableNode, opAsignacionToken, exprNode);
    }

    private NodoAST parseExpresion() {
        logRule("Analizando <expresion>");
        NodoAST izquierda = parseExpresionSimple();
        if (izquierda == null && !erroresSintacticos.isEmpty()) { // Check if error occurred in parseExpresionSimple
             Token lastErrorToken = erroresSintacticos.get(erroresSintacticos.size()-1).getLexemaProblematico() != null ?
                                   new Token(TokenType.ERROR, erroresSintacticos.get(erroresSintacticos.size()-1).getLexemaProblematico(), "", erroresSintacticos.get(erroresSintacticos.size()-1).getLinea(), erroresSintacticos.get(erroresSintacticos.size()-1).getColumna())
                                   : currentToken; // Fallback
            if(lastErrorToken == currentToken || currentTokenIndex > 0 && tokens.get(currentTokenIndex-1) == lastErrorToken ) return null;
        }

        while (currentToken != null && currentToken.getType() == TokenType.OPERATOR && isRelationalOrLogicalOperator(currentToken.getLexeme())) {
            Token operador = currentToken;
            advance();
            NodoAST derecha = parseExpresionSimple();
            if (derecha == null) {
                 reportError("Se esperaba expresión después del operador '" + operador.getLexeme() + "'");
                 return izquierda; // Return what was parsed on left, even if incomplete
            }
            if (izquierda == null) { // Should not happen if first part is mandatory.
                reportError("Operando izquierdo faltante para operador '" + operador.getLexeme() + "'");
                return null;
            }
            izquierda = new ExpresionBinariaNode(izquierda, operador, derecha);
        }
        logRule("Fin <expresion>");
        return izquierda;
    }

    private boolean isRelationalOrLogicalOperator(String lexeme) {
        switch (lexeme.toLowerCase()) {
            case ">": case "<": case ">=": case "<=": case "==":
            case "=": case "<>": case "y": case "o":
                return true;
            default: return false;
        }
    }

    private NodoAST parseExpresionSimple() {
        logRule("Analizando <expresion_simple>");
        NodoAST izquierda = parseTermino();
         if (izquierda == null && !erroresSintacticos.isEmpty()){
            Token lastErrorToken = erroresSintacticos.get(erroresSintacticos.size()-1).getLexemaProblematico() != null ?
                                   new Token(TokenType.ERROR, erroresSintacticos.get(erroresSintacticos.size()-1).getLexemaProblematico(), "", erroresSintacticos.get(erroresSintacticos.size()-1).getLinea(), erroresSintacticos.get(erroresSintacticos.size()-1).getColumna())
                                   : currentToken;
            if(lastErrorToken == currentToken || currentTokenIndex > 0 && tokens.get(currentTokenIndex-1) == lastErrorToken ) return null;
        }

        while (currentToken != null && currentToken.getType() == TokenType.OPERATOR && (currentToken.getLexeme().equals("+") || currentToken.getLexeme().equals("-"))) {
            Token operador = currentToken;
            advance();
            NodoAST derecha = parseTermino();
            if (derecha == null) {
                reportError("Se esperaba término después del operador '" + operador.getLexeme() + "'");
                return izquierda;
            }
            if (izquierda == null) {
                 reportError("Operando izquierdo faltante para operador '" + operador.getLexeme() + "'");
                return null;
            }
            izquierda = new ExpresionBinariaNode(izquierda, operador, derecha);
        }
        logRule("Fin <expresion_simple>");
        return izquierda;
    }

    private NodoAST parseTermino() {
        logRule("Analizando <termino>");
        NodoAST izquierda = parseFactor();
        if (izquierda == null && !erroresSintacticos.isEmpty()){
            Token lastErrorToken = erroresSintacticos.get(erroresSintacticos.size()-1).getLexemaProblematico() != null ?
                                   new Token(TokenType.ERROR, erroresSintacticos.get(erroresSintacticos.size()-1).getLexemaProblematico(), "", erroresSintacticos.get(erroresSintacticos.size()-1).getLinea(), erroresSintacticos.get(erroresSintacticos.size()-1).getColumna())
                                   : currentToken;
            if(lastErrorToken == currentToken || currentTokenIndex > 0 && tokens.get(currentTokenIndex-1) == lastErrorToken ) return null;
        }

        while (currentToken != null && currentToken.getType() == TokenType.OPERATOR && (currentToken.getLexeme().equals("*") || currentToken.getLexeme().equals("/"))) {
            Token operador = currentToken;
            advance();
            NodoAST derecha = parseFactor();
            if (derecha == null) {
                reportError("Se esperaba factor después del operador '" + operador.getLexeme() + "'");
                return izquierda;
            }
            if (izquierda == null) {
                reportError("Operando izquierdo faltante para operador '" + operador.getLexeme() + "'");
                return null;
            }
            izquierda = new ExpresionBinariaNode(izquierda, operador, derecha);
        }
        logRule("Fin <termino>");
        return izquierda;
    }

    private NodoAST parseFactor() {
        logRule("Analizando <factor>");
        if (currentToken == null) {
            reportError("Factor inesperado: Fin de archivo.");
            return null;
        }
        Token factorToken = currentToken; // Captura el token actual para usar en la creación del nodo
        switch (factorToken.getType()) {
            case IDENTIFIER:
                Simbolo s = tablaDeSimbolos.buscarConPrioridad(factorToken.getLexeme(), tablaDeSimbolos.getAlcanceActual());
                if (s == null) {
                    reportError("Variable '" + factorToken.getLexeme() + "' no definida. Línea: " + factorToken.getLineNumber());
                }
                advance();
                return new IdentificadorNode(factorToken);
            case NUMBER:
            case STRING:
                advance();
                return new LiteralNode(factorToken);
            case KEYWORD:
                 if (factorToken.getLexeme().equalsIgnoreCase("verdadero") || factorToken.getLexeme().equalsIgnoreCase("falso")) {
                    advance();
                    return new LiteralNode(factorToken);
                 } else {
                    reportError("Palabra clave inesperada como factor: " + factorToken.getLexeme());
                    advance(); // Consumir el token erróneo para evitar bucles
                    return null;
                 }
            case DELIMITER:
                if (factorToken.getLexeme().equals("(")) {
                    Token parenApertura = match(TokenType.DELIMITER); // Consume "("
                    NodoAST exprInterna = parseExpresion();
                    expect(TokenType.DELIMITER, "Se esperaba ')'");
                    if(exprInterna == null) return null; // Error en la expresión interna
                    return new ExpresionParentizadaNode(exprInterna, parenApertura);
                } else {
                    reportError("Delimitador inesperado como factor: '" + factorToken.getLexeme() + "'");
                    advance(); return null;
                }
            default:
                reportError("Factor inesperado: " + factorToken.getType() + " ('" + factorToken.getLexeme() + "')");
                advance(); return null;
        }
    }

    private SiNode parseSentenciaSi() {
        logRule("Analizando <sent_si>");
        Token tokenSi = expectKeyword("Si", "Se esperaba 'Si'");
        if(tokenSi == null && !erroresSintacticos.isEmpty()) return null;

        NodoAST condicion = parseExpresion();
        expectKeyword("Entonces", "Se esperaba 'Entonces'");
        List<NodoAST> bloqueEntonces = parseBloqueSentencias();
        List<NodoAST> bloqueSiNo = null;

        if (currentToken != null && currentToken.getType() == TokenType.KEYWORD && currentToken.getLexeme().equalsIgnoreCase("SiNo")) {
            matchKeyword("SiNo");
            bloqueSiNo = parseBloqueSentencias();
        }

        expectKeyword("FinSi", "Se esperaba 'FinSi'");
        logRule("Fin <sent_si>");
        if(condicion == null && !erroresSintacticos.isEmpty()) return null;
        return new SiNode(tokenSi, condicion, bloqueEntonces, bloqueSiNo);
    }
}
