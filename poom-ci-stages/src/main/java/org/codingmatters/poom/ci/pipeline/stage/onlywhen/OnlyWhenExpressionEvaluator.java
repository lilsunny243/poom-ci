package org.codingmatters.poom.ci.pipeline.stage.onlywhen;

import org.codingmatters.poom.ci.pipeline.stage.OnlyWenExpressionBaseVisitor;
import org.codingmatters.poom.ci.pipeline.stage.OnlyWenExpressionParser;

import java.util.function.Supplier;

public class OnlyWhenExpressionEvaluator extends OnlyWenExpressionBaseVisitor<Boolean>  {

    private String variableValue = null;
    private String operandValue = null;
    private Supplier<Boolean> operation = () -> false;

    private final OnlyWhenVariableProvider variableProvider;

    public OnlyWhenExpressionEvaluator(OnlyWhenVariableProvider variableProvider) {
        this.variableProvider = variableProvider;
    }

    @Override
    public Boolean visitExpression(OnlyWenExpressionParser.ExpressionContext ctx) {
        super.visitExpression(ctx);
        return this.operation.get();
    }

    @Override
    public Boolean visitOperator(OnlyWenExpressionParser.OperatorContext ctx) {
        if(ctx.IS() != null) {
            this.operation = () -> this.variableValue.equals(this.operandValue);
        }
        return super.visitOperator(ctx);
    }

    @Override
    public Boolean visitVariable(OnlyWenExpressionParser.VariableContext ctx) {
        if(ctx.BRANCH() != null) {
            this.variableValue = this.variableProvider.branch();
        }

        return super.visitVariable(ctx);
    }

    @Override
    public Boolean visitOperand(OnlyWenExpressionParser.OperandContext ctx) {
        if(ctx.STRING() != null) {
            this.operandValue = ctx.STRING().getText();
        } else if(ctx.QUOTED_STRING() != null) {
            String text = ctx.QUOTED_STRING().getText();
            this.operandValue = text.substring(1, text.length() - 1);
        }
        return super.visitOperand(ctx);
    }
}
