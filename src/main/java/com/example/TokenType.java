package com.example;

public enum TokenType {
    KEYWORD, // Palabras reservadas (Si, FinSi, Mientras, etc.)
    OPERATOR, // Operadores (+, -, *, /, =, <, >, etc.)
    DELIMITER, // Delimitadores ((, ), :, ")
    IDENTIFIER, // Identificadores (variables, nombres de funciones)
    NUMBER, // Números (enteros y decimales)
    STRING, // Cadenas de texto (delimitadas por comillas dobles)
    COMMENT, // Comentarios ( // )
    ERROR // Tokens no reconocidos o errores léxicos
}
