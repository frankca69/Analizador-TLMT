package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent; // No se usa directamente aún, pero es común
import java.awt.event.ActionListener; // No se usa directamente aún, pero es común
import java.util.List; // Importación correcta para List
import java.util.ArrayList; // Importación para ArrayList

public class AnalizadorAppGUI extends JFrame {

    private JTextArea areaCodigoEntrada;
    private JButton botonAnalizar;
    private JTable tablaTokens;
    private JTextArea areaErrores;
    private JTextArea areaLogSintactico;
    private JTextArea areaAST; // Nueva área para el AST
    private JTextArea areaArbolSemantico; // Nueva área para el árbol semántico
    private JTable tablaSimbolos;
    private JLabel etiquetaEstado;
    private TokenTableModel tokenTableModel;
    private SimboloTableModel simboloTableModel;


    public AnalizadorAppGUI() {
        setTitle("Analizador Léxico-Sintáctico Pseint");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700); // Tamaño inicial
        setLocationRelativeTo(null); // Centrar en pantalla

        // Layout principal
        setLayout(new BorderLayout(5, 5)); // BorderLayout con espaciado

        // --- Panel de Entrada y Botón ---
        JPanel panelEntrada = new JPanel(new BorderLayout(5, 5));
        areaCodigoEntrada = new JTextArea();
        areaCodigoEntrada.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollEntrada = new JScrollPane(areaCodigoEntrada);
        botonAnalizar = new JButton("Analizar Código");

        panelEntrada.add(new JLabel("Ingrese el código Pseint aquí:"), BorderLayout.NORTH);
        panelEntrada.add(scrollEntrada, BorderLayout.CENTER);
        panelEntrada.add(botonAnalizar, BorderLayout.SOUTH);
        panelEntrada.setPreferredSize(new Dimension(450, getHeight()));

        // --- Panel de Salida con Pestañas ---
        JTabbedPane panelPestanasSalida = new JTabbedPane();

        // Pestaña para Tokens
        tokenTableModel = new TokenTableModel();
        tablaTokens = new JTable(tokenTableModel);
        JScrollPane scrollTokens = new JScrollPane(tablaTokens);
        panelPestanasSalida.addTab("Tokens Léxicos", scrollTokens);

        // Pestaña para Errores
        areaErrores = new JTextArea();
        areaErrores.setEditable(false);
        areaErrores.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaErrores.setForeground(Color.RED);
        JScrollPane scrollErrores = new JScrollPane(areaErrores);
        panelPestanasSalida.addTab("Errores", scrollErrores);

        // Pestaña para Log Sintáctico
        areaLogSintactico = new JTextArea(); // Asignar a campo de instancia
        areaLogSintactico.setEditable(false);
        areaLogSintactico.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollLogSintactico = new JScrollPane(areaLogSintactico);
        panelPestanasSalida.addTab("Log Sintáctico", scrollLogSintactico);

        // Pestaña para AST
        areaAST = new JTextArea();
        areaAST.setEditable(false);
        areaAST.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollAST = new JScrollPane(areaAST);
        panelPestanasSalida.addTab("Árbol Sintáctico (AST)", scrollAST);

        // Pestaña para Árbol Semántico
        areaArbolSemantico = new JTextArea();
        areaArbolSemantico.setEditable(false);
        areaArbolSemantico.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollArbolSemantico = new JScrollPane(areaArbolSemantico);
        panelPestanasSalida.addTab("Árbol Semántico", scrollArbolSemantico);

        // Pestaña para Tabla de Símbolos
        simboloTableModel = new SimboloTableModel();
        tablaSimbolos = new JTable(simboloTableModel);
        JScrollPane scrollTablaSimbolos = new JScrollPane(tablaSimbolos);
        panelPestanasSalida.addTab("Tabla de Símbolos", scrollTablaSimbolos);

        // --- Panel de Estado ---
        etiquetaEstado = new JLabel("Listo.", SwingConstants.CENTER);
        etiquetaEstado.setBorder(BorderFactory.createEtchedBorder());

        // --- JSplitPane para dividir entrada y salida ---
        JSplitPane splitPanePrincipal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelEntrada, panelPestanasSalida);
        splitPanePrincipal.setDividerLocation(450); // Posición inicial del divisor, ajustada

        add(splitPanePrincipal, BorderLayout.CENTER);
        add(etiquetaEstado, BorderLayout.SOUTH);

        // Acción del botón Analizar
        botonAnalizar.addActionListener(e -> analizarCodigo());
    }

    private void analizarCodigo() {
        String codigoFuente = areaCodigoEntrada.getText();

        // Limpiar salidas previas
        areaErrores.setText("");
        areaLogSintactico.setText("");
        areaAST.setText(""); // Limpiar área del AST
        areaArbolSemantico.setText(""); // Limpiar área del árbol semántico
        etiquetaEstado.setText("Analizando...");
        if (tokenTableModel != null) tokenTableModel.clearData();
        if (simboloTableModel != null) simboloTableModel.clearData();


        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.analyze(codigoFuente);
        List<ErrorCompilacion> erroresLexicos = lexer.getErroresLexicos();

        // Actualizar tabla de tokens
        if (tokenTableModel != null) {
            tokenTableModel.setTokens(tokens);
        }


        if (!erroresLexicos.isEmpty()) {
            for (ErrorCompilacion err : erroresLexicos) {
                areaErrores.append(err.toString() + "\n");
            }
            etiquetaEstado.setText("Código Rechazado (Errores Léxicos)");
            return; // Detener si hay errores léxicos
        }

        // Si no hay errores léxicos, proceder con el análisis sintáctico
        // Filtrar tokens de error (aunque ya deberían estar en la lista de errores)
        List<Token> tokensParaParser = new ArrayList<>();
        for (Token t : tokens) {
            if (t.getType() != TokenType.ERROR) {
                tokensParaParser.add(t);
            }
        }

        if (tokensParaParser.isEmpty() && !codigoFuente.trim().isEmpty() && !isOnlyComments(codigoFuente, tokens)) {
            ErrorCompilacion err = new ErrorCompilacion(ErrorCompilacion.TipoError.LEXICO, "El código no produjo tokens válidos para el análisis sintáctico.", 1, 1, "");
            areaErrores.append(err.toString() + "\n");
            etiquetaEstado.setText("Código Rechazado (Error Léxico Crítico)");
            return;
        }
        if (tokensParaParser.isEmpty() && (codigoFuente.trim().isEmpty() || isOnlyComments(codigoFuente, tokens))) {
             etiquetaEstado.setText("Listo (Archivo vacío o solo comentarios)");
             areaLogSintactico.setText("No se requiere análisis sintáctico para archivo vacío o solo con comentarios.");
             return;
        }


        SyntaxAnalyzer syntaxAnalyzer = new SyntaxAnalyzer(tokensParaParser);
        syntaxAnalyzer.parse();
        List<ErrorCompilacion> erroresSintacticos = syntaxAnalyzer.getErroresSintacticos();
        List<String> logSintaxis = syntaxAnalyzer.getSyntaxLog(); // Incluye errores y reglas

        for (String logEntry : logSintaxis) {
            if (logEntry.startsWith("Regla/Acción Sintáctica:")) {
                 areaLogSintactico.append(logEntry + "\n");
            }
            // Los errores sintácticos ya están formateados en el log por reportError
            // Si queremos listarlos por separado de la lista erroresSintacticos:
            // areaErrores.append(logEntry + "\n"); // Si el logEntry es un error
        }

        // Imprimir errores sintácticos de forma dedicada si existen
        if (!erroresSintacticos.isEmpty()) {
             for (ErrorCompilacion err : erroresSintacticos) {
                areaErrores.append(err.toString() + "\n");
            }
        }

        // Mostrar Tabla de Símbolos
        TablaDeSimbolos tablaSimbolosObj = syntaxAnalyzer.getTablaDeSimbolos();
        if (simboloTableModel != null && tablaSimbolosObj != null) {
            simboloTableModel.setSimbolos(tablaSimbolosObj.getSimbolosAsCollection());
        }

        // Obtener y Mostrar AST si no hay errores sintácticos
        if (erroresSintacticos.isEmpty()) {
            com.example.ast.ProgramaNode astRoot = syntaxAnalyzer.getAST(); // Capturar el AST devuelto por parse()
            if (astRoot != null) {
                areaAST.setText(astRoot.aRepresentacionTextual("", true));
                areaAST.setCaretPosition(0); // Scroll al inicio

                // Análisis semántico
                SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer(astRoot);
                String arbolSemantico = semanticAnalyzer.analyze();
                areaArbolSemantico.setText(arbolSemantico);
                areaArbolSemantico.setCaretPosition(0);

            } else {
                // Esto podría pasar si parse() devuelve null incluso sin errores en la lista,
                // por ejemplo, si el código está vacío o solo son comentarios.
                areaAST.setText("No se generó el AST (código vacío, solo comentarios, o error interno no reportado en lista).");
            }
        } else {
            areaAST.setText("No se generó el AST debido a errores sintácticos.");
        }


        if (!erroresLexicos.isEmpty() || !erroresSintacticos.isEmpty()) {
            etiquetaEstado.setText("Código Rechazado");
        } else {
            etiquetaEstado.setText("Código Aceptado");
        }
    }

    // Helper para truncar (si se mueve de Main)
    private String truncate(String text, int maxLength) {
        if (text == null) return "N/A";
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    // Helper para verificar si el código solo tiene comentarios (si se mueve de Main)
     private boolean isOnlyComments(String code, List<Token> tokens) {
        if (code.trim().isEmpty()) return true;
        for (Token t : tokens) {
            if (t.getType() != TokenType.COMMENT) {
                return false;
            }
        }
        return true;
    }


    public static void main(String[] args) {
        // Ejecutar la GUI en el Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new AnalizadorAppGUI().setVisible(true);
            }
        });
    }
}
