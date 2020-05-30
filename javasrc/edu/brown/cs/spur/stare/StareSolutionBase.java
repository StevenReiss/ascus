/********************************************************************************/
/*                                                                              */
/*              StareSolutionBase.java                                          */
/*                                                                              */
/*      Hold information about a potential solution                             */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.spur.stare;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;

import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseRequest.CoseKeywordSet;
import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.spur.lids.LidsFinder;
import edu.brown.cs.spur.lids.LidsConstants.LidsLibrary;
import edu.brown.cs.spur.stare.StareConstants.StareSolution;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;
import edu.brown.cs.spur.sump.SumpData;

class StareSolutionBase implements StareConstants, StareSolution
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SumpModel       for_model;
private CoseResult      cose_result;
private CoseResult      test_result;
private Map<String,String> name_map;
private File            work_directory;
private VelocityContext map_context;

private Random          random_gen;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

StareSolutionBase(StareCandidateSolution sc)
{
   for_model = sc.getModel();
   cose_result = sc.getCoseResult();
   test_result = sc.getTestResult();
   name_map = sc.getNameMap();
   random_gen = new Random();
   work_directory = null;
   map_context = null;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public SumpModel getModel()                   { return for_model; }
@Override public CoseResult getCoseResult()             { return cose_result; }
@Override public CoseResult getTestResult()             { return test_result; }
@Override public Map<String,String> getNameMap()        { return name_map; }




/********************************************************************************/
/*                                                                              */
/*      Methods to generate code                                                */
/*                                                                              */
/********************************************************************************/

@Override public boolean generateCode()
{
   map_context = new VelocityContext();
   
   try {
      clearOldCode();
      CompilationUnit cu = (CompilationUnit) cose_result.getStructure();
      setup(cu);
      producePackageFiles(cu);
      produceTestFiles();
      producePomFile();
      // generate resource files
      // generate test files
      
      File dir = (File) map_context.get("DIRECTORY");
      IvyExec ex = new IvyExec("mvn compile",dir,IvyExec.ERROR_OUTPUT);
      int sts = ex.waitFor();
      if (sts != 0) {
         IvyLog.logI("Mavan failed");
       }
      // run tests
    }
   catch (IOException e) {
      return false;
    }
   
   return true;
}



private void clearOldCode() throws IOException
{
   for (File f : getDirectory().listFiles()) {
      IvyFile.remove(f);
    }
}



private void setup(CompilationUnit cu)
{
   File dir = getDirectory();
   map_context.put("DIRECTORY",dir);
   
   File topdir = new File(dir,"src");
   File srcdir =  new File(topdir,"main");
   File resdir = new File(srcdir,"resources");
   srcdir = new File(srcdir,"java");
   File testdir = new File(topdir,"test");
   testdir = new File(testdir,"java");
   
   PackageDeclaration pd = cu.getPackage();
   if (pd != null) {
      String pkg = pd.getName().getFullyQualifiedName();
      map_context.put("PACKAGE",pkg);
      map_context.put("PACKAGESTMT","package " + pkg + ";\n");
      map_context.put("PACKAGEDOT",pkg + ".");
      StringTokenizer tok = new StringTokenizer(pkg,".");
      while (tok.hasMoreTokens()) {
         String s = tok.nextToken();
         srcdir = new File(srcdir,s);
         testdir = new File(testdir,s);
       }
    }
   else {
      map_context.remove("PACKAGE");
    }
   
   map_context.put("TOPDIR",topdir);
   srcdir.mkdirs();
   map_context.put("SRCDIR",srcdir);
   testdir.mkdirs();
   map_context.put("TESTDIR",testdir);
   resdir.mkdirs();
   map_context.put("RESOURCEDIR",resdir);
   File f = new File(dir,"bin");
   f.mkdirs();
   map_context.put("BINDIR",f);
   
   try {
      File link = new File(dir,"tosrc");
      Path p1 = link.toPath();
      Path p2 = srcdir.toPath();
      Files.createSymbolicLink(p1,p2);
      File tlink = new File(dir,"totest");
      Path p3 = tlink.toPath();
      Path p4 = testdir.toPath();
      Files.createSymbolicLink(p3,p4);
    }
   catch (IOException e) { }
   
   File pomfile = new File(dir,"pom.xml");
   map_context.put("POMFILE",pomfile);
   
   String v = System.getProperty("java.specification.version");
   map_context.put("JAVAVERSION",v);
   map_context.put("ENCODING","UTF-8");
   
   SumpData smd = for_model.getModelData();
   List<LidsLibrary> libs = new ArrayList<>();
   boolean havejunit = false;
   for (LidsLibrary lib : smd.getLibraries()) {
      if (lib.getName().equals("junit")) havejunit = true;
      libs.add(lib);
    }
   if (!havejunit) {
      LidsLibrary ju = LidsFinder.createLibrary("junit:junit:4.13");
      libs.add(ju);
    }
    
   map_context.put("LIBRARIES",libs);
   
   map_context.put("MAXTIME","10000L");
   
   StringBuffer buf = new StringBuffer();
   for (Object o : cu.imports()) {
      ImportDeclaration id = (ImportDeclaration) o;
      buf.append(id.toString());
    }
   map_context.put("IMPORTS",buf.toString());
   
   if (test_result != null) {
      CompilationUnit tcu = (CompilationUnit) test_result.getStructure();
      StringBuffer tbuf = new StringBuffer();
      for (Object o : tcu.imports()) {
         ImportDeclaration id = (ImportDeclaration) o;
         tbuf.append(id.toString());
       }
      map_context.put("TESTIMPORTS",tbuf.toString());
    }
   
   generateReadme();
}



private void generateReadme()
{
   File root = (File) map_context.get("DIRECTORY");
   File readme = new File(root,"README.md");
   try (PrintWriter pw = new PrintWriter(new FileWriter(readme))) {
      pw.println("This package is generated by ASCUS/SPUR");
      pw.println();
      SumpData smd = for_model.getModelData();
      pw.println("Name: " + smd.getName());
      for (String src : smd.getSources()) {
         pw.println("Source: " + src);
       }
      for (LidsLibrary lib : smd.getLibraries()) {
         pw.println("Library: " + lib.getFullId());
       }
      for (String s : smd.getMissingImports()) {
         pw.println("Missing: " + s);
       }
      CoseRequest req = smd.getCoseRequest();
      pw.println("Search Type: " + req.getCoseSearchType());
      pw.println("Scope Type: " + req.getCoseScopeType());
      pw.println("Language: " + req.getLanguage());
      for (CoseKeywordSet cks : req.getCoseKeywordSets()) {
         pw.print("Keywords:");
         for (String s : cks.getWords()) {
            if (s.contains(" ")) {
               pw.print(" \"" + s + "\"");
             }
            else pw.print(" " + s);
          }
         pw.println();
       }
      pw.print("Key terms:");
      for (String s : req.getKeyTerms()) {
         pw.print(" " + s);
       }
      pw.println();
    }
   catch (IOException e) { }
}



private void producePackageFiles(CompilationUnit cu)
{
   File dir = (File) map_context.get("SRCDIR");
   
   for (Object o : cu.types()) {
      AbstractTypeDeclaration td = (AbstractTypeDeclaration) o;
      String cnm = td.getName().getIdentifier();
      File f = new File(dir,cnm + ".java");
      try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
        pw.println(map_context.get("PACKAGESTMT"));
        pw.println(map_context.get("IMPORTS"));
        pw.println();
        pw.write(td.toString());
        pw.println();
        pw.println("// end of " + cnm + ".java");
       }
      catch (IOException e) {
         IvyLog.logE("Problem writing " + cnm,e);
       }
    }
}



private void produceTestFiles()
{
   if (test_result == null) return;
   
   CompilationUnit cu = (CompilationUnit) test_result.getStructure();  File dir = (File) map_context.get("TESTDIR");
   
   for (Object o : cu.types()) {
      AbstractTypeDeclaration td = (AbstractTypeDeclaration) o;
      String cnm = td.getName().getIdentifier();
      File f = new File(dir,cnm + ".java");
      try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
         pw.println(map_context.get("PACKAGESTMT"));
         pw.println(map_context.get("TESTIMPORTS"));
         pw.println();
         pw.write(td.toString());
         pw.println();
         pw.println("// end of " + cnm + ".java");
       }
      catch (IOException e) {
         IvyLog.logE("Problem writing " + cnm,e);
       }
    }
}



private void producePomFile()
{
   File pom = (File) map_context.get("POMFILE");
   try {
      InputStream ins = this.getClass().getClassLoader().getResourceAsStream(TEMPLATE_RESOURCE);
      Reader fr = null;
      if (ins != null) fr = new InputStreamReader(ins);
      else fr = new FileReader(IvyFile.expandName(TEMPLATE_FILE));
      FileWriter fw = new FileWriter(pom);
      
      Velocity.evaluate(map_context,fw,"stare.pom",fr);
      
      fr.close();
      fw.close();
    }
   catch (IOException e) {
      IvyLog.logE("Problem writing pom file",e);
    }
}




/********************************************************************************/
/*                                                                              */
/*      File methods                                                            */
/*                                                                              */
/********************************************************************************/

private File getDirectory()
{
   if (work_directory == null) {
      String nm = System.getenv("STARE_BASE");
      if (nm == null) nm = TEST_BASE;
      File dir = new File(nm);
      dir.mkdirs();
      for ( ; ; ) {
         int n = random_gen.nextInt(1000000);
         String s = TEST_DIRECTORY.replace("#",Integer.toString(n));
         File f = new File(dir,s);
         if (!f.exists() && f.mkdir()) {
            work_directory = f;
            break;
          }
       }
    }
   return work_directory; 
}





}       // end of class StareSolutionBase




/* end of StareSolutionBase.java */

