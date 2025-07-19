package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class CodeOptimizer {

    private List<String> originalCode;
    private List<String> optimizedCode;
    private Map<String, String> constants;
    private Set<String> usedVariables;

    public CodeOptimizer(List<String> threeAddressCode) {
        this.originalCode = threeAddressCode;
        this.optimizedCode = new ArrayList<>(threeAddressCode);
        this.constants = new HashMap<>();
        this.usedVariables = new HashSet<>();
    }

    public List<String> optimize() {
        // Múltiples pasadas para optimización completa
        for (int i = 0; i < 3; i++) {
            propagateConstants();
            constantFolding();
            eliminateUnreachableCode();
            removeRedundantJumps();
            eliminateDeadCode();
        }

        // Final cleanup pass
        cleanupFinalCode();

        return optimizedCode;
    }

    private void cleanupFinalCode() {
        List<String> newCode = new ArrayList<>();
        Set<String> usedLabels = new HashSet<>();

        // First pass: find which labels are actually used
        for (String instruction : optimizedCode) {
            String trimmed = instruction.trim();
            if (trimmed.startsWith("GOTO ")) {
                String label = trimmed.substring(5).trim();
                usedLabels.add(label);
            } else if (trimmed.startsWith("IF") && trimmed.contains("GOTO")) {
                String[] parts = trimmed.split("\\s+");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i].equals("GOTO")) {
                        usedLabels.add(parts[i + 1]);
                        break;
                    }
                }
            }
        }

        // Second pass: keep only used labels and reachable code
        for (String instruction : optimizedCode) {
            String trimmed = instruction.trim();

            if (trimmed.endsWith(":")) {
                String label = trimmed.substring(0, trimmed.length() - 1);
                if (usedLabels.contains(label)) {
                    newCode.add(instruction); // Preserve original formatting
                }
            } else {
                // For PRINT statements, ensure they are complete
                if (trimmed.startsWith("PRINT")) {
                    String correctedInstruction = ensureCompletePrintStatement(instruction);
                    newCode.add(correctedInstruction);
                } else {
                    newCode.add(instruction); // Preserve original formatting
                }
            }
        }

        optimizedCode = newCode;
    }

    private String ensureCompletePrintStatement(String instruction) {
        String trimmed = instruction.trim();

        // Count quotes
        long quoteCount = trimmed.chars().filter(ch -> ch == '"').count();

        // If odd number of quotes, we're missing a closing quote
        if (quoteCount % 2 == 1) {
            // Find the last quote position
            int lastQuote = trimmed.lastIndexOf('"');

            // Check if the instruction ends properly
            if (lastQuote >= 0 && !trimmed.endsWith("\"")) {
                // Add the missing closing quote
                return instruction + "\"";
            }
        }

        return instruction;
    }

    private void propagateConstants() {
        Map<String, String> constantValues = new HashMap<>();
        Map<String, String> variableValues = new HashMap<>();
        List<String> newCode = new ArrayList<>();

        // First pass: collect all constant assignments and expression evaluations
        for (String instruction : optimizedCode) {
            String trimmed = instruction.trim();

            if (trimmed.startsWith("DECLARE") || trimmed.endsWith(":")) {
                continue;
            }

            String[] parts = trimmed.split("\\s+");

            // Handle simple assignments: var = value
            if (parts.length == 3 && parts[1].equals("=")) {
                String var = parts[0];
                String value = parts[2];

                // Propagate if value is a known constant
                if (constantValues.containsKey(value)) {
                    value = constantValues.get(value);
                }

                constantValues.put(var, value);
                variableValues.put(var, value);
            }
            // Handle expressions: var = operand1 operator operand2
            else if (parts.length >= 5 && parts[1].equals("=")) {
                String var = parts[0];
                String operand1 = parts[2];
                String operator = parts[3];
                String operand2 = parts[4];

                // Replace operands with their constant values if known
                if (constantValues.containsKey(operand1)) {
                    operand1 = constantValues.get(operand1);
                }
                if (constantValues.containsKey(operand2)) {
                    operand2 = constantValues.get(operand2);
                }

                // Try to evaluate the expression
                String result = null;
                if (isNumeric(operand1) && isNumeric(operand2)) {
                    double val1 = Double.parseDouble(operand1);
                    double val2 = Double.parseDouble(operand2);

                    // Check if it's arithmetic or comparison
                    if (operator.equals("+") || operator.equals("-") ||
                            operator.equals("*") || operator.equals("/")) {
                        result = evaluateExpression(val1, operator, val2);
                    } else {
                        result = evaluateComparison(val1, operator, val2);
                    }
                }

                if (result != null) {
                    constantValues.put(var, result);
                    variableValues.put(var, result);
                } else {
                    String expression = operand1 + " " + operator + " " + operand2;
                    variableValues.put(var, expression);
                }
            }
        }

        // Second pass: generate optimized code
        for (String instruction : optimizedCode) {
            String trimmed = instruction.trim();

            // Keep declarations and labels
            if (trimmed.startsWith("DECLARE") || trimmed.endsWith(":")) {
                newCode.add(instruction);
                continue;
            }

            String[] parts = trimmed.split("\\s+");

            // Handle assignments
            if (parts.length >= 3 && parts[1].equals("=")) {
                String var = parts[0];

                // Skip temporary variable assignments
                if (var.startsWith("t")) {
                    continue;
                }

                // Use the final computed value
                if (constantValues.containsKey(var)) {
                    newCode.add(var + " = " + constantValues.get(var));
                } else {
                    newCode.add(instruction);
                }
            }
            // Handle other instructions (PRINT, IF, GOTO, etc.)
            else {
                String newInstruction = trimmed;

                // Replace variables with their constant values
                for (Map.Entry<String, String> entry : constantValues.entrySet()) {
                    String varName = entry.getKey();
                    String constValue = entry.getValue();
                    newInstruction = replaceVariableInInstruction(newInstruction, varName, constValue);
                }

                newCode.add(newInstruction);
            }
        }

        optimizedCode = newCode;
    }

    private String replaceVariableInInstruction(String instruction, String varName, String constValue) {
        // Handle different contexts where variables might appear
        String result = instruction;

        // Special handling for PRINT statements to preserve variable names in strings
        if (instruction.startsWith("PRINT")) {
            return handlePrintInstruction(instruction, varName, constValue);
        } else {
            // For other instructions, simple replacement
            result = result.replaceAll("\\b" + varName + "\\b", constValue);
        }

        return result;
    }

    private String handlePrintInstruction(String instruction, String varName, String constValue) {
        StringBuilder result = new StringBuilder();
        boolean insideQuotes = false;
        boolean escapeNext = false;

        for (int i = 0; i < instruction.length(); i++) {
            char c = instruction.charAt(i);

            if (escapeNext) {
                result.append(c);
                escapeNext = false;
                continue;
            }

            if (c == '\\') {
                escapeNext = true;
                result.append(c);
                continue;
            }

            if (c == '"') {
                insideQuotes = !insideQuotes;
                result.append(c);
                continue;
            }

            result.append(c);
        }

        // If we ended inside quotes, add the missing closing quote
        if (insideQuotes) {
            result.append('"');
        }

        // Now do variable replacement on the corrected string
        String correctedInstruction = result.toString();

        // Split by quotes to handle replacement properly
        String[] parts = correctedInstruction.split("\"");
        StringBuilder finalResult = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i % 2 == 0) {
                // Outside quotes - replace variables
                String part = parts[i].replaceAll("\\b" + varName + "\\b", constValue);
                finalResult.append(part);
            } else {
                // Inside quotes - keep original
                finalResult.append(parts[i]);
            }

            if (i < parts.length - 1) {
                finalResult.append("\"");
            }
        }

        return finalResult.toString();
    }

    private void constantFolding() {
        List<String> newCode = new ArrayList<>();

        for (String instruction : optimizedCode) {
            String trimmed = instruction.trim();

            if (trimmed.startsWith("DECLARE") || trimmed.endsWith(":")) {
                newCode.add(instruction);
                continue;
            }

            String[] parts = trimmed.split("\\s+");

            // Handle arithmetic expressions
            if (parts.length == 5 && parts[1].equals("=")) {
                String var = parts[0];
                String operand1 = parts[2];
                String operator = parts[3];
                String operand2 = parts[4];

                if (isNumeric(operand1) && isNumeric(operand2)) {
                    double val1 = Double.parseDouble(operand1);
                    double val2 = Double.parseDouble(operand2);
                    String result = evaluateExpression(val1, operator, val2);
                    newCode.add(var + " = " + result);
                    continue;
                }
            }
            // Handle comparison expressions
            else if (parts.length >= 5 && parts[1].equals("=")) {
                String var = parts[0];
                String operand1 = parts[2];
                String operator = parts[3];
                String operand2 = parts[4];

                if (isNumeric(operand1) && isNumeric(operand2)) {
                    double val1 = Double.parseDouble(operand1);
                    double val2 = Double.parseDouble(operand2);
                    String result = evaluateComparison(val1, operator, val2);
                    if (result != null) {
                        newCode.add(var + " = " + result);
                        continue;
                    }
                }
            }

            newCode.add(instruction);
        }

        optimizedCode = newCode;
    }

    private void eliminateDeadCode() {
        // First pass: find all used variables
        Set<String> used = new HashSet<>();

        for (String instruction : optimizedCode) {
            String trimmed = instruction.trim();

            if (trimmed.startsWith("PRINT") || trimmed.startsWith("IF") ||
                    trimmed.startsWith("GOTO") || trimmed.equals("END")) {
                // Extract variables used in these statements
                String[] parts = trimmed.split("\\s+");
                for (String part : parts) {
                    if (isVariable(part)) {
                        used.add(part);
                    }
                }
            }
        }

        // Second pass: keep only assignments to used variables
        List<String> newCode = new ArrayList<>();

        for (String instruction : optimizedCode) {
            String trimmed = instruction.trim();

            if (trimmed.startsWith("DECLARE") || trimmed.endsWith(":") ||
                    trimmed.startsWith("PRINT") || trimmed.startsWith("IF") ||
                    trimmed.startsWith("GOTO") || trimmed.equals("END")) {
                newCode.add(instruction);
            } else {
                String[] parts = trimmed.split("\\s+");
                if (parts.length >= 3 && parts[1].equals("=")) {
                    String var = parts[0];
                    // Keep assignment if variable is used or it's not a temporary
                    if (used.contains(var) || !var.startsWith("t")) {
                        newCode.add(instruction);
                    }
                } else {
                    newCode.add(instruction);
                }
            }
        }

        optimizedCode = newCode;
    }

    private void eliminateUnreachableCode() {
        List<String> newCode = new ArrayList<>();
        Map<String, Integer> labelMap = new HashMap<>();
        Set<Integer> reachableLines = new HashSet<>();

        // Build label map
        for (int i = 0; i < optimizedCode.size(); i++) {
            String instruction = optimizedCode.get(i).trim();
            if (instruction.endsWith(":")) {
                String label = instruction.substring(0, instruction.length() - 1);
                labelMap.put(label, i);
            }
        }

        // Mark reachable code starting from line 0
        markReachable(0, labelMap, reachableLines);

        // Keep only reachable code
        for (int i = 0; i < optimizedCode.size(); i++) {
            String originalInstruction = optimizedCode.get(i); // Preserve original formatting
            String instruction = originalInstruction.trim();

            if (reachableLines.contains(i)) {
                // Additional check: if it's an IF statement with a constant condition
                if (instruction.startsWith("IF")) {
                    String[] parts = instruction.split("\\s+");
                    if (parts.length >= 2) {
                        String condition = parts[1];
                        if (condition.equals("TRUE")) {
                            // Convert "IF TRUE GOTO Lx" to "GOTO Lx"
                            for (int j = 2; j < parts.length; j++) {
                                if (parts[j].equals("GOTO") && j + 1 < parts.length) {
                                    newCode.add("GOTO " + parts[j + 1]);
                                    break;
                                }
                            }
                        } else if (condition.equals("FALSE")) {
                            // "IF FALSE GOTO Lx" can be completely removed
                            // (fall through to next instruction)
                        } else {
                            newCode.add(originalInstruction); // Use original instruction
                        }
                    } else {
                        newCode.add(originalInstruction); // Use original instruction
                    }
                } else {
                    newCode.add(originalInstruction); // Use original instruction to preserve formatting
                }
            }
        }

        optimizedCode = newCode;
    }

    private void markReachable(int index, Map<String, Integer> labelMap, Set<Integer> reachable) {
        if (index >= optimizedCode.size() || reachable.contains(index)) {
            return;
        }

        reachable.add(index);
        String instruction = optimizedCode.get(index).trim();

        if (instruction.startsWith("IF")) {
            // IF can go to next line or jump to label
            String[] parts = instruction.split("\\s+");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("GOTO") && i + 1 < parts.length) {
                    String label = parts[i + 1];
                    if (labelMap.containsKey(label)) {
                        markReachable(labelMap.get(label), labelMap, reachable);
                    }
                    break;
                }
            }
            markReachable(index + 1, labelMap, reachable);
        } else if (instruction.startsWith("GOTO")) {
            String[] parts = instruction.split("\\s+");
            if (parts.length >= 2) {
                String label = parts[1];
                if (labelMap.containsKey(label)) {
                    markReachable(labelMap.get(label), labelMap, reachable);
                }
            }
        } else if (instruction.equals("END")) {
            // Stop here
            return;
        } else {
            // Continue to next line
            markReachable(index + 1, labelMap, reachable);
        }
    }

    private void removeRedundantJumps() {
        List<String> tempCode = new ArrayList<>();
        Map<String, Integer> labelLocations = new HashMap<>();

        // Find all label locations
        for (int i = 0; i < optimizedCode.size(); i++) {
            String instruction = optimizedCode.get(i).trim();
            if (instruction.endsWith(":")) {
                labelLocations.put(instruction.substring(0, instruction.length() - 1), i);
            }
        }

        for (int i = 0; i < optimizedCode.size(); i++) {
            String instruction = optimizedCode.get(i).trim();
            boolean isRedundant = false;

            if (instruction.startsWith("GOTO ")) {
                String label = instruction.substring(5).trim();
                if (labelLocations.containsKey(label)) {
                    int labelIndex = labelLocations.get(label);
                    // If label is on the next line, jump is redundant
                    if (labelIndex == i + 1) {
                        isRedundant = true;
                    }
                }
            }

            if (!isRedundant) {
                tempCode.add(optimizedCode.get(i));
            }
        }
        optimizedCode = tempCode;
    }

    private String evaluateComparison(double val1, String operator, double val2) {
        switch (operator) {
            case ">":
                return val1 > val2 ? "TRUE" : "FALSE";
            case "<":
                return val1 < val2 ? "TRUE" : "FALSE";
            case ">=":
                return val1 >= val2 ? "TRUE" : "FALSE";
            case "<=":
                return val1 <= val2 ? "TRUE" : "FALSE";
            case "==":
                return val1 == val2 ? "TRUE" : "FALSE";
            case "!=":
                return val1 != val2 ? "TRUE" : "FALSE";
        }
        return null;
    }

    private String evaluateExpression(double val1, String operator, double val2) {
        switch (operator) {
            case "+":
                return String.valueOf((int) (val1 + val2));
            case "-":
                return String.valueOf((int) (val1 - val2));
            case "*":
                return String.valueOf((int) (val1 * val2));
            case "/":
                if (val2 != 0) {
                    return String.valueOf((int) (val1 / val2));
                }
                break;
        }
        return val1 + " " + operator + " " + val2;
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isVariable(String str) {
        return str.matches("[a-zA-Z][a-zA-Z0-9_]*") &&
                !str.equals("GOTO") && !str.equals("IF") &&
                !str.equals("PRINT") && !str.equals("END") &&
                !str.equals("TRUE") && !str.equals("FALSE");
    }
}