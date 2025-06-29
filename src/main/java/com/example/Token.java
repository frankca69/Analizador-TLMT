package com.example;

public class Token {
    private final TokenType type;
    private final String lexeme;
    private final String pattern;
    private final int lineNumber; // Optional: to report error locations

    public Token(TokenType type, String lexeme, String pattern, int lineNumber) {
        this.type = type;
        this.lexeme = lexeme;
        this.pattern = pattern;
        this.lineNumber = lineNumber;
    }

    public Token(TokenType type, String lexeme, String pattern) {
        this(type, lexeme, pattern, 0); // Default line number
    }

    public TokenType getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public String getPattern() {
        return pattern;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public String toString() {
        return String.format("| %-15s | %-20s | %-25s |", type, lexeme, pattern);
    }
}
