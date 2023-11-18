import java.io.IOException;

/**
 * Driver program for a DOPL parser.
 *
 * @author nk465
 * @version 2022.12.30(
 */
public class Main {

    /**
     * Receive the name of a single DOPL file to be parsed.
     *
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Main file");
        } else {
            String filename = args[0];
            if (filename.endsWith(".dopl")) {
                try {
                    Parser parser = new Parser(filename);
                    parser.parse();
                } catch (IOException ex) {
                    System.err.println("Exception parsing: " + filename);
                    System.err.println(ex);
                }
            } else {
                System.err.println("Unrecognised file type: " + filename);
            }
        }
    }
}