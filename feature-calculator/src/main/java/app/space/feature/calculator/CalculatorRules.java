package app.space.feature.calculator;

/**
 * Expression evaluation and result formatting, with no Android in it at all.
 *
 * <p>Kept separate from {@link CalculatorScreen} so the arithmetic runs as a JVM unit
 * test. There is no emulator in this project, so anything that needs one is effectively
 * untested; a recursive-descent parser needs nothing but a String.
 *
 * <p>Operator precedence and parentheses are handled by the grammar rather than by
 * left-to-right folding, which is the usual way a hand-written calculator gets
 * {@code 2+3*4} wrong.
 */
public final class CalculatorRules {

    private CalculatorRules() {
    }

    /** Longest expression the keypad will hold; guards against unbounded input. */
    public static final int MAX_LENGTH = 64;

    /**
     * Evaluates an arithmetic expression.
     *
     * @throws ArithmeticException if the input is malformed, empty, or divides by zero.
     *         Callers show "Error" rather than crashing: a typo is an ordinary outcome.
     */
    public static double evaluate(String expression) {
        Parser p = new Parser(expression);
        double value = p.parseExpression();
        p.skipWhitespace();
        if (!p.atEnd()) throw new ArithmeticException("unexpected trailing input");
        return value;
    }

    /** Formats a result the way the display shows it: no trailing zeros, no exponent. */
    public static String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "Error";
        if (value == Math.rint(value) && Math.abs(value) < 1e15) return String.valueOf((long) value);
        String s = String.format(java.util.Locale.ROOT, "%.10f", value);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    /** Whether a character may be appended to the expression being typed. */
    public static boolean isInputCharacter(char c) {
        return (c >= '0' && c <= '9') || c == '.' || c == '+' || c == '-' || c == '*'
                || c == '/' || c == '(' || c == ')';
    }

    /**
     * Recursive descent: expression = term (('+'|'-') term)*, term = factor
     * (('*'|'/') factor)*, factor = ('-')* (number | '(' expression ')').
     */
    static final class Parser {
        private final String src;
        private int pos;

        Parser(String src) {
            if (src == null) throw new ArithmeticException("empty expression");
            this.src = src.trim();
            if (this.src.isEmpty()) throw new ArithmeticException("empty expression");
        }

        boolean atEnd() { return pos >= src.length(); }

        void skipWhitespace() { while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++; }

        double parseExpression() {
            double value = parseTerm();
            while (true) {
                skipWhitespace();
                if (atEnd()) return value;
                char op = src.charAt(pos);
                if (op != '+' && op != '-') return value;
                pos++;
                double rhs = parseTerm();
                value = op == '+' ? value + rhs : value - rhs;
            }
        }

        double parseTerm() {
            double value = parseFactor();
            while (true) {
                skipWhitespace();
                if (atEnd()) return value;
                char op = src.charAt(pos);
                if (op != '*' && op != '/') return value;
                pos++;
                double rhs = parseFactor();
                if (op == '/') {
                    if (rhs == 0d) throw new ArithmeticException("division by zero");
                    value = value / rhs;
                } else {
                    value = value * rhs;
                }
            }
        }

        double parseFactor() {
            skipWhitespace();
            if (atEnd()) throw new ArithmeticException("missing operand");
            if (src.charAt(pos) == '-') { pos++; return -parseFactor(); }
            if (src.charAt(pos) == '+') { pos++; return parseFactor(); }
            if (src.charAt(pos) == '(') {
                pos++;
                double inner = parseExpression();
                skipWhitespace();
                if (atEnd() || src.charAt(pos) != ')') throw new ArithmeticException("unbalanced parentheses");
                pos++;
                return inner;
            }
            return parseNumber();
        }

        private double parseNumber() {
            int start = pos;
            boolean dot = false;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c >= '0' && c <= '9') { pos++; continue; }
                if (c == '.' && !dot) { dot = true; pos++; continue; }
                break;
            }
            if (pos == start) throw new ArithmeticException("expected a number");
            try {
                return Double.parseDouble(src.substring(start, pos));
            } catch (NumberFormatException e) {
                throw new ArithmeticException("malformed number");
            }
        }
    }
}
