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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;

import edu.brown.cs.spur.sump.SumpConstants.SumpModel;


class EtchTransformInnerClass extends EtchTransform
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,String>      name_map;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

EtchTransformInnerClass(Map<String,String> namemap)
{
   super("InnerClass");
   name_map = namemap;
}


@Override protected EtchMemo applyTransform(ASTNode n,SumpModel target)
{
   List<AbstractTypeDeclaration> todos = findInnerClasses(n);
   if (todos == null || todos.size() == 0) return null;
   
   for (AbstractTypeDeclaration atd : todos) {
      if (atd instanceof TypeDeclaration) {
         TypeDeclaration td = (TypeDeclaration) atd;
         if (td.isInterface()) continue;
       }
    }
   
   InnerClassMapper csm = new InnerClassMapper(todos);
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
/*       Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

private AbstractTypeDeclaration createCopy(AST ast,AbstractTypeDeclaration td,Map<Object,String> names)
{
   AbstractTypeDeclaration ntd = (AbstractTypeDeclaration) ASTNode.copySubtree(ast,td);
   for (Iterator<?> it = ntd.modifiers().iterator(); it.hasNext(); ) {
      IExtendedModifier iem = (IExtendedModifier) it.next();
      if (iem.isAnnotation()) it.remove();
      else if (iem.isModifier()) {
	 Modifier m = (Modifier) iem;
	 if (m.isFinal() || m.isProtected() || m.isPrivate() || m.isStatic()) it.remove();
       }
    }
   SimpleName nnm = JcompAst.getSimpleName(ast,names.get(td));
   ntd.setName(nnm);
   
   fixSubtree(td,ntd,names,td);
   
   return ntd;
}


private void fixSubtree(ASTNode orig,ASTNode copy,Map<Object,String> names,AbstractTypeDeclaration td)
{
   for (Object o : orig.structuralPropertiesForType()) {
      StructuralPropertyDescriptor spd = (StructuralPropertyDescriptor) o;
      if (spd.isChildListProperty()) {
	 List<?> ls1 = (List<?>) orig.getStructuralProperty(spd);
	 List<?> ls2 = (List<?>) copy.getStructuralProperty(spd);
	 if (ls1.size() == ls2.size()) {
	    for (int i = 0; i < ls1.size(); ++i) {
	       fixSubtree((ASTNode) ls1.get(i),(ASTNode) ls2.get(i),names,td);
	     }
	  }
       }
      else if (spd.isChildProperty()) {
	 ASTNode n1 = (ASTNode) orig.getStructuralProperty(spd);
	 ASTNode n2 = (ASTNode) copy.getStructuralProperty(spd);
	 if (n1 != null) fixSubtree(n1,n2,names,td);
       }
    }
   
   if (copy.getNodeType() == ASTNode.SIMPLE_NAME) {
      JcompSymbol js = JcompAst.getReference(orig);
      if (js == null) js = JcompAst.getDefinition(orig);
      if (js != null) {
	 String newname = names.get(js);
	 if (newname != null) {
	    SimpleName sn = (SimpleName) copy;
	    sn.setIdentifier(newname);
	  }
	 else {
	    JcompType jt = js.getClassType();
	    if (jt == JcompAst.getJavaType(td)) {
	       // need to qualify expression here to use new field or class name
	     }
	  }
       }
    }
}




/********************************************************************************/
/*                                                                              */
/*      Transform to make a class static                                        *//*                                                                              */
/********************************************************************************/

private class InnerClassMapper extends EtchMapper {

   private Set<AbstractTypeDeclaration> extract_types;
   private Set<AbstractTypeDeclaration> outer_types;
   private Map<Object,String> sym_mapping;
   private boolean ignore_tree;
   
   InnerClassMapper(List<AbstractTypeDeclaration> tds) {
      super(EtchTransformInnerClass.this);
      extract_types = new HashSet<>(tds);
      ignore_tree = false;
      sym_mapping = new HashMap<>();
      outer_types = new HashSet<>();
      for (AbstractTypeDeclaration atd : tds) {
         JcompSymbol js = JcompAst.getDefinition(atd);
         JcompType jt = JcompAst.getJavaType(atd);
         JcompType outt = jt.getOuterType();
         if (outt != null) {
            AbstractTypeDeclaration xtd = (AbstractTypeDeclaration) outt.getDefinition().getDefinitionNode();
            outer_types.add(xtd);
          }
         String nm = name_map.get(jt.getName());
         int idx = nm.lastIndexOf(".");
         if (idx > 0) nm = nm.substring(idx+1);
         sym_mapping.put(js,nm);
         sym_mapping.put(jt,nm);
         sym_mapping.put(atd,nm);
         sym_mapping.put(js.getName(),nm);
       }
    }
   
   @Override void preVisit(ASTNode n) {
      if (extract_types.contains(n)) ignore_tree = true;
    }
   
   @Override void rewriteTree(ASTNode orig,ASTRewrite rw) {
      if (outer_types.contains(orig)) {
         ListRewrite lw = rw.getListRewrite(orig,TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
	 for (AbstractTypeDeclaration td : extract_types) {
	    lw.remove(td,null);
	  }
	 ListRewrite lw1 = rw.getListRewrite(orig.getParent(),CompilationUnit.TYPES_PROPERTY);
	 for (AbstractTypeDeclaration td : extract_types) {
	    lw1.insertAfter(createCopy(orig.getAST(),td,sym_mapping),orig,null);
	  }
       }
      else if (extract_types.contains(orig)) {
         ignore_tree = false;
       }
      else if (ignore_tree) return;
      else {
         JcompSymbol js = JcompAst.getDefinition(orig);
         if (js != null && orig.getNodeType() == ASTNode.QUALIFIED_NAME ||
	    orig.getNodeType() == ASTNode.QUALIFIED_TYPE) {
               System.err.println("QUAL: " + orig + " " + js);
             }
            
            if (js != null) {
               String newname = sym_mapping.get(js);
               if (newname != null) {
                  rewriteName(orig,rw,newname);
                }
             }
            js = JcompAst.getReference(orig);
            if (js != null) {
               String newname = sym_mapping.get(js);
               if (newname == null && js.isConstructorSymbol()) {
                  JcompType jt = js.getClassType();
                  newname = sym_mapping.get(jt);
                }
               if (newname != null) {
                  rewriteName(orig,rw,newname);
                }
             }
       }
    }
   
   private void rewriteName(ASTNode nd,ASTRewrite rw,String name) {
      if (nd instanceof SimpleName) {
         try {
            rw.set(nd,SimpleName.IDENTIFIER_PROPERTY,name,null);
          }
         catch (IllegalArgumentException e) {
            IvyLog.logE("ETCH","Transform name problem with new name " + name + ": " + e);
          }
       }
    }
   
}	// end of subtype InnerClassMapper

   
   
}       // end of class EtchTransformInnerClass




/* end of EtchTransformInnerClass.java */

