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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

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
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
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

class EtchTransformInnerStatic extends EtchTransform
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

EtchTransformInnerStatic(Map<String,String> namemap)
{
   super("InnerStatic");
   name_map = namemap;
}


@Override protected EtchMemo applyTransform(ASTNode n,SumpModel src,SumpModel target)
{
   FindInnerClassVisitor ficv = new FindInnerClassVisitor();
   n.accept(ficv);
   
   List<AbstractTypeDeclaration> todos = ficv.getInnerClasses();
   Collection<JcompSymbol> acc = ficv.getAccessedItems();
   
   if (todos == null || todos.size() == 0) return null;
   
   List<AbstractTypeDeclaration> stats = new ArrayList<>();
   for (AbstractTypeDeclaration atd : todos) {
      int mods = atd.getModifiers();
      if (atd instanceof TypeDeclaration) {
         TypeDeclaration td = (TypeDeclaration) atd;
         if (td.isInterface()) continue;
       }
      if (!Modifier.isStatic(mods)) {
         stats.add(atd);
       }
    }
   
   if (stats.isEmpty()) return null;
   
   ClassStaticMapper csm = new ClassStaticMapper(stats,acc);
   EtchMemo memo = csm.getMapMemo(n);
   
   return memo;
}




/********************************************************************************/
/*                                                                              */
/*      Methods to find relevant inner classes to move                          */
/*                                                                              */
/********************************************************************************/



private class FindInnerClassVisitor extends ASTVisitor {

   private List<AbstractTypeDeclaration> inner_classes;
   private JcompType in_type;
   private Stack<JcompType> type_stack;
   private Set<JcompSymbol> accessed_items;
   
   FindInnerClassVisitor() {
      inner_classes = new ArrayList<>();
      in_type = null;
      type_stack = new Stack<>();
      accessed_items = new HashSet<>();
    }
   
   List<AbstractTypeDeclaration> getInnerClasses()      { return inner_classes; }
   
   Set<JcompSymbol> getAccessedItems()                  { return accessed_items; }
   
   @Override public boolean visit(TypeDeclaration td) {
      checkType(td);
      return true;
    }
   @Override public void endVisit(TypeDeclaration td) {
      handleEndType();
    }
   
   @Override public boolean visit(EnumDeclaration td) {
      checkType(td);
      return true;
    }
   @Override public void endVisit(EnumDeclaration td) {
      handleEndType();
    }
   
   @Override public void endVisit(SimpleName n) {
      if (in_type != null) {
         JcompSymbol r = JcompAst.getReference(n);
         if (r == null) return;
         if (r.getClassType() == in_type.getOuterType()) {
            accessed_items.add(r);
          }
       }
    }
   
   private void checkType(AbstractTypeDeclaration atd) {
      JcompType jt = JcompAst.getJavaType(atd);
      JcompType sts = checkType(jt);
      type_stack.push(in_type);
      in_type = sts;
    }
   
   private void handleEndType() {
      in_type = type_stack.pop();
    }
   
   private JcompType checkType(JcompType jt) {
      JcompType sts = null;
      if (jt != null && jt.getOuterType() != null && !jt.isInterfaceType()) {
         String nm = jt.getName();
         String tnm = name_map.get(nm);
         if (tnm == null) return null;
         int idx1 = nm.lastIndexOf(".");
         int idx2 = nm.lastIndexOf(".",idx1-1);
         int idx3 = tnm.lastIndexOf(".");
         int idx4 = tnm.lastIndexOf(".",idx3-1);
         String n1 = nm.substring(idx2+1);
         String n2 = tnm.substring(idx4+1);
         if (n1.equals(n2)) return null;
         JcompSymbol js = jt.getDefinition();
         AbstractTypeDeclaration atd = (AbstractTypeDeclaration) js.getDefinitionNode();
         if (atd != null) {
            inner_classes.add(atd);
            sts = jt;
          }
       }
      
      return sts;
    }
   
}       // end of inner class FindInnerClassVisitor


/********************************************************************************/
/*                                                                              */
/*      Transform to make a class static                                        *//*                                                                              */
/********************************************************************************/

private class ClassStaticMapper extends EtchMapper {

   private List<AbstractTypeDeclaration> fix_decls;
   private Set<JcompType> change_types;
   private Set<JcompSymbol> accessed_items;
   private Stack<JcompType> type_stack;
   private JcompType cur_type;
   
   ClassStaticMapper(List<AbstractTypeDeclaration> tds,Collection<JcompSymbol> acc) {
      super(EtchTransformInnerStatic.this);
      fix_decls = tds;
      change_types = new HashSet<>();
      accessed_items = new HashSet<>(acc);
      type_stack = new Stack<>();
      cur_type = null;
      for (AbstractTypeDeclaration td : fix_decls) {
         JcompType jt = JcompAst.getJavaType(td);
         if (jt != null) change_types.add(jt);
       }
    }
   
   @Override boolean preVisit(ASTNode orig,ASTRewrite rw) {
      if (orig instanceof AbstractTypeDeclaration) {
         JcompType jt = JcompAst.getJavaType(orig);
         type_stack.push(cur_type);
         if (fix_decls.contains(orig)) cur_type = jt;
         else cur_type = null;
       }
      return true;
    }
   
   @Override void rewriteTree(ASTNode orig,ASTRewrite rw) {
      if (orig instanceof AbstractTypeDeclaration) {
         if (fix_decls.contains(orig)) {
            makeTypeStatic((TypeDeclaration) orig,rw);
          }
         cur_type = type_stack.pop();
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
         JcompSymbol js = JcompAst.getDefinition(md);
         if (js != null && accessed_items.contains(js)) {
            removePrivate(rw,md.modifiers());
          }
       }
      else if (orig instanceof FieldDeclaration) {
         FieldDeclaration fd = (FieldDeclaration) orig;
         boolean affected = false;
         for (Object o : fd.fragments()) {
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
            JcompSymbol js = JcompAst.getDefinition(vdf);
            if (js != null && accessed_items.contains(js)) affected = true;
          }
         if (affected) removePrivate(rw,fd.modifiers());   
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
      else if (orig instanceof SimpleName && cur_type != null) {
         SimpleName sn = (SimpleName) orig;
         JcompSymbol js = JcompAst.getReference(sn);
         if (js != null && accessed_items.contains(js)) {
            fixNameReference(rw,sn);
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
         if (o instanceof FieldDeclaration) after = (BodyDeclaration) o;
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
      List<?> nmods = rw.getAST().newModifiers(Modifier.PRIVATE);
      fd.modifiers().addAll(nmods);
      
      if (after != null) declrw.insertAfter(fd,after,null);
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
         md.setConstructor(true);
         SimpleName n1 = JcompAst.getSimpleName(rw.getAST(),ourname);
         md.setName(n1);
         SimpleName n3 = JcompAst.getSimpleName(rw.getAST(),outname);
         SimpleType t3 = rw.getAST().newSimpleType(n3);
         SingleVariableDeclaration svd = rw.getAST().newSingleVariableDeclaration();
         SimpleName n4 = JcompAst.getSimpleName(rw.getAST(),OUTER_PARAM);
         svd.setType(t3);
         svd.setName(n4);
         md.parameters().add(svd);
         Block cnts = rw.getAST().newBlock();
         md.setBody(cnts);
         Statement st = getOuterAssignment(rw);
         cnts.statements().add(st);
         declrw.insertLast(md,null);
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
   
   private void removePrivate(ASTRewrite rw,List<?> mods) {
      for (Object o : mods) {
         if (o instanceof Modifier) {
            Modifier m = (Modifier) o;
            if (m.isPrivate()) {
               rw.remove(m,null);
               break;
             }
          }
       }
    }
   
   private void fixNameReference(ASTRewrite rw,SimpleName sn) {
      switch (sn.getParent().getNodeType()) {
         case ASTNode.METHOD_INVOCATION :
            fixMethodReference(rw,(MethodInvocation) sn.getParent());
            return;
         case ASTNode.FIELD_ACCESS :
         case ASTNode.THIS_EXPRESSION :
         case ASTNode.QUALIFIED_NAME :
         case ASTNode.SUPER_FIELD_ACCESS :
            return;
         default : 
            break;
       }
      FieldAccess fac = rw.getAST().newFieldAccess();
      SimpleName n = JcompAst.getSimpleName(rw.getAST(),OUTER_NAME);
      fac.setExpression(n);
      SimpleName n1 = JcompAst.getSimpleName(rw.getAST(),sn.getIdentifier());
      fac.setName(n1);
      rw.replace(sn,fac,null);
    }
   
   private void fixMethodReference(ASTRewrite rw,MethodInvocation mi) {
      if (mi.getExpression() != null) return;
      SimpleName n = JcompAst.getSimpleName(rw.getAST(),OUTER_NAME);
      rw.set(mi,MethodInvocation.EXPRESSION_PROPERTY,n,null);
    }
   
}	// end of subtype ClassStaticMapper

   
   
}       // end of class EtchTransformInnerClass




/* end of EtchTransformInnerClass.java */

