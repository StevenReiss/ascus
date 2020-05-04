/********************************************************************************/
/*                                                                              */
/*              SwiftKgramGenerator.java                                        */
/*                                                                              */
/*      Generate kgrams to do code matching                                     */
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
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

class SwiftKgramGenerator implements SwiftConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private int             k_value;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SwiftKgramGenerator()
{
   k_value = 5;
}


/********************************************************************************/
/*                                                                              */
/*      Generate all kgrams for an AST node                                     */
/*                                                                              */
/********************************************************************************/

List<SwiftKGram> getTokens(ASTNode n)
{
   SwiftTokenGenerator ctgen = new SwiftTokenGenerator();
   List<SwiftCodeToken> ctlist = ctgen.getCTs(n);
   return getKGramsNA(ctlist);
}

List<SwiftKGram> getKGramsNA(List<SwiftCodeToken> ctlist)
{
   List<SwiftKGram> rslt = new ArrayList<>();
   int ctsize = ctlist.size();
   int kgrampos = 0;
   
   int i=0, j=0;
   while (j < ctsize) {
      SwiftCodeToken ct = ctlist.get(j);
      String ct_str = ct.getText();
      if (ct_str.equals("{") || ct_str.equals("}") || ct_str.equals(";")) {
         if (i < j) {
            //Collect the tokens indexed from i to j
            List<SwiftCodeToken> ct_list0 = new ArrayList<SwiftCodeToken>();
            for (int x=i; x<j; x++) {
               ct_list0.add(ctlist.get(x));
             }
            //Generate kgrams, the recorded positions start with kgram_pos
            List<SwiftKGram> pt_list0 = getKGrams(ct_list0, kgrampos);
            int pt_list0_size = pt_list0.size();
            kgrampos += pt_list0_size;
            
            //Add kgrams to kgrams_list
            for (int x=0; x<pt_list0_size; x++) {
               SwiftKGram pt = pt_list0.get(x);
               rslt.add(pt);
             }
          }
         
         i = j + 1;
       }
      j += 1;
    }
   
   //The last part may need to be collected
   if (i < ctsize) {
      List<SwiftCodeToken> ctlist0 = new ArrayList<SwiftCodeToken>();
      for (int x=i; x<ctsize; x++) {
         ctlist0.add(ctlist.get(x));
       }
      List<SwiftKGram> ptlist0 = getKGrams(ctlist0, kgrampos);
      for(SwiftKGram pt : ptlist0){
         rslt.add(pt);
       }
    }
   
   return rslt;
}



List<SwiftKGram> getKGrams(List<SwiftCodeToken> ctlist,int start_pos)
{
   List<SwiftKGram> rslt = new ArrayList<>();
   int ctsize = ctlist.size();
   
   if (ctsize <= k_value) {
      String s = "";
      for (SwiftCodeToken ct : ctlist) {
         s += ct.getText();
       }
      SwiftKGram kgram = new SwiftKGram(s, start_pos);
      rslt.add(kgram);
    }
   
   else {
      for (int i = 0; i <= ctsize - k_value; i++) {
         String s = "";
         for (int j = i; j < i + k_value; j++) {
            SwiftCodeToken ct = ctlist.get(j);
            s += ct.getText();
          }
         SwiftKGram kgram = new SwiftKGram(s, start_pos+i);
         rslt.add(kgram);
       }
    }
   
   return rslt;
}



}       // end of class SwiftKgramGenerator




/* end of SwiftKgramGenerator.java */

