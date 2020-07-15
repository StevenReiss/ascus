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

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

public class SwiftScorer implements SwiftConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SwiftScoredSet base_tfidf;

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

public SwiftScorer(String text,ASTNode n,boolean kgram)
{
   base_tfidf = idf_builder.getTfIdf(text,n,kgram);
   use_kgrams = kgram;
}


/********************************************************************************/
/*                                                                              */
/*      Scoring methods                                                         */
/*                                                                              */
/********************************************************************************/

public double getScore(String text,ASTNode n)
{
   SwiftScoredSet ntf = idf_builder.getTfIdf(text,n,use_kgrams);
   return getScore(ntf);
}


 


public double getScore(SwiftScorer sc)
{
   return getScore(sc.base_tfidf);
}


private double getScore(SwiftScoredSet ntf)
{
   double dot = 0;
   for (String t : ntf.getTerms()) {
      double v = ntf.getScore(t);
      double v1 = base_tfidf.getScore(t);
      dot += v * v1;
    }
   
   return dot;
}



public void limit(int k)
{
   base_tfidf.limit(k);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public List<String> getTopWords()
{
   return base_tfidf.getTopTerms(5);
}
 



}       // end of class SwiftScorer




/* end of SwiftScorer.java */

