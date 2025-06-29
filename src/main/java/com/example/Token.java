package com.example;

public class Token {
    private final TokenType type;
    private final String lexeme;
    private final String pattern;
    private final int lineNumber;
    private final int columnNumber;

    public Token(TokenType type, String lexeme, String pattern, int lineNumber, int columnNumber) {
        this.type = type;
        this.lexeme = lexeme;
        this.pattern = pattern;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    // Existing constructor without column, for compatibility or specific cases (e.g. EOF token)
    // However, for most tokens, column number should be provided.
    // Let's make column mandatory for standard tokens and provide a default for simpler cases if needed.
    // For now, assume all tokens will have a column.
    // public Token(TokenType type, String lexeme, String pattern, int lineNumber) {
    //    this(type, lexeme, pattern, lineNumber, 0); // Default column number
    // }


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

    public int getColumnNumber() {
        return columnNumber;
    }

    @Override
    public String toString() {
        // Updated to include line and column for debugging, though table display might not use all of this.
        return String.format("| %-15s | %-20s | %-25s | L%d:%-3d |",
            type,
            lexeme,
            (pattern != null && pattern.length() > 25) ? pattern.substring(0, 22) + "..." : pattern,
            lineNumber,
            columnNumber);
    }
}
