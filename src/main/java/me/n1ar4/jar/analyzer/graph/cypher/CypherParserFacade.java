/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package me.n1ar4.jar.analyzer.graph.cypher;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.neo4j.cypher.internal.parser.v5.Cypher5Lexer;
import org.neo4j.cypher.internal.parser.v5.Cypher5Parser;

public final class CypherParserFacade {
    public void validate(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("cypher_empty_query");
        }
        Cypher5Lexer lexer = new Cypher5Lexer(CharStreams.fromString(query));
        Cypher5Parser parser = new Cypher5Parser(new CommonTokenStream(lexer));
        ParseErrorListener listener = new ParseErrorListener();
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        lexer.addErrorListener(listener);
        parser.addErrorListener(listener);
        parser.statements();
        if (listener.failed) {
            throw new IllegalArgumentException(listener.message == null ? "cypher_parse_error" : listener.message);
        }
    }

    private static final class ParseErrorListener extends BaseErrorListener {
        private boolean failed;
        private String message;

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line,
                                int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            failed = true;
            message = "cypher_parse_error(line=" + line + ", col=" + charPositionInLine + "): " + msg;
        }
    }
}
