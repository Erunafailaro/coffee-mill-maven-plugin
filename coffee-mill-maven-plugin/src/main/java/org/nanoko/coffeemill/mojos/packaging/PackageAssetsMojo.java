/*
 * Copyright 2013 OW2 Nanoko Project
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

package org.nanoko.coffeemill.mojos.packaging;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.nanoko.coffeemill.mojos.AbstractCoffeeMillWatcherMojo;
import org.nanoko.coffeemill.utils.FSUtils;
import org.nanoko.maven.WatchingException;

/**
 * A processor optimizing JPEG files using JpegTran.
 * JpegTran must be installed on the system and available from the path.
 * -> to install : http://saralinux.blogspot.fr/2013/12/installing-jpegtran-on-mac-or-unixlinux.html
 */
@Mojo(name = "package-assets", threadSafe = false,
requiresDependencyResolution = ResolutionScope.COMPILE,
requiresProject = true,
defaultPhase = LifecyclePhase.PACKAGE)
public class PackageAssetsMojo extends AbstractCoffeeMillWatcherMojo {

	@Parameter(defaultValue="false")
	protected boolean skipAssetsPackage;

    public void execute() throws MojoExecutionException {
    	if(isSkipped())
    		return;
    	if (!this.getWorkDirectory().exists()){
        	getLog().warn("/!\\ Packaging assets skipped - " + this.getWorkDirectory().getAbsolutePath() + " does not exist !");
        	return;
        }
    	
    	File[] assets = getWorkDirectory().listFiles();
    	try {
	    	for(File file : assets){
	    		getLog().info("file : "+file.getAbsolutePath());
	    		if(file.isDirectory()){
	    			getLog().info("isDirectory");
		    		FileUtils.copyDirectoryToDirectory(file, getBuildDirectory());					
	    		}else{
	    			getLog().info("else");
	    			if(file.isFile() && !FSUtils.hasExtension(file, "js","css")){
	    				getLog().info("isnot js/css file");
	    				FileUtils.copyFileToDirectory(file, getBuildDirectory());
	    			}
	    		}
	    	}
    	} catch (IOException e) {
			e.printStackTrace();
		}
    	/*
    	File AssetsResourcesWorkDirectory = new File(this.getWorkDirectory(), "resources");
    	File AssetsResourcesBuildDirectory = new File(this.getBuildDirectory(), "resources");
    	FileFilter htmlFilter = FileFilterUtils.suffixFileFilter(".html");
    	
    	if(!AssetsResourcesWorkDirectory.exists())
    		return;
    	
    	try {   
    		// Copy assets resources to build directory
        	FileUtils.copyDirectory(AssetsResourcesWorkDirectory, AssetsResourcesBuildDirectory);
        	// Copy all html files to build directory (using filter)
        	FileUtils.copyDirectory(this.getWorkDirectory(), this.getBuildDirectory(), htmlFilter);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }*/
       
    }
    
    public boolean accept(File file) {
          return !isSkipped() && file.getParent().contains( getAssetsDir().getAbsolutePath() );
    }
    

    private void packageAssetFile(File file) throws WatchingException {
    	getLog().info("Packaging Asset file "+file.getName()
    			+" to "+this.getBuildDirectory().getAbsolutePath()  );
    	try {    		
    		File relativeWorkFile = FSUtils.computeRelativeFile(file, this.getAssetsDir(), this.getWorkDirectory());
    		File relativeBuildFile = FSUtils.computeRelativeFile(file, this.getAssetsDir(), this.getBuildDirectory());	
    		
    		if (relativeBuildFile.getParentFile() != null) {
    			relativeBuildFile.getParentFile().mkdirs();
                FileUtils.copyFileToDirectory(relativeWorkFile, relativeBuildFile.getParentFile());
            } else 
                getLog().error("Cannot copy file - parent directory not accessible for " + relativeBuildFile);
			
		} catch (IOException e) {
			throw new WatchingException(e.getMessage(), e); }
    }
    
    
    public boolean fileCreated(File file) throws WatchingException {
    	packageAssetFile(file);
    	return true;
    }

    public boolean fileUpdated(File file) throws WatchingException {
		packageAssetFile(file);
    	return true;
    }
    
    public boolean fileDeleted(File file) throws WatchingException {
    	File deletedFromBuild = FSUtils.computeRelativeFile(file, this.getAssetsDir(), this.getBuildDirectory());
        if (deletedFromBuild.isFile()){
        	getLog().info("deleting File : "+file.getName()+" from "+this.getBuildDirectory());    	
        	FileUtils.deleteQuietly(deletedFromBuild); 
        }
        return true;
    }
    
    private boolean isSkipped(){
    	if (skipAssetsPackage) {
            getLog().info("\033[31m Asset packaging skipped \033[37m");
            return true;
        }
    	else return false;
    }
    
    
    
    
}