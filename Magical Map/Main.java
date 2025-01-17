
import java.io.*;

public class Main {
    public static void main(String[] args) {
        try (
                BufferedWriter writer = new BufferedWriter(new FileWriter(args[3])); // File output.
        ) {
            // Pass BufferedWriter to OzNavigator.
            OzNavigator nav = new OzNavigator(writer);

            //Read input files with BufferedReader.
            nav.readNodeFile(args[0]);
            nav.readEdgesFile(args[1]);
            nav.readObjFile(args[2]);

            // Write output.
            nav.writeOutput();

            // Run the main logic.
            nav.run();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}