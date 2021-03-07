'''
Created on Jun 6, 2014

@author: stilltracy
'''

import subprocess
import os

RR_HOME=os.environ['RR_HOME']
MAX_TID=1024
EXCLUDE_DIRS = "-classes=-Harness -classes=-org.dacapo.harness.* -classes=-org.dacapo.parser.* -classes=-TimerCallback -classes=-org.eclipse.core.internal.runtime.auth.AuthorizationHandler"
class Config(object):
    '''
    classdocs
    '''

    
    driver="rrrun"
    tool_to_test=""
    rr_flags=[]
    
    test_name="test"
    thread_count=1

    def __init__(self, tool, driver,flags, tname, trd_cnt):
        '''
        Constructor
        '''
	self.driver=driver
        self.tool_to_test=tool
        self.rr_flags=flags
        self.test_name=tname
        self.thread_count=trd_cnt
    def __build_args(self,program):
        args=[self.driver]
	program.setthreadcount(self.thread_count)
	if not program.args_first:
		args.append(str(self.thread_count))
	args.append("-maxTid="+str(MAX_TID))        
	args.append(EXCLUDE_DIRS)
        if not program.classpath=="":
		args.append("-classpath="+program.classpath)
        args.append("-tool="+self.tool_to_test)
	#args.append("-dump=sth/")
	args=args+self.rr_flags
	args.append("-maxTime=1800")#timeout in 30 minutes
	
	args+=program.build_args()
    	#args.append("-t " + str(self.thread_count))

        return args

    def __build_conf_name(self, program, extraDimensions):
	conf_name=[program.name]
	conf_name.append(self.tool_to_test)
	conf_name.append(str(self.thread_count))
	conf_name=conf_name+extraDimensions
	return ','.join(conf_name)
    
    def run(self,program,post_cmds=None,eds=[]):
	cwd=os.getcwd()
	#eds=["meta2","noArray"]
	conf_name=self.__build_conf_name(program,eds)
	print "conf_name:"+conf_name
	text=""
	#print "what"
        try:
            os.chdir(RR_HOME+"/"+program.path)
            args=self.__build_args(program)
            #conf_name="Running: "+''.join(args)
	    #print conf_name
	    print ' '.join(args)
	    text = subprocess.check_output(args,stderr=subprocess.STDOUT)
        except subprocess.CalledProcessError as e:
            text = e.output
        os.chdir(cwd)
	#print "here"
	#print "run:post_cmds: "+post_cmds
	if post_cmds!=None:
		final_post_cmds=post_cmds.replace("@program_path",RR_HOME+"/"+program.path).replace("@conf_name",conf_name.replace(",","_"))	
		print "final_post_cmds:"+final_post_cmds
        	os.system(final_post_cmds)
	return text
    
    def to_string(self, delimiter):
        return delimiter.join([self.tool_to_test,str(self.thread_count)]+self.rr_flags)
