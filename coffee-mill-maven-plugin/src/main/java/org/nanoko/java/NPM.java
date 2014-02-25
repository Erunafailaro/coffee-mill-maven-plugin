package org.nanoko.java;


import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import org.apache.commons.logging.Log;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Manage an execution of NPM
 */
public class NPM {

    public static final String PACKAGE_JSON = "package.json";
    private final String npmName;
    private final String npmVersion;

    private final NodeManager node;

    private static Log log;

    /**
     * Constructor used to install an NPM.
     * @param log the logger
     * @param manager the node manager
     * @param name the NPM name
     * @param version the NPM version
     */
    private NPM(NodeManager manager, String name, String version) {
        this.node = manager;
        this.npmName = name;
        this.npmVersion = version;
        log = NodeManager.getLog();
        ensureNodeInstalled();
    }

    private void ensureNodeInstalled() {
        try {
            node.installIfNotInstalled();
        } catch (IOException e) {
            log.error("Cannot install node", e);
        }
    }

    /**
     * Executes the current NPM.
     * NPM can have several executable attached to them, so the 'binary' argument specify which one has to be
     * executed. Check the 'bin' entry of the package.json file to determine which one you need. 'Binary' is the key
     * associated with the executable to invoke. For example, in
     * <code>
     *     <pre>
     *      "bin": {
     *           "coffee": "./bin/coffee",
     *           "cake": "./bin/cake"
     *      },
     *     </pre>
     * </code>
     *
     * we have two alternatives: 'coffee' and 'cake'.
     * @param binary the key of the binary to invoke
     * @param args the arguments
     * @return the execution exit status
     * @throws MojoExecutionException if the execution failed
     */
    public int execute(String binary, String... args)  {
        File destination = getNPMDirectory();
        if (! destination.isDirectory()) {
            throw new IllegalStateException("NPM " + this.npmName + " not installed");
        }

        CommandLine cmdLine = new CommandLine(node.getNodeExecutable());
        File npmExec = null;
        try {
            npmExec = findExecutable(binary);
        } catch (IOException | ParseException e) { //NOSONAR
            log.error("NPM::execute::findExecutable ", e);
        }
        if (npmExec == null) {
            throw new IllegalStateException("Cannot execute NPM " + this.npmName + " - cannot find the JavaScript file " +
                    "matching " + binary + " in the " + PACKAGE_JSON + " file");
        }

        // NPM is launched using the main file.
        cmdLine.addArgument(npmExec.getAbsolutePath(), false);
        for (String arg : args) {            
            cmdLine.addArgument(arg.replaceAll(" ","\\ "), false);// escape whitespaces in files path
        }

        DefaultExecutor executor = new DefaultExecutor();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PumpStreamHandler psh = new PumpStreamHandler(bos, System.out);
        executor.setStreamHandler(psh);     

        executor.setExitValue(0);

        try {
            int res = executor.execute(cmdLine);
            bos.close();
            log.info(bos.toString());
            return res;
        } catch (IOException e) {
            log.error("NPM::execute::execute ", e);
            return -1;
        }

    }

    /**
     * Try to find the main JS file.
     * This search is based on the `package.json` file and it's `bin` entry.
     * If there is an entry in the `bin` object matching `binary`, it uses this javascript file.
     * If the search failed, `null` is returned
     * @return the JavaScript file to execute, null if not found
     */
    protected File findExecutable(String binary) throws IOException, ParseException {
        File npmDirectory = getNPMDirectory();
        File packageFile = new File(npmDirectory, PACKAGE_JSON);
        if (! packageFile.isFile()) {
            throw new IllegalStateException("Invalid NPM " + npmName + " - " + packageFile.getAbsolutePath() + " does not" +
                    " exist");
        }
        JSONObject json = (JSONObject) JSONValue.parseWithException(new FileReader(packageFile));
        JSONObject bin = (JSONObject) json.get("bin");
        if (bin == null) {
            log.error("No `bin` object in " + packageFile.getAbsolutePath());
            return null;
        } else {
            String exec = (String) bin.get(binary);
            if (exec == null) {
                log.error("No `" + binary + "` object in the `bin` object from " + packageFile
                        .getAbsolutePath());
                return null;
            }
            File file = new File(npmDirectory, exec);
            if (! file.isFile()) {
                log.error("A matching javascript file was found for " + binary + " but the file does " +
                        "not exist - " + file.getAbsolutePath());
                return null;
            }
            return file;
        }

    }

    private File getNPMDirectory() {
        return new File(node.getNodeModulesDirectory(), npmName);
    }

    private void install() {
        File directory = getNPMDirectory();
        if (directory.isDirectory()) {
            // Check the version
            String version = getVersionFromNPM(directory);
            String warnMessage = "NPM " + npmName + " already installed in " + directory.getAbsolutePath() + " (" + version + ")";
            // Are we looking for a specific version ?
            if (npmVersion != null) {
                // Yes
                if (! npmVersion.equals(version)) {
                    log.warn(warnMessage+"but not in the requested version (requested: " + npmVersion + ") - uninstall it");
                    try {
                        FileUtils.deleteDirectory(directory);
                    } catch (IOException e) { //NOSONAR
                        // ignore it.
                    }
                } else {
                    log.warn(warnMessage);
                    return;
                }
            } else {
                // No
                log.warn(warnMessage);
                return;
            }
        }

        CommandLine cmdLine = new CommandLine(node.getNodeExecutable());
        File npmCli = new File(node.getNodeModulesDirectory(), "npm/bin/npm-cli.js");
        // NPM is launched using the main file, also disable the auto-quoting
        cmdLine.addArgument(npmCli.getAbsolutePath(), false);
        cmdLine.addArgument("install");
        cmdLine.addArgument("-g");
        if (npmVersion != null) {
            cmdLine.addArgument(npmName + "@" + npmVersion);
        } else {
            cmdLine.addArgument(npmName);
        }

        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(0);



        log.debug("Executing " + cmdLine.toString());

        try {
            executor.execute(cmdLine);
        } catch (IOException e) {
            log.error("Error during the installation of the NPM " + npmName + " - check log", e);
        }
    }

    public static String getVersionFromNPM(File npmDirectory) {
        File packageFile = new File(npmDirectory, PACKAGE_JSON);
        if (!packageFile.isFile()) {
            return "0.0.0";
        }

        FileReader reader = null;
        try {
            reader = new FileReader(packageFile);  //NOSONAR
            JSONObject json = (JSONObject) JSONValue.parseWithException(reader);
            return (String) json.get("version");
        } catch (IOException | ParseException e) {
            log.error("Cannot extract version from " + packageFile.getAbsolutePath(), e);
        } finally {
            IOUtils.closeQuietly(reader);
        }

        return null;
    }

    /**
     * Creates an NPM object based on the NPM's name and version (or tag).
     * If the NPM is not installed, it installs it.
     * There returned NPM let you execute it.
     * @param mojo the Wisdom Mojo
     * @param name the NPM name
     * @param version the NPM version or tag
     * @return the NPM object. The NPM may have been installed if it was not installed or installed in another version.
     */
    public static NPM npm( Log customLog, String name, String version) {
        NodeManager.setLog(customLog);
        NPM npm = new NPM( NodeManager.getInstance(), name, version);
        npm.install();
        return npm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }

        NPM npm = (NPM) o;

        return npmName.equals(npm.npmName)
                && !(npmVersion != null ? !npmVersion.equals(npm.npmVersion) : npm.npmVersion != null);

    }

    @Override
    public int hashCode() {
        int result = npmName.hashCode();
        result = 31 * result + (npmVersion != null ? npmVersion.hashCode() : 0);
        return result;
    }
}
