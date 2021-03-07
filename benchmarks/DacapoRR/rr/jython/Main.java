
public class Main {

    public static void main(String args[]) {
	String a[] = {
	      "scratch/jython/pybench/pybench.py", "--with-gc", "--debug", "-n", "1", "-C", "0", "-w", "20"
	      //	     "scratch/jython/sieve.py","50"
	};
	System.setProperty("python.home", "scratch/jython");
	System.setProperty("python.cachedir", "scratch/cachedir");
	System.setProperty("python.verbose", "warning");

	org.python.util.jython.main(new String[] { "scratch/jython/noop.py"});
	org.python.core.PySystemState.setArgv(a);
	org.python.util.jython.main(a);
    }
}