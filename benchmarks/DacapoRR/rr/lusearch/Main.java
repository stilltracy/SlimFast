
public class Main {

    public static void main(String args[]) throws Exception {
	int thread_count=16;
	if(args.length>=1)
		thread_count=Integer.parseInt(args[0]);
	String a[] = {
	    "-index","scratch/lusearch/index-default",
	    "-queries","scratch/lusearch/query", 
	    "-output", "scratch/lusearch.out",
	    "-totalqueries", "64",
	    "-threads", "" + thread_count
	};
	
	org.dacapo.lusearch.Search x = new org.dacapo.lusearch.Search();
	x.main(a);
    }
}
