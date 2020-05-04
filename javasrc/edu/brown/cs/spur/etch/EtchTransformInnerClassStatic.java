/********************************************************************************/
/*                                                                              */
/*              EtchTransformInnerClass.java                                    */
/*                                                                              */
/*      description of class                                                    */
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;

class EtchTransformInnerClassStatic extends EtchTransform
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,String>      name_map;

private static final String OUTER_NAME = "outer_this_name";
private static final String OUTER_PARAM = "outerthis";



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

EtchTransformInnerClassStatic(Map<String,String> namemap)
{
   super("InnerClassStatic");
   name_map = namemap;
}


@Override protected EtchMemo applyTransform(ASTNode n,SumpModel target)
{
   List<AbstractTypeDeclaration> todos = findInnerClasses(n);
   if (todos == null || todos.size() == 0) return null;
   
   List<TypeDeclaration> stats = new ArrayList<>();
   for (AbstractTypeDeclaration atd : todos) {
      int mods = atd.getModifiers();
      if (atd instanceof TypeDeclaration) {
         TypeDeclaration td = (TypeDeclaration) atd;
         if (td.isInterface()) continue;
         if (!Modifier.isStatic(mods)) {
            stats.add(td);
          }
       }
    }
   
   if (stats.isEmpty()) return null;
   
   ClassStaticMapper csm = new ClassStaticMapper(stats);
   EtchMemo memo = csm.getMapMemo(n);
   
   return memo;
}




/********************************************************************************/
/*                                                                              */
/*      Methods to find relevant inner classes to move                          */
/*                                                                              */
/********************************************************************************/

private List<AbstractTypeDeclaration> findInnerClasses(ASTNode n)
{
   FindInnerClassVisitor ficv = new FindInnerClassVisitor();
   n.accept(ficv);
   return ficv.getInnerClasses();
}


private class FindInnerClassVisitor extends ASTVisitor {

   private List<AbstractTypeDeclaration> inner_classes;
   
   FindInnerClassVisitor() {
      inner_classes = new ArrayList<>();
    }
   
   List<AbstractTypeDeclaration> getInnerClasses() {
      return inner_classes;
    }
   
   @Override public void endVisit(TypeDeclaration td) {
      checkType(td);
    }
   
   @Override public void endVisit(EnumDeclaration td) {
      checkType(td);
    }
   
   private void checkType(AbstractTypeDeclaration atd) {
      JcompType jt = JcompAst.getJavaType(atd);
      checkType(jt);
    }
   
   private void checkType(JcompType jt) {
      if (jt != null && jt.getOuterType() != null && !jt.isInterfaceType()) {
         String nm = jt.getName();
         String tnm = name_map.get(nm);
         if (tnm == null) return;
         int idx1 = nm.lastIndexOf(".");
         int idx2 = nm.lastIndexOf(".",idx1-1);
         int idx3 = tnm.lastIndexOf(".");
         int idx4 = tnm.lastIndexOf(".",idx3-1);
         String n1 = nm.substring(idx2+1);
         String n2 = tnm.substring(idx4+1);
         if (n1.equals(n2)) return;
         JcompSymbol js = jt.getDefinition();
         AbstractTypeDeclaration atd = (AbstractTypeDeclaration) js.getDefinitionNode();
         if (atd != null) inner_classes.add(atd);
         checkType(jt.getSuperType());
       }
    }
   
}       // end of inner class FindInnerClassVisitor


/********************************************************************************/
/*                                                                              */
/*      Transform to make a class static                                        *//*                                                                              */
/********************************************************************************/

private class ClassStaticMapper extends EtchMapper {

   private List<TypeDeclaration> fix_decls;
   private Set<JcompType> change_types;
   
   ClassStaticMapper(List<TypeDeclaration> tds) {
      super(EtchTransformInnerClassStatic.this);
      fix_decls = tds;
      change_types = new HashSet<>();
      for (TypeDeclaration td : fix_decls) {
         JcompType jt = JcompAst.getJavaType(td);
         if (jt != null) change_types.add(jt);
       }
    }
   
   void rewriteTree(ASTNode orig,ASTRewrite rw) {
      if (orig instanceof TypeDeclaration && fix_decls.contains(orig)) {
         makeTypeStatic((TypeDeclaration) orig,rw);
       }
      else if (orig instanceof ClassInstanceCreation) {
         ClassInstanceCreation cic = (ClassInstanceCreation) orig;
         JcompType jt = JcompAst.getJavaType(cic.getType());
         if (jt != null && change_types.contains(jt)) {
            addOuterTypeToNew(cic,jt,rw);
          }
       }
      else if (orig instanceof MethodDeclaration) {
         MethodDeclaration md = (MethodDeclaration) orig;
         if (md.isConstructor()) {
            JcompSymbol js = JcompAst.getDefinition(md);
            if (change_types.contains(js.getClassType())) {
               fixConstructor(md,js.getClassType(),rw);
             }
          }
       }
      else if (orig instanceof ThisExpression) {
         ThisExpression texp = (ThisExpression) orig;
         if (texp.getQualifier() != null) {
            JcompType jt = getCurrentType(texp);
            if (change_types.contains(jt)) {
               JcompType outtyp = jt.getOuterType();
               JcompType texptyp = JcompAst.getJavaType(texp.getQualifier());
               if (texptyp == outtyp) {
                  SimpleName sn = JcompAst.getSimpleName(rw.getAST(),OUTER_NAME);
                  rw.replace(texp,sn,null);
                }
             }
          }
       }
    }
   
   @SuppressWarnings("unchecked") private void makeTypeStatic(TypeDeclaration otd,ASTRewrite rw) {
      JcompType ourtyp = JcompAst.getJavaType(otd);
      String ourname = ourtyp.getName();
      int idx = ourname.lastIndexOf(".");
      if (idx > 0) ourname = ourname.substring(idx+1);
      String outname = null;
      JcompType outertype = ourtyp.getOuterType();
      if (outertype != null) {
         outname = outertype.getName();
         int idx1 = outname.lastIndexOf(".");
         if (idx1 > 0) outname = outname.substring(idx1+1);
       }
      
      ListRewrite lrw = rw.getListRewrite(otd,otd.getModifiersProperty());
      if (!Modifier.isStatic(otd.getModifiers())) {
         Modifier mod = otd.getAST().newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD);
         lrw.insertLast(mod,null);
       }
    
      for (Iterator<?> it = otd.modifiers().iterator(); it.hasNext(); ) {
         IExtendedModifier emod = (IExtendedModifier) it.next();
         if (emod instanceof Modifier) {
            Modifier mm = (Modifier) emod;
            if (mm.isAbstract()) {
               lrw.remove(mm,null);
               break;
             }
          }
       }
      
      ListRewrite declrw = rw.getListRewrite(otd,TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
      BodyDeclaration after = null;
      for (Object o : otd.bodyDeclarations()) {
         if (o instanceof FieldDeclaration) after =(BodyDeclaration) o;
         else break;
       }
      
      // insert field declaration for outer this
      VariableDeclarationFragment vdf = rw.getAST().newVariableDeclarationFragment();
      Name n = JcompAst.getQualifiedName(rw.getAST(),outname);
      SimpleType styp = rw.getAST().newSimpleType(n);
      SimpleName on = JcompAst.getSimpleName(rw.getAST(),OUTER_NAME);
      vdf.setName(on);
      FieldDeclaration fd = rw.getAST().newFieldDeclaration(vdf);
      fd.setType(styp);
      if (after == null) declrw.insertAfter(after,fd,null);
      else declrw.insertFirst(fd,null);
      after = fd;
      
      boolean hasconst = false;
      for (Object o : otd.bodyDeclarations()) {
         if (o instanceof MethodDeclaration) {
            MethodDeclaration md = (MethodDeclaration) o;
            if (md.isConstructor()) hasconst = true;
          }
       }
      if (!hasconst) {
         // add default constructor
         MethodDeclaration md = rw.getAST().newMethodDeclaration();
         SimpleName n1 = JcompAst.getSimpleName(rw.getAST(),ourname);
         md.setName(n1);
         SimpleName n3 = JcompAst.getSimpleName(rw.getAST(),outname);
         SimpleType t3 = rw.getAST().newSimpleType(n3);
         SingleVariableDeclaration svd = rw.getAST().newSingleVariableDeclaration();
         svd.setType(t3);
         svd.setName(n3);
         md.parameters().add(svd);
         Block cnts = md.getBody();
         Statement st = getOuterAssignment(rw);
         cnts.statements().add(st);
       }
    }
   
   private void addOuterTypeToNew(ClassInstanceCreation cic,JcompType newt,ASTRewrite rw) {
      JcompType otyp = newt.getOuterType();
      JcompType ctyp = getCurrentType(cic);
      Expression add = null;
      if (ctyp == otyp) add = cic.getAST().newThisExpression();
      else if (change_types.contains(ctyp) && otyp == ctyp.getOuterType()) 
         add = JcompAst.getSimpleName(rw.getAST(),OUTER_NAME);
      else {
         ThisExpression tex = cic.getAST().newThisExpression();
         Name typn = JcompAst.getQualifiedName(rw.getAST(),otyp.getName());
         tex.setQualifier(typn);
         add = tex;
       }
      ListRewrite lrw = rw.getListRewrite(cic,ClassInstanceCreation.ARGUMENTS_PROPERTY);
      lrw.insertLast(add,null);
    }
   
   private void fixConstructor(MethodDeclaration md,JcompType ourtyp,ASTRewrite rw) {
      // add parameter with outer type
      if (change_types.contains(ourtyp)) {
         ListRewrite lrw = rw.getListRewrite(md,MethodDeclaration.PARAMETERS_PROPERTY);
         SingleVariableDeclaration svd = rw.getAST().newSingleVariableDeclaration();
         Name tn = JcompAst.getQualifiedName(rw.getAST(),ourtyp.getOuterType().getName());
         Type tt = rw.getAST().newSimpleType(tn);
         svd.setType(tt);
         SimpleName sn = JcompAst.getSimpleName(rw.getAST(),OUTER_PARAM);
         svd.setName(sn);
         lrw.insertLast(svd,null);
       }
      
      // add parameter to super or this calls in the constructor
      Block bk = md.getBody();
      ListRewrite slrw = rw.getListRewrite(bk,Block.STATEMENTS_PROPERTY);
      ConstructorInvocation thiscall = null;
      SuperConstructorInvocation supercall = null;
      for (Object o : bk.statements()) {
         if (o instanceof ConstructorInvocation) thiscall = (ConstructorInvocation) o;
         else if (o instanceof SuperConstructorInvocation) supercall = (SuperConstructorInvocation) o;
         break;
       }
      ListRewrite clrw = null;
      if (thiscall != null) {
         if (change_types.contains(ourtyp)) {
            clrw = rw.getListRewrite(thiscall,ConstructorInvocation.ARGUMENTS_PROPERTY);
          }
       }
      else if (supercall != null) {
         JcompType styp = ourtyp.getSuperType();
         if (styp != null && change_types.contains(styp)) {
            clrw = rw.getListRewrite(supercall,SuperConstructorInvocation.ARGUMENTS_PROPERTY);
          }
       }
      if (clrw != null) {
         SimpleName argn = JcompAst.getSimpleName(rw.getAST(),OUTER_PARAM);
         clrw.insertLast(argn,null);
       }
      
      // add assignment to field from parameter
      if (thiscall != null) {
         Statement st = getOuterAssignment(rw);
         if (supercall != null) slrw.insertAfter(supercall,st,null);
         else slrw.insertFirst(st,null);
       }
      
      // add super call if none there and one is needed
      if (supercall == null && ourtyp.getSuperType() != null && 
            change_types.contains(ourtyp.getSuperType())) {
         supercall = rw.getAST().newSuperConstructorInvocation();
         clrw = rw.getListRewrite(supercall,SuperConstructorInvocation.ARGUMENTS_PROPERTY);
         SimpleName argn = JcompAst.getSimpleName(rw.getAST(),OUTER_PARAM);
         clrw.insertLast(argn,null);
         slrw.insertFirst(supercall,null);
       }
    }
   
   private JcompType getCurrentType(ASTNode n) {
      for (ASTNode p = n; p != null; p = p.getParent()) {
         if (p instanceof AbstractTypeDeclaration) {
            JcompType jt = JcompAst.getJavaType(p);
            if (jt != null) return jt;
          }
       }
      return null;
    }
   
   private Statement getOuterAssignment(ASTRewrite rw) {
      SimpleName lhs = JcompAst.getSimpleName(rw.getAST(),OUTER_NAME);
      SimpleName rhs = JcompAst.getSimpleName(rw.getAST(),OUTER_PARAM);
      Assignment asg = rw.getAST().newAssignment();
      asg.setLeftHandSide(lhs);
      asg.setRightHandSide(rhs);
      Statement st = rw.getAST().newExpressionStatement(asg);
      return st;
    }
   
}	// end of subtype ClassStaticMapper

   
   
}       // end of class EtchTransformInnerClass




/* end of EtchTransformInnerClass.java */

