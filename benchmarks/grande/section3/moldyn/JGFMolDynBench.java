/**************************************************************************
*                                                                         *
*         Java Grande Forum Benchmark Suite - Thread Version 1.0          *
*                                                                         *
*                            produced by                                  *
*                                                                         *
*                  Java Grande Benchmarking Project                       *
*                                                                         *
*                                at                                       *
*                                                                         *
*                Edinburgh Parallel Computing Centre                      *
*                                                                         * 
*                email: epcc-javagrande@epcc.ed.ac.uk                     *
*                                                                         *
*                                                                         *
*      This version copyright (c) The University of Edinburgh, 2001.      *
*                         All rights reserved.                            *
*                                                                         *
**************************************************************************/

import java.io.*;
import jgfutil.*; 

public class JGFMolDynBench extends md implements JGFSection3 {

  public static int nthreads;

  public JGFMolDynBench(int nthreads) {
        this.nthreads=nthreads;
  }


//   int size;

  public void JGFsetsize(int size){
    this.size = size;
  }

  public void JGFinitialise(){

      initialise();

  }

  public void JGFapplication(){ 

    runiters();

  } 


  public void JGFvalidate(){
    double refval[] = {1731.4306625334357,7397.392307839352};
    double dev = Math.abs(ek[0] - refval[size]);
    if (dev > 1.0e-10 ){
      System.out.println("Validation FAILED");
      System.out.println("Kinetic Energy = " + ek[0] + "  " + dev + "  " + size);
    }else{
        System.out.println("Validation PASSED");
    }
  }

  public void JGFtidyup(){    

//    one = null;
    System.gc();
  }


  public void JGFrun(int size){

    JGFInstrumentor.addTimer("Section3:MolDyn:Total", "Solutions",size);
    JGFInstrumentor.addTimer("Section3:MolDyn:Run", "Interactions",size);

    JGFsetsize(size); 

    JGFInstrumentor.startTimer("Section3:MolDyn:Total");

    JGFinitialise(); 
    JGFapplication(); 
    JGFvalidate(); 
    JGFtidyup(); 

    JGFInstrumentor.stopTimer("Section3:MolDyn:Total");

    JGFInstrumentor.addOpsToTimer("Section3:MolDyn:Run", (double) interactions);
    JGFInstrumentor.addOpsToTimer("Section3:MolDyn:Total", 1);

    JGFInstrumentor.printTimer("Section3:MolDyn:Run"); 
    JGFInstrumentor.printTimer("Section3:MolDyn:Total"); 
  }

  public static void main(String argv[]){

	  int size = 0;
	  
	  if(argv.length != 0 ) {
	    nthreads = Integer.parseInt(argv[0]);
	    if(argv.length > 1){
	    	size = Integer.parseInt(argv[1]);
	    }else{
	    	System.out.println("The size was not specified, defaulting to 0");
		    System.out.println("  ");
	    }
	  } else {
	    System.out.println("The no of threads has not been specified, defaulting to 1");
	    System.out.println("  ");
	    nthreads = 1;
	  }

	    JGFInstrumentor.printHeader(3,0,nthreads);

	    JGFMolDynBench mold = new JGFMolDynBench(nthreads); 
	    mold.JGFrun(size);
	 
	  }

}
 
