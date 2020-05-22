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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.spur.sump.SumpDataType;
import edu.brown.cs.spur.sump.SumpConstants.SumpClass;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;
import edu.brown.cs.spur.sump.SumpConstants.SumpOperation;
import edu.brown.cs.spur.sump.SumpConstants.SumpParameter;

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



/********************************************************************************/
/*                                                                              */
/*      Handle type conversions                                                 */
/*                                                                              */
/********************************************************************************/

protected Statement createCast(AST ast,String nm,JcompType typ,String onm,JcompType otyp)
{
   // needs to be extended to handle more complex mappings
   //           File <-> String <-> CharSequence
   //           Date <-> Sql Date,TimeStamp
   //           URL <-> URI
   //           AWT Shapes
   //           Collection types
   //           number <-> enum
   //           Reader,Writer <-> Streams
   //           Enum <-> Iterator
   //           Exceptions
   // look for getter methods in source type returning target type
   
   Assignment as = ast.newAssignment();
   SimpleName lhs = JcompAst.getSimpleName(ast,nm);
   as.setLeftHandSide(lhs);
   CastExpression cast = ast.newCastExpression();
   cast.setType(typ.createAstNode(ast));
   cast.setExpression(JcompAst.getSimpleName(ast,onm));
   as.setRightHandSide(cast);
   ExpressionStatement est = ast.newExpressionStatement(as);
   return est;
}



/********************************************************************************/
/*                                                                              */
/*      Create emtpy classes and methods                                        */
/*                                                                              */
/********************************************************************************/

@SuppressWarnings("unchecked")
protected TypeDeclaration createDummyClass(AST ast,SumpClass sc)
{
   TypeDeclaration td = ast.newTypeDeclaration();
   if (sc.isInterface()) td.setInterface(true);
   
   td.setName(JcompAst.getSimpleName(ast,sc.getName()));
   if (sc.getSuperClass() != null) {
      // add super class
    }
   Collection<SumpClass> ints = sc.getInterfaces();
   if (ints != null && !ints.isEmpty()) {
      // add interfaces
    }
   
   for (SumpOperation op : sc.getOperations()) {
      MethodDeclaration md = createDummyMethod(ast,op);
      td.bodyDeclarations().add(md);
    }
   
   return td;
}

@SuppressWarnings("unchecked")
protected MethodDeclaration createDummyMethod(AST ast,SumpOperation op)
{
   SumpDataType sdt = op.getReturnType();
   
   MethodDeclaration md = ast.newMethodDeclaration();
   md.setBody(ast.newBlock());
   if (op.getName().equals("<init>")) {
      md.setConstructor(true);
      String fnm = op.getFullName();
      int idx = fnm.lastIndexOf(".");
      String cnm = fnm.substring(0,idx);
      int idx1 = cnm.lastIndexOf(".");
      md.setName(JcompAst.getSimpleName(ast,cnm.substring(idx1+1)));
      sdt = null;
    }
   else {
      md.setName(JcompAst.getSimpleName(ast,op.getName()));
      Type t = createTypeNode(sdt,ast);
      md.setReturnType2(t);
    }
   
   for (SumpParameter sp : op.getParameters()) {
      SingleVariableDeclaration svd = ast.newSingleVariableDeclaration();
      svd.setName(JcompAst.getSimpleName(ast,sp.getName()));
      Type t = createTypeNode(sp.getDataType(),ast);
      svd.setType(t);
      md.parameters().add(svd);
    }
   
   if (sdt != null && !sdt.getName().equals("void")) {
      Expression ret = null;
      switch (sdt.getArgType()) {
         case BOOLEAN :
           ret = ast.newBooleanLiteral(false);
           break;
         case NUMBER :
           if (sdt.getBaseType().isEnumType()) ret = ast.newNullLiteral();
           else ret = ast.newNumberLiteral("0");
           break;
         case VOID :
            break;
         default :
            ret = ast.newNullLiteral();
            break;
       }
      if (ret != null) {
         ReturnStatement rets = ast.newReturnStatement();
         rets.setExpression(ret);
         md.getBody().statements().add(rets);
       }
    }
   
   return md;
}



protected Type createTypeNode(SumpDataType sdt,AST ast)
{
   JcompType jt = sdt.getBaseType();
   if (jt.isCompiledType()) {
      String s = sdt.getName();
      SimpleName sn = JcompAst.getSimpleName(ast,s);
      SimpleType st = ast.newSimpleType(sn);
      return st;
    }
   return jt.createAstNode(ast);
}



/********************************************************************************/
/*                                                                              */
/*      Hnadle model lookup                                                     */
/*                                                                              */
/********************************************************************************/

protected static SumpOperation findOperation(String nm,SumpModel target)
{
   for (SumpClass sc : target.getPackage().getClasses()) {
      if (nm.startsWith(sc.getFullName())) {
         for (SumpOperation op : sc.getOperations()) {
            if (nm.startsWith(op.getFullName())) {
               return op;
             }
          }
       }
    }
   
   return null;
}


protected static String getMapName(JcompSymbol js)
{
   if (js.isMethodSymbol()) {
      StringBuffer buf = new StringBuffer();
      buf.append(js.getFullName());
      buf.append("(");
      JcompType jt = js.getType();
      int ct = 0;
      for (JcompType ptyp : jt.getComponents()) {
         if (ct++ > 0) buf.append(",");
         String s = ptyp.getName();
         int idx1 = s.indexOf("<");
         if (idx1 > 0) s = s.substring(0,idx1);
         int idx2 = s.lastIndexOf(".");
         if (idx2 > 0) s = s.substring(idx2+1);
         buf.append(s);
       }
      buf.append(")");
      return buf.toString();
    }
   
   return js.getFullName();
}




}       // end of class EtchTransform




/* end of EtchTransform.java */

