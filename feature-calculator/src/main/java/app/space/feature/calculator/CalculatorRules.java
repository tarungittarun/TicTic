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
    /**
     * The expression after pressing a key, or null when the key is not acceptable here.
     *
     * <p>A calculator that appends whatever is tapped will happily show "-+-++-52***-+++", which
     * is what this one did: the rules lived only in evaluate(), so nonsense was accepted at
     * input time and refused at equals time. Google's calculator refuses at the key, so the
     * display can never hold something that is not a number in progress.
     *
     * <p>Rules: a digit always appends (within MAX_LENGTH); a decimal point only when the number
     * being typed has none; an operator replaces a trailing operator, and only minus may lead;
     * equals and clear are handled by the caller.
     */
    /** Maps a key cap to the character it types; the rules work in ASCII, the caps are glyphs. */
    public static String normaliseKey(String key) {
        if (key == null || key.isEmpty()) return null;
        char c = key.charAt(key.length() - 1);
        if (c == '\u00f7') return "/";
        if (c == '\u00d7') return "*";
        if (c == '\u2212') return "-";
        return String.valueOf(c);
    }

    public static String append(String expression, String key) {
        String current = expression == null ? "" : expression;
        if (key == null || key.isEmpty()) return null;
        String k = normaliseKey(key);
        if (k == null || k.isEmpty()) return null;
        char c = k.charAt(k.length() - 1);
        if (current.length() >= MAX_LENGTH && Character.isDigit(c)) return null;
        if (Character.isDigit(c)) return current + c;
        if (c == '.') {
            String number = trailingNumber(current);
            return number.indexOf('.') >= 0 ? null : current + c;
        }
        if (c == '+' || c == '-' || c == '*' || c == '/') {
            if (current.isEmpty()) return c == '-' ? "-" : null;
            char last = current.charAt(current.length() - 1);
            if (isOperator(last)) {
                // Replace rather than stack, so 5 + * stays 5 *, and 5 + - becomes 5 - only when
                // the new key is minus; anything else after an operator is refused except minus.
                if (c == '-') return current + c;
                return current.substring(0, current.length() - 1) + c;
            }
            return current + c;
        }
        return null;
    }

    /** The number currently being typed, i.e. everything after the last operator. */
    public static String trailingNumber(String expression) {
        if (expression == null || expression.isEmpty()) return "";
        int cut = -1;
        for (int i = expression.length() - 1; i >= 0; i--) {
            char c = expression.charAt(i);
            if (c == '+' || c == '*' || c == '/') { cut = i; break; }
            if (c == '-' && i > 0 && !isOperator(expression.charAt(i - 1))) { cut = i; break; }
        }
        return expression.substring(cut + 1);
    }

    public static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

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
