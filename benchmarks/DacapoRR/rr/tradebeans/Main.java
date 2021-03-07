
import java.io.File;

public class Main {

    public static void main(String args[]) throws Exception {
	org.dacapo.daytrader.Launcher.initialize(new File("./scratch"), 8, "medium", true);
	org.dacapo.daytrader.Launcher.performIteration();
    }
}
