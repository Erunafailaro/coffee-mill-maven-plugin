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

package org.nanoko.coffeemill.mojos.processresources;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nanoko.coffeemill.mojos.processresources.CopyAssetsMojo;
import org.nanoko.coffeemill.mojos.processresources.OptiPngMojo;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class OptiPngMojoTest {
	
	private final File assetsSourceTestDir = new File("src/test/resources/assets");
	private final File workDir = new File("target/test/OptiPngMojoTest/www");
	private OptiPngMojo mojo;
	
	public OptiPngMojoTest() throws MojoExecutionException{
		CopyAssetsMojo copymojo = new CopyAssetsMojo();
		copymojo.setAssetsDir(assetsSourceTestDir);
		copymojo.setWorkDirectory(workDir);
		copymojo.execute();
	}	
	
	@Before
	public void prepareTestDirectory(){
    	this.mojo = new OptiPngMojo();     
    	this.mojo.setVerbose(true);
    	this.mojo.setWorkDirectory(this.workDir);
    	this.mojo.setAssetsDir(assetsSourceTestDir);
    }
	

    @Test
    public void testPNGOptimization() throws MojoExecutionException, MojoFailureException {
		System.out.println("\n ==> Should optimize the png test file (smaller file size).");

        File file = new File(mojo.getWorkDirectory(), "img/demo.png");
        long size = file.length();

        mojo.execute();

        file = new File(mojo.getWorkDirectory(), "img/demo.png");
        long newSize = file.length();

        // Optimization, so the new size is smaller.
        assertTrue(newSize < size);
    }

    @Test
    public void testPNGOptimizationWhenOptiPNGIsNotInstalled() throws MojoExecutionException,
            MojoFailureException {
    	System.out.println("\n ==> Should not optimize the test png file : should not find \"do_not_exist\" executable.");

        String name = OptiPngMojo.getExecutableName();
        OptiPngMojo.setExecutableName("do_not_exist");

        File file = new File(mojo.getWorkDirectory(), "img/demo.png");
        long size = file.length();

        mojo.execute();

        long newSize = file.length();

        // Nothing happens.
        assertTrue(newSize == size);

        OptiPngMojo.setExecutableName(name);
    }
    
    @After
	public void cleanTestDirectory() {
		if(this.mojo.getWorkDirectory().exists()){
			FileUtils.deleteQuietly(this.mojo.getWorkDirectory());
		}
	}
    
}