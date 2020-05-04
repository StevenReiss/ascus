/********************************************************************************/
/*                                                                              */
/*              EtchRewrite.java                                                */
/*                                                                              */
/*      Tree rewiting visitor                                                   */
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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;

class EtchRewrite extends ASTVisitor implements EtchConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private EtchMapper tree_mapper;
private ASTRewrite tree_rewrite;
private ITrackedNodePosition base_pos;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

EtchRewrite(ASTNode base,EtchMapper tm) 
{
   tree_mapper = tm;
   tree_rewrite = ASTRewrite.create(base.getAST());
   base_pos = tree_rewrite.track(base);
}



/********************************************************************************/
/*                                                                              */
/*      Basic visitation methods                                                *//*                                                                              */
/********************************************************************************/

public void preVisit(ASTNode n) {
   tree_mapper.preVisit(n);
}

public void postVisit(ASTNode n) {
   tree_mapper.rewriteTree(n,tree_rewrite);
}



/********************************************************************************/
/*                                                                              */
/*      Memo creation methods                                                   */
/*                                                                              */
/********************************************************************************/

EtchMemo createMemo() {
   return new EtchMemo(tree_mapper.getMapName(),tree_rewrite,base_pos);
}



}       // end of class EtchRewrite




/* end of EtchRewrite.java */

