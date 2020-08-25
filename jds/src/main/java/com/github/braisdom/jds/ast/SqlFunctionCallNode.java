package com.github.braisdom.jds.ast;

import java.util.ArrayList;
import java.util.List;

public class SqlFunctionCallNode extends Aliasable implements Projectional,
        SqlFunctionOperand, ExpressionOperand, FromNode {
    private List<SqlFunctionOperand> operands = new ArrayList<>();
    private String type;
    private String name;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public SqlFunctionCallNode setName(String name) {
        this.name = name;
        return this;
    }

    public List<SqlFunctionOperand> getOperands() {
        return operands;
    }

    public void addOperands(SqlFunctionOperand operand) {
        this.operands.add(operand);
    }
}