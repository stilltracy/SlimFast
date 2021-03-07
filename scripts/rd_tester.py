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
NPB_S1=[benchmarks.NPB_IS,
	benchmarks.NPB_CG,
	]
NPB_S2=[benchmarks.NPB_FT,
	]
NPB_S4=[benchmarks.NPB_MG,
	]
NPB_S5=[benchmarks.NPB_BT,
	benchmarks.NPB_LU,
	]
NPB_S6=[benchmarks.NPB_SP,
	]

VAR1=[benchmarks.CRYPT,benchmarks.MONTECARLO]
VAR2=[benchmarks.LUFACT, benchmarks.SPARSEMAT]
VAR3=[benchmarks.MOLDYN, benchmarks.SERIES, benchmarks.SOR]
VAR4=[benchmarks.SUNFLOW_RR]
VAR5=[benchmarks.XALAN_RR, benchmarks.LUSEARCH_RR]

BDB =[benchmarks.BDBREAD]

tools=[
	"SF_jld",
	#"FT"
	#"FT_CAS",
	#"N"
	]
rr_flags=[
		["-array=FINE","-field=FINE","-updaters=CAS"],	
		#[ "-array=FINE","-field=FINE","-updaters=CAS","-refCheckOn"],
	]

programs_option={
		  "npb16+bdb": BDB + NPB_S1 + NPB_S6,
		  "bdb" :BDB,
		  "npb1":NPB_S1,
		  "npb2":NPB_S2,
		  "npb4":NPB_S4,
		  "npb5":NPB_S5,
		  "npb6":NPB_S6,
		  "npb":NPB_PROGRAMS,
		  "mnt":[benchmarks.MONTECARLO], 
		  "var1": VAR1,
		  "var2": VAR2,
		  "var3": VAR3,
		  "var4": VAR4,
		  "var5": VAR5,
		  "dacapo_var": benchmarks.DACAPO_RR_VAR,
		  "dacapo_fix": benchmarks.DACAPO_RR_FIX,
		  "avr":[benchmarks.AVRORA_RR],
		  "sor":[benchmarks.SOR],
		  "jyt":[benchmarks.JYTHON_RR],
		  "crypt":[benchmarks.CRYPT],
		  "lufact":[benchmarks.LUFACT],
                  "grande":GRANDE_PROGRAMS,
		  "dacapo":DACAPO_PROGRAMS,
		  "xalan":[benchmarks.XALAN_RR],
		  "tomcat":[benchmarks.TOMCAT_RR],
                 }

parser=argparse.ArgumentParser()
parser.add_argument("exp_name",type=str,help="the name to identify/describe the experiment")
parser.add_argument("programs_set",type=str,help="the set of programs to be run")
parser.add_argument("harness",type=str,help="the harness program to be used")
#parser.add_argument("start_lg_tc",type=int, help="logarithm of the lower_bound of thread count")
#parser.add_argument("end_lg_tc",type=int, help="logarithm of the upper_bound of thread count")
parser.add_argument("thread_counts",type=str,help="the set of thread counts to run")
parser.add_argument("inner_trials", type=int, help="how many times a configuration will be repeated in the inner loop")
parser.add_argument("outer_trials", type=int, help="how many times a configuration will be repeated in the outer loop")
parser.add_argument("-e","--email_notify",action="store_true",help="send a notification email when experiment terminates")
args=parser.parse_args()

exp_name=args.exp_name
programs=programs_option[args.programs_set]
thread_counts=eval(args.thread_counts)

config_tuples=[(p, tool, f, tc, ot) for p in programs for ot in range(0,args.outer_trials) for f in rr_flags for tc in thread_counts for tool in tools ]
post_cmds="mv @program_path/memoryUsage.csv @result_dir/@conf_name_memoryUsage_@id.csv ; mv @program_path/cm.csv @result_dir/@conf_name_cms.csv"
import time
harness=args.harness
startTS=time.time()
driver.run_experiment(exp_name,harness,config_tuples, args.inner_trials,post_cmds)
endTS=time.time()
import socket
host_name=socket.gethostname()
strTime=time.strftime("%H:%M:%S",time.gmtime(endTS-startTS))
print "Time taken:"+strTime
if args.email_notify:
	import sendEmailNotification
	sendEmailNotification.sendNotification(host_name,exp_name,strTime)
	print "Email notification sent."
