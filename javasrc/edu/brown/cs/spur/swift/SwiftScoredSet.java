/********************************************************************************/
/*                                                                              */
/*              SwiftScoredSet.java                                             */
/*                                                                              */
/*      Set of terms and their scores                                           */
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



package edu.brown.cs.spur.swift;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

class SwiftScoredSet implements SwiftConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,SwiftScoredTerm>     term_map;
private Set<SwiftScoredTerm>            ordered_set;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SwiftScoredSet()
{
   term_map = new HashMap<>();
   ordered_set = new TreeSet<>();
}



/********************************************************************************/
/*                                                                              */
/*      Access  methods                                                         */
/*                                                                              */
/********************************************************************************/

double getScore(String s)       
{
   SwiftScoredTerm sst = term_map.get(s);
   if (sst == null) return 0;
   return sst.getScore();
}


Collection<String> getTerms()
{
   return term_map.keySet();
}


List<String> getTopTerms(int k)
{
   List<String> rslt = new ArrayList<>();
   int ct = 0;
   for (SwiftScoredTerm sst : ordered_set) {
      if (ct++ > k) break;
      rslt.add(sst.getText());
    }
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Computation methods                                                     */
/*                                                                              */
/********************************************************************************/

void add(String t,double v)
{
   SwiftScoredTerm sst = new SwiftScoredTerm(t,v);
   term_map.put(t,sst);
   ordered_set.add(sst);
}


void normalize()
{
   double sum2 = 0;
   for (SwiftScoredTerm sst : ordered_set) {
      double s = sst.getScore();
      sum2 += s*s;
    }
   if (sum2 != 0) {
      for (SwiftScoredTerm sst : ordered_set) {
         sst.term_score /= sum2;
       }
    }
}



void limit(int count)
{
   List<SwiftScoredTerm> rset = new ArrayList<>();
   int ct = 0;
   for (SwiftScoredTerm sst : ordered_set) {
      if (ct++ >= count) break;
      rset.add(sst);
    }
   ordered_set.clear();
   term_map.clear();
   for (SwiftScoredTerm sst : rset) {
      term_map.put(sst.getText(),sst);
      ordered_set.add(sst);
    }
   normalize();
}




/********************************************************************************/
/*                                                                              */
/*      Term value holder                                                       */
/*                                                                              */
/********************************************************************************/

private static class SwiftScoredTerm implements Comparable<SwiftScoredTerm> {

   private String          term_text;
   private double          term_score;

   SwiftScoredTerm(String t,double v) {
      term_text = t;
      term_score = v;
    }
   
   String getText()                        { return term_text; }
   double getScore()                       { return term_score; }

   @Override public int compareTo(SwiftScoredTerm st) {
      int v = Double.compare(st.term_score,term_score);
      if (v != 0) return v;
      return term_text.compareTo(st.term_text);
    }
   
   @Override public String toString() {
      return term_text + "=" + term_score;
    }
   
}       // end of class SwiftScoredTerm



}       // end of class SwiftScoredSet




/* end of SwiftScoredSet.java */

