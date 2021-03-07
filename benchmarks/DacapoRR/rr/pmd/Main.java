import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import java.io.File;

public class Main {

    static String args[] = {
	/* "@pmd/small.lst","text",
        "scratch/pmd/rulesets/basic.xml",
         "scratch/pmd/rulesets/braces.xml",
		"scratch/pmd/rulesets/codesize.xml",
		"scratch/pmd/rulesets/controversial.xml",
		"scratch/pmd/rulesets/coupling.xml",
		"scratch/pmd/rulesets/design.xml",
		"scratch/pmd/rulesets/favorites.xml",
		"scratch/pmd/rulesets/finalizers.xml",
		"scratch/pmd/rulesets/imports.xml",
		"scratch/pmd/rulesets/javabeans.xml",
		"scratch/pmd/rulesets/junit.xml",
		"scratch/pmd/rulesets/naming.xml",
		"scratch/pmd/rulesets/newrules.xml",
		"scratch/pmd/rulesets/rulesets.properties",
		"scratch/pmd/rulesets/scratchpad.xml",
		"scratch/pmd/rulesets/strictexception.xml",
		"scratch/pmd/rulesets/strings.xml",
		"scratch/pmd/rulesets/unusedcode.xml"
 */
	"@pmd/default.lst",
	"text",
        "scratch/pmd/rulesets/basic.xml",
	"scratch/pmd/rulesets/braces.xml",
	"scratch/pmd/rulesets/codesize.xml",
	"scratch/pmd/rulesets/controversial.xml",
	"scratch/pmd/rulesets/coupling.xml",
	"scratch/pmd/rulesets/design.xml",
	"scratch/pmd/rulesets/favorites.xml",
	"scratch/pmd/rulesets/finalizers.xml",
	"scratch/pmd/rulesets/imports.xml",
	"scratch/pmd/rulesets/javabeans.xml",
	"scratch/pmd/rulesets/junit.xml",
	"scratch/pmd/rulesets/naming.xml",
	"scratch/pmd/rulesets/newrules.xml",
	"scratch/pmd/rulesets/rulesets.properties",
	"scratch/pmd/rulesets/scratchpad.xml",
	"scratch/pmd/rulesets/strictexception.xml",
	"scratch/pmd/rulesets/strings.xml",
	"scratch/pmd/rulesets/unusedcode.xml" 
    };

    public static void main(String args_[]) throws Exception {
	System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
	args[0] = collectFilesFromFile("scratch/" + args[0].substring(1));
	net.sourceforge.pmd.PMD.main(args);
    }


    private static String collectFilesFromFile(String inputFileName) {
	try {
	    java.io.BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(new FileInputStream(inputFileName)));

	    List<File> files = new ArrayList<File>();

	    for (String l = reader.readLine(); l != null; l = reader.readLine()) {
		files.add(new File("scratch/" + l));
	    }
	    return commaSeparate(files);
	} catch (FileNotFoundException e) {
	    throw new RuntimeException("File " + inputFileName + " error: " + e);
	} catch (java.io.IOException e) {
	    throw new RuntimeException("File " + inputFileName + " error: " + e);
	}

    }

    private static String commaSeparate(List<File> list) {
	String result = "";
	for (Iterator<File> i = list.iterator(); i.hasNext();) {
	    String s = i.next().getPath();
	    result += s;
	    if (i.hasNext())
		result += ",";
	}
	return result;
    }


}