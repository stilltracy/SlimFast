import config
import re
import datetime
import os

RR_HOME=os.environ['RR_HOME']
RESULTS_ROOT=RR_HOME+"/results"

def execute_config(conf, program, dump_dir, dump_file_prefix="",post_cmds=None):
    
    text = conf.run(program,post_cmds)
    print text
    digest=parse_output(text)
    print "digest"+str(digest)
    if dump_file_prefix!="":
        dump_file_name=dump_file_prefix+program.name+'_'+conf.to_string('_')+".txt"
        dump_file=open(dump_dir+"/"+dump_file_name,"w")
        dump_file.write(text)
        dump_file.close()
	os.system("cat "+dump_dir+"/"+dump_file_name+" | grep \"Full GC.*[0-9]\" > "+dump_dir+"/"+dump_file_name+".mem")
    return digest
    
keyFields={
	".*ft_metadata_allocations.*\s(\d+).*":"ft_metadata_allocations",
	".*Total Time.*\s(\d+).*": "runtime",
	".*cas failures.*\s(\d+).*":"cas_failures",
	".*failed.*\s(.+)</failed>.*":"failed",
	".*accesses_count.*\s(\d+).*":"accesses_count",
	".*fp_calls_count.*\s(\d+).*":"fp_calls_count",
	".*total_accesses_count.*\s(\d+).*":"total_accesses_count",
	".*refCheck_misses_count.*\s(\d+).*":"refCheck_misses_count",
	".*rc_miss_rate.*\s(\d+).*":"rc_miss_rate",
	".*refCheckOn_count.*\s(\d+).*":"refCheckOn_count",
	".*host.*\s(.+)</host>.*":"host",
	".*frc_true1.*\s(\d+).*":"frc_true1",
	".*frc_true2.*\s(\d+).*":"frc_true2",
	".*frc_false.*\s(\d+).*":"frc_false",
	".*frc_fallthrough.*\s(\d+).*":"frc_fallthrough",
	".*st_create1.*\s(\d+).*":"st_create1",
	".*st_create2.*\s(\d+).*":"st_create2",
	".*st_hit1.*\s(\d+).*":"st_hit1",
	".*st_hit2.*\s(\d+).*":"st_hit2",
	".*ecv_pool_misses.*\s(\d+).*":"ecv_pool_misses",
	".*mc2.*\s(\d+).*":"mc2",
	".*mc3.*\s(\d+).*":"mc3",
	".*mc4.*\s(\d+).*":"mc4",
	".*FTGSAllocs.*\s(\d+).*":"FTGSAllocs",
	".*threadCount.*\s(\d+).*":"actualThreadCount",
	".*cnt_getWrEpochPair.*\s(\d+).*":"cnt_getWrEpochPair",
	".*cnt_resetWrEpochPair.*\s(\d+).*":"cnt_resetWrEpochPair",
	".*cnt_allocWrEpochPair.*\s(\d+).*":"cnt_allocWrEpochPair",
	".*ecv_pool_hits.*\s(\d+).*":"ecv_pool_hits",



		}
def parse_output(text):
	digest={}
	for line in text.splitlines():
		m = re.match(".*Validation failed.*",line)
		if m:
			digest["Invalid"]="true"
			continue
		m = re.match(".*Deadlock detected.*",line)
		if m:
			digest["Deadlock"]="true"
			continue
		m = re.match(".*Timeout occurred.*",line)
		if m:
			digest["Timeout"]="true"
			continue
		for fieldPattern in keyFields:
			m = re.match(fieldPattern, line)
			if m:
				fieldName=keyFields[fieldPattern]
				digest[fieldName]=m.group(1)
				break;
	return digest


          
def run_experiment(experiment_name,harness,config_tuples,inner_trials,post_cmds=None):
    time_stamp= datetime.datetime.now().strftime("%Y_%m_%d_%H_%M")
    result_dir=RESULTS_ROOT+"/"+experiment_name+"_"+time_stamp
    if post_cmds!=None:
    	post_cmds=post_cmds.replace("@result_dir",result_dir)
    dump_dir=result_dir+"/dumps"
    os.system("mkdir "+result_dir)
    os.system("mkdir "+dump_dir)
    result_id=experiment_name+"_"+time_stamp
    
    result_csv_file_name=result_dir+"/"+result_id+"_summary.csv"
    result_csv_file=open(result_csv_file_name,"w")
   
    base_header="program, tool, thread count, fp,"
    header=base_header
    
    
    for (program,tool,flags,thread_count,outer_trial) in config_tuples:
	for rt in range(0,inner_trials):
		conf=config.Config(tool,harness,flags,experiment_name,thread_count)
		print "post_cmds:"+post_cmds
		post_cmds=post_cmds.replace("@id",str(rt))
		digest=execute_config(conf,program,dump_dir,result_id+str(rt),post_cmds)
		if(header==base_header):
		
			for field in digest:
				header+=field+","
			result_csv_file.write(header+"\n")
		
		result_csv_record=program.name+","+conf.to_string(',')+","+','.join(digest.values())
		result_csv_file.write(result_csv_record+"\n")

    result_csv_file.close()
    
