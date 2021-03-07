package tools.util;

import java.io.File;
import java.io.FileOutputStream;

public class MemoryUsageRecorder extends Thread{

	public boolean alive=true;
	long recordInterval=1000;
	public FileOutputStream fos=null;
	public MemoryUsageRecorder(String filename)
	{
		File dumpFile=new File(filename);
		try
		{
			fos=new FileOutputStream(dumpFile);
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	public MemoryUsageRecorder(int interval)
	{
		recordInterval=interval;
	}
	public MemoryUsageRecorder(String filename, int interval)
	{
		this(filename);
		recordInterval=interval;
	}
	@Override
	public void run() {
		int recordCount=0;
		try
		{
			while(alive)
			{
				long memUsage=getMemoryUsage();
				if(fos!=null)
					fos.write((""+recordCount+","+memUsage+"\n").getBytes());
				recordCount++;
				try {
					Thread.sleep(recordInterval);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			if(fos!=null)
				fos.close();
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
	public static long getMemoryUsage()
	{
		System.gc();
		Runtime runtime=Runtime.getRuntime();
		return runtime.totalMemory()-runtime.freeMemory();
	}

}
