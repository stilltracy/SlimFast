/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.tomcat;
import java.lang.reflect.Constructor;

import java.io.File;
import java.io.IOException;



/**
 * Class to encapsulate pre- and post-iteration startup etc.
 * 
 * Separated into a single class with a single public method for ease of use via
 * reflection.
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Control.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Main {
    private static final int SMALL = 4;
    private static final int DEFAULT = 64;
    private static final int LARGE = 512;
    private static final int PORT = 7080;

    private static Constructor<Runnable> clientConstructor;

    public static void main(String[] args) throws Exception {
	File scratch = new File("./scratch");
	Control myControl = new Control(scratch,Main.class.getClassLoader(),PORT);
	System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
	//	Client myClient = new Client(new File("./logDir"),0,1,false,PORT);
	Class clientClass = Client.class;
	System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "info");
        System.setProperty("catalina.home", scratch.getAbsolutePath());
        System.setProperty("catalina.config", new File("./scratch/catalina.properties").toURL().toExternalForm());

	clientConstructor = clientClass.getConstructor(File.class, int.class, int.class, boolean.class, int.class);

	myControl.exec("prepare");
	myControl.exec("startIteration");

	final int threadCount = 8; // Because that's what it seemed to be using
	final int iterations = DEFAULT;
	final int iterationsPerClient = iterations / threadCount;

				     final Thread[] threads = new Thread[threadCount];
	System.out.println("Creating client threads");
	for(int i = 0; i < threadCount; i++) {
	    Runnable client = clientConstructor.newInstance(scratch, i, iterationsPerClient, false, PORT);
	    threads[i] = new Thread(client);
	    threads[i].start();
	}
				     System.out.println("Waiting for clients to complete");
				     for(int i = 0; i < threadCount; i++) {
	    threads[i].join();
	}
	System.out.println("Client threads complete ... unloading web application");
	myControl.exec("stopIteration");
	myControl.exec("cleanup");
    }

}