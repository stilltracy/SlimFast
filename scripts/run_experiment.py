'''
Created on Jun 7, 2014

@author: stilltracy
'''
import argparse
import sys
import driver
import benchmarks

GRANDE_PROGRAMS=[benchmarks.CRYPT,benchmarks.LUFACT,
		benchmarks.MOLDYN,benchmarks.MONTECARLO,benchmarks.SERIES,benchmarks.SOR,
		benchmarks.SPARSEMAT]
DACAPO_PROGRAMS=benchmarks.DACAPO_RR
NPB_PROGRAMS=benchmarks.NPB

tools=[
	"FT",
	"SF",
	#"N"
	]

rr_flags=[
		["-array=FINE","-field=FINE","-updaters=CAS"],	
	]

programs_option={ 
		  "grande":GRANDE_PROGRAMS,
		  "dacapo":DACAPO_PROGRAMS,
		  "npb":NPB_PROGRAMS,
		  "crypt":[benchmarks.CRYPT],
		  "lufact":[benchmarks.LUFACT],
		  "sunflow":[benchmarks.SUNFLOW_RR],
		  "sparsemat":[benchmarks.SPARSEMAT],
                  "lusearch":[benchmarks.LUSEARCH_RR],
		  "sor"     :[benchmarks.SOR],
		  "tomcat": [benchmarks.TOMCAT_RR],
                 }

parser=argparse.ArgumentParser()
parser.add_argument("exp_name",type=str,help="the name to identify/describe the experiment")
parser.add_argument("programs_set",type=str,help="the set of programs to be run "+str(programs_option.keys()))
parser.add_argument("harness",type=str,help="the harness program to be used (use 'TEST' for dacapo benchmarks, 'rrrun' otherwise)")
parser.add_argument("thread_counts",type=str,help="the set of thread counts to run (e.g. '[2,16]')")
parser.add_argument("inner_trials", type=int, help="how many times a configuration will be repeated in the inner loop")
parser.add_argument("outer_trials", type=int, help="how many times a configuration will be repeated in the outer loop")
args=parser.parse_args()

exp_name=args.exp_name
programs=programs_option[args.programs_set]
thread_counts=eval(args.thread_counts)

config_tuples=[(p, tool, f, tc, ot) for p in programs for ot in range(0,args.outer_trials) for f in rr_flags for tc in thread_counts for tool in tools ]
post_cmds=';'.join(["mv @program_path/memoryUsage.csv @result_dir/@conf_name_memoryUsage_@id.csv",
	  		"mv @program_path/redundancy.csv @result_dir/@conf_name_redundancy_@id.csv"
			])

import time
harness=args.harness
startTS=time.time()
driver.run_experiment(exp_name,harness,config_tuples, args.inner_trials,post_cmds)
endTS=time.time()
strTime=time.strftime("%H:%M:%S",time.gmtime(endTS-startTS))
print "Total time taken:"+strTime

print exp_name,"terminated."
