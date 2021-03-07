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

import jgfutil.*;


public class JGFCryptBench extends IDEATest implements JGFSection2{ 

  private int size; 
  private int datasizes[]={3000000,20000000,50000000};
    public static int nthreads;

public JGFCryptBench(int nthreads)
    {
	this.nthreads=nthreads;
    }


  public void JGFsetsize(int size){
    this.size = size;
  }

  public void JGFinitialise(){
    array_rows = datasizes[size];
    buildTestData();
  }
 
  public void JGFkernel(){
    Do(); 
  }

  public void JGFvalidate(){
    boolean error;

    error = false; 
    for (int i = 0; i < array_rows; i++){
      error = (plain1 [i] != plain2 [i]); 
      if (error){
	System.out.println("Validation FAILED");
	System.out.println("Original Byte " + i + " = " + plain1[i]); 
	System.out.println("Encrypted Byte " + i + " = " + crypt1[i]); 
	System.out.println("Decrypted Byte " + i + " = " + plain2[i]); 
	break;
      }
    }

    if(!error){
        System.out.println("Validation PASSED");
    }
  }


  public void JGFtidyup(){
    freeTestData(); 
  }  



  public void JGFrun(int size){


    JGFInstrumentor.addTimer("Section2:Crypt:Kernel", "Kbyte",size);

    JGFsetsize(size); 
    JGFinitialise(); 
    JGFkernel(); 
    JGFvalidate(); 
    JGFtidyup(); 

     
    JGFInstrumentor.addOpsToTimer("Section2:Crypt:Kernel", (2*array_rows)/1000.); 
    JGFInstrumentor.printTimer("Section2:Crypt:Kernel"); 
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

	    JGFInstrumentor.printHeader(2,0,nthreads);

	    JGFCryptBench cb = new JGFCryptBench(nthreads); 
	    cb.JGFrun(size);
	 
  }
}
