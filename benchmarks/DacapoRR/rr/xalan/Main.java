/*
 * Copyright (c) 2005, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: XSLTBench.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Main {

    public static void main(String[] args) throws Exception{
	XSLTBench benchmark = new XSLTBench(new File("./scratch"));
	if (args.length > 0) {
	    benchmark.createWorkers(Integer.parseInt(args[0]));
	} else { 
	    benchmark.createWorkers(Runtime.getRuntime().availableProcessors());
	}
	benchmark.doWork(100);
    }
}
