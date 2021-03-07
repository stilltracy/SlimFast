'''
Created on Jun 6, 2014

@author: stilltracy
'''
class Benchmark:
	def setthreadcount(self,tc):
		self.thread_count=str(tc)
	def build_args(self):
		return self.args;

class BDBBenchmark(Benchmark):
	path="benchmarks/berkeleydb-je-6.4.25/slimfast-benchmark"
	classpath="../build/classes" 
	args_first=True
	def __init__(self, na, ma, input_size="db5k"):
		self.name = na
		self.main = ma
		self.input_size = input_size
	def build_args(self):
		return [self.main,
			#"-cp "+self.classpath,
			#"RandomReads",
			"randomread",
			self.input_size,
			"10000",
			"5000"]
			
			
class NASParallelBenchmark(Benchmark):
	path="benchmarks/NPB3.0/NPB3.0-JAV"
	classpath="."
	args_first=True
	def __init__(self,na,ma,input_size="A"):
		self.name=na
		self.main=ma
		self.input_size="CLASS="+input_size
	def build_args(self):
		return [self.main,
			"-np"+self.thread_count,
			self.input_size
			]

class GrandeBenchmark(Benchmark):
	path="benchmarks/grande/classes"
	main=""
	classpath="."
	args="2"
	args_first=True
	def __init__(self,na,ma,input_size=2):
		self.name=na
		self.main=ma
		self.input_size=str(input_size)
	def build_args(self):
		return [self.main,
			self.thread_count,
			self.input_size
			]

class DacapoRRBenchmark(Benchmark):
	root_path="benchmarks/DacapoRR/rr"
	main=""
	classpath=""
	args=""
	args_first=False
	def __init__(self,na):
		self.name=na
		self.path=DacapoRRBenchmark.root_path+"/"+na

	def build_args(self):
		return []

class DacapoBenchmark(Benchmark):
	name=""
	path="benchmarks"
	main="Harness"
	classpath=".:dacapo.jar"
	args=""
	args_first=False

	def __init__(self,na):
		self.name=na
		self.args=na


NPB_MG=NASParallelBenchmark("NPB_MG","NPB3_0_JAV.MG","A")
NPB_CG=NASParallelBenchmark("NPB_CG","NPB3_0_JAV.CG","A")
NPB_BT=NASParallelBenchmark("NPB_BT","NPB3_0_JAV.BT","W")
NPB_LU=NASParallelBenchmark("NPB_LU","NPB3_0_JAV.LU","W")
NPB_FT=NASParallelBenchmark("NPB_FT","NPB3_0_JAV.FT","A")
NPB_SP=NASParallelBenchmark("NPB_SP","NPB3_0_JAV.SP","W")
NPB_IS=NASParallelBenchmark("NPB_IS","NPB3_0_JAV.IS","A")

NPB=[NPB_MG,NPB_CG,NPB_BT,NPB_LU,NPB_FT,NPB_IS,NPB_SP]

AVRORA_RR=DacapoRRBenchmark("avrora")
JYTHON_RR=DacapoRRBenchmark("jython")
LUSEARCH_RR=DacapoRRBenchmark("lusearch")
PMD_RR=DacapoRRBenchmark("pmd")
XALAN_RR=DacapoRRBenchmark("xalan")   
H2_RR=DacapoRRBenchmark("h2") 
TOMCAT_RR=DacapoRRBenchmark("tomcat")
ECLIPSE_RR=DacapoRRBenchmark("eclipse")
SUNFLOW_RR=DacapoRRBenchmark("sunflow")
BATIK_RR=DacapoRRBenchmark("batik")
FOP_RR=DacapoRRBenchmark("fop")
LUINDEX_RR=DacapoRRBenchmark("luindex")
TRADEBEANS_RR=DacapoRRBenchmark("tradebeans")
DACAPO_RR_VAR=[LUSEARCH_RR,XALAN_RR,SUNFLOW_RR]
DACAPO_RR_FIX=[AVRORA_RR,JYTHON_RR,PMD_RR,TOMCAT_RR,FOP_RR,BATIK_RR,LUINDEX_RR]
DACAPO_RR = DACAPO_RR_VAR+DACAPO_RR_FIX

AVRORA=DacapoBenchmark("avrora")
JYTHON=DacapoBenchmark("jython")
LUSEARCH=DacapoBenchmark("lusearch")
PMD=DacapoBenchmark("pmd")
XALAN=DacapoBenchmark("xalan")   
H2=DacapoBenchmark("h2") 
TOMCAT=DacapoBenchmark("tomcat")
ECLIPSE=DacapoBenchmark("eclipse")
TPCC=DacapoBenchmark("tpcc")
DACAPO = [AVRORA,JYTHON,LUSEARCH,PMD,XALAN]

CRYPT=GrandeBenchmark("crypt","JGFCryptBench")
LUFACT=GrandeBenchmark("lufact","JGFLUFactBench")
MOLDYN=GrandeBenchmark("moldyn","JGFMolDynBench",1)
MONTECARLO=GrandeBenchmark("montecarlo","JGFMonteCarloBench",1)
SERIES=GrandeBenchmark("series","JGFSeriesBench")
SOR=GrandeBenchmark("sor","JGFSORBench")
SPARSEMAT=GrandeBenchmark("sparsemat","JGFSparseMatmultBench")

BDBREAD=BDBBenchmark("bdb-read","RandomReads", "db1k-64kb")
