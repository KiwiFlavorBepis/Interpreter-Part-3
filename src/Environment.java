import java.util.HashMap;
import java.util.Map;

public class Environment {
    Environment enclosing = null;

    public Environment() {
    }

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    private Map<String, Object> variables = new HashMap<>();

    // Define - Create a variable
    void define(String name, Object value) {
        variables.put(name, value);
    }

    Object get(String name) {
        // TODO: Return variable if it exists in our current environment, otherwise, check enclosing, otherwise,
        //  return null (it does not exist)

        return null;
    }

    // Assign - Replace the value of an existing variable
    void assign(Token name, Object value) {
        // TODO: If the variable exists, then we can assign, otherwise we have an error

        // TODO: If we don't have it in our current environment, try assigning in the enclosing environment

        // Exit on error if we get this far since the variable is undefined
        System.err.println("Undefined variable: " + name.text);
        System.exit(ErrorCode.INTERPRET_ERROR);
    }
}
