package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssemblyGenerator {

    private List<String> optimizedCode;
    private TablaDeSimbolos symbolTable;
    private StringBuilder dataSection;
    private StringBuilder textSection;
    private Map<String, String> stringLiterals;
    private int stringLiteralCounter;
    private Map<String, String> tempVarValues;
    private Map<String, String> declaredVariables; // Nuevas variables detectadas

    public AssemblyGenerator(List<String> optimizedCode, TablaDeSimbolos symbolTable) {
        this.optimizedCode = optimizedCode;
        this.symbolTable = symbolTable;
        this.dataSection = new StringBuilder();
        this.textSection = new StringBuilder();
        this.stringLiterals = new HashMap<>();
        this.stringLiteralCounter = 1;
        this.tempVarValues = new HashMap<>();
        this.declaredVariables = new HashMap<>();
    }

    public String generate() {
        // Primer pase: detectar todas las variables declaradas
        detectVariables();

        // Inicializar secciones
        dataSection.append("section .data\n");

        // Declarar variables de la tabla de símbolos
        for (Simbolo symbol : symbolTable.getSimbolosAsCollection()) {
            if (symbol.getTipo().equals("Entero") || symbol.getTipo().equals("ENTERO")) {
                dataSection.append(String.format("    %s dd 0\n", symbol.getNombre()));
                declaredVariables.put(symbol.getNombre(), "ENTERO");
            } else if (symbol.getTipo().equals("Boolean") || symbol.getTipo().equals("BOOLEAN")
                    || symbol.getTipo().equals("Logico") || symbol.getTipo().equals("LOGICO")) {
                dataSection.append(String.format("    %s dd 0\n", symbol.getNombre()));
                declaredVariables.put(symbol.getNombre(), "BOOLEAN");
            }
        }

        // Declarar variables adicionales detectadas en el código optimizado
        for (Map.Entry<String, String> entry : declaredVariables.entrySet()) {
            String varName = entry.getKey();
            // Solo declarar si no fue declarada por la tabla de símbolos
            if (!isInSymbolTable(varName)) {
                dataSection.append(String.format("    %s dd 0\n", varName));
            }
        }

        textSection.append("\nsection .text\n");
        textSection.append("    global _start\n\n");

        // Código boilerplate para imprimir números
        addPrintNumberFunction();

        textSection.append("_start:\n");

        // Procesar código optimizado línea por línea
        for (String line : optimizedCode) {
            processLine(line.trim());
        }

        // Finalizar programa
        textSection.append("\n    ; Terminar programa\n");
        textSection.append("    mov rax, 60\n");
        textSection.append("    mov rdi, 0\n");
        textSection.append("    syscall\n");

        // Añadir literales de cadena y buffer a .data
        for (Map.Entry<String, String> entry : stringLiterals.entrySet()) {
            String literal = entry.getKey();
            String content = literal.substring(1, literal.length() - 1); // Quitar comillas
            dataSection.append(String.format("    %s db '%s', 0\n", entry.getValue(), content));
        }
        if (!stringLiterals.isEmpty()) {
            dataSection.append("    newline db 10, 0\n");
            dataSection.append("    buffer db 12 dup(0)\n");
        }

        return dataSection.toString() + textSection.toString();
    }

    private void detectVariables() {
        for (String line : optimizedCode) {
            line = line.trim();
            if (line.isEmpty() || line.equals("END"))
                continue;

            String[] parts = line.split(" ");

            // Detectar declaraciones DECLARE
            if (parts.length >= 3 && parts[0].equals("DECLARE")) {
                String varName = parts[1];
                String type = parts[2];
                declaredVariables.put(varName, type);
            }

            // Detectar asignaciones para encontrar variables no declaradas
            if (line.contains(" = ")) {
                String[] assignParts = line.split(" = ");
                if (assignParts.length == 2) {
                    String varName = assignParts[0].trim();
                    // Si no está en la tabla de símbolos y no la hemos visto antes
                    if (!isInSymbolTable(varName) && !declaredVariables.containsKey(varName)) {
                        // Inferir el tipo basado en el valor asignado
                        String value = assignParts[1].trim();
                        if (value.equalsIgnoreCase("TRUE") || value.equalsIgnoreCase("FALSE")) {
                            declaredVariables.put(varName, "BOOLEAN");
                        } else if (isNumeric(value)) {
                            declaredVariables.put(varName, "ENTERO");
                        } else {
                            declaredVariables.put(varName, "ENTERO"); // Por defecto
                        }
                    }
                }
            }
        }
    }

    private boolean isInSymbolTable(String varName) {
        for (Simbolo symbol : symbolTable.getSimbolosAsCollection()) {
            if (symbol.getNombre().equals(varName)) {
                return true;
            }
        }
        return false;
    }

    private void addPrintNumberFunction() {
        textSection.append("print_number:\n");
        textSection.append("    ; Convierte el número en eax a string e imprime\n");
        textSection.append("    push rbx\n");
        textSection.append("    push rcx\n");
        textSection.append("    push rdx\n");
        textSection.append("    push rsi\n");
        textSection.append("    \n");
        textSection.append("    mov ebx, 10          ; Divisor\n");
        textSection.append("    mov ecx, 0           ; Contador de dígitos\n");
        textSection.append("    mov rsi, buffer + 11 ; Apuntar al final del buffer\n");
        textSection.append("    mov byte [rsi], 0    ; Null terminator\n");
        textSection.append("    \n");
        textSection.append("    ; Caso especial para 0\n");
        textSection.append("    cmp eax, 0\n");
        textSection.append("    jne convert_loop\n");
        textSection.append("    dec rsi\n");
        textSection.append("    mov byte [rsi], '0'\n");
        textSection.append("    inc ecx\n");
        textSection.append("    jmp print_it\n");
        textSection.append("    \n");
        textSection.append("convert_loop:\n");
        textSection.append("    cmp eax, 0\n");
        textSection.append("    je print_it\n");
        textSection.append("    xor edx, edx         ; Limpiar edx para división\n");
        textSection.append("    div ebx              ; eax / 10, residuo en edx\n");
        textSection.append("    add dl, '0'          ; Convertir dígito a ASCII\n");
        textSection.append("    dec rsi              ; Retroceder en buffer\n");
        textSection.append("    mov [rsi], dl        ; Almacenar dígito\n");
        textSection.append("    inc ecx              ; Incrementar contador\n");
        textSection.append("    jmp convert_loop\n");
        textSection.append("    \n");
        textSection.append("print_it:\n");
        textSection.append("    mov rax, 1           ; sys_write\n");
        textSection.append("    mov rdi, 1           ; stdout\n");
        textSection.append("    mov rdx, rcx         ; longitud\n");
        textSection.append("    syscall\n");
        textSection.append("    \n");
        textSection.append("    pop rsi\n");
        textSection.append("    pop rdx\n");
        textSection.append("    pop rcx\n");
        textSection.append("    pop rbx\n");
        textSection.append("    ret\n\n");
    }

    private void processLine(String line) {
        if (line.isEmpty() || line.equals("END"))
            return;

        String[] parts = line.split(" ");
        if (parts.length == 0)
            return;

        // Manejar declaraciones
        if (parts[0].equals("DECLARE")) {
            // Ya se procesaron en detectVariables()
            return;
        }

        // Manejar etiquetas
        if (line.endsWith(":")) {
            textSection.append(line + "\n");
            return;
        }

        // Manejar asignaciones
        if (line.contains(" = ")) {
            handleAssignment(line);
        }
        // Manejar PRINT
        else if (parts[0].equals("PRINT")) {
            handlePrint(line);
        }
        // Manejar IF GOTO
        else if (parts[0].equals("IF")) {
            handleIfGoto(line);
        }
        // Manejar GOTO simple
        else if (parts[0].equals("GOTO")) {
            handleGoto(line);
        }
    }

    private void handleAssignment(String line) {
        String[] parts = line.split(" = ");
        if (parts.length != 2)
            return;

        String dest = parts[0].trim();
        String source = parts[1].trim();

        textSection.append(String.format("    ; %s = %s\n", dest, source));

        // Si la fuente es una variable temporal, usar su valor conocido
        if (tempVarValues.containsKey(source)) {
            source = tempVarValues.get(source);
        }

        // Si el destino es una variable temporal, almacenar su valor
        if (isNumeric(source)) {
            // Es un número
            tempVarValues.put(dest, source);

            // Si también es una variable declarada, generar código assembly
            if (isVariableDeclaredAnywhere(dest)) {
                textSection.append(String.format("    mov dword [%s], %s\n", dest, source));
            }
        } else if (source.equalsIgnoreCase("TRUE")) {
            // Booleano TRUE
            tempVarValues.put(dest, "1");
            if (isVariableDeclaredAnywhere(dest)) {
                textSection.append(String.format("    mov dword [%s], 1\n", dest));
            }
        } else if (source.equalsIgnoreCase("FALSE")) {
            // Booleano FALSE
            tempVarValues.put(dest, "0");
            if (isVariableDeclaredAnywhere(dest)) {
                textSection.append(String.format("    mov dword [%s], 0\n", dest));
            }
        } else {
            // Es una variable
            if (isVariableDeclaredAnywhere(source)) {
                textSection.append(String.format("    mov eax, [%s]\n", source));
                if (isVariableDeclaredAnywhere(dest)) {
                    textSection.append(String.format("    mov [%s], eax\n", dest));
                }
                // Almacenar en tempVarValues como referencia a variable
                tempVarValues.put(dest, source);
            }
        }
    }

    private void handlePrint(String line) {
        // PRINT "texto", variable, "más texto"
        String content = line.substring(5).trim(); // Quitar "PRINT"
        textSection.append(String.format("    ; PRINT %s\n", content));

        // Si la línea está vacía después de quitar PRINT, salir
        if (content.isEmpty()) {
            return;
        }

        // Dividir por comas pero preservar las comillas
        List<String> items = parseCommaSeparated(content);

        // Si no hay items, intentar procesar la línea completa como un solo item
        if (items.isEmpty()) {
            items.add(content);
        }

        for (String item : items) {
            item = item.trim();
            if (item.isEmpty())
                continue;

            if (item.startsWith("\"") && item.endsWith("\"")) {
                // Es un literal de cadena
                String label = getStringLiteralLabel(item);
                int length = item.length() - 2; // Quitar las comillas
                textSection.append("    mov rax, 1\n");
                textSection.append("    mov rdi, 1\n");
                textSection.append(String.format("    mov rsi, %s\n", label));
                textSection.append(String.format("    mov rdx, %d\n", length));
                textSection.append("    syscall\n");
            } else if (isNumeric(item)) {
                // Es un número literal
                textSection.append(String.format("    mov eax, %s\n", item));
                textSection.append("    call print_number\n");
            } else if (item.equalsIgnoreCase("TRUE")) {
                // Booleano TRUE - imprimir "TRUE"
                String trueLabel = getStringLiteralLabel("\"TRUE\"");
                textSection.append("    mov rax, 1\n");
                textSection.append("    mov rdi, 1\n");
                textSection.append(String.format("    mov rsi, %s\n", trueLabel));
                textSection.append("    mov rdx, 4\n");
                textSection.append("    syscall\n");
            } else if (item.equalsIgnoreCase("FALSE")) {
                // Booleano FALSE - imprimir "FALSE"
                String falseLabel = getStringLiteralLabel("\"FALSE\"");
                textSection.append("    mov rax, 1\n");
                textSection.append("    mov rdi, 1\n");
                textSection.append(String.format("    mov rsi, %s\n", falseLabel));
                textSection.append("    mov rdx, 5\n");
                textSection.append("    syscall\n");
            } else {
                // Es una variable o temporal
                String value = tempVarValues.get(item);
                if (value != null) {
                    if (isNumeric(value)) {
                        // Variable temporal con valor numérico conocido
                        textSection.append(String.format("    mov eax, %s\n", value));
                        textSection.append("    call print_number\n");
                    } else if (value.equals("1")) {
                        // Booleano TRUE
                        String trueLabel = getStringLiteralLabel("\"TRUE\"");
                        textSection.append("    mov rax, 1\n");
                        textSection.append("    mov rdi, 1\n");
                        textSection.append(String.format("    mov rsi, %s\n", trueLabel));
                        textSection.append("    mov rdx, 4\n");
                        textSection.append("    syscall\n");
                    } else if (value.equals("0")) {
                        // Booleano FALSE
                        String falseLabel = getStringLiteralLabel("\"FALSE\"");
                        textSection.append("    mov rax, 1\n");
                        textSection.append("    mov rdi, 1\n");
                        textSection.append(String.format("    mov rsi, %s\n", falseLabel));
                        textSection.append("    mov rdx, 5\n");
                        textSection.append("    syscall\n");
                    } else if (isVariableDeclaredAnywhere(value)) {
                        // Referencia a otra variable
                        textSection.append(String.format("    mov eax, [%s]\n", value));
                        textSection.append("    call print_number\n");
                    }
                } else if (isVariableDeclaredAnywhere(item)) {
                    // Variable declarada
                    String varType = getVariableType(item);
                    if (varType != null && (varType.equals("BOOLEAN") || varType.equals("Boolean"))) {
                        // Variable booleana - verificar valor y imprimir TRUE/FALSE
                        String trueLabel = getStringLiteralLabel("\"TRUE\"");
                        String falseLabel = getStringLiteralLabel("\"FALSE\"");
                        String skipLabel = "skip_" + stringLiteralCounter++;

                        textSection.append(String.format("    mov eax, [%s]\n", item));
                        textSection.append("    cmp eax, 0\n");
                        textSection.append(String.format("    je print_false_%s\n", skipLabel));
                        textSection.append("    mov rax, 1\n");
                        textSection.append("    mov rdi, 1\n");
                        textSection.append(String.format("    mov rsi, %s\n", trueLabel));
                        textSection.append("    mov rdx, 4\n");
                        textSection.append("    syscall\n");
                        textSection.append(String.format("    jmp %s\n", skipLabel));
                        textSection.append(String.format("print_false_%s:\n", skipLabel));
                        textSection.append("    mov rax, 1\n");
                        textSection.append("    mov rdi, 1\n");
                        textSection.append(String.format("    mov rsi, %s\n", falseLabel));
                        textSection.append("    mov rdx, 5\n");
                        textSection.append("    syscall\n");
                        textSection.append(String.format("%s:\n", skipLabel));
                    } else {
                        // Variable entera
                        textSection.append(String.format("    mov eax, [%s]\n", item));
                        textSection.append("    call print_number\n");
                    }
                } else {
                    // Si no se reconoce el item, agregarlo como comentario
                    textSection.append(String.format("    ; Item no reconocido: %s\n", item));
                }
            }
        }

        // Imprimir nueva línea al final
        textSection.append("    mov rax, 1\n");
        textSection.append("    mov rdi, 1\n");
        textSection.append("    mov rsi, newline\n");
        textSection.append("    mov rdx, 1\n");
        textSection.append("    syscall\n");
    }

    private void handleIfGoto(String line) {
        // IF condicion GOTO L0
        String[] parts = line.split(" ");
        if (parts.length >= 4) {
            String condition = parts[1];
            String label = parts[3];

            textSection.append(String.format("    ; IF %s GOTO %s\n", condition, label));

            // Evaluar la condición
            String value = tempVarValues.get(condition);
            if (value != null) {
                if (value.equals("1") || value.equalsIgnoreCase("true")) {
                    // Condición siempre verdadera
                    textSection.append(String.format("    jmp %s\n", label));
                } else {
                    // Condición siempre falsa - no generar salto
                    textSection.append("    ; Condición siempre falsa - no salto\n");
                }
            } else if (isVariableDeclaredAnywhere(condition)) {
                // Variable declarada - verificar en tiempo de ejecución
                textSection.append(String.format("    mov eax, [%s]\n", condition));
                textSection.append("    cmp eax, 0\n");
                textSection.append(String.format("    jne %s\n", label));
            }
        }
    }

    private void handleGoto(String line) {
        // GOTO L1
        String[] parts = line.split(" ");
        if (parts.length >= 2) {
            String label = parts[1];
            textSection.append(String.format("    ; GOTO %s\n", label));
            textSection.append(String.format("    jmp %s\n", label));
        }
    }

    private List<String> parseCommaSeparated(String content) {
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == ',' && !inQuotes) {
                String item = current.toString().trim();
                if (!item.isEmpty()) {
                    items.add(item);
                }
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        String lastItem = current.toString().trim();
        if (!lastItem.isEmpty()) {
            items.add(lastItem);
        }

        return items;
    }

    private String getStringLiteralLabel(String literal) {
        if (stringLiterals.containsKey(literal)) {
            return stringLiterals.get(literal);
        }
        String label = "msg" + (stringLiteralCounter++);
        stringLiterals.put(literal, label);
        return label;
    }

    private boolean isNumeric(String str) {
        if (str == null)
            return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isVariableDeclaredAnywhere(String varName) {
        // Verificar en tabla de símbolos
        for (Simbolo symbol : symbolTable.getSimbolosAsCollection()) {
            if (symbol.getNombre().equals(varName)) {
                return true;
            }
        }
        // Verificar en variables detectadas
        return declaredVariables.containsKey(varName);
    }

    private String getVariableType(String varName) {
        // Verificar en tabla de símbolos primero
        for (Simbolo symbol : symbolTable.getSimbolosAsCollection()) {
            if (symbol.getNombre().equals(varName)) {
                return symbol.getTipo();
            }
        }
        // Verificar en variables detectadas
        return declaredVariables.get(varName);
    }

    private Simbolo getSymbol(String varName) {
        for (Simbolo symbol : symbolTable.getSimbolosAsCollection()) {
            if (symbol.getNombre().equals(varName)) {
                return symbol;
            }
        }
        return null;
    }
}