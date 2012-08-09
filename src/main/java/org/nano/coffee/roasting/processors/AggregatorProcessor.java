package org.nano.coffee.roasting.processors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Common facet of aggregator.
 */
public abstract class AggregatorProcessor implements Processor {

    public static Logger logger = LoggerFactory.getLogger(AggregatorProcessor.class);

    public List<File> computeFileList(List<String> names, File directory, String extension,
                                      boolean failedOnMissingFile) throws MojoExecutionException {
        List<File> result = new ArrayList<File>();

        if (names == null || names.isEmpty()) {
            if (directory.exists()) {
                result.addAll(FileUtils.listFiles(directory, new String[]{extension}, true));
            } else {
                // Else we just skip.
                logger.debug("Aggregation skipped - no files to aggregate");
                return result;
            }
        } else {
            if (!directory.exists()) {
                throw new MojoExecutionException("Aggregation failed : " + directory.getAbsolutePath() + " does not exist");
            }

            for (String name : names) {
                File file = new File(directory, name);
                if (!file.exists()) {
                    file = new File(directory, name + ".js");
                    if (!file.exists())
                        if (failedOnMissingFile) {
                            throw new MojoExecutionException("Aggregation failed : " + name + " file missing in " + directory
                                    .getAbsolutePath());
                        } else {
                            logger.warn("Issue detected during aggregation : " + name + " missing");
                        }
                }
                // The file exists.
                result.add(file);
            }
        }

        return result;
    }

    public void aggregate(List<File> files, File to) throws FileNotFoundException, MojoExecutionException {
        if (files.isEmpty()) {
            return;
        }

        logger.info("Aggregating  " + files.size() + " into " + to.getAbsolutePath());
        to.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(to);
        try {
            for (File file : files) {
                if (file.getPath().equals(to.getPath())) {
                    continue;
                }
                logger.debug("Copying " + file.getAbsolutePath() + " to " + to.getName());
                FileInputStream in = new FileInputStream(file);
                try {
                    IOUtils.copy(in, out);
                    separator(out);
                } catch (IOException e) {
                    logger.error("Aggregation failed : Cannot build aggregate file - " + e.getMessage());
                    throw new MojoExecutionException("Aggregation failed : cannot build aggregate file", e);
                } finally {
                    IOUtils.closeQuietly(in);
                }
            }
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public abstract void separator(OutputStream out) throws IOException;

}