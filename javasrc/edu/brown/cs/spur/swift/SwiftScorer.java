/********************************************************************************/
/*                                                                              */
/*              SwiftScorer.java                                                */
/*                                                                              */
/*      Get a similarity score based on TF/IDF                                  */
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



package edu.brown.cs.spur.swift;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SwiftScorer implements SwiftConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,Double> base_tfidf;

private static SwiftIdfBuilder idf_builder;
private boolean use_kgrams;


static {
   idf_builder = new SwiftIdfBuilder(OUTPUT_FILE);
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SwiftScorer(String text,boolean kgram)
{
   base_tfidf = idf_builder.getTfIdf(text,kgram);
   use_kgrams = kgram;
}


/********************************************************************************/
/*                                                                              */
/*      Scoring methods                                                         */
/*                                                                              */
/********************************************************************************/

public double getScore(String text)
{
   Map<String,Double> ntf = idf_builder.getTfIdf(text,use_kgrams);
   
   double dot = 0;
   for (Map.Entry<String,Double> ent : ntf.entrySet()) {
      String wd = ent.getKey();
      Double base = base_tfidf.get(wd);
      if (base == null) continue;
      dot += base*ent.getValue();
    }
   
   return dot;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public List<String> getTopWords()
{
   Map<Double,String> inv = new TreeMap<>();
   for (Map.Entry<String,Double> ent : base_tfidf.entrySet()) {
      inv.put(ent.getValue(),ent.getKey());
    }
   
   List<String> rslt = new ArrayList<>(inv.values());
   Collections.reverse(rslt);
   while (rslt.size() > 5) {
      rslt.remove(5);
    }
   
   return rslt;
}


}       // end of class SwiftScorer




/* end of SwiftScorer.java */

