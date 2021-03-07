
import java.io.File;

public class MainCreate {


    static final String WKSP_DIRECTORY_RELATIVE_TO_SCRATCH = "workspace";
    static final String PLUGIN_ID = "org.dacapo.eclipse.Harness";
    static final String OSGI_BOOTSTRAP_JAR = "eclipse" + File.separator + "plugins" + File.separator + "org.eclipse.osgi_3.5.1.R35x_v20090827.jar";

    public static void main(String args_[]) {
	
	String oldJavaHome = null;
	String size;
	
	try {
	    System.setProperty("osgi.os", "linux");
	    System.setProperty("osgi.ws", "gtk");
	    System.setProperty("osgi.arch", "x86");
	    System.setProperty("osgi.install.area", "file:" + "scratch/eclipse/");
	    System.setProperty("osgi.noShutdown", "true");
	    System.setProperty("osgi.framework", "file:" + "scratch/" + OSGI_BOOTSTRAP_JAR);

	    String[] args = new String[4];
	    args[0] = "-data"; // identify the workspace
	    args[1] = "scratch/" + WKSP_DIRECTORY_RELATIVE_TO_SCRATCH;
	    args[2] = "-application"; // identify the plugin
	    args[3] = PLUGIN_ID;
	    org.eclipse.core.runtime.adaptor.EclipseStarter.startup(args, null);
	} catch (Exception e) {
	    e.printStackTrace();
	}

	try {
	    String[] pluginArgs = { "unzip" };
	    org.eclipse.core.runtime.adaptor.EclipseStarter.run(pluginArgs);
	} catch (Exception e) {
	    e.printStackTrace();
	}

	try {
	    String[] pluginArgs = {"setup" }; // get this from the bm
	    org.eclipse.core.runtime.adaptor.EclipseStarter.run(pluginArgs );
	} catch (Exception e) {
	    e.printStackTrace();
	}


	try {
	    org.eclipse.core.runtime.adaptor.EclipseStarter.shutdown();
	} catch (Exception e) {
	    e.printStackTrace();
	}
 

   }
}