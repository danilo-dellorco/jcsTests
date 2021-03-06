package it.uniroma2.dicii.isw2.jcs.paramTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jcs.JCS;
import org.apache.jcs.access.exception.CacheException;
import org.apache.jcs.engine.stats.behavior.IStatElement;
import org.apache.jcs.engine.stats.behavior.IStats;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import it.uniroma2.dicii.isw2.jcs.paramTests.JCSThrashTest.Executable;

/**
 * This is based on a test that was posted to the user's list:
 * http://www.opensubscriber.com/message/jcs-users@jakarta.apache.org/2435965.html
 *
 */

@RunWith(Parameterized.class)
@Category(JUnitTest.class)
public class JCSThrashTest {
	
	private static final Log LOG = LogFactory.getLog( JCSThrashTest.class.getName() );
	protected static JCS jcs;
	private int numThreads;
	private int numKeys;
	
	public JCSThrashTest(int numThreads, int numKeys) {
		this.numKeys = numKeys;
		this.numThreads = numThreads;
	}
	
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {15,500},{-1,-1},{50,5000},{0,0},{0,-1},
        });
    }
	
	@BeforeClass
	public static void configure() throws CacheException {
        JCS.setConfigFilename("/TestThrash.ccf");
		jcs = JCS.getInstance("testcache");
	}
	
    @AfterClass
    public static void tearDown() throws Exception {
        jcs.clear();
        jcs.dispose();
    }
    
    
    /*
     *  I added this method because testRemove fails if runned after testPut. This
     *  because the assertions in testRemove are based on the assumption that the list of entry is empty
     *  while testPut doesn't clear the jcs entries. 
     */
    @After
    public void cleanCache() throws Exception {
    	jcs.clear();
    }


    @Test
    public void testPut() throws Exception {
        final String value = "value";
        final String key = "key";

        // Make sure the element is not found
        assertEquals( 0, getListSize() );
        assertNull( jcs.get( key ) );

        jcs.put( key, value );

        // Get the element
        LOG.info( "jcs.getStats(): " + jcs.getStatistics() );
        
        // Verify that the element has been correctly inserted.
        assertEquals( 1, getListSize() );
        assertNotNull( jcs.get( key ) );
        assertEquals( value, jcs.get( key ) );
    }

   
    
    /**
     * Test elements can be removed from the store
     * @throws Exception
     */
    @Test
    public void testRemove() throws Exception {
        jcs.put( "key1", "value1" );
        assertEquals( 1, getListSize() );

        jcs.remove( "key1" );
        assertEquals( 0, getListSize() );

        jcs.put( "key2", "value2" );
        jcs.put( "key3", "value3" );
        assertEquals( 2, getListSize() );

        jcs.remove( "key2" );
        assertEquals( 1, getListSize() );

        // Try to remove an object that is not there in the store
        jcs.remove( "key4" );
        assertEquals( 1, getListSize() );
    }

    
    /**
     * This does a bunch of work and then verifies that the memory has not grown by much.
     * Most of the time the amount of memory used after the test is less.
     *
     * @throws Exception
     */
    @Test
    public void testForMemoryLeaks()
        throws Exception
    {
        long differenceMemoryCache = thrashCache();
        LOG.info( "Memory Difference is: " + differenceMemoryCache );
        assertTrue( differenceMemoryCache < 500000 );

        //LOG.info( "Memory Used is: " + measureMemoryUse() );
    }

    /**
     * @return
     * @throws Exception
     */
    public long thrashCache() throws Exception {

        long startingSize = measureMemoryUse();
        LOG.info( "Memory Used is: " + startingSize );

        final String value = "value";
        final String key = "key";

        // Add the entry
        jcs.put( key, value );

        // Create 15 threads that read the keys;
        final List<Executable> executables = new ArrayList<Executable>();
        for ( int i = 0; i < numThreads; i++ )
        {
            final JCSThrashTest.Executable executable = new JCSThrashTest.Executable()
            {
                public void execute()
                    throws Exception
                {
                    for ( int i = 0; i < numKeys; i++ )
                    {
                        final String key = "key" + i;
                        jcs.get( key );
                    }
                    jcs.get( "key" );
                }
            };
            executables.add( executable );
        }

        // Create 15 threads that are insert 500 keys with large byte[] as
        // values
        for ( int i = 0; i < numThreads; i++ )
        {
            final JCSThrashTest.Executable executable = new JCSThrashTest.Executable()
            {
                public void execute()
                    throws Exception
                {

                    // Add a bunch of entries
                    for ( int i = 0; i < numKeys; i++ )
                    {
                        // Use a random length value
                        final String key = "key" + i;
                        byte[] value = new byte[10000];
                        jcs.put( key, value );
                    }
                }
            };
            executables.add( executable );
        }

        runThreads( executables );
        jcs.clear();

        long finishingSize = measureMemoryUse();
        LOG.info( "Memory Used is: " + finishingSize );
        return finishingSize - startingSize;
    }

    
    /**
     * Runs a set of threads, for a fixed amount of time.
     * @param executables
     * @throws Exception
     */
    public void runThreads( final List<Executable> executables )
        throws Exception
    {

        final long endTime = System.currentTimeMillis() + 10000;
        final Throwable[] errors = new Throwable[1];

        // Spin up the threads
        final Thread[] threads = new Thread[executables.size()];
        for ( int i = 0; i < threads.length; i++ )
        {
            final JCSThrashTest.Executable executable = (JCSThrashTest.Executable) executables.get( i );
            threads[i] = new Thread()
            {
                public void run()
                {
                    try
                    {
                        // Run the thread until the given end time
                        while ( System.currentTimeMillis() < endTime )
                        {
                            executable.execute();
                        }
                    }
                    catch ( Throwable t )
                    {
                        // Hang on to any errors
                        errors[0] = t;
                    }
                }
            };
            threads[i].start();
        }

        // Wait for the threads to finish
        for ( int i = 0; i < threads.length; i++ )
        {
            threads[i].join();
        }

        // Throw any error that happened
        if ( errors[0] != null )
        {
            throw new Exception( "Test thread failed.", errors[0] );
        }
    }

    
    /**
     * Measure memory used by the VM.
     *
     * @return
     * @throws InterruptedException
     */
    protected long measureMemoryUse() throws InterruptedException {
        System.gc();
        Thread.sleep( 3000 );
        System.gc();
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }


    /**
     *
     * @return
     */
    private int getListSize()
    {
        final String listSize = "List Size";
        final String lruMemoryCache = "LRU Memory Cache";
        String result = "0";
        IStats istats[] = jcs.getStatistics().getAuxiliaryCacheStats();
        for ( int i = 0; i < istats.length; i++ )
        {
            IStatElement statElements[] = istats[i].getStatElements();
            if ( lruMemoryCache.equals( istats[i].getTypeName() ) )
            {
                for ( int j = 0; j < statElements.length; j++ )
                {
                    if ( listSize.equals( statElements[j].getName() ) )
                    {
                        result = statElements[j].getData();
                    }
                }
            }

        }
        return Integer.parseInt( result );
    }
    
    
    
    /**
     * A runnable, that can throw an exception.
     */   
    protected interface Executable
    {
    	/**
    	 * Executes this object.
    	 *
    	 * @throws Exception
    	 */
    	void execute()
    			throws Exception;
    }
}

