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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.CompilationUnit;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.spur.etch.EtchFactory;
import edu.brown.cs.spur.sump.SumpConstants;
import edu.brown.cs.spur.sump.SumpData;
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



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ScrapCandidateBuilder(List<CoseResult> ar)
{
   all_results = ar;
   jcomp_control = new JcompControl();
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
      Map<String,String> namemap = cm.getNameMap();
      CoseResult cr = cm.getCoseResult();
      CoseResult cr1 = etcher.fixNames(cr,namemap);
      cm.updateResult(cr1);
    }
   
   return new ArrayList<>(match);
}




private List<CandidateMatch> findInitialMatches(SumpModel model)
{
   Map<CoseResult,SumpModel> mmap = new HashMap<>();
   for (CoseResult orig : all_results) {
      CompilationUnit cu = (CompilationUnit) orig.getStructure();
      JcompProject proj = null;
      if (!JcompAst.isResolved(cu)) {
         proj = JcompAst.getResolvedAst(jcomp_control,cu);
         JcompAst.setProject(cu,proj);
         if (proj == null) continue;
       }
      SumpData sdata = new SumpData(null,orig);
      SumpModel mdl = SumpConstants.SumpFactory.createModel(sdata,cu);
      mmap.put(orig,mdl);
      if (proj != null) {
         jcomp_control.freeProject(proj);
         JcompAst.setProject(cu,null);
       }
    }
   
   Set<CandidateMatch> match = new TreeSet<>(); 
   for (Map.Entry<CoseResult,SumpModel> ent : mmap.entrySet()) {
      Map<String,String> rmap = new HashMap<>();
      double sv = ent.getValue().matchScore(model,rmap);
      if (sv != 0) {
         CandidateMatch cm = new CandidateMatch(model,ent.getKey(),sv,rmap);
         match.add(cm);
       }
    }
   
   return new ArrayList<>(match);
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
   
   CandidateMatch(SumpModel mdl,CoseResult cr,double v,Map<String,String> nmap) {
      for_model = mdl;
      for_result = cr;
      match_value = v;
      name_map = new HashMap<>(nmap);
    }
   
   @Override public SumpModel getModel()                        { return for_model; }
   @Override public CoseResult getCoseResult()                  { return for_result; }
   @Override public Map<String,String> getNameMap()             { return name_map; }
   
   void updateResult(CoseResult cr)                             { for_result = cr; }
   
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

