package org.codingmatters.poom.ci.pipeline.stage;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.codingmatters.poom.ci.pipeline.descriptors.Stage;
import org.codingmatters.poom.ci.pipeline.stage.onlywhen.OnlyWhenExpressionEvaluator;
import org.codingmatters.poom.ci.pipeline.stage.onlywhen.OnlyWhenVariableProvider;

public class OnlyWhenProcessor {

    private final OnlyWhenVariableProvider variableProvider;

    public OnlyWhenProcessor(OnlyWhenVariableProvider variableProvider) {
        this.variableProvider = variableProvider;
    }

    public boolean isExecutable(Stage stage) throws OnlyWhenParsingException {
        if(stage.onlyWen() != null) {
            for (String expressionText : stage.onlyWen()) {
                OnlyWenExpressionParser.ExpressionContext expression = this.parseExpression(expressionText);
                Boolean result = new OnlyWhenExpressionEvaluator(this.variableProvider).visitExpression(expression);
                if (result == null || !result) return false;
            }
        }
        return true;
    }

    private OnlyWenExpressionParser.ExpressionContext parseExpression(String text) throws OnlyWhenParsingException {
        try {
            ThrowingErrorListener errorListener = new ThrowingErrorListener();

            CodePointCharStream input = CharStreams.fromString(text);

            OnlyWenExpressionLexer lexer = new OnlyWenExpressionLexer(input);
            lexer.removeErrorListeners();
            lexer.addErrorListener(errorListener);

            CommonTokenStream tokens = new CommonTokenStream(lexer);

            OnlyWenExpressionParser parser = new OnlyWenExpressionParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(errorListener);

            return parser.expression();
        } catch(ParseCancellationException e) {
            throw new OnlyWhenParsingException("error parsing expression : " + text, e);
        }
    }

    class ThrowingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) throws ParseCancellationException {
            throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    }
}
