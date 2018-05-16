package org.codingmatters.poom.ci.pipeline.stage;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class OnlyWenExpressionTest {

    private List<String> tokens = new LinkedList<>();

    private OnlyWenExpressionVisitor<Void> visitor = new OnlyWenExpressionBaseVisitor<Void>() {
        @Override
        public Void visitVariable(OnlyWenExpressionParser.VariableContext ctx) {
            tokens.add(ctx.getText());
            return super.visitVariable(ctx);
        }

        @Override
        public Void visitOperand(OnlyWenExpressionParser.OperandContext ctx) {
            tokens.add(ctx.getText());
            return super.visitOperand(ctx);
        }

        @Override
        public Void visitOperator(OnlyWenExpressionParser.OperatorContext ctx) {
            tokens.add(ctx.getText());
            return super.visitOperator(ctx);
        }
    };

    @Test
    public void branchIsSyntax__withSimpleToken() {
        this.visitor.visit(this.parseExpression("branch is develop"));
        assertThat(this.tokens, contains("branch", "is", "develop"));
    }

    @Test
    public void branchIsSyntax__withTokenIncludingSpecialChars() {
        this.visitor.visit(this.parseExpression("branch is feature/deployment-#1"));
        assertThat(this.tokens, contains("branch", "is", "feature/deployment-#1"));
    }

    @Test
    public void branchIsSyntax__withQuotedString() {
        this.visitor.visit(this.parseExpression("branch is 'string with spaces'"));
        assertThat(this.tokens, contains("branch", "is", "'string with spaces'"));
    }

    @Test
    public void parseError() {
        parseExpression("not quite parsable");
    }

    private OnlyWenExpressionParser.ExpressionContext parseExpression(String text) {
        CodePointCharStream input = CharStreams.fromString(text);
        CommonTokenStream tokens = new CommonTokenStream(new OnlyWenExpressionLexer(input));
        OnlyWenExpressionParser parser = new OnlyWenExpressionParser(tokens);
        return parser.expression();
    }
}