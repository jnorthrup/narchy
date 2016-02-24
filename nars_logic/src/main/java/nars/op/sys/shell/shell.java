package nars.op.sys.shell;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.*;

import com.google.common.collect.Lists;
import com.gs.collections.api.set.ImmutableSet;
import com.gs.collections.impl.factory.Sets;
import nars.$;
import nars.NAR;
import nars.op.sys.java.Lobjects;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.operations.FileOperationProvider;

/**
 * Created by me on 2/24/16.
 */
public class shell {


    private final sh shell;

    public shell(NAR n) throws Exception {

        Lobjects l = new Lobjects(n);
        this.shell = l.the("I", sh.class);


    }



    /*
     * Licensed to the Apache Software Foundation (ASF) under one or more
     * contributor license agreements.  See the NOTICE file distributed with
     * this work for additional information regarding copyright ownership.
     * The ASF licenses this file to You under the Apache License, Version 2.0
     * (the "License"); you may not use this file except in compliance with
     * the License.  You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */

    /**
     * A simple command-line shell for performing file operations.
     * <p>
     * See
     * <a href="https://wiki.apache.org/commons/VfsExampleShell">Commons VFS Shell Examples</a>
     * in Apache Commons Wiki.
     */
    public static class sh {
        private final FileSystemManager mgr;
        private FileObject cwd;
        private final BufferedReader reader;

        public sh() throws IOException {
            mgr = VFS.getManager();
            cwd = mgr.toFileObject(new File(System.getProperty("user.dir")));
            reader = new BufferedReader(
                    new InputStreamReader(System.in/*, Charset.defaultCharset()*/));
        }

        public static void main(final String[] args) {
            try {
                new sh().repl();
            } catch (final Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
            System.exit(0);
        }

        private void repl() throws Exception {
            System.out.println("VFS Shell " + getVersion(nars.op.sys.shell.shell.class));
            while (true) {
                final String[] cmd = nextCommand();
                if (cmd == null) {
                    return;
                }
                if (cmd.length == 0) {
                    continue;
                }
                final String cmdName = cmd[0];
                if (cmdName.equalsIgnoreCase("exit") || cmdName.equalsIgnoreCase("quit")) {
                    return;
                }
                try {
                    handleCommand(cmd);
                } catch (final Exception e) {
                    System.err.println("Command failed:");
                    e.printStackTrace(System.err);
                }
            }
        }

        /**
         * Handles a command.
         */
        private void handleCommand(final String[] cmd) throws Exception {
            final String cmdName = cmd[0];
            if (cmdName.equalsIgnoreCase("cat")) {
                cat(cmd);
            } else if (cmdName.equalsIgnoreCase("cd")) {
                cd(cmd);
            } else if (cmdName.equalsIgnoreCase("cp")) {
                cp(cmd);
            } else if (cmdName.equalsIgnoreCase("help") || cmdName.equals("?")) {
                help();
            } else if (cmdName.equalsIgnoreCase("ls")) {
                ls(cmd);
            } else if (cmdName.equalsIgnoreCase("pwd")) {
                pwd();
            } else if (cmdName.equalsIgnoreCase("rm")) {
                rm(cmd);
            } else if (cmdName.equalsIgnoreCase("touch")) {
                touch(cmd);
            } else if (cmdName.equalsIgnoreCase("info")) {
                info(cmd);
            } else {
                System.err.println("Unknown command \"" + cmdName + "\" (Try 'help').");
            }
        }

        private void info(String[] cmd) throws Exception {
            if (cmd.length > 1) {
                info(cmd[1]);
            } else {
                System.out.println("Default manager: \"" + mgr.getClass().getName() + "\" " +
                        "version " + getVersion(mgr.getClass()));
                String[] schemes = mgr.getSchemes();
                List<String> virtual = new ArrayList<String>();
                List<String> physical = new ArrayList<String>();
                for (int i = 0; i < schemes.length; i++) {
                    Collection<Capability> caps = mgr.getProviderCapabilities(schemes[i]);
                    if (caps != null) {
                        if (caps.contains(Capability.VIRTUAL) ||
                                caps.contains(Capability.COMPRESS) ||
                                caps.contains(Capability.DISPATCHER)) {
                            virtual.add(schemes[i]);
                        } else {
                            physical.add(schemes[i]);
                        }
                    }
                }
                if (!physical.isEmpty()) {
                    System.out.println("  Provider Schemes: " + physical);
                }
                if (!virtual.isEmpty()) {
                    System.out.println("   Virtual Schemes: " + virtual);
                }
            }
        }

        private void info(String scheme) throws Exception {
            System.out.println("Provider Info for scheme \"" + scheme + "\":");
            Collection<Capability> caps;
            caps = mgr.getProviderCapabilities(scheme);
            if (caps != null && !caps.isEmpty()) {
                System.out.println("  capabilities: " + caps);
            }
            FileOperationProvider[] ops = mgr.getOperationProviders(scheme);
            if (ops != null && ops.length > 0) {
                System.out.println("  operations: " + ops);
            }
        }

        /**
         * Does a 'help' command.
         */
        private void help() {
            System.out.println("Commands:");
            System.out.println("cat <file>         Displays the contents of a file.");
            System.out.println("cd [folder]        Changes current folder.");
            System.out.println("cp <src> <dest>    Copies a file or folder.");
            System.out.println("help               Shows this message.");
            System.out.println("info [scheme]      Displays information about providers.");
            System.out.println("ls [-R] [path]     Lists contents of a file or folder.");
            System.out.println("pwd                Displays current folder.");
            System.out.println("rm <path>          Deletes a file or folder.");
            System.out.println("touch <path>       Sets the last-modified time of a file.");
            System.out.println("exit, quit         Exits this program.");
        }

        /**
         * Does an 'rm' command.
         */
        private void rm(final String[] cmd) throws Exception {
            if (cmd.length < 2) {
                throw new Exception("USAGE: rm <path>");
            }

            final FileObject file = resolveFileRelativeToCwd(cmd, 1);
            file.delete(Selectors.SELECT_SELF);
        }

        /**
         * Does a 'cp' command.
         */
        private void cp(final String[] cmd) throws Exception {
            if (cmd.length < 3) {
                throw new Exception("USAGE: cp <src> <dest>");
            }

            final FileObject src = resolveFileRelativeToCwd(cmd, 1);
            FileObject dest = resolveFileRelativeToCwd(cmd, 2);
            if (dest.exists() && dest.getType() == FileType.FOLDER) {
                dest = dest.resolveFile(src.getName().getBaseName());
            }

            dest.copyFrom(src, Selectors.SELECT_ALL);
        }

        /**
         * Does a 'cat' command.
         */
        private void cat(final String[] cmd) throws Exception {
            if (cmd.length < 2) {
                throw new Exception("USAGE: cat <path>");
            }

            // Locate the file
            final FileObject file = resolveFileRelativeToCwd(cmd, 1);

            // Dump the contents to System.out
            FileUtil.writeContent(file, System.out);
            System.out.println();
        }

        /**
         * Does a 'pwd' command.
         */
        public String pwd() {
            FileName c = cwd.getName();
            //System.out.println("Current folder is " + c);
            return c.toString();
        }

        /**
         * Does a 'cd' command.
         * If the taget directory does not exist, a message is printed to <code>System.err</code>.
         */
        private void cd(final String[] cmd) throws Exception {
            final String path;
            if (cmd.length > 1) {
                path = cmd[1];
            } else {
                path = System.getProperty("user.home");
            }

            // Locate and validate the folder
            final FileObject tmp = mgr.resolveFile(cwd, path);
            if (tmp.exists()) {
                cwd = tmp;
            } else {
                System.out.println("Folder does not exist: " + tmp.getName());
            }
            System.out.println("Current folder is " + cwd.getName());
        }


        /**
         * Does an 'ls' command.
         */
        private void ls(final String[] cmd) throws FileSystemException {
            int pos = 1;
            final boolean recursive;
            if (cmd.length > pos && cmd[pos].equals("-R")) {
                recursive = true;
                pos++;
            } else {
                recursive = false;
            }

            final FileObject file;
            if (cmd.length > pos) {
                file = resolveFileRelativeToCwd(cmd, pos);
            } else {
                file = cwd;
            }

            if (file.getType() == FileType.FOLDER) {
                // List the contents
                System.out.println("Contents of " + file.getName());
                listChildren(file, recursive, "");
            } else {
                // Stat the file
                System.out.println(file.getName());
                final FileContent content = file.getContent();

                System.out.println("Size: " + content.getSize() + " bytes.");
                final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
                final String lastMod = dateFormat.format(new Date(content.getLastModifiedTime()));
                System.out.println("Last modified: " + lastMod);
            }
        }

        private FileObject resolveFileRelativeToCwd(String[] cmd, int pos) throws FileSystemException {
            return mgr.resolveFile(cwd, cmd[pos]);
        }

        /**
         * Does a 'touch' command.
         */
        private void touch(final String[] cmd) throws Exception {
            if (cmd.length < 2) {
                throw new Exception("USAGE: touch <path>");
            }
            final FileObject file = resolveFileRelativeToCwd(cmd, 1);
            if (!file.exists()) {
                file.createFile();
            }
            file.getContent().setLastModifiedTime(System.currentTimeMillis());
        }

        public Object ls() {
            try {
                final FileObject file = cwd;

                        //resolveFileRelativeToCwd(new String[] { "." }, 0);
                if (file.getType() == FileType.FOLDER) {
                    return $.seteCollection(Lists.newArrayList(file.getChildren()));
                } else {
                    return file;
                }
            } catch (Exception e) {
                return null; //throw new RuntimeException(e.getCause());
            }
        }
        /**
         * Lists the children of a folder.
         */
        private void listChildren(final FileObject dir,
                                  final boolean recursive,
                                  final String prefix)
                throws FileSystemException {
            final FileObject[] children = dir.getChildren();
            for (final FileObject child : children) {
                System.out.print(prefix);
                System.out.print(child.getName().getBaseName());
                if (child.getType() == FileType.FOLDER) {
                    System.out.println("/");
                    if (recursive) {
                        listChildren(child, recursive, prefix + "    ");
                    }
                } else {
                    System.out.println();
                }
            }

        }

        /**
         * Returns the next command, split into tokens.
         */
        private String[] nextCommand() throws IOException {
            System.out.print("> ");
            final String line = reader.readLine();
            if (line == null) {
                return null;
            }
            final ArrayList<String> cmd = new ArrayList<String>();
            final StringTokenizer tokens = new StringTokenizer(line);
            while (tokens.hasMoreTokens()) {
                cmd.add(tokens.nextToken());
            }
            return cmd.toArray(new String[cmd.size()]);
        }

        private static String getVersion(Class<?> cls) {
            try {
                return cls.getPackage().getImplementationVersion();
            } catch (Exception ignored) {
                return "N/A";
            }
        }
    }
}
