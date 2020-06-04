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
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

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
   Set<AbstractTypeDeclaration> todos = findInnerClasses(n);
   if (todos == null || todos.size() == 0) return null;
   
   Map<Object,String> changenames = new HashMap<>();
   
   for (AbstractTypeDeclaration atd : todos) {
      JcompSymbol js = JcompAst.getDefinition(atd);
      JcompType jt = JcompAst.getJavaType(atd);
      String outnm = jt.getOuterType().getName();
      int idx = outnm.lastIndexOf(".");
      if (idx > 0) outnm = outnm.substring(idx+1);
      String newname = outnm + "_" + js.getName();
      changenames.put(js,newname);
      changenames.put(jt,newname);
      changenames.put(atd,newname);
      changenames.put(js.getName(),newname);
      for (Object o : atd.bodyDeclarations()) {
         if (o instanceof MethodDeclaration) {
            MethodDeclaration md = (MethodDeclaration) o;
            if (md.isConstructor()) {
               JcompSymbol cjs = JcompAst.getDefinition(md);
               changenames.put(cjs,newname);
             }
          }
       }
      String onm = jt.getName();
      int idx1 = onm.lastIndexOf(".");
      int idx2 = onm.lastIndexOf(".",idx1-1);
      String nnm = onm.substring(0,idx2+1) + newname;
      Map<String,String> newnames = new HashMap<>();
      for (Map.Entry<String,String> ent : name_map.entrySet()) {
         String k = ent.getKey();
         if (k.startsWith(onm)) {
            String s1 = k.substring(onm.length());
            String s2 = nnm + s1;
            newnames.put(s2,ent.getValue());
          }
       }
      name_map.putAll(newnames);
    }
   
   UsedFinder used = new UsedFinder(todos);
   n.accept(used);
   Set<JcompSymbol> usednames = used.getUsedNames();
   
   InnerClassMapper csm = new InnerClassMapper(todos,changenames,usednames);
   EtchMemo memo = csm.getMapMemo(n);
   
   return memo;
}




/********************************************************************************/
/*                                                                              */
/*      Methods to find relevant inner classes to move                          */
/*                                                                              */
/********************************************************************************/

private Set<AbstractTypeDeclaration> findInnerClasses(ASTNode n)
{
   FindInnerClassVisitor ficv = new FindInnerClassVisitor();
   n.accept(ficv);
   return ficv.getInnerClasses();
}


private class FindInnerClassVisitor extends ASTVisitor {

   private Set<AbstractTypeDeclaration> inner_classes;
   
   FindInnerClassVisitor() {
      inner_classes = new HashSet<>();
    }
   
   Set<AbstractTypeDeclaration> getInnerClasses() {
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
         String xnm = nm.substring(0,idx2) + nm.substring(idx1);
         name_map.put(xnm,tnm);
         JcompSymbol js = jt.getDefinition();
         AbstractTypeDeclaration atd = (AbstractTypeDeclaration) js.getDefinitionNode();
         if (atd != null) inner_classes.add(atd);
         checkType(jt.getSuperType());
         
       }
    }
   
}       // end of inner class FindInnerClassVisitor



/********************************************************************************/
/*                                                                              */
/*      Methods to find used calls to inner methods from outside                */
/*                                                                              */
/********************************************************************************/

private class UsedFinder extends ASTVisitor {

   private Set<AbstractTypeDeclaration> inner_classes;
   private Set<JcompType> inner_types;
   private Set<JcompSymbol> used_names;
   
   UsedFinder(Set<AbstractTypeDeclaration> inners) {
      inner_classes = inners;
      inner_types = new HashSet<>();
      for (AbstractTypeDeclaration atd : inners) {
         JcompType jt = JcompAst.getJavaType(atd);
         inner_types.add(jt);
       }
      used_names = new HashSet<>();
    }
   
   Set<JcompSymbol> getUsedNames()                      { return used_names; }
   
   @Override public boolean visit(TypeDeclaration n) {
      if (inner_classes.contains(n)) return false;
      return true;
    }
   
   @Override public boolean visit(EnumDeclaration n) {
      if (inner_classes.contains(n)) return false;
      return true;
    }
   
   @Override public void postVisit(ASTNode n) {
      JcompSymbol js = JcompAst.getReference(n);
      if (js != null) {
         JcompType jt = js.getClassType();
         if (jt != null && inner_types.contains(jt)) used_names.add(js);
       }
    }
   
}       // end of inner class UsedFinder


/********************************************************************************/
/*                                                                              */
/*       Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

private AbstractTypeDeclaration createCopy(AST ast,AbstractTypeDeclaration td,Map<Object,String> names,Set<JcompSymbol> used)
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
   
   fixSubtree(td,ntd,names,used,td);
   
   return ntd;
}


private void fixSubtree(ASTNode orig,ASTNode copy,Map<Object,String> names,Set<JcompSymbol> used,AbstractTypeDeclaration td)
{
   for (Object o : orig.structuralPropertiesForType()) {
      StructuralPropertyDescriptor spd = (StructuralPropertyDescriptor) o;
      if (spd.isChildListProperty()) {
	 List<?> ls1 = (List<?>) orig.getStructuralProperty(spd);
	 List<?> ls2 = (List<?>) copy.getStructuralProperty(spd);
	 if (ls1.size() == ls2.size()) {
	    for (int i = 0; i < ls1.size(); ++i) {
	       fixSubtree((ASTNode) ls1.get(i),(ASTNode) ls2.get(i),names,used,td);
	     }
	  }
       }
      else if (spd.isChildProperty()) {
	 ASTNode n1 = (ASTNode) orig.getStructuralProperty(spd);
	 ASTNode n2 = (ASTNode) copy.getStructuralProperty(spd);
	 if (n1 != null) fixSubtree(n1,n2,names,used,td);
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
       }
    }
   else if (copy.getNodeType() == ASTNode.METHOD_DECLARATION) {
      MethodDeclaration md = (MethodDeclaration) copy;
      JcompSymbol js = JcompAst.getDefinition(orig);
      if (used.contains(js)) {
         removePrivate(md.modifiers());
       }
    }
   else if (copy.getNodeType() == ASTNode.FIELD_DECLARATION) {
      FieldDeclaration fd = (FieldDeclaration) orig;
      boolean rempri = false;
      for (Object o : fd.fragments()) {
         VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
         JcompSymbol js1 = JcompAst.getDefinition(vdf);
         if (used.contains(js1)) rempri = true; 
       }
      if (rempri) {
         FieldDeclaration nfd = (FieldDeclaration) copy;
         removePrivate(nfd.modifiers());
       }
    }
   else {
      JcompType jt = JcompAst.getJavaType(orig);
      if (jt != null) {
         String newname = names.get(jt);
         if (newname != null) {
            
          }
       }
    }
}


private void removePrivate(List<?> mods) 
{
   for (Iterator<?> it = mods.iterator(); it.hasNext(); ) {
      IExtendedModifier iem = (IExtendedModifier) it.next();
      if (iem.isModifier()) {
         Modifier m = (Modifier) iem;
         if (m.isPrivate()) it.remove();
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
   private Set<JcompSymbol> used_names;
   private Map<Object,String> sym_mapping;
   private boolean inside_move;
   
   InnerClassMapper(Set<AbstractTypeDeclaration> tds,Map<Object,String> nms,Set<JcompSymbol> used) {
      super(EtchTransformInnerClass.this);
      extract_types = tds;
      used_names = used;
      inside_move = false;
      sym_mapping = new HashMap<>(nms);
      outer_types = new HashSet<>();
      for (AbstractTypeDeclaration atd : tds) {
         JcompType jt = JcompAst.getJavaType(atd);
         JcompType outt = jt.getOuterType();
         if (outt != null) {
            AbstractTypeDeclaration xtd = (AbstractTypeDeclaration) outt.getDefinition().getDefinitionNode();
            outer_types.add(xtd);
          }
       }
    }
   
   @Override boolean preVisit(ASTNode n,ASTRewrite rw) {
      if (extract_types.contains(n)) inside_move = true;
      return true;
    }
   
   @Override void rewriteTree(ASTNode orig,ASTRewrite rw) {
      if (outer_types.contains(orig)) {
         ListRewrite lw = rw.getListRewrite(orig,TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
         for (AbstractTypeDeclaration td : extract_types) {
            lw.remove(td,null);
          }
         ListRewrite lw1 = rw.getListRewrite(orig.getParent(),CompilationUnit.TYPES_PROPERTY);
         for (AbstractTypeDeclaration td : extract_types) {
            lw1.insertAfter(createCopy(orig.getAST(),td,sym_mapping,used_names),orig,null);
          }
       }
      else if (extract_types.contains(orig)) {
         inside_move = false;
       }
      else if (!inside_move) {
         JcompSymbol js = JcompAst.getReference(orig);
         JcompType jt = JcompAst.getJavaType(orig);
         if (js != null) {
            String newname = sym_mapping.get(js);
            if (newname == null && js.isConstructorSymbol()) {
               newname = sym_mapping.get(jt);
             }
            rewriteName(orig,rw,newname);
          }
         else if (jt != null) {
            String newname = sym_mapping.get(jt);
            rewriteType(orig,rw,newname);
          }
       }
    }
   
   
   
   
   
}	// end of subtype InnerClassMapper

   
   
}       // end of class EtchTransformInnerClass




/* end of EtchTransformInnerClass.java */

