package com.example;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.ArrayList;

public class TokenTableModel extends AbstractTableModel {
    private final String[] columnNames = {"Tipo", "Lexema", "Patrón", "Línea", "Columna"};
    private List<Token> tokens;

    public TokenTableModel() {
        this.tokens = new ArrayList<>();
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = new ArrayList<>(); // Crear nueva lista para evitar modificar la original directamente
        for (Token t : tokens) {
            if (t.getType() != TokenType.ERROR) { // No incluir tokens de error léxico si se muestran por separado
                this.tokens.add(t);
            }
        }
        fireTableDataChanged(); // Notificar a la JTable que los datos cambiaron
    }

    public void clearData() {
        this.tokens.clear();
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return tokens.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Token token = tokens.get(rowIndex);
        switch (columnIndex) {
            case 0: return token.getType();
            case 1: return token.getLexeme();
            case 2: return token.getPattern();
            case 3: return token.getLineNumber();
            case 4: return token.getColumnNumber();
            default: return null;
        }
    }

    // Opcional: definir tipos de columna si se necesita un renderizado especial
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0: return TokenType.class;
            case 1: return String.class;
            case 2: return String.class;
            case 3: return Integer.class;
            case 4: return Integer.class;
            default: return Object.class;
        }
    }
}
