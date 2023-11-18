import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Tokenizer, aka Scanner, aka Lexical analyser
 * for DOPL files.
 */
public class Tokenizer {
    public final ArrayList<String> allTokens = new ArrayList<>();
    private boolean inQuotes, inOperators;
    public boolean compileTimeError = false;
    private StringBuilder currentToken;

    /**
     * @param filename The file to be analysed.
     * @throws IOException on any file issue
     */
    public Tokenizer(String filename) throws IOException {
        readFile(filename);
    }

    private void readFile(String filename) throws FileNotFoundException {
        File myFile = new File(filename);
        Scanner myReader = new Scanner(myFile);

        while (myReader.hasNextLine()) {
            currentToken = new StringBuilder();
            String thisLine = myReader.nextLine();
            int characters = thisLine.length();

            for (int i = 0; i < characters; i++) {
                String thisChar = Character.toString(thisLine.charAt(i));
                if (thisChar.equals(" ") && !inQuotes) addCurrentToken();
                else if (thisChar.equals(".") && !inQuotes) { //.something.
                    inOperators = !inOperators;
                    if (!inOperators) currentToken.append(thisChar);
                    addCurrentToken();
                    if (inOperators) currentToken.append(thisChar);
                } else if (thisChar.toCharArray()[0] == 34 && !inOperators) { // "something"
                    inQuotes = !inQuotes;
                    addCurrentTokenAndChar(thisChar);
                } else {
                    if (inOperators) {
                        if (i == characters - 1 && !myReader.hasNextLine()) { // line ends and quote is still open
                            compileTimeError = true;
                            break;
                        }
                        currentToken.append(thisChar);
                    } else if (inQuotes) {
                        if (i == characters - 1 && !myReader.hasNextLine()) { // line ends and quote is still open
                            compileTimeError = true;
                            break;
                        }
                        currentToken.append(thisChar);
                    } else {
                        if (thisChar.equals(",") || thisChar.equals(";") || thisChar.equals(")") || thisChar.equals("(")) {
                            addCurrentTokenAndChar(thisChar);
                        } else if (thisChar.equals("<")) { // must be followed by "-"
                            addCurrentToken();
                            currentToken.append(thisChar);
                            String nextChar = Character.toString(thisLine.charAt(++i));
                            if (nextChar.equals("-")) {
                                currentToken.append(nextChar);
                                addCurrentToken();
                            } else compileTimeError = true;
                        } else if (i == characters - 1) { // line ends
                            currentToken.append(thisChar);
                            allTokens.add(currentToken.toString());
                        } else currentToken.append(thisChar);
                    }
                }
            }
        }

        myReader.close();
    }

    private void addCurrentToken() {
        if (currentToken.length() > 0) { // if there is such (current token)
            allTokens.add(currentToken.toString());
            currentToken = new StringBuilder();
        }
    }

    private void addCurrentTokenAndChar(String thisChar) {
        addCurrentToken();
        allTokens.add(thisChar);
    }
}