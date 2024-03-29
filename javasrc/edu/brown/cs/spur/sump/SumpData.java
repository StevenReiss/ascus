/********************************************************************************/
/*                                                                              */
/*              SumpData.java                                                   */
/*                                                                              */
/*      Data for output Java and UML                                            */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2013 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.spur.sump;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseSource;
import edu.brown.cs.spur.lids.LidsFinder;
import edu.brown.cs.spur.lids.LidsConstants.LidsLibrary;
import edu.brown.cs.spur.swift.SwiftScorer;

public class SumpData implements SumpConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          model_name;
private String          model_source;
private Set<LidsLibrary> library_set;
private Set<String>     source_set;
private Set<String>     import_set;
private CoseRequest     cose_request;
private Set<String>     missing_imports;
private SumpParameters  sump_parameters;
private String          context_path;
private List<String>    suggested_words;
private double          model_score;
private Set<File>       test_files;
private String          base_package;
private Set<String>     used_packages;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SumpData(CoseRequest req,CoseResult rslt,SumpParameters sp)
{
   model_name = null;
   model_source = null;
   library_set = new HashSet<>();
   source_set = new HashSet<>();
   missing_imports = new HashSet<>();
   import_set = new TreeSet<>();
   cose_request = req;
   sump_parameters = sp;
   if (sp == null) sump_parameters = new SumpParameters();
   context_path = null;
   suggested_words = new ArrayList<>();
   model_score = 0;
   test_files = new HashSet<>();
   base_package = null;
   used_packages = new HashSet<>();
   
   if (rslt != null) {
      addSource(rslt.getSource());
      String nm = rslt.getSource().getDisplayName();
      int idx = nm.lastIndexOf("/");
      if (idx > 0) nm = nm.substring(idx+1);
      idx = nm.lastIndexOf(".");
      if (idx > 0) nm = nm.substring(0,idx);
      model_name = nm;
      base_package = rslt.getBasePackage();
      used_packages.addAll(rslt.getPackages());
      used_packages.add(base_package);
      
      SwiftScorer scorer = new SwiftScorer(rslt.getText(),(ASTNode) rslt.getStructure(),false);
      suggested_words.addAll(scorer.getTopWords());
    }
}






/********************************************************************************/
/*                                                                              */
/*      Public Setup Methods                                                    */
/*                                                                              */
/********************************************************************************/

public void addLibrary(LidsLibrary lib)
{
   if (lib == null) return;
   
   for (LidsLibrary ll : library_set) {
      if (lib.sameLibrary(ll)) return;
    }
    
   library_set.add(lib);
}


public void addLibrary(String lib)
{
   addLibrary(LidsFinder.createLibrary(lib));
}


public void addSource(CoseSource src)
{
   source_set.add(src.getName());
}


public void addSource(String src)
{
   source_set.add(src);
}


public void addMissingImport(String imp)
{
   missing_imports.add(imp);
}


public void setName(String name)
{
   model_name = name;
}


public void setSource(String source)
{
   model_source = source;
}

 
public void setContextPath(String path)
{
   context_path = path;
}


public void addSuggestedWord(String wd)
{
   suggested_words.add(wd);
}


public void setModelScore(double sc)
{
   model_score = sc;
}



public void addTestFile(String file)
{
   File f = new File(file);
   if (f.exists()) {
      test_files.add(f);
    }
}

public void addTestFile(File f)
{
   if (f.exists()) { 
      test_files.add(f);
    }
}


public void setBasePackage(String s)
{
   base_package = s;
}



/********************************************************************************/
/*                                                                              */
/*      Internal access methods                                                 */
/*                                                                              */
/********************************************************************************/

public Collection<LidsLibrary> getLibraries()   { return library_set; }

public Collection<String> getSources()          { return source_set; }

public Collection<String> getMissingImports()   { return missing_imports; }

public CoseRequest getCoseRequest()             { return cose_request; }

public String getName()                         { return model_name; }

public String getSource()                       { return model_source; }

public SumpParameters getParameters()           { return sump_parameters; }

public String getContextPath()                  { return context_path; }

public List<String> getSuggestedWords()         { return suggested_words; }

public Collection<File> getTestFiles()          { return test_files; }

public double getModelScore()                   { return model_score; }

public String getBasePackage()                  { return base_package; }

public Set<String> getUsedPackages()            { return used_packages; }



/********************************************************************************/
/*                                                                              */
/*      Import managment                                                        */
/*                                                                              */
/********************************************************************************/

public void beginImport()
{
   import_set.clear();
}

public void addImport(String typ)
{
   // if (CoseConstants.isStandardJavaLibrary(typ)) return;
   int idx = typ.lastIndexOf(".");
   if (idx > 0) {
      String pkg = typ.substring(0,idx);
      if (pkg.equals("java.lang")) return;
    }
   import_set.add(typ);
}


Collection<String> getImports()                 { return import_set; }



}       // end of class SumpData




/* end of SumpData.java */

