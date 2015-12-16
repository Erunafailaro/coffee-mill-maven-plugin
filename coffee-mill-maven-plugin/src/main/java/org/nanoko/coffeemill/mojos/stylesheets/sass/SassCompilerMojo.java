package org.nanoko.coffeemill.mojos.stylesheets.sass;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.nanoko.maven.WatchingException;
import org.nanoko.coffeemill.mojos.AbstractCoffeeMillWatcherMojo;
import org.nanoko.coffeemill.utils.FSUtils;
import org.nanoko.coffeemill.utils.MavenLoggerWrapper;
import org.nanoko.java.NPM;


import java.io.File;
import java.util.Collection;

import static org.nanoko.java.NPM.npm;

/**
 * Compiles SASS files.
 */
@Mojo(name = "compile-sass", threadSafe = false,
requiresDependencyResolution = ResolutionScope.COMPILE,
requiresProject = true,
defaultPhase = LifecyclePhase.COMPILE)
public class SassCompilerMojo extends AbstractCoffeeMillWatcherMojo {

    public static final String SASS_NPM_NAME = "node-sass";
    public static final String SASS_NPM_VERSION = "3.4.2";

    private NPM sass;


    public void execute() throws MojoExecutionException {    	
        if(isSkipped()) { 
            return; 
        }

        sass = npm(new MavenLoggerWrapper(this.getLog()), SASS_NPM_NAME, SASS_NPM_VERSION);
        try {
            if ( getStylesheetsDir().isDirectory()) {
                getLog().info("Compiling sass files from " + getStylesheetsDir().getAbsolutePath());
                Collection<File> files = FileUtils.listFiles(getStylesheetsDir(), new String[]{"scss"}, true);
                for (File file : files) {
                    if (file.isFile()) {
                        compile(file);
                    }
                }
            }
        } catch (WatchingException e) {
            throw new MojoExecutionException("Error during execute() on SassCompilerMojo : cannot compile", e);
        }
    }


    public boolean accept(File file) {
        return !isSkipped()
                && file.getParent().contains( getStylesheetsDir().getAbsolutePath() )
                && FSUtils.hasExtension(file, "scss");
    }

    public boolean fileCreated(File file) throws WatchingException {
        compile(file);
        return true;
    }

    public boolean fileUpdated(File file) throws WatchingException {
        compile(file);
        return true;
    }

    public boolean fileDeleted(File file) {
        File out = FSUtils.computeRelativeFile(file, this.getStylesheetsDir(), getWorkDirectory());
        File newName = new File( out.getAbsolutePath().substring(0, out.getAbsolutePath().length() - ".scss".length()) + ".css" );
        if(newName.exists()){
            FileUtils.deleteQuietly(newName);
        }
        return true;
    }
  

    private void compile(File file) throws WatchingException {
        File out = FSUtils.computeRelativeFile(file, this.getStylesheetsDir(), getWorkDirectory());
        String newName = out.getAbsolutePath().substring(0, out.getAbsolutePath().length() - ".scss".length()) + ".css";
        getLog().info("Compiling " + file.getAbsolutePath() + " to " + newName);
        try {
            int exit = sass.execute("node-sass", file.getAbsolutePath(), newName);
            getLog().debug("Sass execution exiting with " + exit + " status");
        } catch (MojoExecutionException e) {
            throw new WatchingException("Error during the compilation of " + file.getName(), e);
        }
        

        if (!new File(newName).exists()) {
            throw new WatchingException("Error during the compilation of " + file.getAbsoluteFile() + " check log");
        }
    }

    private boolean isSkipped(){
        if (skipCssCompilation) {
            getLog().info("\033[31m Sass Compilation skipped \033[0m");
            return true;
        } else {
            return false;
        }
    }

}
