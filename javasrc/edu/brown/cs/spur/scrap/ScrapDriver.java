/********************************************************************************/
/*                                                                              */
/*              ScrapDriver.java                                                */
/*                                                                              */
/*      Command line (and test) driver for Search-based Abstraction             */
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
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;

import edu.brown.cs.cose.cosecommon.CoseConstants;
import edu.brown.cs.cose.cosecommon.CoseDefaultRequest;
import edu.brown.cs.cose.cosecommon.CoseMaster;
import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseScores;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseScopeType;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseSearchEngine;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseSearchType;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.spur.lids.LidsFinder;
import edu.brown.cs.spur.stare.StareDriver;
import edu.brown.cs.spur.sump.SumpConstants;
import edu.brown.cs.spur.sump.SumpData;
import edu.brown.cs.spur.sump.SumpParameters;
import edu.brown.cs.spur.sump.SumpVisitor;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;
import edu.brown.cs.spur.swift.SwiftScorer;

public class ScrapDriver implements ScrapConstants
{


/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   ScrapDriver sd = new ScrapDriver(args);
   sd.processAbstractor();
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CoseDefaultRequest      search_request;
private ScrapResultSet          search_result;
private SumpParameters          search_params;

static {
   IvyLog.setupLogging("SCRAP",true);
   IvyLog.useStdErr(false);
   IvyLog.setLogFile("/ws/volfred/spr/scrap.log");
   IvyLog.setLogLevel(IvyLog.LogLevel.DEBUG);
}

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ScrapDriver(String... args)
{
   search_request = new ScrapRequest();
   search_result = new ScrapResultSet();
   search_params = new SumpParameters();
   
   scanArgs(args);
}



ScrapDriver(SumpData sd)
{
   search_request = (CoseDefaultRequest) sd.getCoseRequest();
   search_result = new ScrapResultSet();
   search_params = sd.getParameters();
}


/********************************************************************************/
/*                                                                              */
/*      Argument scanning                                                       */
/*                                                                              */
/********************************************************************************/

private void scanArgs(String [] args)
{
   boolean termsonly = false;
   
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-") && args[i].length() > 1) {
         if (args[i].startsWith("-nt") && i+1 < args.length) {          // -nthreads #
            try {
               int nt = Integer.parseInt(args[++i]);
               search_request.setNumberOfThreads(nt);
             }
            catch (NumberFormatException e) {
               badArgs();
             }
          }
         else if (args[i].startsWith("-nr") && i+1 < args.length) {     // -nresults #
            try {
               int nr = Integer.parseInt(args[++i]);
               search_request.setNumberOfResults(nr);
             }
            catch (NumberFormatException e) {
               badArgs();
             }
          }   
         else if (args[i].startsWith("-D")) {                           // -Debug
            search_request.setDoDebug(true);
          }
         else if (args[i].startsWith("-m")) {                           // -method
            search_request.setCoseSearchType(CoseSearchType.METHOD);
            search_request.setCoseScopeType(CoseScopeType.FILE);
          }
         else if (args[i].startsWith("-c")) {                           // -class
            search_request.setCoseSearchType(CoseSearchType.CLASS);
            search_request.setCoseScopeType(CoseScopeType.FILE);
          }  
         else if (args[i].startsWith("-pi")) {                           // -package-interface
            search_request.setCoseSearchType(CoseSearchType.PACKAGE);
            search_request.setCoseScopeType(CoseScopeType.PACKAGE_IFACE);
          } 
         else if (args[i].startsWith("-pu")) {                           // -package-used
            search_request.setCoseSearchType(CoseSearchType.PACKAGE);
            search_request.setCoseScopeType(CoseScopeType.PACKAGE_USED);
          } 
         else if (args[i].startsWith("-p")) {                           // -package
            search_request.setCoseSearchType(CoseSearchType.PACKAGE);
            search_request.setCoseScopeType(CoseScopeType.PACKAGE);
          } 
         else if (args[i].startsWith("-sys")) {                         // -system
            search_request.setCoseSearchType(CoseSearchType.PACKAGE);
            search_request.setCoseScopeType(CoseScopeType.SYSTEM);
          } 
         else if (args[i].startsWith("-sou")) {
            String src = args[++i];
            search_request.addSpecificSource(src);
          }
         else if (args[i].startsWith("-P") && i+1 < args.length) {      // -P name=value
            String what = args[++i];
            int idx = what.indexOf("=");
            if (idx > 0) {
               String key = what.substring(0,idx).trim();
               String val = what.substring(idx+1).trim();
               try {
                  double d = Double.valueOf(val);
                  search_params.set(key,d);
                }
               catch (NumberFormatException e) {
                  badArgs();
                }
             }
            else badArgs();
          }
         else if (args[i].startsWith("-GIT")) {
            search_request.setSearchEngine(CoseSearchEngine.GITHUB);
          }
         else if (args[i].startsWith("-REP")) {
            search_request.setSearchEngine(CoseSearchEngine.GITREPO);
          }
         else if (args[i].startsWith("-ZIP")) {
            search_request.setSearchEngine(CoseSearchEngine.GITZIP);
          }
         else if (args[i].startsWith("-SEA")) {
            search_request.setSearchEngine(CoseSearchEngine.SEARCHCODE);
          }
         else if (args[i].startsWith("-t")) {
            termsonly = true;
          }
       }
      else {
         if (args[i].equals("-")) {
            search_request.addKeywordSet();
          }
         else if (termsonly) search_request.addKeyTerm(args[i]);
         else search_request.addKeyword(args[i]);
       }
    }
}
   
   
   
private void badArgs()
{
   System.err.println("SCRAP [-nt #thread] [-nr #result] keyword ... ");
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

void processAbstractor()
{
   List<CoseResult> rslts = getSearchResults();
   long start = System.currentTimeMillis();
   IvyLog.logS("SCRAP","RETURNED " + rslts.size() + " RESULTS");
   IvyLog.logS("SCRAP","SKIPPED " + search_result.getNumberRemoved() + " RESULTS");
   
   rslts = removeOverlaps(rslts);
   IvyLog.logS("SCRAP","NON-OVERLAPPING " + rslts.size() + " RESULTS");
   
   List<CoseResult> trslts = getFilteredResults(rslts);
   IvyLog.logS("SCRAP","FILTERED " + trslts.size() + " RESULTS");
   IvyLog.logS("SCRAP","Filter time " + (System.currentTimeMillis() - start));
   
   try {
      AbstractionType at = getAbstractionType();
      findAbstraction(at,trslts,rslts);
    }
   catch (Throwable t) {
      t.printStackTrace();
      System.err.println("Problem creating abstraction");
    }
}



void processBuildCandidates(SumpModel mdl)
{
   long start0 = System.currentTimeMillis();
   
   List<CoseResult> rslts = getSearchResults();
   rslts = removeOverlaps(rslts);
   
   long start1 = System.currentTimeMillis();
   IvyLog.logS("SCRAP","Search Time: " + (start1-start0));
   
   List<ScrapCandidate> cands = null;
   ScrapCandidateBuilder scb = new ScrapCandidateBuilder(search_request,rslts,search_params);
   try {
      cands = scb.buildCandidates(mdl);
    }
   catch (Throwable t) {
      System.err.println("Problem building candidate: " + t);
      t.printStackTrace();
    }
   
   long start2 = System.currentTimeMillis();
   IvyLog.logS("SCRAP","Candidate Time: " + (start2-start1));
   IvyLog.logS("SCRAP","Candidate Count: " + (cands == null ? 0 : cands.size()));
   
   if (cands == null) return;
   
   StareDriver stare = new StareDriver();
   stare.addInitialSolutions(cands);
   stare.process();
   
   long start3 = System.currentTimeMillis();
   IvyLog.logS("SCRAP","Build/Compile Time: " + (start3-start2));
   
   for (ScrapCandidate sc : cands) {
      CoseResult cr = sc.getCoseResult();
      CoseScores cs = cr.getScores(search_request,cr.getStructure());
      if (cs == null) continue;
      IvyLog.logS("SPUR","SIZE," + (cs.getInt("TYPES") - cs.getInt("INNER")) + "," + 
            cs.getString("LINES") + "," +
            cs.getString("TYPES") + "," + cs.getString("INNER") + "," + 
            cs.getString("INTERFACES") + "," + cs.getString("METHODS") + "," +
            cs.getString("FIELDS"));
    }
}


/********************************************************************************/
/*                                                                              */
/*      Library processing                                                      */
/*                                                                              */
/********************************************************************************/

static LidsFinder findLibraries(CompilationUnit cu,CoseResult cr)
{
   Set<String> imports = new HashSet<>();
   PackageDeclaration pd = cu.getPackage();
   String pnm = null;
   if (pd != null) pnm = pd.getName().getFullyQualifiedName();
   for (Object o : cu.imports()) {
      ImportDeclaration id = (ImportDeclaration) o;
      String idnm = id.getName().getFullyQualifiedName();
      if (CoseConstants.isRelatedPackage(pnm,idnm)) {
         // check if the class exists
         continue;
       }     
      if (CoseConstants.isStandardJavaLibrary(idnm)) continue; 
      if (id.isOnDemand()) idnm += ".*";
      imports.add(idnm);
    }
   
   LidsFinder fndr = new LidsFinder(cr);
   for (String s : imports) fndr.addImportPath(s);
   
   return fndr;
}




/********************************************************************************/
/*                                                                              */
/*      Searching                                                               */
/*                                                                              */
/********************************************************************************/

private List<CoseResult> getSearchResults()
{
   CoseMaster cm = CoseMaster.createMaster(search_request);
   try {
      cm.computeSearchResults(search_result);
    }
   catch (Throwable t) {
      IvyLog.logE("PROBLEM GETTING RESULTS: " + t,t);
    }
   
   List<CoseResult> rslts = search_result.getResults();
 
   return rslts;
}


private List<CoseResult> getFilteredResults(List<CoseResult> rslts)
{
   List<CoseResult> trslts = filterResults(rslts,true);
   IvyLog.logI("RETURNED " + trslts.size() + " TIGHT FILTERED RESULTS");
   if (trslts.size() <= 4 || !search_params.useTightFiltering()) {
      trslts = filterResults(rslts,false);
      IvyLog.logI("RETURNED " + trslts.size() + " FILTERED RESULTS");
    }
   return trslts;
}


private AbstractionType getAbstractionType()
{
   AbstractionType at = null;
   switch (search_request.getCoseSearchType()) {
      case CLASS :
         at = AbstractionType.CLASS;
         break;
      case METHOD :
         at = AbstractionType.METHOD;
         break;
      case PACKAGE :
         at = AbstractionType.PACKAGE;
         break;
      default :
         break;
    }
   
   return at;
}




/********************************************************************************/
/*                                                                              */
/*      Abstraction finding                                                     */
/*                                                                              */
/********************************************************************************/

private void findAbstraction(AbstractionType at,List<CoseResult> rslts,List<CoseResult> all)
{
   Set<String> done = new HashSet<>();
   long start = System.currentTimeMillis();
   
   try {
      ScrapAbstractor sa = new ScrapAbstractor(search_request,search_params);
      int dupct = 0;
      for (CoseResult cr : rslts) {
         String txt = cr.getKeyText();
         String key = IvyFile.digestString(txt);
         if (!done.add(key)) {
            ++dupct;
            continue;
          }
         sa.addToAbstractor(cr);
       }
      if (dupct > 0) {
         IvyLog.logS("SCRAP","REMOVED " + dupct + " DUPICATE SOLUTIONS");
       }
      long start1 = System.currentTimeMillis();
      IvyLog.logS("SCRAP","Abstraction time: " + (start1 - start));
      
      sa.orderAndPrune();
      sa.outputAbstractor(at);
      long start2 = System.currentTimeMillis();
      IvyLog.logS("SCRAP","Order, Prune, Output Time: " + (start2 - start1));  
      
      computeSizes(sa,at);
      computeTextMatches(sa,at,all,false);
      computeTextMatches(sa,at,all,true);
      computeTestCases(sa,at);
      computeUmlMatches(sa,at,all);
    }
   catch (Throwable t) {
      t.printStackTrace();
      IvyLog.logE("Problem finding abstraction: " + t,t);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Compute sizes of the result                                             */
/*                                                                              */
/********************************************************************************/

private void computeSizes(ScrapAbstractor sa,AbstractionType at)
{
   if (at != AbstractionType.PACKAGE) return;
   
   SizeVisitor sv = new SizeVisitor();
   for (ScrapAbstraction abs : sa.getAbstractions(at)) {
      for (SumpModel mdl : abs.getUmlModels()) {
         mdl.accept(sv);
       }
    }
   sv.outputSizes();
}


private class SizeVisitor extends SumpVisitor {
   
   private double num_abstraction;
   private double num_class;
   private double num_operation;
   private double num_attribute;
   private double num_dependency;
   
   SizeVisitor() {
      num_abstraction = 0;
      num_class = 0;
      num_operation = 0;
      num_attribute = 0;
      num_dependency = 0;
    }
   
   void outputSizes() {
      IvyLog.logS("SCRAP","Total Abstractions: " + num_abstraction);
      if (num_abstraction == 0) return;
      IvyLog.logS("SCRAP","Classes/Abstraction: " + num_class / num_abstraction);
      IvyLog.logS("SCRAP","Depends/Abstraction: " + num_dependency / num_abstraction);
      if (num_class == 0) return;
      IvyLog.logS("SCRAP","Operations/Class: " + num_operation / num_class);
      IvyLog.logS("SCRAP","Attributes/Class: " + num_attribute / num_class);
    }

   @Override public void endVisit(SumpModel m)          { ++num_abstraction; }
   @Override public void endVisit(SumpClass c)          { ++num_class; }
   @Override public void endVisit(SumpOperation o)      { ++num_operation; }
   @Override public void endVisit(SumpAttribute a)      { ++num_attribute; }
   @Override public void endVisit(SumpDependency d)     { ++num_dependency; }
   
}       // end of inner class SizeVisitor



/********************************************************************************/
/*                                                                              */
/*      Look for text maches                                                    */
/*                                                                              */
/********************************************************************************/

private void computeTextMatches(ScrapAbstractor sa,AbstractionType at,List<CoseResult> all,boolean kgram)
{
   for (ScrapAbstraction pa : sa.getAbstractions(at)) {
      SortedSet<ScoredResult> queue = new TreeSet<>();
      System.err.println("FOR " + (kgram ? "KGRAM " : "TEXT ") + pa.getCoseResult().getSource());
      CoseResult par = pa.getCoseResult();
      SwiftScorer scorer = new SwiftScorer(par.getText(),(ASTNode) par.getStructure(),kgram);
      if (!kgram) {
         List<String> words = scorer.getTopWords();
         System.err.print("   WORDS: ");
         for (String s: words) System.err.print(" " + s);
         System.err.println();
       }
      for (CoseResult orig : all) {
         double sc = scorer.getScore(orig.getText(),(ASTNode) orig.getStructure());
         if (sc < 0.4) continue;
         if (sc >= 0.90) {
            if (orig == pa.getCoseResult()) continue;
            // String txt1 = pa.getCoseResult().getText();
            // String txt2 = orig.getText();
            // if (txt1.equals(txt2)) continue;
            // System.err.println("COMPARE1:\n" + txt1);
            // System.err.println("COMPARE2:\n" + txt2);
            // sc = scorer.getScore(orig.getText());
            // same text, but different order? -- is continue correct or should we accept?
            // continue;
          }
         queue.add(new ScoredResult(orig,sc));
       }
      for (ScoredResult sr : queue) {
         System.err.println(sr);
       }
    }
}


/********************************************************************************/
/*                                                                              */
/*      Find test cases                                                         */
/*                                                                              */
/********************************************************************************/

private void computeTestCases(ScrapAbstractor sa,AbstractionType at)
{
   // compute test cases for the abstraction as originally given
   
   for (ScrapAbstraction abs : sa.getAbstractions(at)) {
      System.err.println("TESTS FOR " + abs.getCoseResult().getSource());
      List<CoseResult> rslts = abs.getTestResults();
      for (CoseResult tr : rslts) {
         System.err.println("\t" + tr.getSource().getDisplayName());
       }
    }
}



 private void computeUmlMatches(ScrapAbstractor sa,AbstractionType at,List<CoseResult> all)
{
   if (at != AbstractionType.PACKAGE) return;
   
   Map<CoseResult,SumpModel> mmap = new HashMap<>();
   for (CoseResult orig : all) {
      CompilationUnit cu = (CompilationUnit) orig.getStructure();
      JcompProject proj = null;
      if (!JcompAst.isResolved(cu)) {
         proj = sa.getResolvedAst(cu);
         JcompAst.setProject(cu,proj);
         if (proj == null) continue;
       }
      SumpData sdata = new SumpData(null,orig,search_params);
      SumpModel mdl = SumpConstants.SumpFactory.createModel(sdata,cu);
      mmap.put(orig,mdl);
      
      // System.err.println("SOURCEMODEL FOR " + orig.getSource() + ":");
      // IvyXmlWriter xw = new IvyXmlWriter();
      // mdl.outputXml(xw);
      // System.err.println(xw.toString());
      // xw.close();
      
      if (proj != null) {
         sa.getJcompControl().freeProject(proj);
         JcompAst.setProject(cu,null);
       }
    }
   
   for (ScrapAbstraction pa : sa.getAbstractions(at)) {
      System.err.println("UMLMATCH FOR " + pa.getCoseResult().getSource());
      Set<CoseResult> match = new HashSet<>();
      for (SumpModel mdl : pa.getUmlModels()) {
         for (Map.Entry<CoseResult,SumpModel> ent : mmap.entrySet()) {
            if (ent.getValue().contains(mdl)) {
               match.add(ent.getKey());
             }
          }
         for (CoseResult cr : match) {
            System.err.println("\tUML MATCH WITH " + cr.getSource());
          }
       }
    }
}




private List<CoseResult> filterResults(List<CoseResult> results,boolean tight)
{
   List<CoseResult> filtered = new ArrayList<>();
   for (CoseResult cr : results) {
      CoseScores scores = cr.getScores(search_request);
      if (acceptResult(scores,tight)) filtered.add(cr);
    }
   
   filtered = removeOverlaps(filtered);
   
   return filtered;
}



private List<CoseResult> removeOverlaps(List<CoseResult> results)
{
   if (search_request.getCoseSearchType() != CoseSearchType.PACKAGE) 
      return results;
   
   Map<String,List<CoseResult>> projmap = new HashMap<>();
   for (CoseResult cr : results) {
      String pid = cr.getSource().getProjectId();
      List<CoseResult> rs = projmap.get(pid);
      if (rs == null) {
         rs = new ArrayList<>();
         projmap.put(pid,rs);
       }
      rs.add(cr);
    }
   
   Set<CoseResult> remove = new HashSet<>();
   for (List<CoseResult> check : projmap.values()) {
      if (check.size() <= 1) continue;
      for (int i = 0; i < check.size(); ++i) {
         CoseResult cr1 = check.get(i);
         if (remove.contains(cr1)) continue;
         Collection<String> p1 = cr1.getPackages();
         for (int j = i+1; j < check.size(); ++j) {
            CoseResult cr2 = check.get(j);
            if (remove.contains(cr2)) continue;
            Collection<String> p2 = cr2.getPackages();
            if (p1.containsAll(p2)) remove.add(cr2);
            else if (p2.containsAll(p1)) remove.add(cr1);
          }
       }
    }
   
   if (remove.isEmpty()) return results;
   
   List<CoseResult> rslt = new ArrayList<>(results);
   rslt.removeAll(remove);
   return rslt;
}









private boolean acceptResult(CoseScores scores,boolean tight)
{
   CoseSearchType styp = search_request.getCoseSearchType();
   
   if (scores.getBoolean("ABSTRACT")) return false;
   
   int minmatch = 1;
   switch (styp) {
      case PACKAGE :
         minmatch = search_params.getMinPackageTermMatches();
         if (scores.getInt("FIELDS") < search_params.getMinFields()) return false;
         if (scores.getInt("METHODS") < search_params.getMinMethods()) return false;
         if (scores.getInt("TYPES") < search_params.getMinTypes()) return false;
         if (scores.getInt("TYPES") > search_params.getMaxTypes()) return false;
         break; 
      case CLASS :
         minmatch = 10;
         if (scores.getInt("ACCESSIBLE") < 2) return false;
         if (scores.getInt("FIELDS") == 0) return false;
         if (scores.getInt("METHODS") < 2) return false;
         if (scores.getBoolean("TESTCASE")) return false;
         break;
      case METHOD : 
         if (scores.getBoolean("TESTCASE")) return false;
         if (scores.getBoolean("NO_LOOPS")) return false;
         if (scores.getBoolean("TRIVIAL")) return false;
         break;
      default :
         break;
    }
   
   if (scores.getInt("TERMMATCHES") < minmatch) return false;
   
   if (tight) {
      if (scores.getInt("TERMTITLEMATCH") == 0 && 
            scores.getDouble("KEYMATCH") != 1) 
         return false;
    }
   
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Order results by score                                                  */
/*                                                                              */
/********************************************************************************/

private static class ScoredResult implements Comparable<ScoredResult> {

   private double score_value;
   private CoseResult cose_result;
   
   ScoredResult(CoseResult cr,double v) {
      score_value = v;
      cose_result = cr;
    }
   
   @Override public int compareTo(ScoredResult sr) {
      return Double.compare(sr.score_value,score_value);
    }
   
   @Override public String toString() {
      return "   " + score_value + " :\t" + cose_result.getSource();
    }
   
}       // end of inner class ScoredResult







}       // end of class ScrapDriver




/* end of ScrapDriver.java */

