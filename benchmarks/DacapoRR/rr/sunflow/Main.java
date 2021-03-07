public class Main {

    public static void main(String args[]) throws Exception {
	String a[] = {
	    "256"
	};

	int numThreads = Runtime.getRuntime().availableProcessors();

	if(args.length>=1)
		numThreads=Integer.parseInt(args[0]);
	org.sunflow.Benchmark x = new org.sunflow.Benchmark(Integer.parseInt(a[0]), false, false, false, numThreads);
	x.kernelBegin();
	x.kernelMain();
	x.kernelEnd();
  }
}
