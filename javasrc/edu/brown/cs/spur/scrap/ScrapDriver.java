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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.CompilationUnit;

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
import edu.brown.cs.spur.stare.StareDriver;
import edu.brown.cs.spur.sump.SumpConstants;
import edu.brown.cs.spur.sump.SumpData;
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
   
   scanArgs(args);
}



ScrapDriver(SumpData sd)
{
   search_request = (CoseDefaultRequest) sd.getCoseRequest();
   search_result = new ScrapResultSet();
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
         else if (args[i].startsWith("-pu")) {                           // -package-interface
            search_request.setCoseSearchType(CoseSearchType.PACKAGE);
            search_request.setCoseScopeType(CoseScopeType.PACKAGE_USED);
          } 
         else if (args[i].startsWith("-p")) {                           // -package
            search_request.setCoseSearchType(CoseSearchType.PACKAGE);
            search_request.setCoseScopeType(CoseScopeType.PACKAGE);
          } 
         else if (args[i].startsWith("-s")) {                           // -system
            search_request.setCoseSearchType(CoseSearchType.PACKAGE);
            search_request.setCoseScopeType(CoseScopeType.SYSTEM);
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
   List<CoseResult> trslts = getFilteredResults(rslts);
   AbstractionType at = getAbstractionType();
   
   try {
      findAbstraction(at,trslts,rslts);
    }
   catch (Throwable t) {
      t.printStackTrace();
      System.err.println("Problem creating abstraction");
    }
}



void processBuildCandidates(SumpModel mdl)
{
   List<CoseResult> rslts = getSearchResults();
  
   List<ScrapCandidate> cands = null;
   ScrapCandidateBuilder scb = new ScrapCandidateBuilder(rslts);
   try {
      cands = scb.buildCandidates(mdl);
    }
   catch (Throwable t) {
      System.err.println("Problem building candidate: " + t);
      t.printStackTrace();
    }
   
   StareDriver stare = new StareDriver();
   stare.addInitialSolutions(cands);
   stare.process();
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
   IvyLog.logI("RETURNED " + rslts.size() + " RESULTS");
   
   return rslts;
}


private List<CoseResult> getFilteredResults(List<CoseResult> rslts)
{
   List<CoseResult> trslts = filterResults(rslts,true);
   IvyLog.logI("RETURNED " + trslts.size() + " TIGHT FILTERED RESULTS");
   if (trslts.size() <= 4) {
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
   
   try {
      ScrapAbstractor sa = new ScrapAbstractor(search_request);
      for (CoseResult cr : rslts) {
         String txt = cr.getKeyText();
         String key = IvyFile.digestString(txt);
         if (!done.add(key)) {
            IvyLog.logI("Duplicate result");
            continue;
          }
         sa.addToAbstractor(cr);
       }
      sa.orderAndPrune();
      sa.outputAbstractor(at);
      
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



private void computeTextMatches(ScrapAbstractor sa,AbstractionType at,List<CoseResult> all,boolean kgram)
{
   for (ScrapAbstraction pa : sa.getAbstractions(at)) {
      SortedSet<ScoredResult> queue = new TreeSet<>();
      System.err.println("FOR " + (kgram ? "KGRAM " : "TEXT ") + pa.getCoseResult().getSource());
      SwiftScorer scorer = new SwiftScorer(pa.getCoseResult().getText(),kgram);
      if (!kgram) {
         List<String> words = scorer.getTopWords();
         System.err.print("   WORDS: ");
         for (String s: words) System.err.print(" " + s);
         System.err.println();
       }
      for (CoseResult orig : all) {
         double sc = scorer.getScore(orig.getText());
         String t1 = pa.getCoseResult().getSource().toString();
         String t2 = orig.getSource().toString();
         if (t1.contains("AcceptTask") || t1.contains("SimpleWebServer")) {
            if (t2.contains("AcceptTask") || t2.contains("SimpleWebServer")) {
             }
          }
         
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


private void computeTestCases(ScrapAbstractor sa,AbstractionType at)
{
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
      SumpData sdata = new SumpData(null,orig);
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
   return filtered;
}





private boolean acceptResult(CoseScores scores,boolean tight)
{
   CoseSearchType styp = search_request.getCoseSearchType();
   
   if (scores.getBoolean("TESTCASE")) return false;
   if (scores.getBoolean("TRIVIAL")) return false;
   if (scores.getBoolean("NO_LOOPS")) return false;
   if (scores.getBoolean("ABSTRACT")) return false;
   
   int minmatch = 1;
   switch (styp) {
      case PACKAGE :
         minmatch = 25;
         if (scores.getInt("FIELDS") < 5) return false;
         if (scores.getInt("METHODS") < 10) return false;
         if (scores.getInt("TYPES") < 3) return false;
         break; 
      case CLASS :
         minmatch = 10;
         if (scores.getInt("ACCESSIBLE") < 2) return false;
         if (scores.getInt("FIELDS") == 0) return false;
         if (scores.getInt("METHODS") < 2) return false;
         break;
      case METHOD : 
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

