/********************************************************************************/
/*                                                                              */
/*              ScrapComponent.java                                             */
/*                                                                              */
/*      Component of a class or package                                         */
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.Modifier;

import edu.brown.cs.cose.cosecommon.CoseScores;
import edu.brown.cs.ivy.file.IvyStringDiff;

import edu.brown.cs.spur.rowel.RowelConstants.RowelMatch;
import edu.brown.cs.spur.rowel.RowelConstants.RowelMatchType;

abstract class ScrapComponent implements ScrapConstants, RowelMatch
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected Map<String,Integer> component_names;
protected int component_modifiers;
protected CoseScores component_scores;
private int instance_count;

private static Set<String> standard_names;


static {
   standard_names = new HashSet<>();
   standard_names.add("hashCode");
   standard_names.add("equals");
   standard_names.add("toString");
   standard_names.add("clone");
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected ScrapComponent() 
{
   component_names = new HashMap<>();
   component_modifiers = 0;
   component_scores = null;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

CoseScores getScores()               { return component_scores; }

void clearData() 
{
   component_scores = null;
}

boolean isPublic()
{
   return Modifier.isPublic(component_modifiers);
}

boolean isStatic() 
{
   return Modifier.isStatic(component_modifiers);
}



abstract ComponentType getComponentType(); 



/********************************************************************************/
/*                                                                              */
/*      Name management                                                         */
/*                                                                              */
/********************************************************************************/

void addName(String name) 
{   
   Integer v = component_names.get(name);
   if (v == null) v = 1;
   else v = v+1;
   component_names.put(name,v);
}



String getNames()
{
   StringBuffer buf = new StringBuffer();
   int i = 0;
   for (String s : component_names.keySet()) {
      if (i++ > 0) buf.append(" ");
      buf.append(s);
      Integer v = component_names.get(s);
      if (v > 1) {
         buf.append(" (" + v + ")");
       }
    }
   return buf.toString();
}



String getName()
{
   String nm = null;
   int ctr = -1;
   for (Map.Entry<String,Integer> ent : component_names.entrySet()) {
      if (ent.getValue() > ctr) {
         nm = ent.getKey();
         ctr = ent.getValue();
       }
    }
   return nm;
}


int getCount()                  { return instance_count; }


double getNameMatch(ScrapComponent cc) 
{
   double max = -1;
   for (String s1 : component_names.keySet()) {
      if (getComponentType() == ComponentType.METHOD) {
         if (standard_names.contains(s1)) continue;
         
       }
      s1 = s1.toLowerCase();
      for (String s2 : cc.component_names.keySet()) {
         if (cc.getComponentType() == ComponentType.METHOD &&
               standard_names.contains(s2)) continue;
         s2 = s2.toLowerCase();
         // handle special cases: 1 character names -> match start or end
         // handle splitting names and matching words
         double sc = IvyStringDiff.normalizedStringDiff(s1,s2);
         max = Math.max(max,sc);
       }
    }
   return max;
}


@Override public double getMatchScore(RowelMatch rm,RowelMatchType mp)
{
   if (rm.getClass() == getClass()) {
      ScrapComponent sc = (ScrapComponent) rm;
      if (mp == MatchType.MATCH_APPROXIMATE) {
         if (!isComparableTo(sc)) return 0;
       }
      else { 
         if (!isCompatibleWith(sc)) return 0;
       }
      return 1 + getNameMatch(sc);
    }
   
   return 0;
}



boolean containsName(String nm)
{
   return component_names.containsKey(nm);
}



/********************************************************************************/
/*                                                                              */
/*      Handle merging                                                          */
/*                                                                              */
/********************************************************************************/

abstract boolean isCompatibleWith(ScrapComponent sc);

boolean isComparableTo(ScrapComponent sc)
{
   return isCompatibleWith(sc);
}


void mergeWith(ScrapComponent cc) 
{
   component_scores = null;
   component_modifiers |= cc.component_modifiers;
   for (Map.Entry<String,Integer> ent : cc.component_names.entrySet()) {
      Integer v = component_names.get(ent.getKey());
      if (v == null) v = ent.getValue();
      else v = v+ent.getValue();
      component_names.put(ent.getKey(),v);
    }
   instance_count += cc.instance_count;
   
}



}       // end of class ScrapComponent




/* end of ScrapComponent.java */

