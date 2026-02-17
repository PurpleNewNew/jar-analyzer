package me.n1ar4.jar.analyzer.gui.swing;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public final class SwingTextSync {
    private static final String KEY_TRACKING = "jar.analyzer.sync.tracking";
    private static final String KEY_DIRTY = "jar.analyzer.sync.dirty";
    private static final String KEY_PROGRAMMATIC = "jar.analyzer.sync.programmatic";

    private SwingTextSync() {
    }

    public static void setTextIfIdle(JTextField field, String value) {
        if (field == null || field.isFocusOwner()) {
            return;
        }
        installTracking(field);
        String next = safe(value);
        String current = safe(field.getText());
        if (next.equals(current)) {
            field.putClientProperty(KEY_DIRTY, Boolean.FALSE);
            return;
        }
        if (isDirty(field)) {
            return;
        }
        if (next.isBlank() && !current.isBlank()) {
            return;
        }
        runProgrammatic(field, () -> field.setText(next));
    }

    private static void installTracking(JTextField field) {
        if (Boolean.TRUE.equals(field.getClientProperty(KEY_TRACKING))) {
            return;
        }
        field.putClientProperty(KEY_TRACKING, Boolean.TRUE);
        field.putClientProperty(KEY_DIRTY, Boolean.FALSE);
        field.putClientProperty(KEY_PROGRAMMATIC, Boolean.FALSE);
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                markDirty();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                markDirty();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                markDirty();
            }

            private void markDirty() {
                if (!isProgrammatic(field)) {
                    field.putClientProperty(KEY_DIRTY, Boolean.TRUE);
                }
            }
        });
    }

    private static void runProgrammatic(JTextField field, Runnable action) {
        field.putClientProperty(KEY_PROGRAMMATIC, Boolean.TRUE);
        try {
            action.run();
            field.putClientProperty(KEY_DIRTY, Boolean.FALSE);
        } finally {
            field.putClientProperty(KEY_PROGRAMMATIC, Boolean.FALSE);
        }
    }

    private static boolean isDirty(JTextField field) {
        return Boolean.TRUE.equals(field.getClientProperty(KEY_DIRTY));
    }

    private static boolean isProgrammatic(JTextField field) {
        return Boolean.TRUE.equals(field.getClientProperty(KEY_PROGRAMMATIC));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

