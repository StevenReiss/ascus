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
import edu.brown.cs.spur.rowel.RowelMatcher;
import edu.brown.cs.spur.scrap.ScrapConstants.MatchType;

class SumpMatcher implements SumpConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

static final double     CLASS_CUTOFF = 0.5;
static final double     ATTR_CUTOFF = 0.5;
static final double     METHOD_CUTOFF = 0.5;
static final double     DEPEND_CUTOFF = 0.33;
static final double     SCORE_CUTOFF = 0.50;

static final double     INTERFACE_FRACTION = 0.25;
static final double     ENUM_FRACTION = 0.75;




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
      String n1 = getPrefixName(base);
      String n2 = getPrefixName(pat);
      nmap.put(n1,n2);
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
         double score = computeClassMatchScore(bcls,cls,namemap);
         double nmscore = IvyStringDiff.normalizedStringDiff(cls.getName(),bcls.getName());
         int sz = cls.getOperations().size() + cls.getAttributes().size();
         double f = 0.8;
         if (sz == 0) f = 0.2;
         else if (sz == 1) f = 0.5;
         score = score * f + nmscore * (1.0-f);
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
      if (pat.getPackage().getDependencies().size() == 0) ascore = 1;
      else ascore = 0;
      return inscore+ascore;
    }
   
   MatchSet ms = sets.first();
   if (ms.getSize() == 0) {
      // no match for a given class
      Map<SumpClass,SumpClass> nmap = new HashMap<>(cmap);
      SortedSet<MatchSet> nsets = new TreeSet<>();
      for (MatchSet s : sets) {
         if (ms.getFromClass() == s.getFromClass()) continue;
         MatchSet ns = new MatchSet(s,null,null,null,pat);
         nsets.add(ns);
       }
      double rscore = matchClasses(base,pat,nsets,nmap,inscore,max,maxtogo-1,rsltmap);
      return rscore;
    }
   
   Map<String,String> bestmap = null;
   double bestscore = 0;
   SumpClass bestcls = null;
   SumpClass fromcls = ms.getFromClass();
   
   int ct = 4;
   if (max > 4) ct = 3;
   if (max > 6) ct = 2;
   if (max > 8) ct = 1;
 
   for (ScoredClass sccls : ms.getToClasses(ct)) {
      SumpClass sc = sccls.getSumpClass();
      Map<SumpClass,SumpClass> nmap = new HashMap<>(cmap);
      nmap.put(ms.getFromClass(),sc);
      double usescore = sccls.getScore();
      System.err.println("MAP " + fromcls + " ->  " + sc);
      
      Collection<SumpClass> i1 = pat.getInheritedClasses(fromcls);
      Collection<SumpClass> i2 = base.getInheritedClasses(sc);
      if (!i1.isEmpty()) {
         for (SumpClass ifc : i1) {
            SumpClass nfc = nmap.get(ifc);
            if (nfc != null && !i2.contains(nfc)) usescore = 0;
          }
       }
      else {
        if (!i2.isEmpty()) 
           usescore = 0;
       }
      Collection<SumpClass> i3 = getSubClasses(pat,fromcls);
      if (!i3.isEmpty()) {
         Collection<SumpClass> i4 = getSubClasses(base,sc);
         for (SumpClass bas : i3) {
            SumpClass nbas = nmap.get(bas);
            if (nbas != null && !i4.contains(nbas)) usescore = 0;
          }
       }
      
      Collection<SumpClass> d1 = pat.getDependentClasses(fromcls);
      Collection<SumpClass> d2 = base.getDependentClasses(sc); 
      if (usescore == 0) {
         nmap.remove(ms.getFromClass());
         d1 = null;
         d2 = null;
       }
      
      SortedSet<MatchSet> nsets = new TreeSet<>();
      for (MatchSet s : sets) {
         if (ms.getFromClass() == s.getFromClass()) continue;
         MatchSet ns = new MatchSet(s,sc,d1,d2,pat);
         nsets.add(ns);
       }
      double score = inscore + usescore;
      Map<String,String> rmap = null;
      if (rsltmap != null) rmap = new HashMap<>();
      double rscore = matchClasses(base,pat,nsets,nmap,score,max,maxtogo-1,rmap);
      if (rscore > 0) {
         if (rscore > bestscore) {
            if (rmap != null) {
               bestmap = new HashMap<>(rmap);
               if (usescore != 0) bestmap.putAll(sccls.getNameMap());
             }           
            bestscore = rscore;
            bestcls = sc;
          }
       }
    }
   
   if (bestscore == 0) return 0;
   
   if (rsltmap != null) {
      rsltmap.putAll(bestmap);
      rsltmap.put(bestcls.getFullName(),fromcls.getFullName());
    }
   
   return bestscore;
}



private Collection<SumpClass> getSubClasses(SumpModel pat,SumpClass sc)
{
   Set<SumpClass> rslt = new HashSet<>();
   
   for (SumpClass cls : pat.getPackage().getClasses()) {
      Collection<SumpClass> ifcs = pat.getInheritedClasses(cls);
      if (ifcs.contains(sc)) rslt.add(cls);
    }
   
   return rslt;
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
   
   MatchSet(MatchSet orig,SumpClass remove,Collection<SumpClass> od,Collection<SumpClass> nd,
         SumpModel pat) {
      for_class = orig.for_class;
      match_comparer = orig.match_comparer;
      matched_classes = new TreeSet<>();
      for (ScoredClass sc : orig.matched_classes) {
         if (sc.getSumpClass() != remove) {
            double score = sc.getScore();
            SumpClass tocls = sc.getSumpClass();
            if (od != null && od.contains(for_class)) {
               if (nd != null && nd.contains(tocls)) {
                  double x = pat.getPackage().getDependencies().size();
                  score += 1/x;
                }
             }
            addMatch(tocls,score,sc.getNameMap());
          }   
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
         if (max > 0 && ++ct >= max) break;
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
/*      Match classes using maximal matching algorithm                          */
/*                                                                              */
/********************************************************************************/

@SuppressWarnings("unused")
private double maxMatchClasss(SumpModel base,SumpModel pat,Map<String,String> rsltmap)
{
   Collection<SumpClass> basecls = base.getPackage().getClasses();
   Collection<SumpClass> patcls = pat.getPackage().getClasses();
   RowelMatcher<SumpClass> matcher = new RowelMatcher<>(basecls,patcls);
   Map<SumpClass,SumpClass> rslt = matcher.bestMatch(null);
  
   // next go through the result and compute the individual scores, accumulating names
   double total = 0;
   for (Map.Entry<SumpClass,SumpClass> ent : rslt.entrySet()) {
      SumpClass bc = ent.getKey();
      SumpClass pc = ent.getValue();
      if (rsltmap != null) rsltmap.put(bc.getFullName(),pc.getFullName());
      // this computes score and adds attribute and operator names to results
      double score = computeClassMatchScore(bc,pc,rsltmap);     
      total += score;
    }
   
   // associations should be accounted for as part of matching
   double assoc = checkAssociations(base,pat,rslt);
   total += assoc;
   
   return total;
}

/********************************************************************************/
/*                                                                              */
/*      Matching between classes                                                */
/*                                                                              */
/********************************************************************************/

static double computeClassMatchScore(SumpClass base,SumpClass pat,Map<String,String> namemap)
{
   // Set<Object> done = new HashSet<>();
   
   // matchable helps to eliminate using classes that require a definition that isn't
   //   present in the match
   if (!pat.isMatchable()) 
      return 0;
   
   RowelMatcher<SumpAttribute> rm = new RowelMatcher<>(pat.getAttributes(),base.getAttributes());
   Map<SumpAttribute,SumpAttribute> map = rm.bestMatch(MatchType.MATCH_EXACT);
   for (Map.Entry<SumpAttribute,SumpAttribute> ent : map.entrySet()) {
      namemap.put(ent.getValue().getFullName(),ent.getKey().getFullName());
    }
   double atot = pat.getAttributes().size();
   double afnd = map.size();
   
   if (atot == 2  && afnd == 0) return 0;
   else if (atot == 3 && afnd <= 1) return 0;
   else if (atot == 4 && afnd <= 2) return 0;
   else if (atot > 4 && afnd / atot < ATTR_CUTOFF) return 0;
   
   RowelMatcher<SumpOperation> orm = new RowelMatcher<>(pat.getOperations(),base.getOperations());
   Map<SumpOperation,SumpOperation> omap = orm.bestMatch(MatchType.MATCH_EXACT);
   for (Map.Entry<SumpOperation,SumpOperation> ent : omap.entrySet()) {
      Map<String,String> pnamemap = matchOperation(ent.getValue(),ent.getKey());
      String mapname = ent.getValue().getMapName();
      namemap.put(mapname,ent.getKey().getFullName());
      for (Map.Entry<String,String> pent : pnamemap.entrySet()) {
         String key = mapname + "." + pent.getKey();
         namemap.put(key,pent.getValue());
       }
    }  
   double mtot = pat.getOperations().size();
   double mfnd = omap.size();
   
   // double mtot = 0;
   // double mfnd = 0;
   // for (SumpOperation op : pat.getOperations()) {
      // mtot += 1;
      // double v = -1;
      // SumpOperation best = null;
      // Map<String,String> bestmap = null;
      // for (SumpOperation bop : base.getOperations()) {
         // if (done.contains(bop)) continue;
         // Map<String,String> pnamemap = matchOperation(bop,op);
         // if (pnamemap != null) {
            // double v1 = IvyStringDiff.normalizedStringDiff(bop.getName(),op.getName());
            // if (best == null || v1 > v) {
               // v = v1;
               // best = bop;
               // bestmap = pnamemap;
             // }
          // }
       // }
      // if (best != null) {
         // mfnd += 1;
         // done.add(best);
         // if (namemap != null) {
            // namemap.put(best.getFullName(),op.getFullName());
            // namemap.putAll(bestmap);
          // }
       // }
    // }
   
   if (mtot == 1 && mfnd == 0) return 0;
   else if (mtot == 2  && mfnd == 0) return 0;
   else if (mtot == 3 && mfnd <= 1) return 0;
   else if (mtot == 4 && mfnd <= 2) return 0;
   else if (mtot > 4 && mfnd / mtot < METHOD_CUTOFF) return 0;
   
   double score = 1;
   if (atot != 0 && mtot != 0) {
      score = (afnd + mfnd) / (atot + mtot);
    }
   else if (mtot != 0) score = mfnd/mtot;
   else if (atot != 0) score = afnd/atot;
   
   Collection<String> pecs = pat.getEnumConstants();
   Collection<String> becs = base.getEnumConstants();
   if (pecs != null || becs != null) {
      if (pecs == null || becs == null) return 0;
      double eptot = pecs.size();
      double ebtot = becs.size();
      double ecnt = 0;
      for (String s : pecs) {
         if (becs.contains(s)) ++ecnt;
       }
      double emax = Math.max(eptot,ebtot);
      double escore = 1;
      if (emax > 0) escore = ecnt/emax;
      score = escore * ENUM_FRACTION + score * (1 - ENUM_FRACTION);
    }
   
   return score;
}


boolean matchAttribute(SumpAttribute base,SumpAttribute pat)
{
   SumpDataType bdt = base.getDataType();
   SumpDataType pdt = pat.getDataType();
   
   return matchType(bdt,pdt);
}


static boolean matchType(SumpDataType base,SumpDataType pat)
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

private static Map<String,String> matchOperation(SumpOperation base,SumpOperation pat)
{
   // should return name map or null
   Map<String,String> namemap = new HashMap<>();
   
   SumpDataType bdt = base.getReturnType();
   SumpDataType pdt = pat.getReturnType();
   
   if (!matchType(bdt,pdt)) return null;
   
   Collection<SumpParameter> bsps = base.getParameters();
   Collection<SumpParameter> psps = pat.getParameters();
   if (bsps.size() != psps.size()) return null;
   
   RowelMatcher<SumpParameter> rm = new RowelMatcher<>(bsps,psps);
   Map<SumpParameter,SumpParameter> map = rm.bestMatch(null);
   for (Map.Entry<SumpParameter,SumpParameter> ent : map.entrySet()) {
      namemap.put(ent.getKey().getName(),ent.getValue().getFullName());
    }
 
   // Set<SumpParameter> done = new HashSet<>();
   // for (SumpParameter pp : psps) {
   // match with best based on name mapping
      // SumpParameter best = null;
      // double bestval = 0;
      // for (SumpParameter bp : bsps) {
         // if (done.contains(bp)) continue;
         // if (matchType(pp.getDataType(),bp.getDataType())) {
            // double v = IvyStringDiff.normalizedStringDiff(pp.getName(),bp.getName());
            // if (best == null || v > bestval) {
               // best = bp;
               // bestval = v;
             // }
          // }
       // }
      // if (best == null) return null;
      // namemap.put(best.getFullName(),pp.getFullName());
      // add name mapping here
      // done.add(best);
    // }
   
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

