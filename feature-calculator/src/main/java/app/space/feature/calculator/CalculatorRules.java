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
        return evaluate(expression, true);
    }

    /**
     * Google's scientific panel is degrees by default with a DEG/RAD toggle, so the mode
     * travels with the evaluation rather than living in a global.
     */
    public static double evaluate(String expression, boolean degrees) {
        Parser p = new Parser(expression, degrees);
        double value = p.parseExpression();
        p.skipWhitespace();
        if (!p.atEnd()) throw new ArithmeticException("unexpected trailing input");
        return value;
    }

    /** Formats a result the way the display shows it: no trailing zeros, no exponent. */
    public static String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "Error";
        if (value == Math.rint(value) && Math.abs(value) < 1e15) return String.valueOf((long) value);
        // The reference spec's number handling, adopted: a magnitude too big or too small to
        // read as plain digits goes out in scientific notation instead of a 21-digit string
        // or a run of zeroes. Rounding here is display only; the next calculation keeps the
        // full double.
        if (Math.abs(value) >= 1e15 || Math.abs(value) < 1e-9) {
            return new java.math.BigDecimal(value).stripTrailingZeros().toString().replace('E', 'e');
        }
        String s = String.format(java.util.Locale.ROOT, "%.10f", value);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    /**
     * The nearest fraction with a denominator up to 10,000, via continued-fraction
     * convergents, or null when the value is not a number. Integers return as integers;
     * everything else returns reduced, sign first: 0.5 is "1/2", 2.5 is "5/2", because a
     * calculator that shows 2.5 as "2 1/2" has picked a typography over an answer.
     */
    public static String toFraction(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return null;
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        double sign = value < 0 ? -1 : 1;
        double x = Math.abs(value);
        long bestNum = 0, bestDen = 1;
        double bestErr = Double.MAX_VALUE;
        long h0 = 0, k0 = 1, h1 = 1, k1 = 0;
        double y = x;
        for (int i = 0; i < 24; i++) {
            long a = (long) Math.floor(y);
            long h2 = a * h1 + h0;
            long k2 = a * k1 + k0;
            if (k2 > 10000) break;
            h0 = h1; k0 = k1; h1 = h2; k1 = k2;
            double err = Math.abs(x - (double) h1 / (double) k1);
            if (err < bestErr) { bestErr = err; bestNum = h1; bestDen = k1; }
            if (err < 1e-9) break;
            double frac = y - a;
            if (frac < 1e-9) break;
            y = 1 / frac;
        }
        if (bestDen == 1) return String.valueOf((long) (sign * bestNum));
        return (sign < 0 ? "-" : "") + bestNum + "/" + bestDen;
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
            // Except over a pending operator, where the owner's 2026-10-26 list puts it:
            // 10+ then % becomes 10%, the percent joining the number already typed.
            if (isOperator(last) && current.length() >= 2) {
                char before = current.charAt(current.length() - 2);
                if (Character.isDigit(before) || before == ')' || before == '!') {
                    return current.substring(0, current.length() - 1) + c;
                }
            }
            return null;
        }
        if (c == '+' || c == '-' || c == '*' || c == '/') {
            if (current.isEmpty()) return c == '-' ? "-" : null;
            if (isOperator(last)) {
                // The owner's first two points, 2026-10-26: he typed 10+ , changed his mind to
                // minus, and had to backspace first. A trailing operator with no operand after
                // it is a changeable mind: EVERY operator key -- minus included -- replaces it,
                // so 10+ then - is 10- in one tap. The old carve-out that let a minus append as
                // a negation (5+-) is exactly what read to him as "not taking"; negation now
                // lives behind the bracket key, 10+(-3), where it is visible and editable.
                // A lone leading minus is not an operand to swap operators over.
                // A lone leading minus is not an operand to swap operators over; minus over
                // itself is simply the same pending operator, so it stands.
                if ("-".equals(current)) return c == '-' ? "-" : null;
                // A minus sitting after another operator is the legacy negation slot, from
                // stores and editors that predate this rule. Swapping operators drops it
                // with them -- 5*- then + is 5+, not 5*-+ -- because stacked operators are
                // the one thing this method exists to prevent, reachable input or not.
                if (last == '-' && current.length() >= 2
                        && isOperator(current.charAt(current.length() - 2))) {
                    return current.substring(0, current.length() - 2) + c;
                }
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
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }

    public static boolean isInputCharacter(char c) {
        return (c >= '0' && c <= '9') || c == '.' || c == '+' || c == '-' || c == '*'
                || c == '/' || c == '(' || c == ')' || c == '%' || c == '^' || c == '!'
                || c == '\u03c0' || c == 'e' || Character.isLetter(c);
    }

    /**
     * Multi-character scientific tokens, validated the same way single keys are: a function
     * opens an operand slot, a constant is an operand, postfix '!' and infix '^' need an
     * operand already on screen. Anything else is refused rather than guessed.
     */
    public static String appendToken(String expression, String token) {
        String current = expression == null ? "" : expression;
        if (token == null || token.isEmpty()) return null;
        char last = current.isEmpty() ? 0 : current.charAt(current.length() - 1);
        boolean valueBefore = Character.isDigit(last) || last == ')'
                || last == '\u03c0' || last == 'e';
        boolean operandBefore = valueBefore || last == '!';
        boolean slotOpen = current.isEmpty() || isOperator(last) || last == '(';
        if (token.endsWith("(")) return slotOpen ? current + token : null;
        if ("\u03c0".equals(token) || "e".equals(token)) return slotOpen ? current + token : null;
        // One factorial per value: 5!! is double-factorial mathematics Google's pad does not
        // offer, so the second '!' is refused. A power over a factorialed value is ordinary
        // mathematics, so '^' keeps the wider notion of "an operand is on screen".
        if ("!".equals(token)) return valueBefore ? current + token : null;
        if ("^".equals(token)) return operandBefore ? current + token : null;
        return append(current, token);
    }

    /**
     * The history store, as pure text so it can be tested without a device: one line per
     * calculation, "expression=result", newest first, deduplicated, capped. The characters
     * an expression and a formatted result can contain never include '=' or newline, so the
     * encoding cannot lie about where a line ends.
     */
    public static String pushHistory(String store, String expression, String result, int cap) {
        // The owner's 2026-10-25 tenth point: every history line carries when it happened,
        // "25th Oct, 17:15", so a calculation from last Tuesday says so. The timestamp rides
        // the line after an '@' -- neither an expression nor a formatted result can contain
        // one, so the encoding still cannot lie about where a field ends. Lines written by
        // older builds carry no '@' and read back with an empty timestamp rather than
        // vanishing: history is the user's, not a migration casualty.
        String line = expression + "=" + result + "@" + System.currentTimeMillis();
        java.util.List<String> out = new java.util.ArrayList<>();
        out.add(line);
        if (store != null) {
            for (String s : store.split("\n")) {
                if (s.isEmpty()) continue;
                int eq = s.indexOf('=');
                if (eq <= 0) continue;
                String rest = s.substring(eq + 1);
                int at = rest.lastIndexOf('@');
                String older = at > 0 ? rest.substring(0, at) : rest;
                // A repeat moves to the top instead of duplicating, timestamp refreshed.
                if (s.substring(0, eq).equals(expression) && older.equals(result)) continue;
                out.add(s);
            }
        }
        while (out.size() > cap) out.remove(out.size() - 1);
        return String.join("\n", out);
    }

    public static String[][] historyEntries(String store) {
        java.util.List<String[]> out = new java.util.ArrayList<>();
        if (store == null || store.isEmpty()) return out.toArray(new String[0][]);
        for (String line : store.split("\n")) {
            int eq = line.indexOf('=');
            if (eq <= 0 || eq == line.length() - 1) continue;
            String rest = line.substring(eq + 1);
            int at = rest.lastIndexOf('@');
            String ts = "";
            String result = rest;
            if (at > 0) {
                String maybe = rest.substring(at + 1);
                // A timestamp that is not digits is not a timestamp; it is old or corrupt
                // text, and the result keeps the whole field rather than losing a suffix.
                boolean digits = !maybe.isEmpty();
                for (int i = 0; i < maybe.length(); i++) digits &= Character.isDigit(maybe.charAt(i));
                if (digits) { ts = maybe; result = rest.substring(0, at); }
            }
            out.add(new String[]{line.substring(0, eq), result, ts});
        }
        return out.toArray(new String[0][]);
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

        private final boolean degrees;

        Parser(String src, boolean degrees) {
            if (src == null) throw new ArithmeticException("empty expression");
            this.src = src.trim();
            if (this.src.isEmpty()) throw new ArithmeticException("empty expression");
            this.degrees = degrees;
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
            double value = parsePower(percentBase);
            while (true) {
                skipWhitespace();
                if (atEnd()) return value;
                char op = src.charAt(pos);
                if (op != '*' && op != '/') return value;
                pos++;
                double rhs = parsePower(percentBase);
                if (op == '/') {
                    if (rhs == 0d) throw new ArithmeticException("division by zero");
                    value = value / rhs;
                } else {
                    value = value * rhs;
                }
            }
        }

        /** power binds tighter than * and / and is right-associative: 2^3^2 is 512. */
        double parsePower(double percentBase) {
            double value = parsePostfix(parseFactor(percentBase), percentBase);
            skipWhitespace();
            if (!atEnd() && src.charAt(pos) == '^') {
                pos++;
                value = Math.pow(value, parsePower(Double.NaN));
            }
            return value;
        }

        double parseFactor(double percentBase) {
            skipWhitespace();
            if (atEnd()) throw new ArithmeticException("missing operand");
            if (src.charAt(pos) == '-') { pos++; return -parsePower(percentBase); }
            if (src.charAt(pos) == '+') { pos++; return parsePower(percentBase); }
            if (src.charAt(pos) == '(') {
                pos++;
                double inner = parseExpression();
                skipWhitespace();
                if (atEnd() || src.charAt(pos) != ')') throw new ArithmeticException("unbalanced parentheses");
                pos++;
                return inner;
            }
            if (src.charAt(pos) == '\u03c0') { pos++; return Math.PI; }
            if (Character.isLetter(src.charAt(pos)) || src.charAt(pos) == '\u221a') {
                return parseFunctionOrConstant();
            }
            return parseNumber();
        }

        /** The scientific panel's vocabulary: constants and prefix functions over a group. */
        private double parseFunctionOrConstant() {
            int start = pos;
            while (pos < src.length()
                    && (Character.isLetter(src.charAt(pos)) || src.charAt(pos) == '\u221a')) pos++;
            String name = src.substring(start, pos);
            if ("e".equals(name)) return Math.E;
            skipWhitespace();
            if (atEnd() || src.charAt(pos) != '(') throw new ArithmeticException("function needs ( )");
            pos++;
            double arg = parseExpression();
            skipWhitespace();
            if (atEnd() || src.charAt(pos) != ')') throw new ArithmeticException("unbalanced parentheses");
            pos++;
            if ("sin".equals(name)) return degrees ? Math.sin(Math.toRadians(arg)) : Math.sin(arg);
            if ("cos".equals(name)) return degrees ? Math.cos(Math.toRadians(arg)) : Math.cos(arg);
            if ("tan".equals(name)) return degrees ? Math.tan(Math.toRadians(arg)) : Math.tan(arg);
            if ("asin".equals(name)) {
                if (arg < -1 || arg > 1) throw new ArithmeticException("asin domain");
                return degrees ? Math.toDegrees(Math.asin(arg)) : Math.asin(arg);
            }
            if ("acos".equals(name)) {
                if (arg < -1 || arg > 1) throw new ArithmeticException("acos domain");
                return degrees ? Math.toDegrees(Math.acos(arg)) : Math.acos(arg);
            }
            if ("atan".equals(name)) return degrees ? Math.toDegrees(Math.atan(arg)) : Math.atan(arg);
            if ("ln".equals(name)) {
                if (arg <= 0) throw new ArithmeticException("ln domain");
                return Math.log(arg);
            }
            if ("log".equals(name)) {
                if (arg <= 0) throw new ArithmeticException("log domain");
                return Math.log10(arg);
            }
            if ("sqrt".equals(name) || "\u221a".equals(name)) {
                if (arg < 0) throw new ArithmeticException("sqrt domain");
                return Math.sqrt(arg);
            }
            throw new ArithmeticException("unknown function " + name);
        }

        /**
         * The postfix family: Google's percent plus factorial. Percent under + and - means
         * that percent of the running sum; factorial is integers only, 0 to 170, because
         * 171! overflows a double into infinity and a calculator that shows infinity for a
         * factorial has stopped calculating.
         */
        private double parsePostfix(double value, double percentBase) {
            while (true) {
                skipWhitespace();
                if (!atEnd() && src.charAt(pos) == '%') {
                    pos++;
                    value = Double.isNaN(percentBase) ? value / 100d : percentBase * value / 100d;
                    continue;
                }
                if (!atEnd() && src.charAt(pos) == '!') {
                    pos++;
                    value = factorial(value);
                    continue;
                }
                return value;
            }
        }

        private double factorial(double v) {
            if (v < 0 || v > 170 || v != Math.rint(v)) throw new ArithmeticException("factorial domain");
            double r = 1;
            for (int i = 2; i <= (int) v; i++) r *= i;
            return r;
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
