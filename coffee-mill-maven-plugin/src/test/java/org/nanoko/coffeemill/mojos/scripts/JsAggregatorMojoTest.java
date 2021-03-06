/*
 * Copyright 2013-2014 OW2 Nanoko Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nanoko.coffeemill.mojos.scripts;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nanoko.coffeemill.mojos.scripts.js.JsAggregatorMojo;

/**
 * Test the CoffeeScriptCompilerMojo.
 */
public class JsAggregatorMojoTest {

	private final File jsSourceTestDir = new File("src/test/resources/js");
	private final String testDir = "target/test/JsAggregatorMojoTest";
	private final File workDir = new File(testDir, "www");
	private final File buildDir = new File(testDir, "www-release");
	private final File libDir = new File(workDir, "libs");
	
	private JsAggregatorMojo mojo;
	
	
	@Before
	public void prepareTestDirectory() throws MojoExecutionException{
    	this.mojo = new JsAggregatorMojo();     
    	this.mojo.setJavaScriptDir( this.jsSourceTestDir );
    	this.mojo.setWorkDirectory(this.workDir);
    	this.mojo.setBuildDirectory(this.buildDir);
    	this.mojo.setLibDirectory(this.libDir);
        
        Collection<File> files = FileUtils.listFiles(this.mojo.getJavaScriptDir(), new String[]{"js"}, true);    	
        for(File file : files){
			try {
				FileUtils.copyFileToDirectory(file, this.workDir);
			} catch (IOException e) { 
				throw new MojoExecutionException("Cannot copy file to prepare JsAggregatorMojoTest", e); 
			}        
        }
    }
	
	
    @Test
    public void testJavaScriptAggregation() throws MojoExecutionException {
    	System.out.println("\n ==> Should aggregate 2 files \"test.js\" and \"test2.js\" to "+this.workDir);

    	this.mojo.execute();    	
    	assertTrue(new File(this.mojo.getWorkDirectory(), "test.js").exists());
    	assertTrue(new File(this.mojo.getWorkDirectory(), "test2.js").exists());
    	assertTrue(new File(this.mojo.getWorkDirectory(), this.mojo.getDefaultOutputFilename()+".js").exists());
    }
    
    
    @Test
    public void testSkippedJavaScriptAggregationBecauseOfEmptyFolder() throws MojoExecutionException {
    	System.out.println("\n ==> Should aggregate nothing : Empty Folder.");
    	
    	this.mojo.setWorkDirectory( new File(this.testDir, "empty") );
    	this.mojo.execute();
    	
    	assertTrue( this.mojo.getBuildDirectory().isDirectory()
    			&& this.mojo.getBuildDirectory().list().length==0 );
    }
    
    
    @After
	public void cleanTestDirectory() {
		if(this.mojo.getWorkDirectory().exists()){
			FileUtils.deleteQuietly(this.mojo.getWorkDirectory());
		}
		if(this.mojo.getBuildDirectory().exists()){
			FileUtils.deleteQuietly(this.mojo.getBuildDirectory());
		}
	}

}
