/********************************************************************************/
/*                                                                              */
/*              SumpMatcher.java                                                */
/*                                                                              */
/*      Code for matching two UML diagrams                                      */
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.brown.cs.ivy.file.IvyStringDiff;

class SumpMatcher implements SumpConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static final double     CLASS_CUTOFF = 0.5;
private static final double     ATTR_CUTOFF = 0.75;
private static final double     METHOD_CUTOFF = 0.75;
private static final double     DEPEND_CUTOFF = 0.5;
private static final double     SCORE_CUTOFF = 0.75;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SumpMatcher()
{
}


/********************************************************************************/
/*                                                                              */
/*      Matching entry points                                                   */
/*                                                                              */
/********************************************************************************/

boolean contains(SumpModel base,SumpModel pat)
{
   SortedSet<MatchSet> sets = setupClassMatchings(base,pat);
   double max = sets.size() + 1;
      
   double rscore = matchClasses(base,pat,sets,null,0,max,max,null);   
   if (rscore == 0) return false;
   if (rscore < max * SCORE_CUTOFF) return false;
   
   return true;
}



double matchScore(SumpModel base,SumpModel pat,Map<String,String> nmap)
{
   SortedSet<MatchSet> sets = setupClassMatchings(base,pat);
   double max = sets.size() + 1;
   
   double rscore = matchClasses(base,pat,sets,null,0,max,max,nmap);   
   if (rscore == 0) return 0;
   
   rscore = rscore/max;
   if (rscore < SCORE_CUTOFF) rscore = 0;
   
   if (nmap !=  null) {
      nmap.put(getPrefixName(base),getPrefixName(pat));
    }
   
   return rscore;
}


private String getPrefixName(SumpModel mdl)
{
   String nm = mdl.getPackage().getName();
   return nm;
}


double containScore(SumpModel base,SumpModel pat)
{
   SortedSet<MatchSet> sets = setupClassMatchings(base,pat);
   double max = sets.size() + 1;
   
   double rscore = matchClasses(base,pat,sets,null,0,max,max,null);
   
   return rscore;
}




/********************************************************************************/
/*                                                                              */
/*      Setup potential matching between classes in a package                   */
/*                                                                              */
/********************************************************************************/

private SortedSet<MatchSet> setupClassMatchings(SumpModel base,SumpModel pat)
{
   SortedSet<MatchSet> rslt = new TreeSet<>();
   
   for (SumpClass cls : pat.getPackage().getClasses()) {
      MatchSet ms = new MatchSet(cls);
      for (SumpClass bcls : base.getPackage().getClasses()) {
         Map<String,String> namemap = new HashMap<>();
         double score = containsClass(bcls,cls,namemap);
         if (score >= CLASS_CUTOFF) {
            ms.addMatch(bcls,score,namemap);
          }
       }
      rslt.add(ms);
    } 
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Computer best class matching                                            */
/*                                                                              */
/********************************************************************************/

private double matchClasses(SumpModel base,SumpModel pat,
      SortedSet<MatchSet> sets,
      Map<SumpClass,SumpClass> cmap,double inscore,double max,double maxtogo,
      Map<String,String> rsltmap)
{
   if ((inscore + maxtogo)/max < SCORE_CUTOFF) return 0; 
   
   if (cmap == null) cmap = new HashMap<>();
   
   if (sets.isEmpty()) {
      double ascore = checkAssociations(base,pat,cmap);
      if (ascore == 0) return 0;
      return inscore+ascore;
    }
   
   MatchSet ms = sets.first();
   if (ms.getSize() == 0) {
      // no match for a given class
      Map<SumpClass,SumpClass> nmap = new HashMap<>(cmap);
      SortedSet<MatchSet> nsets = new TreeSet<>();
      for (MatchSet s : sets) {
         if (ms.getFromClass() == s.getFromClass()) continue;
         MatchSet ns = new MatchSet(s,null);
         nsets.add(ns);
       }
      double rscore = matchClasses(base,pat,nsets,nmap,inscore,max,maxtogo-1,rsltmap);
      return rscore;
    }
   
   Map<String,String> bestmap = null;
   double bestscore = 0;
   SumpClass bestcls = null;
 
   for (ScoredClass sccls : ms.getToClasses(5)) {
      SumpClass sc = sccls.getSumpClass();
      // System.err.println("MAP " + ms.getFromClass() + " ->  " + sc);

      Map<SumpClass,SumpClass> nmap = new HashMap<>(cmap);
      nmap.put(ms.getFromClass(),sc);
      SortedSet<MatchSet> nsets = new TreeSet<>();
      for (MatchSet s : sets) {
         if (ms.getFromClass() == s.getFromClass()) continue;
         MatchSet ns = new MatchSet(s,sc);
         nsets.add(ns);
       }
      double score = inscore + sccls.getScore();
      Map<String,String> rmap = null;
      if (rsltmap != null) rmap = new HashMap<>();
      double rscore = matchClasses(base,pat,nsets,nmap,score,max,maxtogo-1,rmap);
      if (rscore > 0) {
         if (rscore > bestscore) {
            if (rmap != null) {
               bestmap = new HashMap<>(rmap);
               bestmap.putAll(sccls.getNameMap());
             }           
            bestscore = rscore;
            bestcls = sc;
          }
       }
    }
   
   if (bestscore == 0) return 0;
   
   if (rsltmap != null) {
      rsltmap.putAll(bestmap);
      rsltmap.put(bestcls.getFullName(),ms.getFromClass().getFullName());
    }
   
   return bestscore;
}



private static class MatchSet implements Comparable<MatchSet> {

   private SumpClass for_class;
   private Set<ScoredClass> matched_classes;
   private MatchComparer match_comparer;
  
   
   MatchSet(SumpClass base) {
      for_class = base;
      match_comparer = new MatchComparer(base.getName());
      matched_classes = new TreeSet<>();
    }
   
   MatchSet(MatchSet orig,SumpClass remove) {
      for_class = orig.for_class;
      match_comparer = orig.match_comparer;
      matched_classes = new TreeSet<>();
      for (ScoredClass sc : orig.matched_classes) {
         if (sc.getSumpClass() != remove) addMatch(sc.getSumpClass(),sc.getScore(),sc.getNameMap());
       }
    }
   
   void addMatch(SumpClass match,double score,Map<String,String> namemap) {
      matched_classes.add(new ScoredClass(match,score,match_comparer,namemap));
    }
   
   int getSize()                        { return matched_classes.size(); }
   SumpClass getFromClass()             { return for_class; }
   
   List<ScoredClass> getToClasses(int max) {
      List<ScoredClass> rslt = new ArrayList<>();
      int ct = 0;
      for (ScoredClass sc : matched_classes) {
         rslt.add(sc);
         if (max > 0 && ct++ >= max) break;
       }
      return rslt;
    }
   
   @Override public int compareTo(MatchSet ms) {
      int delta = matched_classes.size() - ms.matched_classes.size();
      if (delta != 0) return delta;
      return for_class.getName().compareTo(ms.for_class.getName());
    }
   
}       // end of inner class MatchSet



private static class ScoredClass implements Comparable<ScoredClass> {

   private SumpClass sump_class;
   private double class_score;
   private MatchComparer name_comparer;
   private Map<String,String> name_map;
   
   ScoredClass(SumpClass sc,double score,MatchComparer mc,Map<String,String> namemap) {
      sump_class = sc;
      class_score = score;
      name_comparer = mc;
      name_map = new HashMap<>(namemap);
    }
   
   SumpClass getSumpClass()                     { return sump_class; }
   double getScore()                            { return class_score; }
   Map<String,String> getNameMap()              { return name_map; }
   
   @Override public int compareTo(ScoredClass sc) {
      if (class_score > sc.class_score) return -1;
      if (class_score < sc.class_score) return 1;
      return name_comparer.compare(this.getSumpClass(),sc.getSumpClass());
    }
   
}       // end of inner class ScoredClass




private static class MatchComparer implements Comparator<SumpClass> 
{
   private String base_name;
   
   MatchComparer(String base) {
      base_name = base;
    }
   
   @Override public int compare(SumpClass c1,SumpClass c2) {
      double v1 = IvyStringDiff.normalizedStringDiff(base_name,c1.getName());
      double v2 = IvyStringDiff.normalizedStringDiff(base_name,c2.getName());
      if (v1 < v2) return 1;
      if (v1 > v2) return -1;
      return c1.getName().compareTo(c2.getName());
    }
   
}       // end of inner class MatchComparer




/********************************************************************************/
/*                                                                              */
/*      Matching between classes                                                */
/*                                                                              */
/********************************************************************************/

private double containsClass(SumpClass base,SumpClass pat,Map<String,String> namemap)
{
   Set<Object> done = new HashSet<>();
   
   double atot = 0;
   double afnd = 0;
   for (SumpAttribute att : pat.getAttributes()) {
      atot += 1;
      double v = -1;
      SumpAttribute best = null;
      for (SumpAttribute batt : base.getAttributes()) {
         if (done.contains(batt)) continue;
         if (matchAttribute(batt,att)) {
            double v1 = IvyStringDiff.normalizedStringDiff(batt.getName(),att.getName());
            if (best == null || v1 > v) {
               v = v1;
               best = batt;
             }
          }
       }
      if (best != null) {
         afnd += 1;
         done.add(best);
         if (namemap != null) {
            namemap.put(best.getFullName(),att.getFullName());
          }
       }
    }
   if (atot > 0 && afnd / atot < ATTR_CUTOFF) return 0;
   
   double mtot = 0;
   double mfnd = 0;
   for (SumpOperation op : pat.getOperations()) {
      mtot += 1;
      double v = -1;
      SumpOperation best = null;
      Map<String,String> bestmap = null;
      for (SumpOperation bop : base.getOperations()) {
         if (done.contains(bop)) continue;
         Map<String,String> pnamemap = matchOperation(bop,op);
         if (pnamemap != null) {
            double v1 = IvyStringDiff.normalizedStringDiff(bop.getName(),op.getName());
            if (best == null || v1 > v) {
               v = v1;
               best = bop;
               bestmap = pnamemap;
             }
          }
       }
      if (best != null) {
         mfnd += 1;
         done.add(best);
         if (namemap != null) {
            namemap.put(best.getFullName(),op.getFullName());
            namemap.putAll(bestmap);
          }
       }
    }
   if (mtot > 0 && mfnd / mtot < METHOD_CUTOFF) return 0; 
   
   double score = 1;
   if (atot != 0 && mtot != 0) score = (afnd/atot + mfnd/mtot)/2.0;
   else if (mtot != 0) score = mfnd/mtot;
   else if (atot != 0) score = afnd/atot;
   
   return score;
}


private boolean matchAttribute(SumpAttribute base,SumpAttribute pat)
{
   SumpDataType bdt = base.getDataType();
   SumpDataType pdt = pat.getDataType();
   
   return matchType(bdt,pdt);
}


private boolean matchType(SumpDataType base,SumpDataType pat)
{
   // check for OBJECT in pattern or base? 
   
   if (base.getArgType() != pat.getArgType()) return false;
   
   if (base.getArgType() == SumpArgType.USERTYPE) {
      // compare actual types and ensure they are consistent
      // note that this requires a type mapping
      // and this function is used before the type mappings exist
    }
   
   return true; 
}

private Map<String,String> matchOperation(SumpOperation base,SumpOperation pat)
{
   // should return name map or null
   Map<String,String> namemap = new HashMap<>();
   
   SumpDataType bdt = base.getReturnType();
   SumpDataType pdt = pat.getReturnType();
   
   if (!matchType(bdt,pdt)) return null;
   
   Collection<SumpParameter> bsps = base.getParameters();
   Collection<SumpParameter> psps = pat.getParameters();
   if (bsps.size() != psps.size()) return null;
   
   Set<SumpParameter> done = new HashSet<>();
   for (SumpParameter pp : psps) {
      // match with best based on name mapping
      SumpParameter best = null;
      double bestval = 0;
      for (SumpParameter bp : bsps) {
         if (done.contains(bp)) continue;
         if (matchType(pp.getDataType(),bp.getDataType())) {
            double v = IvyStringDiff.normalizedStringDiff(pp.getName(),bp.getName());
            if (best == null || v > bestval) {
               best = bp;
               bestval = v;
             }
          }
       }
      if (best == null) return null;
      namemap.put(best.getFullName(),pp.getFullName());
      // add name mapping here
      done.add(best);
    }
   
   return namemap;
}



/********************************************************************************/
/*                                                                              */
/*      Association matching                                                    */
/*                                                                              */
/********************************************************************************/

private double checkAssociations(SumpModel base,SumpModel pat,Map<SumpClass,SumpClass> cmap)
{
   double tota = 0;
   double totm = 0;
   for (SumpDependency sd : pat.getPackage().getDependencies()) {
      SumpClass bfrm = cmap.get(sd.getFromClass());
      SumpClass bto = cmap.get(sd.getToClass());
      if (bfrm == null || bto == null) continue;
      tota += 1;
      boolean fnd = false;
      for (SumpDependency bsd : base.getPackage().getDependencies()) {
         if (bsd.getFromClass() == bfrm && bsd.getToClass() == bto) {
            fnd = true;
            break;
          }
         if (bsd.getFromClass() == bto && bsd.getToClass() == bfrm) {
            fnd = true;
            break;
          }
       }
      if (fnd) totm += 1;
    }
   
   if (tota == 0) return 1.0;
   else if (totm/tota < DEPEND_CUTOFF) return 0;
   
   return totm/tota;
}



}       // end of class SumpMatcher




/* end of SumpMatcher.java */

