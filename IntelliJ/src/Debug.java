import java.io.IOException;

public class Debug {
    private static final String[] fileNames = new String[]{"assign1", "brief-example", "char", "expr1", "expr3", "expr6", "if1", "if3", "longer_test", "loop2", "minimal", "myTest1", "one_line", "print1", "singleline", "var1", "var3", "unaryOp", "unaryOpComplicated"};
    private static final String[] folders = new String[]{"true-dopl-samples/", "false-dopl-samples/"};

    public static void main(String[] args) {
        debugMany();
        debugOne();
    }

    private static void debugMany() {
        for (String folder : folders) {
            for (String filename : fileNames) {
                try {
                    String suffix = ".dopl";
                    Parser parser = new Parser(folder + filename + suffix);
                    parser.parse();
                } catch (IOException ex) {
                    System.err.println("Exception parsing: " + filename);
                    System.err.println(ex);
                }
            }
            System.out.println("\n==============\n");
        }
    }

    private static void debugOne() {
        String filename = "nikola.dopl";
        try {
            Parser parser = new Parser(filename);
            parser.parse();
        } catch (IOException ex) {
            System.err.println("Exception parsing: " + filename);
            System.err.println(ex);
        }
    }
}
