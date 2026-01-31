/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.semantic;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.type.Type;

public final class FlowAnalyzer {
    private FlowAnalyzer() {
    }

    public static boolean isInSameBranch(Node candidate, Position usagePos, Node scopeRoot) {
        if (usagePos == null) {
            return true;
        }
        Node cur = candidate;
        while (cur != null && cur != scopeRoot) {
            Node parent = cur.getParentNode().orElse(null);
            if (parent instanceof IfStmt) {
                IfStmt ifs = (IfStmt) parent;
                Node thenStmt = ifs.getThenStmt();
                Node elseStmt = ifs.getElseStmt().orElse(null);
                boolean candidateInThen = isWithinNode(thenStmt, candidate);
                boolean candidateInElse = elseStmt != null && isWithinNode(elseStmt, candidate);
                boolean usageInThen = isWithinNode(thenStmt, usagePos);
                boolean usageInElse = elseStmt != null && isWithinNode(elseStmt, usagePos);
                if (candidateInThen && !usageInThen) {
                    return false;
                }
                if (candidateInElse && !usageInElse) {
                    return false;
                }
            }
            if (parent instanceof SwitchEntry) {
                SwitchEntry entry = (SwitchEntry) parent;
                boolean candidateInEntry = isWithinNode(entry, candidate);
                boolean usageInEntry = isWithinNode(entry, usagePos);
                if (candidateInEntry && !usageInEntry) {
                    return false;
                }
            }
            cur = parent;
        }
        return true;
    }

    public static Type findInstanceofType(Expression condition, String name, boolean positive) {
        if (condition == null) {
            return null;
        }
        Expression expr = unwrapEnclosed(condition);
        if (expr instanceof UnaryExpr) {
            UnaryExpr unary = (UnaryExpr) expr;
            if (unary.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
                return findInstanceofType(unary.getExpression(), name, !positive);
            }
        }
        if (expr instanceof InstanceOfExpr) {
            if (!positive) {
                return null;
            }
            InstanceOfExpr inst = (InstanceOfExpr) expr;
            Expression left = unwrapEnclosed(inst.getExpression());
            if (left instanceof NameExpr
                    && name.equals(((NameExpr) left).getNameAsString())) {
                return inst.getType();
            }
            return null;
        }
        if (expr instanceof BinaryExpr) {
            BinaryExpr binary = (BinaryExpr) expr;
            if (binary.getOperator() == BinaryExpr.Operator.AND) {
                Type left = findInstanceofType(binary.getLeft(), name, positive);
                if (left != null) {
                    return left;
                }
                return findInstanceofType(binary.getRight(), name, positive);
            }
            if (binary.getOperator() == BinaryExpr.Operator.OR) {
                return null;
            }
        }
        return null;
    }

    private static Expression unwrapEnclosed(Expression expr) {
        Expression cur = expr;
        while (cur instanceof EnclosedExpr) {
            cur = ((EnclosedExpr) cur).getInner();
        }
        return cur;
    }

    public static boolean isWithinNode(Node node, Node target) {
        if (node == null || target == null) {
            return false;
        }
        Range r1 = node.getRange().orElse(null);
        Range r2 = target.getRange().orElse(null);
        if (r1 == null || r2 == null) {
            return false;
        }
        return contains(r1, r2.begin);
    }

    public static boolean isWithinNode(Node node, Position pos) {
        if (node == null || pos == null) {
            return false;
        }
        Range range = node.getRange().orElse(null);
        return range != null && contains(range, pos);
    }

    private static boolean contains(Range range, Position pos) {
        if (range == null || pos == null) {
            return false;
        }
        if (pos.line < range.begin.line || pos.line > range.end.line) {
            return false;
        }
        if (pos.line == range.begin.line && pos.column < range.begin.column) {
            return false;
        }
        if (pos.line == range.end.line && pos.column > range.end.column) {
            return false;
        }
        return true;
    }
}
