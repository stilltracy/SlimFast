
public class Main {

    public static void main(String args[]) {
	String a[] = {
	    "-seconds=30",
	    "-platform=mica2",
	    "-simulation=sensor-network",
	    "-nodecount=4,2",
	    "-stagger-start=1000000",
	    "scratch/test/tinyos/CntToRfm.elf",
	    "scratch/test/tinyos/RfmToLeds.elf"
	};
	avrora.Main.main(a);
    }
}