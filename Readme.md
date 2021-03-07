SlimFast is a sound & complete dynamic data race detector.

Files accompanied with this file provide an implementation of SlimFast, as well as FastTrack, on RoadRunner.

For details about SlimFast, please refer to our [paper](https://ieeexplore.ieee.org/abstract/document/8425237) published at IPDPS 2018' : 

SlimFast: Reducing Metadata Redundancy in Sound and Complete Dynamic Data Race Detection
Yuanfeng Peng, Christian DeLozier, Ariel Eizenberg, William Mansky, Joseph Devietti

If you have any questions & comments about SlimFast, feel free to drop me an email at yuanfeng@cis.upenn.edu.

For updates, stay tuned with our GitHub repository: https://github.com/stilltracy/SlimFast.git .

========================================

Preliminaries


Before building or running SlimFast, make sure $JAVA_HOME & $JAVA_ARGS are correctly set in env_setup.  

We used 64GB heaps in our evaluation.  Since some benchmarks require large heap to run to completion (e.g., crypt may run out of memory on FastTrack under heaps smaller than 12GB ), we recommand setting the JVM heap to be greater than 16GB.

Note that the current version of RoadRunner (v0.3) is not compatible with Java 8.  We recommend using JDK 1.7.

========================================

Build

To build SlimFast, simply execute the following commands:

	source env_setep
	ant

Once the compilation succeeds, you should be able to run the following:

	rrrun -tools

which will list all the tools available.


========================================

Run

You could run SlimFast and/or FastTrack by using either rrrun or scripts/run_experiment.py.

If you want to run with rrrun directly, you can execute something like this:

	rrrun -array=FINE -field=FINE -updaters=CAS -tool=SF -maxTid=1024 <JavaClassName> <args>

where <JavaClassName> is the name of the Java class you want to run, and <args> are the arguments passed to it.

For details about the various options supported by RoadRunner, please use rrrun -help.

An easier way to run SlimFast or FastTrack is to use scripts/run_experiment.py:

	cd scripts
	python run_experiment.py <exp_name> <benchmark_set> <harness> <thread_counts> <inner_trials> <outer_trials> 

For example, the following command:

	python run_experiment.py test_crypt crypt rrrun '[16]' 1 3 

will run crypt with SlimFast and FastTrack for 3 times, with the results saved to results/test_crypt_some_timestamp/.

You may want to run python run_experiment.py -h to see the helping text. 

You could also view/modify run_experiment.py to run different benchmarks. 

In our evaluation, we measure heap usage by forcing System GC's periodically.  If you want to measure heap usage of FastTrack or SlimFast, you need to set rr.tool.Tool.MEASURE_MEMORY to be true in srcs/rr/tool/Tool.java, and rebuild the source code.  This will instruct a background thread to force a GC every 1000 ms.  Then if you use run_experiment.py to run an experiment named 'test_crypt', the raw heap usage data could be found under results/test_crypt_some_timestamp/dumps/test_crypt_some_timestamp_args.mem .

========================================

Benchmarks

We've provided three benchmark suites under benchmarks/, which includes Java Grande, Dacapo, and Nas Parallel Benchmarks.

Running FastTrack or SlimFast on these benchmarks may take ~10 seconds to ~30 minutes.  Also, some of the Dacapo benchmarks(more precisely, those not used in our evaluation, such as eclipse, daytrader, etc.) have unresolved compatibility issues with RoadRunner, which may cause FastTrack/SlimFast to crash or hang.  

If you want to run/test SlimFast/FastTrack on a particular benchmark, we recommend you using/modifying the python scripts under scripts/.

========================================

Quick guide about the code

Since SlimFast (as well as FastTrack) is built as a RoadRunner tool, most of its code resides in src/tools/slimfast.  Similarly, code of the two versions of FastTrack is under src/tools/fasttrack and src/tools/fasttrack_cas.  

Below is a description of the relevant files under src/tools/slimfast:

- EpochAndCV.java : implementation of the EpochPlusVC structure in the SlimFast algorithm (see Section 4.3 of the paper)
- EpochPair.java  : implementation of the EpochPair structure in the SlimFast algorithm ( see Section 4.2 of the paper)
- FastTrackBarrierState.java: per-barrier state used in both SlimFast and FastTrack
- FastTrackLockData.java    : per-lock state used in both SlimFast and FastTrack
- FastTrackVolatileData.java : metadata for volatile variables, used in both SlimFast and FastTrack
- SlimFastFTTool.java        : the main implementation of the SlimFast tool
- TLSObject.java             : the thread-local storage object, which implements S_t, Q_t, W_t in the SlimFast algorithm (see Section 4 of the paper)

To use and/or modify SlimFast, you generally do not need to look at other files in the source tree.  If you are interested in details about RoadRunner, you may want to read the RoadRunner paper at : http://dept.cs.williams.edu/~freund/rr/rr.pdf . The code repository of RoadRunner could be checked out at https://github.com/stephenfreund/RoadRunner.git . 
