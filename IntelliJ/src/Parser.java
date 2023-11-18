import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Parse a DOPL source file and report either that the file is ok
 * or it contains an error.
 * <br><br>
 * Requirements:
 * <ul>
 * <li> Reporting happens with a single word print statement.
 * <li> No error detection required.
 * <li> Parsing terminates on the first error.
 * </ul>
 *
 * @author nk465
 * @version 2023.18.01
 */
public class Parser {
    private final ArrayList<String> tokens;
    private final HashSet<String> keywords = new HashSet<>(Arrays.asList("character", "do", "else", "endif", "endloop", "finish", "if", "integer", "logical", "loopif", "print", "start", "then"));
    private final HashSet<String> dataTypes = new HashSet<>(Arrays.asList("character", "integer", "logical"));
    private final HashSet<String> arithmeticOp = new HashSet<>(Arrays.asList(".plus.", ".minus.", ".mul.", ".div."));
    private final HashSet<String> logicalOp = new HashSet<>(Arrays.asList(".and.", ".or."));
    private final HashSet<String> relationalOp = new HashSet<>(Arrays.asList(".eq.", ".ne.", ".lt.", ".gt.", ".le.", ".ge."));
    private final HashSet<String> unaryOp = new HashSet<>(Arrays.asList(".minus.", ".not."));
    private final HashSet<String> expressionEnder = new HashSet<>(Arrays.asList(";", "then", "do", "else", ")"));
    private final HashSet<String> statementEnder = new HashSet<>(Arrays.asList("finish", "else", "endif", "endloop"));
    private final HashMap<String, String> identifiers = new HashMap<>();
    private boolean inProgram, inStatements, error;
    private int skipSteps = 0, tokenIndex = -1;

    /**
     * Create a parser.
     *
     * @param filename The file to be translated.
     * @throws IOException on any input issue.
     */
    public Parser(String filename) throws IOException {
        Tokenizer tokenizer = new Tokenizer(filename);
        tokens = tokenizer.allTokens;
        error = tokenizer.compileTimeError;
    }

    public void parse() throws IOException {
        for (String token : tokens) {
            if (error) {
                System.out.println("error");
                return;
            } else if (skipSteps > 0) skipSteps--;
            else {
                try {
                    tokenIndex++;
                    if (!inProgram) { // if the program hasn't started -> it should start now
                        if (token.equals("start")) inProgram = true;
                        else error();
                    } else if (token.equals("finish")) inProgram = false; // if it finished
                    else if (!inStatements) parseDeclarations(token); // if (inDeclarations)
                    else if (!parseStatements()) error(); // if (inStatements)
                } catch (Exception e) {
                    error();
                }
            }
        }

        if (!(inProgram || error)) System.out.println("ok");
        else System.out.println("error");
    }

    /**
     * parseDeclarations() handles with the declarations' section
     * that should be at the beginning of a DOPL file.
     */
    private void parseDeclarations(String token) {
        while (parseDeclaration(token)) token = getNextToken();

        incrementTokenIndex(-1);
        inStatements = true;
    }

    /**
     * parseDeclaration() finds the dataType that the identifiers
     * getting declared are going to be and then evaluates them
     */
    private boolean parseDeclaration(String token) {
        boolean inDeclaration = true;
        String dataType = token;

        if (!dataTypes.contains(token)) return false;

        int commas = 0;
        int newIdentifiers = 0;
        token = getNextToken();

        while (inDeclaration) {
            if (keywords.contains(token)) return false;
            else if (token.equals(";")) {
                if (newIdentifiers == 0) return false;
                else if (commas == newIdentifiers) return false;
                else inDeclaration = false;
            } else if (token.equals(",")) {
                if (++commas > newIdentifiers) return false;
                token = getNextToken();
            } else {
                if (isIdentifier(token)) return false;
                else if (!Character.isAlphabetic(token.charAt(0))) return false;
                else if (checkInvalidCharacters(token)) return false;
                else identifiers.put(token, dataType);

                if (++newIdentifiers - commas > 1) return false;
                token = getNextToken();
            }
        }

        return true;
    }

    private boolean parseStatements() {
        while (!(statementEnder.contains(getCurrentToken()))) {
            if (parseStatement()) getNextToken();
            else return false;
        }
        incrementTokenIndex(-1);
        return true;
    }

    /**
     * parseStatement() uses the getStatementType() method to find what
     * statement it is dealing with and then pass it to the
     * statement-type-specific methods for further parsing and evaluation.
     */
    private boolean parseStatement() {
        String token = getCurrentToken();
        getNextToken();
        if (isIdentifier(token)) return parseAssignment(identifiers.get(token)); // inAssignment
        else if (token.equals("if")) return parseConditional(); // inConditional
        else if (token.equals("print")) return parsePrint(); // inPrint
        else if (token.equals("loopif")) return parseLoop(); // inLoop
        else return false;
    }

    /**
     * parseTerm()'s function is to find if a token is a term and its type.
     *
     * @return String[termType, isTerm]
     * @main_if-statement <ul>
     * <li> if a number           -> true
     * <li> else if "             -> check if a char follows
     * <li> else if an identifier -> true
     * <li> else if a urinary op  -> check if a term follows
     * <li> else if (             -> check if an expression follows
     * </ul>
     */
    private String[] parseTerm(String token) {
        if (isNumeric(token)) {
            return new String[]{"integer", "true"}; // integer constant
        } else if (token.toCharArray()[0] == 34 && getNextToken().length() == 1 && getNextToken().toCharArray()[0] == 34) {
            return new String[]{"character", "true"}; // character constant
        } else if (isIdentifier(token)) {
            return new String[]{identifiers.get(token), "true"}; // identifier
        } else if (unaryOp.contains(token)) {
            if (token.equals(".minus.")) return parseTerm(getNextToken()); // unaryOp term
            else return new String[]{"logical", parseTerm(getNextToken())[1]}; // unaryOp term
        } else if (token.equals("(")) {
            getNextToken();
            return parseExpression();  // '(' expression ')'
        } else return new String[]{"", "false"};
    }

    /**
     * parseExpression()'s function is to find if a group of tokens
     * form an expression.
     * <br><br>
     * - if the group of tokens begins with a term that is
     * followed by 0 or more binaryOp + term combinations
     */
    private String[] parseExpression() {
        String[] termInfo = parseTerm(getCurrentToken());

        if (termInfo[1].equals("true")) {
            String expressionType = termInfo[0];
            while (!expressionEnder.contains(getNextToken())) {
                if (isBinaryOp(getCurrentToken())) {
                    if (!arithmeticOp.contains(getCurrentToken())) expressionType = "logical";
                    String[] nextTermInfo = parseTerm(getNextToken());
                    if (nextTermInfo[1].equals("true")) {
                        if (!expressionType.equals("logical") && nextTermInfo[0].equals("character")) {
                            expressionType = "character";
                        }
                    } else return new String[]{"", "false"};
                } else return new String[]{"", "false"};
            }

            return new String[]{expressionType, "true"};
        } else return new String[]{"", "false"};
    }

    /**
     * first token is already parsed by getStatementType()
     * and is classified as an identifier
     * <ul>
     * <li> if "<-" follows
     * <li> && then an expression
     * <li> && then ";"
     * </ul>
     * -> return true
     */
    private boolean parseAssignment(String identifierType) {
        if (getCurrentToken().equals("<-")) {
            getNextToken();
            String[] info = parseExpression();
            if (info[0].equals(identifierType) && info[1].equals("true")) return getCurrentToken().equals(";");
        }
        return false;
    }

    /**
     * first token is already parsed by getStatementType()
     * and is identified to be the 'if' keyword
     * <ul>
     * <li> if and expression follows
     * <li> && then the 'then' keyword
     * <li> && then a statement
     * <li> optionally followed by 'else'
     * <li> and another statement
     * <li> && then the 'endif' keyword
     * </ul>
     * -> return true
     */
    private boolean parseConditional() {
        String[] expressionInfo = parseExpression();
        if (expressionInfo[0].equals("logical") && expressionInfo[1].equals("true")) {
            if (getCurrentToken().equals("then")) {
                getNextToken();
                if (parseStatements()) {
                    if (getNextToken().equals("else")) {
                        getNextToken();
                        if (!parseStatements()) return false;
                    } else incrementTokenIndex(-1);
                } else return false;

                return getNextToken().equals("endif") && getNextToken().equals(";");
            }
        }
        return false;
    }

    /**
     * first token is already parsed by getStatementType()
     * and is identified to be the 'print' keyword
     * <ul>
     * <li> if and expression follows
     * <li> && then ';'
     * </ul>
     * -> return true
     */
    private boolean parsePrint() {
        if (parseExpression()[1].equals("true")) return getCurrentToken().equals(";");
        else return false;
    }

    /**
     * first token is already parsed by getStatementType()
     * and is identified to be the 'loopif' keyword
     * <ul>
     * <li> if and expression follows
     * <li> && then the 'do' keyword
     * <li> && then a statement
     * <li> && then the 'endloop' keyword</li>
     * </ul>
     * -> return true
     */
    private boolean parseLoop() {
        String[] expressionInfo = parseExpression();
        if (expressionInfo[0].equals("logical") && expressionInfo[1].equals("true")) {
            if (getCurrentToken().equals("do")) {
                getNextToken();
                if (parseStatements()) {
                    return getNextToken().equals("endloop") && getNextToken().equals(";");
                }
            }
        }
        return false;
    }

    private boolean isBinaryOp(String token) {
        return arithmeticOp.contains(token) || logicalOp.contains(token) || relationalOp.contains(token);
    }

    private boolean isIdentifier(String token) {
        return identifiers.containsKey(token);
    }

    private static boolean isNumeric(String token) {
        try {
            Integer.parseInt(token);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private boolean checkInvalidCharacters(String token) {
        char[] tokenChars = token.toCharArray();
        for (Character c : tokenChars) {
            if (!(Character.isAlphabetic(c) || isNumeric(c.toString()) || c == 95)) return true;
        }
        return false;
    }

    private String getCurrentToken() {
        return tokens.get(tokenIndex);
    }

    private String getNextToken() {
        return tokens.get(incrementTokenIndex(1));
    }

    private int incrementTokenIndex(int incrementWith) {
        skipSteps += incrementWith;
        return (tokenIndex += incrementWith);
    }

    private void error() {
        error = true;
    }
}