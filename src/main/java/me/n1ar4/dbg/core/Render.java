/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.dbg.core;

import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import me.n1ar4.dbg.gui.MainForm;
import me.n1ar4.dbg.gui.TableManager;
import me.n1ar4.dbg.parser.MethodObject;
import me.n1ar4.dbg.parser.OpcodeObject;
import me.n1ar4.dbg.utils.ASMUtil;
import me.n1ar4.log.LogManager;
import me.n1ar4.log.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.util.List;

public class Render {
    private static final Logger logger = LogManager.getLogger();

    private static void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    @SuppressWarnings("all")
    private static String randOperands(String operands) {
        if (operands == null || operands.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<font style=\"font-weight: bold;\">");
        sb.append(operands);
        sb.append(" (");
        int unsignedInt = Integer.parseInt(operands, 16);
        int signedInt = unsignedInt > 0x7FFF ? unsignedInt - 0x10000 : unsignedInt;
        sb.append(signedInt);
        sb.append(") ");
        sb.append("</font>");
        sb.append("</html>");
        return sb.toString();
    }

    @SuppressWarnings("all")
    private static String rendOpcode(String opcodeStr) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<font style=\"color: blue; font-weight: bold;\">");
        sb.append(opcodeStr);
        sb.append("</font>");
        sb.append("</html>");
        return sb.toString();
    }

    public static void refreshMethodModel(MethodObject object, long l) {
        if (object == null || object.getOpcodes() == null) {
            logger.error("method object is null");
            return;
        }
        String[] columnNames = {"null", "null", "null", "null"};
        Object[][] data = new Object[object.getOpcodes().size() + 1][];
        data[0] = new Object[]{null, "Method", "JVM Opcode", "Operands"};

        String methodText = ASMUtil.convertMethodDesc(
                object.getMethodName(),
                object.getMethodDec());
        String classText = ASMUtil.renderClass(object.getClassName());

        int debugRow = 0;
        long jumpTarget = -1;
        for (int i = 1; i < data.length; i++) {
            OpcodeObject op = object.getOpcodes().get(i - 1);
            if (op.getOpcodeIndex() == l) {
                debugRow = i;
            }
            data[i] = new Object[]{null,
                    String.format("%08x", op.getOpcodeIndex()),
                    rendOpcode(op.getOpcodeStr()),
                    randOperands(op.getOperands())};
            if (op.getOpcodeIndex() == l) {
                if (op.getOpcodeStr().equals("GOTO")) {
                    int unsignedInt = Integer.parseInt(op.getOperands(), 16);
                    int signedInt = unsignedInt > 0x7FFF ? unsignedInt - 0x10000 : unsignedInt;
                    long target = l + signedInt;
                    logger.info("goto location: {}", target);
                    jumpTarget = target;
                }
            }
        }

        if (debugRow == 0) {
            logger.error("current debug line error");
            return;
        }
        int jumpRow = -1;
        if (jumpTarget != -1) {
            for (int i = 1; i < data.length; i++) {
                OpcodeObject op = object.getOpcodes().get(i - 1);
                if (op.getOpcodeIndex() == jumpTarget) {
                    jumpRow = i;
                    break;
                }
            }
        }

        final int highlightRow = debugRow;
        final long finalJumpTarget = jumpTarget;
        final int finalJumpRow = jumpRow;
        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        runOnEdt(() -> {
            JTable bytecodeTable = MainForm.getInstance().getBytecodeTable();
            MainForm.getInstance().getCurMethodText().setText(methodText);
            MainForm.getInstance().getCurClassText().setText(classText);
            if (finalJumpTarget != -1) {
                TableManager.addJump(finalJumpTarget);
            }
            TableManager.addHighlight(highlightRow);
            TableManager.setCur(l);
            if (finalJumpRow != -1) {
                TableManager.addJumpRow(finalJumpRow);
            }
            bytecodeTable.setModel(model);
            TableColumn column = bytecodeTable.getColumnModel().getColumn(0);
            column.setMinWidth(30);
            column.setMaxWidth(30);
            column.setPreferredWidth(30);
            bytecodeTable.repaint();
        });
    }

    public static void refreshFrames(List<StackFrame> frames) {
        if (frames == null) {
            return;
        }
        String[] columnNames = {"thread name", "stack method"};
        Object[][] data = new Object[frames.size()][];
        int i = 0;
        for (StackFrame frame : frames) {
            Location object = frame.location();
            data[i] = new Object[]{
                    frame.thread().name(),
                    ASMUtil.convertMethodDescWithClass(
                            object.declaringType().name(),
                            object.method().name(),
                            object.method().signature())};
            i++;
        }
        DefaultTableModel model = new DefaultTableModel(data, columnNames);
        runOnEdt(() -> {
            JTable frameTable = MainForm.getInstance().getThreadStackTable();
            frameTable.setModel(model);
            TableColumn column = frameTable.getColumnModel().getColumn(0);
            column.setMinWidth(100);
            column.setMaxWidth(100);
            column.setPreferredWidth(100);
            frameTable.repaint();
        });
    }

    public static void refreshVariables(StackFrame frame, List<LocalVariable> localVariables) {
        if (frame == null || localVariables == null) {
            return;
        }
        String[] columnNames = {"variable name", "variable type", "variable value"};
        Object[][] data = new Object[localVariables.size()][];
        int i = 0;
        for (LocalVariable var : localVariables) {
            Value value = frame.getValue(var);
            data[i] = new Object[]{var.name(), var.signature(), value == null ? "null" : value.toString()};
            i++;
        }
        DefaultTableModel model = new DefaultTableModel(data, columnNames);
        runOnEdt(() -> {
            JTable varTable = MainForm.getInstance().getLocalVariablesTable();
            varTable.setModel(model);
            varTable.repaint();
        });
    }
}
