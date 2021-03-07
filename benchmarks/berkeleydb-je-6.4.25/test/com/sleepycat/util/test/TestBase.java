/*-
 *
 *  This file is part of Oracle Berkeley DB Java Edition
 *  Copyright (C) 2002, 2015 Oracle and/or its affiliates.  All rights reserved.
 *
 *  Oracle Berkeley DB Java Edition is free software: you can redistribute it
 *  and/or modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation, version 3.
 *
 *  Oracle Berkeley DB Java Edition is distributed in the hope that it will be
 *  useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License in
 *  the LICENSE file along with Oracle Berkeley DB Java Edition.  If not, see
 *  <http://www.gnu.org/licenses/>.
 *
 *  An active Oracle commercial licensing agreement for this product
 *  supercedes this license.
 *
 *  For more information please contact:
 *
 *  Vice President Legal, Development
 *  Oracle America, Inc.
 *  5OP-10
 *  500 Oracle Parkway
 *  Redwood Shores, CA 94065
 *
 *  or
 *
 *  berkeleydb-info_us@oracle.com
 *
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  EOF
 *
 */

package com.sleepycat.util.test;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * The base class for all JE unit tests. 
 */
public abstract class TestBase {

    private static final boolean copySucceeded =
        Boolean.getBoolean("test.copySucceeded");

    /*
     * Need to provide a customized name suffix for those tests which are 
     * Parameterized.
     *
     * This is because we need to provide a unique directory name for those 
     * failed tests. Parameterized class would reuse test cases, so class name 
     * plus the test method is not unique. User should set the customName
     * in the constructor of a Parameterized test. 
     */
    protected String customName;
    
    /**
     * The rule we use to control every test case, the core of this rule is 
     * copy the testing environment, files, sub directories to another place
     * for future investigation, if any of test failed. But we do have a limit
     * to control how many times we copy because of disk space. So once the 
     * failure counter exceed limit, it won't copy the environment any more.
     */
    @Rule
    public TestRule watchman = new TestWatcher() {

        /* Copy Environments when the test failed. */
        @Override
        protected void failed(Throwable t, Description desc) {
            doCopy(desc);
        }
        
        @Override
        protected void succeeded(Description desc){
            if (copySucceeded) {
                doCopy(desc);
            }
        }

        private void doCopy(Description desc) {
            String dirName = makeFileName(desc);
            try {
                copyEnvironments(dirName);
            } catch (Exception e) {
                throw new RuntimeException
                    ("can't copy env dir to " + dirName  + " after failure", e);
            }
        }
    };
    
    @Before
    public void setUp() 
        throws Exception {
        
        SharedTestUtils.cleanUpTestDir(SharedTestUtils.getTestDir());
    }
    
    @After
    public void tearDown() throws Exception {
        // Provision for future use
    }
    
    /**
     *  Copy the testing directory to other place. 
     */
    private void copyEnvironments(String path) throws Exception{
        
        File failureDir = SharedTestUtils.getFailureCopyDir();
        if (failureDir.list().length < SharedTestUtils.getCopyLimit()) {
            SharedTestUtils.copyDir(SharedTestUtils.getTestDir(),
                                    new File(failureDir, path));
        }
    }
    
    /**
     * Get failure copy directory name. 
     */
    private String makeFileName(Description desc) {
        String name = desc.getClassName() + "-" + desc.getMethodName();
        if (customName != null) {
            name = name + "-" + customName;
        }
        return name;
    }
}
