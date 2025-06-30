package com.example;

import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String filePath = "input_syntax_errors.txt"; // Test file with syntax errors
        // String filePath = "input_syntax_errors.txt"; // Test file with syntax errors
        // String filePath = "input.txt"; // Original file with lexical errors
        // String filePath = "input_syntax_test.txt"; // Correct file
        // String pseintCode = "";

        // // Código de consola anterior comentado/eliminado.
        // // Main ahora solo lanza la GUI.

        AnalizadorAppGUI.main(args); // Llama al main de la GUI
    }

    // Todos los métodos anteriores de consola han sido eliminados para limpiar esta clase.
    // Su funcionalidad está ahora en AnalizadorAppGUI o ya no es necesaria si solo se usa la GUI.
}