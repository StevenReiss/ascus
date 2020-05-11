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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.spur.stare.StareConstants.StareSolution;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;

class StareSolutionBase implements StareConstants, StareSolution
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SumpModel       for_model;
private CoseResult      cose_result;
private Map<String,String> name_map;
private File            work_directory;

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
   name_map = sc.getNameMap();
   random_gen = new Random();
   work_directory = null;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public SumpModel getModel()                   { return for_model; }
@Override public CoseResult getCoseResult()             { return cose_result; }
@Override public Map<String,String> getNameMap()        { return name_map; }




/********************************************************************************/
/*                                                                              */
/*      Methods to generate code                                                */
/*                                                                              */
/********************************************************************************/

@Override public boolean generateCode()
{
   Map<String,String> idmap = new HashMap<>();
   
   try {
      clearOldCode();
      CompilationUnit cu = (CompilationUnit) cose_result.getStructure();
      setup(cu,idmap);
      producePackageFiles(cu,idmap);
      // generate resource files
      // genearte test files
      // generate maven file
      // run mvn to compile
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



private void setup(CompilationUnit cu,Map<String,String> idmap)
{
   File dir = getDirectory();
   
   idmap.put("DIRECTORY",dir.getPath());
   idmap.put("SRCDIR",dir.getPath());
   
   PackageDeclaration pd = cu.getPackage();
   if (pd != null) {
      String pkg = pd.getName().getFullyQualifiedName();
      idmap.put("PACKAGE",pkg);
      idmap.put("PACKAGESTMT","package " + pkg + ";\n");
      idmap.put("PACKAGEDOT",pkg + ".");
    }
   else {
      idmap.remove("PACKAGE");
    }
   
   idmap.put("MAXTIME","10000L");
   
   StringBuffer buf = new StringBuffer();
   for (Object o : cu.imports()) {
      ImportDeclaration id = (ImportDeclaration) o;
      buf.append(id.toString() + ";\n");
    }
   idmap.put("IMPORTS", buf.toString());
   
   File f = new File(dir,"bin");
   f.mkdir();
   idmap.put("BINDIR",f.getPath());
}



private void producePackageFiles(CompilationUnit cu,Map<String,String> idmap)
{
   String dir = idmap.get("SRCDIR");
   
   for (Object o : cu.types()) {
      AbstractTypeDeclaration td = (AbstractTypeDeclaration) o;
      String cnm = td.getName().getIdentifier();
      File f = new File(dir,cnm + ".java");
      try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
        pw.println(idmap.get("PACKAGESTMT"));
        pw.println(idmap.get("IMPORTS"));
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

