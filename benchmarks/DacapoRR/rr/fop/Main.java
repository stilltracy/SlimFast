
public class Main {

    public static void main(String args[]) {
	String a[] = {
	    "-q", "scratch/fop/test.fo", "-ps", "scratch/test.ps"
	};
	org.apache.fop.cli.Main.startFOP(a);
    }
}