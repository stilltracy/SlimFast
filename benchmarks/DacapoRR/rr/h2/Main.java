
import java.io.File;

public class Main {
  
    public static void main(String args[]) throws Exception {
	org.dacapo.h2.TPCC x = org.dacapo.h2.TPCC.make(null, new File("scratch/"), true, true);
	String size="default"; // was small...
	x.prepare(size);
	x.preIteration(size);
	x.iteration(size);
	x.postIteration(size);
    }
}
