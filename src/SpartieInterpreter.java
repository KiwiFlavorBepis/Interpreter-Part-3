import java.util.List;

public class SpartieInterpreter {
    private Environment globalEnvironment = new Environment();

    public void run(List<Statement> statements) {
        for(Statement statement : statements) {
            interpret(statement);
        }
    }

    private void interpret(Statement statement) {
        switch(statement) {
            case Statement.PrintStatement printStatement ->             interpretPrintStatement(printStatement);
            case Statement.ExpressionStatement expressionStatement ->   interpretExpressionStatement(expressionStatement);
            case Statement.VariableStatement variableStatement ->       interpretVariableStatement(variableStatement);
            case Statement.BlockStatement blockStatement ->             interpretBlockStatement(blockStatement);
            case Statement.IfStatement ifStatement ->                   interpretIfStatement(ifStatement);
            case Statement.WhileStatement whileStatement ->             interpretWhileStatement(whileStatement);
            case null, default -> {}
        }
    }

    private Object interpret(Expression expression) {
        return switch (expression) {
            case Expression.LogicalExpression       logicalExpression     -> interpretLogical(logicalExpression);
            case Expression.AssignmentExpression    assignmentExpression  -> interpretAssign(assignmentExpression);
            case Expression.VariableExpression      variableExpression    -> interpretVariable(variableExpression);
            case Expression.LiteralExpression       literalExpression     -> interpretLiteral(literalExpression);
            case Expression.ParenthesesExpression   parenthesesExpression -> interpretParenthesis(parenthesesExpression);
            case Expression.UnaryExpression         unaryExpression       -> interpretUnary(unaryExpression);
            case Expression.BinaryExpression        binaryExpression      -> interpretBinary(binaryExpression);
            case null, default -> null;
        };
    }

    // Statement Implementation
    private void interpretWhileStatement(Statement.WhileStatement statement) {
        Expression condition = statement.condition;
        Statement body = statement.body;

        if (isTrue(interpret(condition))) {
            interpret(body);
            interpretWhileStatement(statement);
        }
    }

    private void interpretIfStatement(Statement.IfStatement statement) {
        // Evaluate the condition and then execute the appropriate branch
        Expression condition = statement.condition;
        Statement thenBranch = statement.thenBranch;
        Statement elseBranch = statement.elseBranch;

        if (isTrue(interpret(condition))) interpret(thenBranch);
        else interpret(elseBranch);
    }

    private void interpretBlockStatement(Statement.BlockStatement statement) {
        interpretBlock(statement.statements, new Environment(globalEnvironment));
    }

    private void interpretVariableStatement(Statement.VariableStatement statement) {
        Object value = null;

        // If the initializer is provided, evaluate the variable assignment expression
        if (statement.initializer != null) value = interpret(statement.initializer);

        globalEnvironment.define(statement.name.text, value);
    }

    private void interpretExpressionStatement(Statement.ExpressionStatement statement) {
        // We can re-use our previous interpret
        interpret(statement.expression);
    }

    private void interpretPrintStatement(Statement.PrintStatement statement) {
        // First evaluate the expression
        Object value = interpret(statement.expression);

        if (value != null) System.out.println(value);
        else System.out.println("null");
    }

    private void interpretBlock(List<Statement> statements, Environment environment) {
        // Store a reference to the previous environment and swap it out with the new environment
        Environment previous = globalEnvironment;

        globalEnvironment = environment;
        for (Statement statement : statements) interpret(statement);

        // Restore environment
        globalEnvironment = previous;
    }

    private Object interpretLogical(Expression.LogicalExpression logicalExpression) {
        Object left = interpret(logicalExpression.left);

        // Or
        if (logicalExpression.operator.type == TokenType.OR) {
            // Short-circuit, no need to check right
            if (isTrue(left)) return left;
        }

        // And (the only other possible logical operator
        else {
            // Check left first
            if (!isTrue(left)) return left;
        }

        //Or or And
        // Expression value is now determined by right
        return interpret(logicalExpression.right);
    }

    private Object interpretAssign(Expression.AssignmentExpression expression) {
        // Interpret the expression for the assignment and then assign it to our global environment, then return the value
        Object value = interpret(expression.value);
        globalEnvironment.assign(expression.name, value);

        return value;
    }

    private Object interpretVariable(Expression.VariableExpression expression) {
        // Return the value from our global environment
        return globalEnvironment.get(expression.name.text);
    }

    private Object interpretLiteral(Expression.LiteralExpression expression) {
        return expression.literalValue;
    }

    private Object interpretParenthesis(Expression.ParenthesesExpression expression) {
        // Take what is inside and send it back
        return this.interpret(expression.expression);
    }

    private Object interpretUnary(Expression.UnaryExpression expression) {
        Object right = interpret(expression.right);

        return switch (expression.operator.type) {
            case NOT -> !isTrue(right);
            case SUBTRACT -> {
                // Need to validate right first
                validateOperand(expression.operator, right);
                yield -1 * (double) right;
            }
            default -> null;
        };
    }

    private Object interpretBinary(Expression.BinaryExpression expression) {
        Object left = interpret(expression.left);
        Object right = interpret(expression.right);

        // Handle unique case with add operator that can be applied to Strings and Doubles
        if (expression.operator.type == TokenType.ADD) {
            // Addition of Doubles
            if (left instanceof Double && right instanceof Double) return (double) left + (double) right;

            // Concatenation of Strings
            if (left instanceof String && right instanceof String) return left + (String) right;

            // Concatenation of String and Double, the only remaining possibility
            // Left is Double
            if (left instanceof Double) return String.format("%.2f%s", (Double)left, right);
            else                        return String.format("%s%.2f", left, (Double)right);
        }

        // Handle equivalencies
        switch(expression.operator.type) {
            case EQUIVALENT:
                return isEquivalent(left, right);
            case NOT_EQUAL:
                return !isEquivalent(left, right);
        }

        // If we ge this far, then validate operands
        validateOperands(expression.operator, left, right);

        return switch (expression.operator.type) {
            case SUBTRACT       ->  (double) left -  (double) right;
            case MULTIPLY       ->  (double) left *  (double) right;
            case DIVIDE         ->  (double) left /  (double) right;
            case GREATER_THAN   ->  (double) left >  (double) right;
            case GREATER_EQUAL  ->  (double) left >= (double) right;
            case LESS_THAN      ->  (double) left <  (double) right;
            case LESS_EQUAL     ->  (double) left <= (double) right;
            default -> null;
        };

    }

    // Helper Methods

    // Test equivalency
    private boolean isEquivalent(Object left, Object right) {
        // Both values are null is equivalent
        if (left == null && right == null) return true;
        // One value is null is not equivalent
        if (left == null || right == null) return false;
        // Return .equals() equivalency
        return left.equals(right);
    }

    // False is literal false or null
    private boolean isTrue(Object object) {
        // Null is falsy
        if (object == null) return false;
        // Return value if it's a boolean
        if (object instanceof Boolean) return (boolean) object;
        // Anything else is truthy
        return true;
    }

    // Validate the type
    private void validateOperand(Token operator, Object operand) {
        // Not is only for booleans
        if (operator.type == TokenType.NOT      && operand instanceof Boolean)  return;
        // Negative (subtract) is only for doubles
        if (operator.type == TokenType.SUBTRACT && operand instanceof Double)   return;
        // No other possibilities, error
        error("Invalid type on line " + operator.line + " : " + operator.text + operand);
    }

    // Validate the types
    private void validateOperands(Token operator, Object operand1, Object operand2) {
        // Function is only used for validating math between two numbers
        if (operand1 instanceof Double && operand2 instanceof Double) return;
        error("Invalid type on line " + operator.line + " : " + operand1 + operator.text + operand2);
    }

    private void error(String message) {
        System.err.println(message);
        System.exit(2);
    }
}
