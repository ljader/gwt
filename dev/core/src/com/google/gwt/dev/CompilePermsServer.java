/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.jjs.UnifiedAst;
import com.google.gwt.dev.util.arg.ArgHandlerLogLevel;
import com.google.gwt.dev.util.arg.OptionLogLevel;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.ToolBase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * An out-of-process implementation of CompilePerms that will connect back to an
 * existing compiler host. This class is intended to be launched by
 * {@link ExternalPermutationWorkerFactory} and not by users directly.
 */
public class CompilePermsServer {
  /**
   * Adds host and port information.
   */
  public interface CompileServerOptions extends OptionLogLevel {
    String getCompileHost();

    int getCompilePort();

    String getCookie();

    void setCompileHost(String host);

    void setCompilePort(int port);

    void setCookie(String cookie);
  }

  static final class ArgHandlerCompileHost extends ArgHandlerString {
    private final CompileServerOptions options;

    public ArgHandlerCompileHost(CompileServerOptions options) {
      this.options = options;
    }

    @Override
    public String getPurpose() {
      return "The host to which the permutation server should connect";
    }

    @Override
    public String getTag() {
      return "-host";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"hostname"};
    }

    @Override
    public boolean isRequired() {
      return true;
    }

    @Override
    public boolean setString(String str) {
      options.setCompileHost(str);
      return true;
    }
  }

  static final class ArgHandlerCompilePort extends ArgHandlerString {
    private final CompileServerOptions options;

    public ArgHandlerCompilePort(CompileServerOptions options) {
      this.options = options;
    }

    @Override
    public String getPurpose() {
      return "The port to which the permutation server should connect";
    }

    @Override
    public String getTag() {
      return "-port";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"1234"};
    }

    @Override
    public boolean isRequired() {
      return true;
    }

    @Override
    public boolean setString(String str) {
      Integer port = Integer.parseInt(str);
      if (port <= 0) {
        return false;
      }
      options.setCompilePort(port);
      return true;
    }
  }

  static final class ArgHandlerCookie extends ArgHandlerString {

    private final CompileServerOptions options;

    public ArgHandlerCookie(CompileServerOptions option) {
      this.options = option;
    }

    @Override
    public String getPurpose() {
      return "Specifies the security cookie the server expects";
    }

    @Override
    public String getTag() {
      return "-cookie";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"cookie"};
    }

    @Override
    public boolean isRequired() {
      return true;
    }

    @Override
    public boolean setString(String str) {
      options.setCookie(str);
      return true;
    }
  }

  static class ArgProcessor extends ToolBase {
    public ArgProcessor(CompileServerOptions options) {
      registerHandler(new ArgHandlerLogLevel(options));
      registerHandler(new ArgHandlerCompileHost(options));
      registerHandler(new ArgHandlerCompilePort(options));
      registerHandler(new ArgHandlerCookie(options));
    }

    /*
     * Overridden to make public.
     */
    @Override
    public final boolean processArgs(String[] args) {
      return super.processArgs(args);
    }

    @Override
    protected String getName() {
      return CompilePermsServer.class.getName();
    }
  }

  static class CompileServerOptionsImpl implements CompileServerOptions {

    private String compileHost;
    private int compilePort;
    private String cookie;
    private Type logLevel;

    public void copyFrom(CompileServerOptions other) {
      setCompileHost(other.getCompileHost());
      setCompilePort(other.getCompilePort());
      setCookie(other.getCookie());
      setLogLevel(other.getLogLevel());
    }

    public String getCompileHost() {
      return compileHost;
    }

    public int getCompilePort() {
      return compilePort;
    }

    public String getCookie() {
      return cookie;
    }

    public Type getLogLevel() {
      return logLevel;
    }

    public void setCompileHost(String host) {
      assert host != null;
      compileHost = host;
    }

    public void setCompilePort(int port) {
      assert port > 0;
      compilePort = port;
    }

    public void setCookie(String cookie) {
      this.cookie = cookie;
    }

    public void setLogLevel(Type logLevel) {
      this.logLevel = logLevel;
    }
  }

  public static void main(String[] args) {
    final CompileServerOptions options = new CompileServerOptionsImpl();
    if (new ArgProcessor(options).processArgs(args)) {
      PrintWriterTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(options.getLogLevel());
      if (run(options, logger)) {
        System.exit(0);
      }
    }
    System.exit(-1);
  }

  public static boolean run(CompileServerOptions options, TreeLogger logger) {
    try {
      Socket s = new Socket(options.getCompileHost(), options.getCompilePort());
      logger.log(TreeLogger.DEBUG, "Socket opened");

      ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
      ObjectInputStream in = new ObjectInputStream(s.getInputStream());

      // Write my cookie
      out.writeUTF(options.getCookie());
      out.flush();

      // Read the File that contains the serialized UnifiedAst
      File astFile = (File) in.readObject();
      ObjectInputStream astIn = new ObjectInputStream(new FileInputStream(
          astFile));
      UnifiedAst ast = (UnifiedAst) astIn.readObject();
      ast.prepare();
      logger.log(TreeLogger.SPAM, "Created new UnifiedAst instance");

      // Report on the amount of memory we think we're using
      long estimatedMemory = Runtime.getRuntime().totalMemory()
          - Runtime.getRuntime().freeMemory();
      out.writeLong(estimatedMemory);
      out.flush();

      boolean keepGoing = in.readBoolean();
      while (keepGoing) {
        compilePermutation(logger, ast, in, out);

        keepGoing = in.readBoolean();
        logger.log(TreeLogger.SPAM, "keepGoing = " + keepGoing);
      }

      logger.log(TreeLogger.DEBUG, "Successfully terminating");
      return true;

    } catch (UnknownHostException e) {
      logger.log(TreeLogger.ERROR, "Invalid hostname", e);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Communication error", e);
    } catch (ClassNotFoundException e) {
      logger.log(TreeLogger.ERROR, "Probable client/server mismatch or "
          + "classpath misconfiguration", e);
    }
    return false;
  }

  static void compilePermutation(TreeLogger logger, UnifiedAst ast,
      ObjectInputStream in, ObjectOutputStream out)
      throws ClassNotFoundException, IOException {
    Permutation p = (Permutation) in.readObject();
    logger.log(TreeLogger.SPAM, "Permutation read");

    PermutationResult result = CompilePerms.compile(logger.branch(
        TreeLogger.DEBUG, "Compiling"), p, ast);
    logger.log(TreeLogger.DEBUG, "Successfully compiled permutation");
    out.writeObject(result);
    out.flush();
    logger.log(TreeLogger.SPAM, "Sent result");
  }
}
