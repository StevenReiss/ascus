/********************************************************************************/
/*                                                                              */
/*              LidsInstaller.java                                              */
/*                                                                              */
/*      Install and get class path for libraries                                */
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



package edu.brown.cs.spur.lids;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.StringTokenizer;

import edu.brown.cs.ivy.file.IvyFile;

public class LidsInstaller implements LidsConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private File            repo_directory;

private static LidsInstaller the_installer;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private LidsInstaller()
{
   File home = new File(System.getProperty("user.home"));
   File f1 = new File(home,".m2");
   repo_directory = new File(f1,"repository");
   if (!repo_directory.exists()) repo_directory.mkdirs();
}



public synchronized static LidsInstaller getInstaller()
{
   if (the_installer == null) {
      the_installer = new LidsInstaller();
    }
   return the_installer;
}


/********************************************************************************/
/*                                                                              */
/*      Get class path for a library                                            */
/*                                                                              */
/********************************************************************************/

public String getClassPath(LidsLibrary lib)
{
   String path = "https://repo1.maven.org/maven";
   
   File libpath = repo_directory;
   String libnm = lib.getFullId();
   StringTokenizer tok = new StringTokenizer(libnm,".:");
   while (tok.hasMoreTokens()) {
      String elt = tok.nextToken();
      libpath = new File(libpath,elt);
      path += "/" + elt;
    }
   
   String jarnm = lib.getName() + "-" + lib.getVersion() + ".jar";
   path += "/" + jarnm;
   File cpath = new File(libpath,jarnm);
   if (cpath.exists()) return cpath.getPath();
   
   try {
      URL u = new URL(path);
      InputStream ins = u.openStream();
      IvyFile.copyFile(ins,cpath);
    }
   catch (IOException e) {
      return null;
    }
   
   return cpath.getPath();
}




}       // end of class LidsInstaller




/* end of LidsInstaller.java */

