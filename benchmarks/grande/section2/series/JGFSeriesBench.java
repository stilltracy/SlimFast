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

public class JGFSeriesBench extends SeriesTest implements JGFSection2{ 

  public static int nthreads;
  private int size; 
  private int datasizes[]={10000,100000,1000000};

  public JGFSeriesBench(int nthreads) {
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
     double ref[][] = {{2.8729524964837996, 0.0},
                       {1.1161046676147888, -1.8819691893398025},
                       {0.34429060398168704, -1.1645642623320958},
                       {0.15238898702519288, -0.8143461113044298}};
/* 
// for 200 points 
    double ref[][] = {{2.8377707562588803, 0.0},
		       {1.0457844730995536, -1.8791032618587762},
		       {0.27410022422635033, -1.158835123403027},
		       {0.08241482176581083, -0.8057591902785817}};
*/ 
  
     boolean foundError = false;

    for (int i = 0; i < 4; i++){
      for (int j = 0; j < 2; j++){
	double error = Math.abs(TestArray[j][i] - ref[i][j]);
	if (error > 1.0e-12 ){
	  System.out.println("Validation FAILED for coefficient " + j + "," + i);
	  System.out.println("Computed value = " + TestArray[j][i]);
	  System.out.println("Reference value = " + ref[i][j]);
          foundError = true;
	}
      }
    }

    if(!foundError){
        System.out.println("Validation PASSED");
    }
  }

  public void JGFtidyup(){
    freeTestData(); 
  }  



  public void JGFrun(int size){


    JGFInstrumentor.addTimer("Section2:Series:Kernel", "coefficients",size);
    JGFsetsize(size); 
    JGFinitialise(); 
    JGFkernel(); 
    JGFvalidate(); 
    JGFtidyup(); 

     
    JGFInstrumentor.addOpsToTimer("Section2:Series:Kernel", (double) (array_rows * 2 - 1));
 
    JGFInstrumentor.printTimer("Section2:Series:Kernel"); 
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

	    JGFSeriesBench se = new JGFSeriesBench(nthreads); 
	    se.JGFrun(size);
	 
	  }
}
