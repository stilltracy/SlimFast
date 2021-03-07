
import java.io.File;

public class Main {

    public static void main(String args[]) throws Exception {
	String a[] = {
	    "luindex/william","luindex/kjv"
	};
	
	Index x = new Index(new File("scratch"));
	new File("scratch/index").delete();
	x.main(new File("scratch/index"), a);
    }
}