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

public class JGFMonteCarloBench extends CallAppDemo implements JGFSection3 {

  public static int nthreads;

  public JGFMonteCarloBench(int nthreads) {
        this.nthreads=nthreads;
  }

  public void JGFsetsize(int size){
    this.size = size;
  }

  public void JGFinitialise(){

      initialise();

  }

  public void JGFapplication(){ 

    JGFInstrumentor.startTimer("Section3:MonteCarlo:Run");  

    runiters();

    JGFInstrumentor.stopTimer("Section3:MonteCarlo:Run");  
    
    presults();
  } 


  public void JGFvalidate(){
   double refval[] = {-0.0333976656762814,-0.03215796752868655};
   double dev = Math.abs(AppDemo.JGFavgExpectedReturnRateMC - refval[size]);
   if (dev > 1.0e-12 ){
     System.out.println("Validation FAILED");
     System.out.println(" expectedReturnRate= " + AppDemo.JGFavgExpectedReturnRateMC + "  " + dev + "  " + size);
   }else{
       System.out.println("Validation PASSED");
   }
  }

  public void JGFtidyup(){    

    System.gc();
  }


  public void JGFrun(int size){

    JGFInstrumentor.addTimer("Section3:MonteCarlo:Total", "Solutions",size);
    JGFInstrumentor.addTimer("Section3:MonteCarlo:Run", "Samples",size);

    JGFsetsize(size); 

    JGFInstrumentor.startTimer("Section3:MonteCarlo:Total");

    JGFinitialise(); 
    JGFapplication(); 
    JGFvalidate(); 
    JGFtidyup(); 

    JGFInstrumentor.stopTimer("Section3:MonteCarlo:Total");

    JGFInstrumentor.addOpsToTimer("Section3:MonteCarlo:Run", (double) input[1] );
    JGFInstrumentor.addOpsToTimer("Section3:MonteCarlo:Total", 1);

    JGFInstrumentor.printTimer("Section3:MonteCarlo:Run"); 
    JGFInstrumentor.printTimer("Section3:MonteCarlo:Total"); 
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

	    JGFMonteCarloBench mc = new JGFMonteCarloBench(nthreads); 
	    mc.JGFrun(size);
	 
	  }

}
 
