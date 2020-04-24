package org.codingmatters.poom.ci.pipeline.stage.onlywhen;

import org.codingmatters.poom.ci.pipeline.stage.OnlyWenExpressionBaseVisitor;
import org.codingmatters.poom.ci.pipeline.stage.OnlyWenExpressionParser;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public class OnlyWhenExpressionEvaluator extends OnlyWenExpressionBaseVisitor<Boolean>  {

    private String variableValue = null;
    private List<String> operandValue = new LinkedList<>();
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
            this.operation = () -> this.singleMatch(this.variableValue, this.operandValue.get(0));
        } else if(ctx.IN() != null) {
            this.operation = () -> this.oneOfMatch(this.variableValue, this.operandValue);
        }
        return super.visitOperator(ctx);
    }

    private Boolean singleMatch(String variable, String pattern) {
        return variable.matches(pattern);
    }

    private Boolean oneOfMatch(String variable, List<String> patterns) {
        for (String pattern : patterns) {
            if(this.singleMatch(variable, pattern)) return true;
        }
        return false;
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
            this.operandValue.add(ctx.STRING().getText());
        } else if(ctx.QUOTED_STRING() != null) {
            String text = ctx.QUOTED_STRING().getText();
            this.operandValue.add(text.substring(1, text.length() - 1));
        }
        return super.visitOperand(ctx);
    }
}
