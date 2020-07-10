/********************************************************************************/
/*                                                                              */
/*              ScrapAbstraction.java                                           */
/*                                                                              */
/*      Holder of an abstraction                                                */
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

import org.eclipse.jdt.core.dom.ASTNode;

import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseResult;

import edu.brown.cs.ivy.file.IvyWordPluralFilter;
import edu.brown.cs.ivy.file.IvyWordStemmer;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.spur.lids.LidsConstants.LidsLibrary;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;

public abstract class ScrapAbstraction implements ScrapConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<ASTNode,CoseResult> result_set;

protected JcompControl          jcomp_main;
protected int                   use_count;
protected Set<String>           name_set;
protected ScrapAbstractor       scrap_abstractor;
protected List<CoseResult>      test_results;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected ScrapAbstraction(ScrapAbstractor abs,CoseResult cr,ASTNode an)
{
   if (an == null) an = (ASTNode) cr.getStructure();
   result_set = new HashMap<>();
   result_set.put(an,cr);
   
   jcomp_main = abs.getJcompControl();
   use_count = 1;
   name_set = new HashSet<>();
   scrap_abstractor = abs;
   
   test_results = null;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

int getUseCount()                               { return use_count; }

int getResultCount()                            { return result_set.size(); }


Collection<CoseResult> getAllResults()          { return result_set.values(); }

CoseResult getCoseResult() 
{
   for (CoseResult cr : result_set.values()) {
      return cr;
    }
   return null;
}

abstract AbstractionType getAbstractionType();


/********************************************************************************/
/*                                                                              */
/*      Merge methods                                                           */
/*                                                                              */
/********************************************************************************/

abstract public boolean mergeWith(ScrapAbstraction cr);



protected boolean checkMatch(ScrapMergeData md,List<? extends ScrapComponent> c1,List<? extends ScrapComponent> c2,
      double tha,double thb,double thm)
{
   double compa = 0;
   double compb = 0;
   double compna = 0;
   double compnb = 0;
   double compab = 0;
   for (ScrapComponent cf : c1) {
      if (md.isUsed(cf)) {
         ++compa;
         if (md.getMapping(cf) != null) ++compab;
       }
      else ++compna;
    }
   for (ScrapComponent cf : c2) {
      if (md.isUsed(cf)) {
         ++compb;
       }
      else ++compnb;
    }
   double compatot = compa+compna;
   double compbtot = compb+compnb;
   double matcha = 1.0;
   if (compatot > 0) {
      matcha = compa / compatot;
    }
   double matchb = 1.0;
   if (compbtot > 0) {
      matchb = compb / compbtot;
    }
   double matched = 1.0;
   if (compatot > 0 && compbtot > 0) {
      matched = compab / Math.min(compatot,compbtot);
    }
   
   if (matcha > matchb) {
      double t = matcha;
      matcha = matchb;
      matchb = t;
    }
   if (matcha < tha) return false;
   if (matchb < thb) return false;
   if (matched < thm) return false;
   
   return true;
}




protected void matchItems(List<? extends ScrapComponent> orig,
      List<? extends ScrapComponent> add,
      ScrapMergeData md,boolean wholename,boolean approx)
{
   for (ScrapComponent nsc : add) {
      if (md.isUsed(nsc)) continue;
      String name = nsc.getName();
      ScrapComponent bestc = null;
      double bestv = -1;
      for (ScrapComponent osc : orig) {
         if (md.isUsed(osc)) continue;
         boolean match = (approx ? nsc.isComparableTo(osc) : nsc.isCompatibleWith(osc));
         if (match) {
            double score = -1;
            if (wholename) {
               if (osc.containsName(name)) {
                  score = osc.getCount();
                }
             }
            else {
               score = osc.getNameMatch(nsc);
             }
            if (score >= 0 && score > bestv) {
               bestv = score;
               bestc = osc;
             }
          }
       }
      if (bestc == null) continue;
      md.addMapping(nsc,bestc); 
    }
}



public void superMergeWith(ScrapAbstraction cr)
{
   use_count += cr.use_count;
   result_set.putAll(cr.result_set);
   name_set.addAll(cr.name_set);
}



/********************************************************************************/
/*                                                                              */
/*      Test case methods                                                       */
/*                                                                              */
/********************************************************************************/

public List<CoseResult> getTestResults()
{
   if (test_results != null) return test_results;
  
   CoseRequest req = scrap_abstractor.getRequest();
   Map<String,CoseResult> found = new HashMap<>();
   ScrapTestFinder finder = new ScrapTestFinder(req,result_set.values());
   List<CoseResult> rslts = finder.getTestResults();
   for (CoseResult cr : rslts) {
      String src = cr.getSource().getDisplayName();
      found.put(src,cr);
    }
   
   test_results = new ArrayList<>(found.values());
   
   return test_results;
}



/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

protected JcompProject getResolvedAst(ASTNode an)
{
   return scrap_abstractor.getResolvedAst(an);
}




/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

static Set<String> getNameWords(String text)
{
   int len = text.length();
   if (len < 3) return null;
   List<Integer> breaks = new ArrayList<>();
   
   char prev = 0;
   for (int i = 0; i < len; ++i) {
      char ch = text.charAt(i);
      if (Character.isUpperCase(ch) && Character.isLowerCase(prev)) {
         breaks.add(i);
       }
      else if (Character.isDigit(ch) && !Character.isDigit(prev) && i > 0) {
         breaks.add(i);
       }
      else if (Character.isDigit(prev) && !Character.isDigit(ch)) {
         breaks.add(i);
       }
      else if (ch == '_' || ch == '-') {
         breaks.add(i);
       }
      prev = ch;
    }
   
   IvyWordStemmer stemmer = null;
   Set<String> rslt = new HashSet<>();
   addNameWords(text,stemmer,rslt);
   
   if (breaks.size() > 0) {
      int lbrk = 0;
      for (Integer brki : breaks) {
         if (brki - lbrk >= 2) {
            String wd = text.substring(lbrk,brki);
            addNameWords(wd,stemmer,rslt);
          }
         lbrk = brki;
       }
      if (len - lbrk >= 2) {
         String wd = text.substring(lbrk);
         addNameWords(wd,stemmer,rslt);
       }
    }
   
   return rslt;
}



private static void addNameWords(String wd,IvyWordStemmer stemmer,Set<String> rslt)
{
   if (wd == null || wd.length() < 2) return;
   
   wd = wd.toLowerCase();
   rslt.add(wd);
   
   String sing = IvyWordPluralFilter.findSingular(wd);
   if (sing != null && !sing.equals(wd)) {
      rslt.add(sing);
      wd = sing;
    }
   
   if (stemmer != null) {
      String swd = stemmer.stem(wd);
      if (swd != null && !swd.equals(wd)) rslt.add(swd);
    }
}





/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

abstract void outputAbstraction(); 

void outputShortAbstraction()
{
   outputAbstraction();
}



/********************************************************************************/
/*                                                                              */
/*      UML related methods                                                     */
/*                                                                              */
/********************************************************************************/

List<SumpModel> getUmlModels()          
{
   return null;
}


void buildUmlModels()
{ }


static protected boolean isChild(ASTNode par,ASTNode child)
{
   if (par.getAST() != child.getAST()) return false;
   for (ASTNode p = child; p != null; p = p.getParent()) {
      if (p == par) return true;
    }
   return false;
}




/********************************************************************************/
/*                                                                              */
/*      Library related methods                                                 */
/*                                                                              */
/********************************************************************************/

List<LidsLibrary> getReferencedLibraries()
{
   return null;
}





}       // end of class ScrapAbstraction




/* end of ScrapAbstraction.java */

