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
        // Declared once, up here: the bracket rules and the operator rules both need the
        // character before the caret, and declaring it inside one branch left the other
        // referring to a symbol the compiler had never seen.
        char last = current.isEmpty() ? 0 : current.charAt(current.length() - 1);
        if (current.length() >= MAX_LENGTH && Character.isDigit(c)) return null;
        if (Character.isDigit(c)) return current + c;
        if (c == '.') {
            String number = trailingNumber(current);
            return number.indexOf('.') >= 0 ? null : current + c;
        }
        if (c == '(') {
            // An open group starts an operand: at the beginning, after an operator, or nested.
            // After a digit or a close paren it would mean implicit multiplication, which this
            // grammar does not have, so it is refused rather than guessed.
            if (current.isEmpty() || isOperator(last) || last == '(') return current + c;
            return null;
        }
        if (c == ')') {
            // A close group needs something closable inside it and an open group to close.
            if (!Character.isDigit(last) && last != ')') return null;
            int open = 0, close = 0;
            for (int i = 0; i < current.length(); i++) {
                if (current.charAt(i) == '(') open++;
                else if (current.charAt(i) == ')') close++;
            }
            return close < open ? current + c : null;
        }
        if (c == '%') {
            // Google's percent is postfix: it belongs to the number just typed. After an
            // operator or at the start there is no number for it to belong to, so it is
            // refused rather than guessed -- the same discipline as every other key here.
            if (Character.isDigit(last) || last == ')') return current + c;
            return null;
        }
        if (c == '+' || c == '-' || c == '*' || c == '/') {
            if (current.isEmpty()) return c == '-' ? "-" : null;
            if (isOperator(last)) {
                // Replace rather than stack, so 5 + * stays 5 *. A minus after an operator is the
                // negation of the number being typed, so 5 * - appends once -- but a minus after a
                // minus is not a deeper negation, it is a changed mind, so it replaces and the
                // chain can never grow. On device, "0/0------85522*3*-" was typed one tap at a
                // time under the rule that let every minus append.
                if (c == '-') {
                    if (last != '-') return current + c;
                    return current.substring(0, current.length() - 1) + c;
                }
                // Swapping operators over a pending negation drops the negation with them:
                // 5 * - then * is 5 *, not 5 * *. Replacing only the last character there would
                // stack two operators, which is the one thing this method exists to prevent.
                if (last == '-' && current.length() >= 2
                        && isOperator(current.charAt(current.length() - 2))) {
                    return current.substring(0, current.length() - 2) + c;
                }
                // A lone leading minus is not an operand to swap operators over.
                if ("-".equals(current)) return null;
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
                || c == '/' || c == '(' || c == ')' || c == '%';
    }

    /**
     * Recursive descent: expression = term (('+'|'-') term)*, term = factor
     * (('*'|'/') factor)*, factor = ('-')* (number | '(' expression ')') ['%'].
     *
     * <p>The percent is Google's, not the primary-school one: under '+' and '-' it means
     * "that percent OF THE RUNNING SUM" -- 200+10% is 220, because 10% of 200 is 20 -- while
     * under '*' and '/', or on its own, it is plain hundredths: 200*10% is 20. The running
     * sum therefore travels down the parse as percentBase, NaN where there is none.
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
            double value = parseTerm(Double.NaN);
            while (true) {
                skipWhitespace();
                if (atEnd()) return value;
                char op = src.charAt(pos);
                if (op != '+' && op != '-') return value;
                pos++;
                double rhs = parseTerm(value);
                value = op == '+' ? value + rhs : value - rhs;
            }
        }

        double parseTerm(double percentBase) {
            double value = parseFactor(percentBase);
            while (true) {
                skipWhitespace();
                if (atEnd()) return value;
                char op = src.charAt(pos);
                if (op != '*' && op != '/') return value;
                pos++;
                double rhs = parseFactor(percentBase);
                if (op == '/') {
                    if (rhs == 0d) throw new ArithmeticException("division by zero");
                    value = value / rhs;
                } else {
                    value = value * rhs;
                }
            }
        }

        double parseFactor(double percentBase) {
            skipWhitespace();
            if (atEnd()) throw new ArithmeticException("missing operand");
            if (src.charAt(pos) == '-') { pos++; return -parseFactor(percentBase); }
            if (src.charAt(pos) == '+') { pos++; return parseFactor(percentBase); }
            if (src.charAt(pos) == '(') {
                pos++;
                double inner = parseExpression();
                skipWhitespace();
                if (atEnd() || src.charAt(pos) != ')') throw new ArithmeticException("unbalanced parentheses");
                pos++;
                return applyPercent(inner, percentBase);
            }
            return applyPercent(parseNumber(), percentBase);
        }

        /** Consumes a postfix '%' if one follows: of percentBase under + and -, else hundredths. */
        private double applyPercent(double value, double percentBase) {
            skipWhitespace();
            if (atEnd() || src.charAt(pos) != '%') return value;
            pos++;
            return Double.isNaN(percentBase) ? value / 100d : percentBase * value / 100d;
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
