/********************************************************************************/
/*                                                                              */
/*              EtchMapper.java                                                 */
/*                                                                              */
/*      Abstract mapping for a transformation                                   */
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



package edu.brown.cs.spur.etch;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

abstract class EtchMapper implements EtchConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private EtchTransform   for_transform;
private EtchMemo        saved_memo;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected EtchMapper(EtchTransform trans)
{
   for_transform = trans;
   saved_memo = null;
}



/********************************************************************************/
/*                                                                              */
/*      Mapping methods                                                         */
/*                                                                              */
/********************************************************************************/


final String getMapName() 
{
   String nm = for_transform.getName();
   String sn = getSpecificsName();
   if (sn == null) return nm;
   return nm + "@" + sn;
}


protected String getSpecificsName()		{ return null; }

void preVisit(ASTNode n)			{ }

abstract void rewriteTree(ASTNode orig,ASTRewrite rw);



EtchMemo getMapMemo(ASTNode base) 
{
   if (saved_memo == null) {
      EtchRewrite tr = new EtchRewrite(base,this);
      base.getRoot().accept(tr);
      saved_memo = tr.createMemo();
    }
   return saved_memo;
}


}       // end of class EtchMapper




/* end of EtchMapper.java */

