/********************************************************************************/
/*                                                                              */
/*              EtchTransform.java                                              */
/*                                                                              */
/*      Editing Transforms for Candidate Helper transform abstract class        */
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

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;

import edu.brown.cs.spur.sump.SumpConstants.SumpModel;

abstract class EtchTransform implements EtchConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          transform_name;
private static JcompControl     jcomp_control = null;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected EtchTransform(String name)
{
   transform_name = name;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public String getName()                         { return transform_name; }



/********************************************************************************/
/*                                                                              */
/*      Transform methods                                                       */
/*                                                                              */
/********************************************************************************/

public CoseResult transform(CoseResult orig,SumpModel target)
{
   ASTNode an = (ASTNode) orig.getStructure();
   JcompProject proj = null;
   if (!JcompAst.isResolved(an)) {
      if (jcomp_control == null) jcomp_control = new JcompControl();
      proj = JcompAst.getResolvedAst(jcomp_control,an);
      JcompAst.setProject(an,proj);
      if (proj == null) return orig;
    }

   try {
      EtchMemo em = applyTransform(an,target);
      if (em == null) return orig;
      return orig.cloneResult(em.getRewrite(),em.getPosition());
    }
   finally {
      if (proj != null) jcomp_control.freeProject(proj);
    }
}



protected EtchMemo applyTransform(ASTNode n,SumpModel target)
{
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Utility methods                                                         */
/*                                                                              */
/********************************************************************************/

protected static ASTNode copyAst(ASTNode base)
{
   TreeCopy tc = new TreeCopy(base);
   tc.copyTree(base.getRoot());
   return tc.getNewBase();
}


static ASTNode dupNode(AST ast,ASTNode n)
{
   TreeCopy tc = new TreeCopy(ast);
   return tc.copyTree(n);
}



private static class TreeCopy {

   private AST new_ast;
   private ASTNode old_base;
   private ASTNode new_base;
   
   TreeCopy(ASTNode base) {
      new_ast = AST.newAST(AST.JLS12,true);
      old_base = base;
      new_base = null;
    }
   
   TreeCopy(AST ast) {
      new_ast = ast;
      old_base = null;
      new_base = null;
    }
   
   ASTNode getNewBase() 		{ return new_base; }
   
   @SuppressWarnings("unchecked")
   ASTNode copyTree(ASTNode n) {
      if (n == null) return null;
      ASTNode nn = new_ast.createInstance(n.getNodeType());
      nn.setFlags(n.getFlags());
      nn.setSourceRange(n.getStartPosition(),n.getLength());
      
      for (Iterator<?> it = n.structuralPropertiesForType().iterator(); it.hasNext(); ) {
         StructuralPropertyDescriptor spd = (StructuralPropertyDescriptor) it.next();
         if (spd.isSimpleProperty()) {
            nn.setStructuralProperty(spd,n.getStructuralProperty(spd));
          }
         else if (spd.isChildProperty()) {
            ChildPropertyDescriptor cpd = (ChildPropertyDescriptor) spd;
            ASTNode cn = (ASTNode) n.getStructuralProperty(spd);
            if (cn == null) nn.setStructuralProperty(spd,null);
            else {
               ASTNode ncn = copyTree(cn);
               if (ncn != null) nn.setStructuralProperty(spd,ncn);
               else if (!cpd.isMandatory()) nn.setStructuralProperty(spd,null);
             }
          }
         else {
            List<ASTNode> lncn = (List<ASTNode>) nn.getStructuralProperty(spd);
            List<?> lcn = (List<?>) n.getStructuralProperty(spd);
            for (Iterator<?> it1 = lcn.iterator(); it1.hasNext(); ) {
               ASTNode cn = (ASTNode) it1.next();
               ASTNode ncn = copyTree(cn);
               if (ncn != null) lncn.add(ncn);
             }
          }
       }
      
      if (n == old_base) new_base = nn;
      
      return nn;
    }
   
}	// end of subclass TreeCopy   



}       // end of class EtchTransform




/* end of EtchTransform.java */

