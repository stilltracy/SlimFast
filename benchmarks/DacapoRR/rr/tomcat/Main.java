/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
import org.dacapo.tomcat.*;
import java.lang.reflect.Constructor;

import java.io.File;
import java.io.IOException;

public class Main {

    private static final int SMALL = 4;
    private static final int DEFAULT = 64;
    private static final int LARGE = 512;
    private static final int PORT = 7080;

    private static Constructor<Runnable> clientConstructor;

    public static void main(String[] args) throws Exception {
	File scratch = new File("scratch");
	Control myControl = new Control(scratch,Main.class.getClassLoader(),PORT);
	System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
	System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "info");
	System.setProperty("catalina.home", scratch.getAbsolutePath());
	System.setProperty("catalina.config", new File("scratch/catalina.properties").toURL().toExternalForm());

	myControl.exec("prepare");
	myControl.exec("startIteration");

	final int threadCount = args.length == 0 ? Runtime.getRuntime().availableProcessors() : Integer.parseInt(args[0]); 
	final int iterations = DEFAULT;
	final int iterationsPerClient = iterations / threadCount;

	final Thread[] threads = new Thread[threadCount];
	System.out.println("Creating client threads");
	for(int i = 0; i < threadCount; i++) {
	    Runnable client = new Client(scratch, i, iterationsPerClient, false, PORT);
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
