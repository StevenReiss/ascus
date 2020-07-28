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
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.spur.sump.SumpDataType;
import edu.brown.cs.spur.sump.SumpConstants.SumpAttribute;
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

public CoseResult transform(CoseResult orig,CoseResult base,SumpModel srcmdl,SumpModel target)
{
   CompilationUnit an = (CompilationUnit) orig.getStructure();
   JcompProject proj = null;
   CompilationUnit baseunit = null;
   if (base != null) baseunit = (CompilationUnit) base.getStructure();
   
   if (!JcompAst.isResolved(an)) {
      if (jcomp_control == null) jcomp_control = new JcompControl();
      proj = srcmdl.resolveModel(jcomp_control,an,baseunit);  
      if (proj == null) return orig;
    }

   try {
      EtchMemo em = applyTransform(an,srcmdl,target);
      if (em == null) return orig;
      return orig.cloneResult(em.getRewrite(),em.getPosition());
    }
   finally {
      if (proj != null) jcomp_control.freeProject(proj);
    }
}



protected EtchMemo applyTransform(ASTNode n,SumpModel source,SumpModel target)
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
/*      Naming methods                                                          */
/*                                                                              */
/********************************************************************************/

protected static void rewriteName(ASTNode nd,ASTRewrite rw,String name)
{
   if (name == null) return;
   
   if (nd instanceof SimpleName) {
      String nm0 = name;
      int idx1 = nm0.lastIndexOf(".");
      if (idx1 > 0) nm0 = nm0.substring(idx1+1);
      try {
         rw.set(nd,SimpleName.IDENTIFIER_PROPERTY,nm0,null); 
       }
      catch (IllegalArgumentException e) {
         IvyLog.logE("ETCH","Problem with new transform name " + name + ": " + e);
       }
    } 
}


protected static boolean rewriteType(ASTNode nd,ASTRewrite rw,String name)
{
   if (name != null) {
      if (nd instanceof QualifiedName) {
         Name nm = JcompAst.getQualifiedName(rw.getAST(),name);
         rw.replace(nd,nm,null);
         return true;
       }
      else if (nd instanceof SimpleName) {
         int idx = name.lastIndexOf(".");
         if (idx > 0) name = name.substring(idx+1);
         SimpleName sn = JcompAst.getSimpleName(rw.getAST(),name);
         rw.replace(nd,sn,null);
         return true;
       }
      else if (nd instanceof SimpleType) {
         int idx = name.lastIndexOf(".");
         if (idx > 0) name = name.substring(idx+1);
         SimpleName sn = JcompAst.getSimpleName(rw.getAST(),name);
         SimpleType st = rw.getAST().newSimpleType(sn);
         rw.replace(nd,st,null);
         return true;
       }
    }
   
   return false;
}




/********************************************************************************/
/*                                                                              */
/*      Handle type conversions                                                 */
/*                                                                              */
/********************************************************************************/

protected Statement createCast(AST ast,String nm,JcompType typ,String onm,JcompType otyp)
{
   Assignment as = ast.newAssignment();
   SimpleName lhs = JcompAst.getSimpleName(ast,nm);
   as.setLeftHandSide(lhs);
   Expression cast = createCastExpr(ast,typ,JcompAst.getSimpleName(ast,onm),otyp);
   as.setRightHandSide(cast);
   ExpressionStatement est = ast.newExpressionStatement(as);
   return est;
}



protected Expression createCastExpr(AST ast,JcompType typ,Expression onm,JcompType otyp)
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
   
   CastExpression cast = ast.newCastExpression();
   cast.setType(typ.createAstNode(ast));
   cast.setExpression(onm);
   return cast;
}






/********************************************************************************/
/*                                                                              */
/*      Create emtpy classes and methods                                        */
/*                                                                              */
/********************************************************************************/

@SuppressWarnings("unchecked")
protected TypeDeclaration createDummyClass(AST ast,SumpClass sc,ASTNode n)
{
   TypeDeclaration td = ast.newTypeDeclaration();
   if (sc.isInterface()) td.setInterface(true);
   JcompTyper typer = JcompAst.getTyper(n);
   
   td.setName(JcompAst.getSimpleName(ast,sc.getName()));
   
   Type st = getTypeFromName(ast,typer,sc.getSuperClassName());
   if (st != null) td.setSuperclassType(st);
   Collection<String> ints = sc.getInterfaceNames();
   if (ints != null && !ints.isEmpty()) {
      for (String ifn : ints) {
         Type it = getTypeFromName(ast,typer,ifn);
         if (it != null) {
            td.superInterfaceTypes().add(it); 
          }
       }
    }
   
   for (SumpOperation op : sc.getOperations()) {
      MethodDeclaration md = createDummyMethod(ast,op);
      td.bodyDeclarations().add(md);
    }
   
   return td;
}


private Type getTypeFromName(AST ast,JcompTyper typer,String nm)
{
   if (nm == null) return null;
   
   JcompType jt = typer.findType(nm);
   if (jt != null) return createTypeNode(jt,ast);
  
   Name nnm = null;
   if (nm.contains(".")) {
      nnm = JcompAst.getQualifiedName(ast,nm);
    }
   else {
      nnm = JcompAst.getSimpleName(ast,nm);
    }
   SimpleType st = ast.newSimpleType(nnm);
   return st;
}

@SuppressWarnings("unchecked")
protected MethodDeclaration createDummyMethod(AST ast,SumpOperation op)
{
   SumpDataType sdt = op.getReturnType();
   
   MethodDeclaration md = ast.newMethodDeclaration();
   md.setBody(ast.newBlock());
   Modifier mod = ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
   md.modifiers().add(mod);
   
   if (op.getName().equals("<init>") || sdt == null) {
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
      if (sdt != null) {
         Type t = createTypeNode(sdt,ast);
         md.setReturnType2(t);
       }
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
      Name nm = null;
      if (s.contains(".")) {
         nm = JcompAst.getQualifiedName(ast,s);
       }
      else {
         nm = JcompAst.getSimpleName(ast,s);
       }
      SimpleType st = ast.newSimpleType(nm);
      return st;
    }
   return createTypeNode(jt,ast);
}


private Type createTypeNode(JcompType jt,AST ast) 
{
   if (jt.isCompiledType()) {
      String s = jt.getName();
      int idx = s.lastIndexOf(".");
      if (idx > 0) s = s.substring(idx+1);
      SimpleName sn = JcompAst.getSimpleName(ast,s);
      SimpleType st = ast.newSimpleType(sn);
      return st;
    }
   if (jt.isParameterizedType()) {
      ParameterizedType pt = ast.newParameterizedType(jt.getBaseType().createAstNode(ast));
      @SuppressWarnings("unchecked") List<ASTNode> l = pt.typeArguments();
      for (JcompType pjt : jt.getComponents()) {
         l.add(createTypeNode(pjt,ast));
       }
      return pt;
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
               // might want to check for . or end of name here
               return op;
             }
          }
       }
    }
   
   return null;
}


protected static SumpOperation findOperation(ASTNode n,SumpModel basemodel)
{
   SumpOperation useop = null;
   JcompType ctyp = null;
   String mnm = null;
   
   if (n instanceof MethodInvocation) {
      MethodInvocation mi = (MethodInvocation) n;
      if (mi.getExpression() != null) {
         ctyp = JcompAst.getExprType(mi.getExpression());
       }
      else {
         for (ASTNode p = n; p != null; p = p.getParent()) {
            if (p instanceof AbstractTypeDeclaration) {
               ctyp = JcompAst.getJavaType(n);
               break;
             }
          }
       }
      mnm = mi.getName().getIdentifier();
      if (ctyp != null && ctyp.isErrorType()) ctyp = null;
    }
   else if (n instanceof ClassInstanceCreation) {
      ClassInstanceCreation ci = (ClassInstanceCreation) n;
      ctyp = JcompAst.getJavaType(ci.getType());
      mnm = "<init>";
    }
   if (ctyp == null) return null;
   
   SumpClass tcls = findClass(ctyp.getName(),basemodel);
   if (tcls == null) return null;
   for (SumpOperation op : tcls.getOperations()) {
      if (op.getName().equals(mnm)) {
         // match parameters if there is more than one
         useop = op;
         break;
       }
    }

   return useop;
}


protected static SumpAttribute findAttribute(ASTNode n,SumpModel basemodel)
{
   SumpAttribute useatt = null;
   JcompType ctyp = null;
   String mnm = null;
   if (n instanceof FieldAccess) {
      FieldAccess fac = (FieldAccess) n;
      if (fac.getExpression() != null) {
         ctyp = JcompAst.getExprType(fac.getExpression());
       }
      else {
         for (ASTNode p = n; p != null; p = p.getParent()) {
            if (p instanceof AbstractTypeDeclaration) {
               ctyp = JcompAst.getJavaType(n);
               break;
             }
          }
       }
      mnm = fac.getName().getIdentifier();
    }
   
   if (ctyp == null) return null;
   SumpClass tcls = findClass(ctyp.getName(),basemodel);
   if (tcls == null) return null;
   for (SumpAttribute att : tcls.getAttributes()) {
      if (att.getName().equals(mnm)) {
         // match parameters if there is more than one
         useatt = att;
         break;
       }
    }
   
   return useatt;
}


protected static SumpAttribute findAttribute(String nm,SumpModel target)
{
   for (SumpClass sc : target.getPackage().getClasses()) {
      if (nm.startsWith(sc.getFullName())) {
         for (SumpAttribute att : sc.getAttributes()) {
            if (nm.startsWith(att.getFullName())) {
               // might want to check for . or end of name here
               return att;
             }
          }
       }
    }
   
   return null;
}


protected static SumpClass findClass(String nm,SumpModel target)
{
   for (SumpClass sc : target.getPackage().getClasses()) {
      if (nm.equals(sc.getFullName())) return sc;
      if (nm.equals(sc.getName())) return sc;
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
      if (jt != null) {
         for (JcompType ptyp : jt.getComponents()) {
            if (ct++ > 0) buf.append(",");
            String s = ptyp.getName();
            int idx1 = s.indexOf("<");
            if (idx1 > 0) s = s.substring(0,idx1);
            int idx2 = s.lastIndexOf(".");
            if (idx2 > 0) s = s.substring(idx2+1);
            buf.append(s);
          }
       }
      buf.append(")");
      return buf.toString();
    }
   
   return js.getFullName();
}




}       // end of class EtchTransform




/* end of EtchTransform.java */

