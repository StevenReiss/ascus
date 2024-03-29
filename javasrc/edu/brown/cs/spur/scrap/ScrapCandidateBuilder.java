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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.brown.cs.cose.cosecommon.CoseConstants;
import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseSource;
import edu.brown.cs.cose.result.ResultFactory;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompType;
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
private CoseResult              global_tests;



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
   global_tests = null;
}


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

List<ScrapCandidate> buildCandidates(SumpModel model)
{
   List<CandidateMatch> match = findInitialMatches(model);
   
   if (match.size() == 0) return null;
   
   EtchFactory etcher = new EtchFactory(model);
   
   long start0 = System.currentTimeMillis();
   for (CandidateMatch cm : match) {
      addTestCases(cm,etcher);
    }
   
   long start1 = System.currentTimeMillis();
   if (global_tests == null || global_tests.getInnerResults() == null ||
         global_tests.getInnerResults().size() == 0) {
      IvyLog.logS("SCRAP","Global Test Size: " + 0);
      IvyLog.logS("SCRAP","Global Test Count: " + 0);
      IvyLog.logS("SCRAP","Average Test Time: " + 0);
    }
   else {
      int sz = global_tests.getInnerResults().size();
      IvyLog.logS("SCRAP","Global Test Size: " + sz);
      IvyLog.logS("SCRAP","Global Test Count: " + getTestCount(global_tests));
      IvyLog.logS("SCRAP","Average Test Time: " + (start1-start0)/sz);
    }
   IvyLog.logS("SCRAP","Test Case Time: " + (start1-start0));
   
   for (CandidateMatch cm : match) {
      cm.updateGlobalTestResult(global_tests);
      System.err.println("WORK ON MATCH: " + cm.getCoseResult().getSource() + ":\n" + 
            cm.getCoseResult().getEditText());
      Map<String,String> namemap = cm.getNameMap();
      for (Map.Entry<String,String> ent : namemap.entrySet()) {
         System.err.println("MMAP: " + ent.getKey() + " => " + ent.getValue());
       }
      CoseResult cr = cm.getCoseResult();
      CoseResult cr1 = etcher.fixCode(cr,cm.getModel(),namemap);
      cm.updateResult(cr1);
      System.err.println("MAPPED MATCH: " + cm.getCoseResult().getSource() + ":\n" + 
            cm.getCoseResult().getEditText());   
      CoseResult tr1 = updateTestResult(cm.getLocalTestResult(),cr1,cm,etcher);
      cm.updateLocalTestResult(tr1);
      CoseResult tr2 = updateTestResult(cm.getGlobalTestResult(),cr1,cm,etcher);
      cm.updateGlobalTestResult(tr2);
      IvyLog.logS("SCRAP","Output Test Count: " + getTestCount(tr1));
      IvyLog.logS("SCRAP","Output Global Count: " + getTestCount(tr2));
    }
   
   long start2 = System.currentTimeMillis();
   IvyLog.logS("SCRAP","Average Map Time: " + (start2-start1)/match.size());  
   
   return new ArrayList<>(match);
}



private CoseResult updateTestResult(CoseResult testresult,CoseResult base,CandidateMatch cm,EtchFactory etcher)
{
   if (testresult == null) return null;
   
   Map<String,String> testmap = cm.getNameMap();
   String pkg = testresult.getBasePackage();
   String origpkg = base.getBasePackage();
   if (!origpkg.equals(pkg)) {
      testmap = new HashMap<>(testmap);
      String top = testmap.get(origpkg);
      if (top != null) testmap.put(pkg,top);
    }
   
   CoseResult tr1 = etcher.fixTests(testresult,base,cm.getModel(),testmap,false,true);
   return tr1;
}




private List<CandidateMatch> findInitialMatches(SumpModel model)
{
   if (all_results.size() == 0) return new ArrayList<>();
   
   long start0 = System.currentTimeMillis();

   SumpData patdata = model.getModelData();
   Map<CoseResult,SumpModel> mmap = new HashMap<>();
   for (CoseResult orig : all_results) {
      CompilationUnit cu = (CompilationUnit) orig.getStructure();
      System.err.println("CHECK RESULT: " + orig.getSource() + ":\n" + cu);
      JcompProject proj = null;
      try { 
         if (!JcompAst.isResolved(cu)) {
            proj = JcompAst.getResolvedAst(jcomp_control,cu);
            JcompAst.setProject(cu,proj);
            if (proj == null) continue;
          }
         SumpData sdata = new SumpData(model.getModelData().getCoseRequest(),orig,search_params);
         sdata.setContextPath(patdata.getContextPath());
         for (LidsLibrary ll : patdata.getLibraries()) {
            if (ll.getVersion() == null || ll.getVersion().equals("LATEST")) {
               continue;
             }
            sdata.addLibrary(ll);
            System.err.println("ADD LIBRARY PATTERN " + ll);
          }
         LidsFinder lids = ScrapDriver.findLibraries(cu,orig);
         for (LidsLibrary ll : lids.findLibraries()) {
            if (ll.getVersion() == null || ll.getVersion().equals("LATEST")) {
               continue;
             }
            sdata.addLibrary(ll);
            System.err.println("ADD LIBRARY " + ll);
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
   
   long start1 = System.currentTimeMillis();
   IvyLog.logS("SCRAP","Built Models: " + mmap.size());
   IvyLog.logS("SCRAP","Average Build Time: " + (start1-start0)/all_results.size());
   
   Set<CandidateMatch> match = new TreeSet<>(); 
   for (Map.Entry<CoseResult,SumpModel> ent : mmap.entrySet()) {
      SumpModel emdl = ent.getValue();
      Map<String,String> rmap = new HashMap<>();
      double sv = emdl.matchScore(model,rmap);
      if (sv != 0) {
         CandidateMatch cm = new CandidateMatch(model,emdl,ent.getKey(),sv,rmap);
         match.add(cm);
         System.err.println("ADD MATCH " + sv + ": " + ent.getKey().getSource());
       }
    }
   
   long start2 = System.currentTimeMillis();
   IvyLog.logS("SCRAP","Matched models: " + match.size());
   IvyLog.logS("SCRAP","Total Match Time: " + (start2-start1));
   IvyLog.logS("SCRAP","Average Match Time: " + (start2-start1)/mmap.size());
   
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

   ScrapTestFinder finder = new ScrapTestFinder(original_request,cm.getCoseResult());
   CoseRequest testreq = finder.getTestRequest();
   List<CoseResult> rawtests = finder.getTestResults();

   Set<String> done = new HashSet<>();
   Set<CoseResult> doneresults = new HashSet<>();
   Map<String,String> namemap = cm.getNameMap();
   CoseResult testresult = null;
   for (CoseResult test : rawtests) {
      CoseResult ftest = test.getParent();
      if (doneresults.contains(ftest)) continue;
      String enc = IvyFile.digestString(test.getKeyText());
      if (!done.add(enc)) continue;
      IvyLog.logD("SCRAP","CONSIDER TEST CODE " + test.getSource().getDisplayName());
      if (!isViableTestCase(test,cm)) continue;
      if (testresult == null) {
         ResultFactory rf = new ResultFactory(testreq);
         testresult = rf.createPackageResult(test.getSource());
         for (String s : cr.getPackages()) {
            testresult.addPackage(s);
          }
       }
      testresult.addInnerResult(ftest);
      doneresults.add(ftest);
    }
   if (testresult != null) {
      String pkg = testresult.getBasePackage();
      String origpkg = cr.getBasePackage();
      if (!origpkg.equals(pkg)) {
         namemap = new HashMap<>(namemap);
         String top = namemap.get(origpkg);
         if (top != null) namemap.put(pkg,top);
       }
      CoseResult test1 = etcher.fixTests(testresult,cr,cm.getModel(),namemap,false,false);
      if (getTestCount(test1) == 0) test1 = null;
      cm.updateLocalTestResult(test1);
      if (test1 != null) System.err.println("TEST CODE:\n" + test1.getEditText());
      CoseResult test2 = etcher.fixTests(testresult,cr,cm.getModel(),namemap,true,false);
      if (getTestCount(test2) == 0) test2 = null;
      addToGlobalTests(testreq,cm,test2);
      IvyLog.logS("SCRAP","Original Tests: " + testresult.getInnerResults().size());
      IvyLog.logS("SCRAP","Start Local Tests: " + getTestCount(test1));
    }
   else {
      IvyLog.logS("SCRAP","Original Tests: " + 0);
      IvyLog.logS("SCRAP","Start Local Tests: " + 0);
    }
}


private int getTestCount(CoseResult test)
{
   if (test == null) return 0;
   CompilationUnit cu = (CompilationUnit) test.getStructure();
   if (cu == null) return 0;
   int ct = 0;
   for (Object o : cu.types()) {
      AbstractTypeDeclaration atd = (AbstractTypeDeclaration) o;
      for (Object o1 : atd.bodyDeclarations()) {
         if (o1 instanceof MethodDeclaration) {
            MethodDeclaration md = (MethodDeclaration) o1;
            if (md.isConstructor()) continue;
            for (Object o2 : md.modifiers()) {
               if (o2 instanceof Annotation) {
                  Annotation an = (Annotation) o2;
                  JcompType jt = JcompAst.getJavaType(an.getTypeName());
                  if (jt != null) {
                     String jtn = jt.getName();
                     if (jtn.contains("junit") && jtn.endsWith("Test")) {
                        ++ct;
                        break;
                      }
                   }
                  else {
                     String nm = an.getTypeName().getFullyQualifiedName();
                     if (nm.equals("Test") ||
                           (nm.contains("junit") && nm.endsWith("Test"))) {
                        ++ct;
                        break;
                      }
                   }
                }
             }
          }
       }
    }
   return ct;
}



private boolean isViableTestCase(CoseResult cr,CandidateMatch cm)
{
   boolean allok = true;
   boolean someok = true;
   
   String pkg = cm.getCoseResult().getBasePackage();
   Map<String,String> namemap = cm.getNameMap();
   ASTNode td = (ASTNode) cr.getStructure();
   CompilationUnit cu = (CompilationUnit) td.getRoot();
   for (Object o : cu.imports()) {
      ImportDeclaration id = (ImportDeclaration) o;
      String nm = id.getName().getFullyQualifiedName();
      if (namemap.containsKey(nm)) continue;
      if (id.isOnDemand() && !id.isStatic()) continue;
      if (id.isStatic() && !id.isOnDemand()) {
         int idx = nm.lastIndexOf(".");
         if (idx > 0) nm = nm.substring(0,idx);
         if (namemap.containsKey(nm)) continue;
       }
      if (nm.contains("junit")) continue;
      if (nm.contains("org.hamcrest")) continue;
      if (CoseConstants.isStandardJavaLibrary(nm)) continue;
      int idx = nm.lastIndexOf(".");
      String pnm = null;
      if (idx < 0) {
         pnm = pkg + "." + nm;
       }
      else {
         pnm = pkg + nm.substring(idx);
       }
      if (namemap.containsKey(pnm)) {
         someok = true;
         continue;
       }     
      System.err.println("MISSING IMPORT: " + nm);
      allok = false;
    }
   
   return allok || someok;
}


private void addToGlobalTests(CoseRequest req,CandidateMatch cm,CoseResult gt)
{
   if (gt == null) return;
   if (!isViableTestCase(gt,cm)) return;
   
   if (global_tests == null) {
      global_tests = createEmptyGlobalTest(req,gt.getSource(),cm);
    }
   global_tests.addInnerResult(gt);
}



private CoseResult createEmptyGlobalTest(CoseRequest req,CoseSource src,CandidateMatch cm)
{
   String pkg = cm.getModel().getPackage().getFullName();
   String p1 = cm.getNameMap().get(pkg);
   if (p1 != null) pkg = p1;
   
   StringWriter sw = new StringWriter();
   PrintWriter pw = new PrintWriter(sw); 
   pw.println("package " + pkg + ";");
   ResultFactory rf = new ResultFactory(req);
   CoseResult r = rf.createFileResult(src,sw.toString());
   CoseResult pr = rf.createPackageResult(src);
   pr.addInnerResult(r);
   return pr;
}



/********************************************************************************/
/*                                                                              */
/*      Potential match information                                             */
/*                                                                              */
/********************************************************************************/

private static class CandidateMatch implements Comparable<CandidateMatch>, ScrapCandidate {
 
   private SumpModel for_model;
   private SumpModel pattern_model;
   private CoseResult for_result;
   private Map<String,String> name_map;
   private double match_value;
   private CoseResult local_tests;
   private CoseResult global_tests;
   
   CandidateMatch(SumpModel pat,SumpModel mdl,CoseResult cr,double v,Map<String,String> nmap) {
      for_model = mdl;
      pattern_model = pat;
      for_result = cr; 
      match_value = v;
      name_map = new HashMap<>(nmap);
      local_tests = null;
      global_tests = null;
      mdl.getModelData().setModelScore(v);
    }
   
   @Override public SumpModel getModel()                        { return for_model; }
   @Override public SumpModel getPatternModel()                 { return pattern_model; }
   @Override public CoseResult getCoseResult()                  { return for_result; }
   @Override public Map<String,String> getNameMap()             { return name_map; }
   @Override public CoseResult getLocalTestResult()             { return local_tests; }
   @Override public CoseResult getGlobalTestResult()            { return global_tests; }
   
   void updateResult(CoseResult cr)                             { for_result = cr; }
   void updateLocalTestResult(CoseResult cr)                    { local_tests = cr; }
   void updateGlobalTestResult(CoseResult cr)                   { global_tests = cr; }
   
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

