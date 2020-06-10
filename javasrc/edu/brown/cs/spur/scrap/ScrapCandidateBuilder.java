/********************************************************************************/
/*                                                                              */
/*              ScrapCandidateBuilder.java                                      */
/*                                                                              */
/*      Code to build the candidate sets for an input model                     */
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



package edu.brown.cs.spur.scrap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;

import edu.brown.cs.cose.cosecommon.CoseConstants;
import edu.brown.cs.cose.cosecommon.CoseMaster;
import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseScopeType;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseSearchEngine;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseSearchType;
import edu.brown.cs.cose.result.ResultFactory;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.spur.etch.EtchFactory;
import edu.brown.cs.spur.lids.LidsFinder;
import edu.brown.cs.spur.lids.LidsConstants.LidsLibrary;
import edu.brown.cs.spur.sump.SumpConstants;
import edu.brown.cs.spur.sump.SumpData;
import edu.brown.cs.spur.sump.SumpParameters;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;

class ScrapCandidateBuilder implements ScrapConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<CoseResult>        all_results;
private JcompControl            jcomp_control;
private CoseRequest             original_request;
private SumpParameters          search_params;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ScrapCandidateBuilder(CoseRequest req,List<CoseResult> ar,SumpParameters sp)
{
   original_request = req;
   all_results = ar;
   jcomp_control = new JcompControl();
   search_params = sp;
}


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

List<ScrapCandidate> buildCandidates(SumpModel model)
{
   List<CandidateMatch> match = findInitialMatches(model);
   
   System.err.println("FOUND " + match.size() + " MATCHES");
   
   EtchFactory etcher = new EtchFactory(model);
   
   for (CandidateMatch cm : match) {
      addTestCases(cm,etcher);
    }
   
   for (CandidateMatch cm : match) {
      System.err.println("MATCH:\n" + cm.getCoseResult().getEditText());
      Map<String,String> namemap = cm.getNameMap();
      CoseResult cr = cm.getCoseResult();
      CoseResult cr1 = etcher.fixCode(cr,namemap);
      cm.updateResult(cr1);
    }
   
   return new ArrayList<>(match);
}




private List<CandidateMatch> findInitialMatches(SumpModel model)
{
   Map<CoseResult,SumpModel> mmap = new HashMap<>();
   for (CoseResult orig : all_results) {
      CompilationUnit cu = (CompilationUnit) orig.getStructure();
      System.err.println("CHECK RESULT:\n" + cu);
      JcompProject proj = null;
      try { 
         if (!JcompAst.isResolved(cu)) {
            proj = JcompAst.getResolvedAst(jcomp_control,cu);
            JcompAst.setProject(cu,proj);
            if (proj == null) continue;
          }
         SumpData sdata = new SumpData(model.getModelData().getCoseRequest(),orig,search_params);
         
         LidsFinder lids = ScrapDriver.findLibraries(cu);
         for (LidsLibrary ll : lids.findLibraries()) {
            sdata.addLibrary(ll);
          }
         Collection<String> missing = lids.getMissingImports();
         if (missing != null && !missing.isEmpty()) {
            for (String s : missing) sdata.addMissingImport(s);
            for (String s : missing) {
               System.err.println("MISSING IMPORT " + s + " FROM " + orig.getSource());
             }
            // continue;
          } 
         
         SumpModel mdl = SumpConstants.SumpFactory.createModel(sdata,cu);
         mmap.put(orig,mdl);
       }
      finally {   
         if (proj != null) {
            jcomp_control.freeProject(proj);
            JcompAst.setProject(cu,null);
          }
       }
    }
   
   Set<CandidateMatch> match = new TreeSet<>(); 
   for (Map.Entry<CoseResult,SumpModel> ent : mmap.entrySet()) {
      SumpModel emdl = ent.getValue();
      Map<String,String> rmap = new HashMap<>();
      double sv = emdl.matchScore(model,rmap);
      if (sv != 0) {
         CandidateMatch cm = new CandidateMatch(emdl,ent.getKey(),sv,rmap);
         match.add(cm);
       }
    }
   
   return new ArrayList<>(match);
}




/********************************************************************************/
/*                                                                              */
/*      Find associated test cases                                              */
/*                                                                              */
/********************************************************************************/

private void addTestCases(CandidateMatch cm,EtchFactory etcher)
{
   CoseResult cr = cm.getCoseResult();
   ScrapRequest req = new ScrapRequest();
   for (CoseSearchEngine seng : original_request.getEngines()) {
      if (seng == CoseSearchEngine.GITREPO) seng = CoseSearchEngine.GITHUB;
      req.setSearchEngine(seng);
    }
   req.setCoseSearchType(CoseSearchType.TESTCLASS);
   req.setCoseScopeType(CoseScopeType.FILE);
   for (String s : cr.getPackages()) {
      req.addKeywordSet("package " + s,"junit","test");
    }
  
   req.setDoDebug(true);
   CoseMaster master = CoseMaster.createMaster(req);
   ScrapResultSet tests = new ScrapResultSet();
   try {
      master.computeSearchResults(tests);
    }
   catch (Throwable t) {
      IvyLog.logE("PROBLEM GETTING TEST RESULTS: " + t,t);
    }
   Set<String> done = new HashSet<>();
   Map<String,String> namemap = cm.getNameMap();
   CoseResult testresult = null;
   for (CoseResult test : tests.getResults()) {
      String enc = IvyFile.digestString(test.getKeyText());
      if (!done.add(enc)) continue;
      System.err.println("CONSIDER TEST CODE " + test.getSource().getDisplayName());
      if (!isViableTestCase(test,cm)) continue;
      if (testresult == null) {
         ResultFactory rf = new ResultFactory(req);
         testresult = rf.createPackageResult(test.getSource());
         for (String s : cr.getPackages()) {
            testresult.addPackage(s);
          }
       }
      CoseResult ftest = test.getParent();
      testresult.addInnerResult(ftest);
      // ensure that all classes referenced are in the model code
      // otherwise, prune the model code to only include known classes
      // ensure that this test class is not in the model code
      // then add the class to the model code
    }
   if (testresult != null) {
      String pkg = testresult.getBasePackage();
      String origpkg = cr.getBasePackage();
      if (!origpkg.equals(pkg)) {
         String top = namemap.get(origpkg);
         namemap.put(pkg,top);
       }
      CoseResult test1 = etcher.fixTests(testresult,namemap);
      cm.updateTestResult(test1);
      System.err.println("TEST CODE:\n" + test1.getEditText());
    }
}



private boolean isViableTestCase(CoseResult cr,CandidateMatch cm)
{
   boolean allok = true;
   
   String pkg = cm.getCoseResult().getBasePackage();
   Map<String,String> namemap = cm.getNameMap();
   ASTNode td = (ASTNode) cr.getStructure();
   CompilationUnit cu = (CompilationUnit) td.getRoot();
   for (Object o : cu.imports()) {
      ImportDeclaration id = (ImportDeclaration) o;
      String nm = id.getName().getFullyQualifiedName();
      if (namemap.containsKey(nm)) continue;
      if (id.isOnDemand() && !id.isStatic()) continue;
      if (id.isStatic()) {
         int idx = nm.lastIndexOf(".");
         if (idx > 0) nm = nm.substring(0,idx);
         if (namemap.containsKey(nm)) continue;
       }
      if (nm.contains("junit")) continue;
      if (nm.contains("org.hamcrest")) continue;
      if (CoseConstants.isStandardJavaLibrary(nm)) continue;
      int idx = nm.lastIndexOf(".");
      String pnm = pkg + nm.substring(idx);
      if (namemap.containsKey(pnm)) continue;
      System.err.println("MISSING IMPORT: " + nm);
      allok = false;
    }
   
   return allok;
}




/********************************************************************************/
/*                                                                              */
/*      Potential match information                                             */
/*                                                                              */
/********************************************************************************/

private static class CandidateMatch implements Comparable<CandidateMatch>, ScrapCandidate {
 
   private SumpModel for_model;
   private CoseResult for_result;
   private Map<String,String> name_map;
   private double match_value;
   private CoseResult test_result;
   
   CandidateMatch(SumpModel mdl,CoseResult cr,double v,Map<String,String> nmap) {
      for_model = mdl;
      for_result = cr;
      match_value = v;
      name_map = new HashMap<>(nmap);
      test_result = null;
    }
   
   @Override public SumpModel getModel()                        { return for_model; }
   @Override public CoseResult getCoseResult()                  { return for_result; }
   @Override public Map<String,String> getNameMap()             { return name_map; }
   @Override public CoseResult getTestResult()                  { return test_result; }
   
   void updateResult(CoseResult cr)                             { for_result = cr; }
   void updateTestResult(CoseResult cr)                         { test_result = cr; }
   
   @Override public int compareTo(CandidateMatch cm) {
      int v = Double.compare(cm.match_value,match_value);
      if (v != 0) return v;
      return for_result.getSource().toString().compareTo(cm.for_result.getSource().toString());
    }
   
   @Override public String toString() {
      return match_value + ": " + for_result;
    }
   
}       // end of inner class CandidateMatch




}       // end of class ScrapCandidateBuilder




/* end of ScrapCandidateBuilder.java */

