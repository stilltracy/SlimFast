
public class Main {

    public static void main(String args[]) {
	String a[] = {
	    "-d", 
	    "scratch",
	    "-scriptSecurityOff",
	    "scratch/batik/mapWaadt.svg",
	    "scratch/batik/mapSpain.svg",
	    "scratch/batik/sydney.svg"
	};
	org.apache.batik.apps.rasterizer.Main m = new org.apache.batik.apps.rasterizer.Main(a);
	m.execute();
    }
}
