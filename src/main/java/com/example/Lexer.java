package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.LinkedHashMap; // To maintain insertion order for patterns
import java.util.Map;

public class Lexer {

    private static final Map<TokenType, String> tokenPatterns = new LinkedHashMap<>();
    private List<ErrorCompilacion> erroresLexicos;

    static {
        // Order is important for matching. Comments and keywords should generally take precedence.
        // Comentarios ( // hasta el final de la línea )
        tokenPatterns.put(TokenType.COMMENT, "//.*");

        // Palabras reservadas (case-insensitive for Pseint)
        // Added Proceso, FinProceso and data types
        tokenPatterns.put(TokenType.KEYWORD, "(?i)\\b(Proceso|FinProceso|Si|Entonces|SiNo|FinSi|Mientras|Hacer|FinMientras|Repetir|HastaQue|Escribir|Leer|Definir|Como|Inicio|Fin|Segun|FinSegun|Caso|De|Funcion|FinFuncion|Entero|Real|Caracter|Logico|Cadena|Verdadero|Falso)\\b");

        // Cadenas de texto (delimitadas por comillas simples o dobles), with escape support
        tokenPatterns.put(TokenType.STRING, "\"(\\\\.|[^\"\\\\])*\"|'(\\\\.|[^'\\\\])*'");

        // Operadores
        tokenPatterns.put(TokenType.OPERATOR, "(<=|>=|<>|==|<-|->|[+\\-*/%^=<>]|(?i)\\b(Y|O|NO)\\b)");

        // Delimitadores
        tokenPatterns.put(TokenType.DELIMITER, "[\\(\\)\\[\\]:,;]");

        // Números (enteros y reales)
        tokenPatterns.put(TokenType.NUMBER, "\\b\\d+(\\.\\d+)?\\b");

        // Identificadores (deben empezar con letra, pueden contener letras, números y guión bajo)
        // This should generally be one of the last patterns to avoid consuming keywords.
        tokenPatterns.put(TokenType.IDENTIFIER, "\\b[a-zA-Z_][a-zA-Z0-9_]*\\b");
    }

    public List<Token> analyze(String code) {
        this.erroresLexicos = new ArrayList<>();
        List<Token> tokens = new ArrayList<>();
        // String remainingCode = code; // We will use code.substring(currentGlobalPos) directly
        int currentGlobalPos = 0;
        int currentLine = 1;
        int lineStartGlobalPos = 0; // Global position in 'code' where the current line started

        while (currentGlobalPos < code.length()) {
            int currentColumn = currentGlobalPos - lineStartGlobalPos + 1;
            boolean matchFound = false;
            String sliceToMatch = code.substring(currentGlobalPos);

            for (Map.Entry<TokenType, String> entry : tokenPatterns.entrySet()) {
                TokenType type = entry.getKey();
                String patternString = entry.getValue();

                Pattern pattern = Pattern.compile("^" + patternString);
                Matcher matcher = pattern.matcher(sliceToMatch);

                if (matcher.lookingAt()) {
                    String lexeme = matcher.group(0);
                    // int lexemeStartLine = currentLine; // currentLine is already the start line
                    // int lexemeStartColumn = currentColumn; // currentColumn is already the start column

                    if (type != TokenType.COMMENT) {
                        tokens.add(new Token(type, lexeme, patternString, currentLine, currentColumn));
                    }

                    // Advance global position and update line/column based on lexeme content
                    for (int i = 0; i < lexeme.length(); i++) {
                        if (lexeme.charAt(i) == '\n') {
                            currentLine++;
                            lineStartGlobalPos = currentGlobalPos + i + 1; // Update lineStart based on original code string pos
                        }
                    }
                    currentGlobalPos += lexeme.length();
                    // remainingCode = code.substring(currentGlobalPos); // Not needed if using sliceToMatch
                    matchFound = true;
                    break;
                }
            }

            if (!matchFound) {
                // Try to match whitespace if no token was found
                Pattern whitespacePattern = Pattern.compile("^[\\s]+");
                Matcher whitespaceMatcher = whitespacePattern.matcher(sliceToMatch);
                if (whitespaceMatcher.lookingAt()) { // Use lookingAt for consistency
                    String ws = whitespaceMatcher.group(0);
                    for (int i = 0; i < ws.length(); i++) {
                        if (ws.charAt(i) == '\n') {
                            currentLine++;
                            lineStartGlobalPos = currentGlobalPos + i + 1;
                        }
                    }
                    currentGlobalPos += ws.length();
                    // remainingCode = code.substring(currentGlobalPos);
                } else {
                    // If not a token and not whitespace, it's a lexical error
                    char errorChar = sliceToMatch.charAt(0);
                    String errorLexeme = String.valueOf(errorChar);
                    String errorMessage = "Caracter no reconocido: '" + errorLexeme + "'";

                    erroresLexicos.add(new ErrorCompilacion(
                        ErrorCompilacion.TipoError.LEXICO,
                        errorMessage,
                        currentLine,
                        currentColumn,
                        errorLexeme));
                    // Still add an ERROR token for now, Main.java might use it.
                    // This could be removed if Main switches to only using ErrorCompilacion list.
                    tokens.add(new Token(TokenType.ERROR, errorLexeme, "Error Léxico", currentLine, currentColumn));

                    // Advance past the error character
                    if (errorChar == '\n') { // Should be caught by whitespace, but as a safeguard
                        currentLine++;
                        lineStartGlobalPos = currentGlobalPos + 1;
                    }
                    currentGlobalPos++;
                    // remainingCode = code.substring(currentGlobalPos);
                }
            }
        }
        return tokens;
    }

    public List<ErrorCompilacion> getErroresLexicos() {
        return erroresLexicos;
    }
}
