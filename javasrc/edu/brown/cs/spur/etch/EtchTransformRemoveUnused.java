/********************************************************************************/
/*                                                                              */
/*              EtchTransformRemoveUnused.java                                  */
/*                                                                              */
/*      Remove code that is not needed by target model                          */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.spur.etch;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.spur.sump.SumpConstants.SumpAttribute;
import edu.brown.cs.spur.sump.SumpConstants.SumpClass;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;
import edu.brown.cs.spur.sump.SumpConstants.SumpOperation;

class EtchTransformRemoveUnused extends EtchTransform
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

EtchTransformRemoveUnused(Map<String,String> namemap)
{
   super("RemoveUnsed");
}



/********************************************************************************/
/*                                                                              */
/*      Apply the transform                                                     */
/*                                                                              */
/********************************************************************************/

@Override protected EtchMemo applyTransform(ASTNode n,SumpModel src,SumpModel target)
{ 
   RemoveMapper mapper = findMappings(n,src,target);
   if (mapper == null) return null;
   
   EtchMemo memo =  mapper.getMapMemo(n);
   
   return memo;
}



/********************************************************************************/
/*                                                                              */
/*      Handle finding unneeded items                                           */
/*                                                                              */
/********************************************************************************/

private RemoveMapper findMappings(ASTNode n,SumpModel src,SumpModel tgt)
{
   RemoveMapper rm = new RemoveMapper();
   
   Set<JcompSymbol> used = getInitialItems(n,src,tgt);
   addDependencies(n,used);
   findUnusedItems(rm,used,n);
   
   if (rm.isEmpty()) return null;
   
   return null;
}


/********************************************************************************/
/*                                                                              */
/*      Find all starting points                                                */
/*                                                                              */
/********************************************************************************/

private Set<JcompSymbol> getInitialItems(ASTNode n,SumpModel src,SumpModel tgt)
{
   InitialVisitor iv = new InitialVisitor(src,tgt);
   n.accept(iv);
   return iv.getItems();
}


private class InitialVisitor extends ASTVisitor {

   private Set<JcompSymbol> initial_items;
   private SumpModel target_model;
   
   InitialVisitor(SumpModel src,SumpModel tgt) {
      initial_items = new HashSet<>();
      target_model = tgt;
    }
   
   Set<JcompSymbol> getItems()                  { return initial_items; }
   
   @Override public void postVisit(ASTNode n) {
      JcompSymbol js = JcompAst.getDefinition(n);
      if (js == null) return;
      SumpAttribute sa = findAttribute(js.getFullName(),target_model);
      SumpClass sc = findClass(js.getFullName(),target_model);
      SumpOperation so = findOperation(js.getFullName(),target_model);
      if (sc == null && so == null && js.isConstructorSymbol()) {
         JcompType jt = js.getClassType();
         sc = findClass(jt.getName(),target_model);
       }
      if (sa != null || sc != null || so != null) initial_items.add(js);
    }
   
   @Override public void endVisit(MethodDeclaration md) {
      JcompSymbol js = JcompAst.getDefinition(md);
      if (js == null) return;
      if (initial_items.contains(js)) return;
      boolean istest = false;
      for (Object o : md.modifiers()) {
         if (o instanceof Annotation) {
            Annotation an = (Annotation) o;
            String nm = an.getTypeName().getFullyQualifiedName();
            if (nm.equals("Test") || nm.endsWith(".test")) istest = true;
          }
       }
      // if (md.getName().getIdentifier().startsWith("test")) istest = true;
      if (istest) initial_items.add(js);
    }   
}       // end of inner class InitialVisitor



/********************************************************************************/
/*                                                                              */
/*      Add all dependent symbols                                               */
/*                                                                              */
/********************************************************************************/

private void addDependencies(ASTNode cu,Set<JcompSymbol> used)
{
   boolean chng = true;
   while (chng) {
      chng = false;
      int ct = used.size();
      DependChecker dc = new DependChecker(used);
      cu.accept(dc);
      if (used.size() != ct) chng = true;
    }
}



private class DependChecker extends ASTVisitor {

   private Set<JcompSymbol> used_items;
   private Stack<Boolean> class_stack;
   private Stack<Boolean> method_stack;
   private boolean use_class;
   private boolean use_method;
   
   DependChecker(Set<JcompSymbol> used) {
      used_items = used;
      class_stack = new Stack<>();
      method_stack = new Stack<>();
      use_class = false;
      use_method = false;
    }
   
   @Override public boolean visit(TypeDeclaration td) {
      JcompSymbol js = JcompAst.getDefinition(td);
      if (used_items.contains(js)) {
         class_stack.push(use_class);
         use_class = true;
         return true;
       }
      System.err.println("SKIP " + td.getName());
      return false;
    }
   @Override public void endVisit(TypeDeclaration td) {
      JcompSymbol js = JcompAst.getDefinition(td);
      if (used_items.contains(js)) {
         use_class = class_stack.pop();
       }   
    }
   
   @Override public boolean visit(EnumDeclaration td) {
      JcompSymbol js = JcompAst.getDefinition(td);
      if (used_items.contains(js)) {
         class_stack.push(use_class);
         use_class = true;
         return true;
       }
      return false;
    }
   @Override public void endVisit(EnumDeclaration td) {
      JcompSymbol js = JcompAst.getDefinition(td);
      if (used_items.contains(js)) {
         use_class = class_stack.pop();
       }  
    }
   
   @Override public boolean visit(MethodDeclaration md) {
      JcompSymbol js = JcompAst.getDefinition(md);
      if (used_items.contains(js)) {
         method_stack.push(use_method);
         use_method = true;
         return true;
       }
      return false;
    }
   @Override public void endVisit(MethodDeclaration md) {
      JcompSymbol js = JcompAst.getDefinition(md);
      if (used_items.contains(js)) {
         use_method = method_stack.pop();
       }
    }
   
   @Override public boolean visit(VariableDeclarationFragment vdf) {
      if (vdf.getParent() instanceof FieldDeclaration) {
         JcompSymbol js = JcompAst.getDefinition(vdf);
         if (!used_items.contains(js)) return false;
       }
      return true;
    }
   
   @Override public void postVisit(ASTNode n) {
      JcompSymbol js = JcompAst.getReference(n);
      if (js == null) return;
      used_items.add(js);
      JcompType jt = js.getClassType();
      if (jt != null) {
         JcompSymbol jts = jt.getDefinition();
         if (jts != null) used_items.add(jts);
       }
      jt = JcompAst.getJavaType(n);
      if (jt == null) jt = JcompAst.getExprType(n);
      if (jt != null) {
         JcompSymbol jts = jt.getDefinition();
         if (jts != null) used_items.add(jts);
       }
    }
   
}       // end of inner class DependChecker



/********************************************************************************/
/*                                                                              */
/*      Finally find unused items                                               */
/*                                                                              */
/********************************************************************************/

private void findUnusedItems(RemoveMapper rm,Set<JcompSymbol> used,ASTNode n)
{
   UnusedVisitor uv = new UnusedVisitor(rm,used);
   n.accept(uv);
}



private class UnusedVisitor extends ASTVisitor {

   private RemoveMapper remove_mapper;
   private Set<JcompSymbol> used_items;
   
   UnusedVisitor(RemoveMapper rm,Set<JcompSymbol> used) {
      remove_mapper = rm;
      used_items = used;
    }
   
   @Override public boolean visit(TypeDeclaration td) {
      JcompSymbol js = JcompAst.getDefinition(td);
      if (js == null) return true;
      if (!used_items.contains(js)) {
         remove_mapper.addRemove(td);
         return false;
       }
      return true;
    }
   
   @Override public boolean visit(EnumDeclaration td) {
      JcompSymbol js = JcompAst.getDefinition(td);
      if (js != null && !used_items.contains(js)) {
         remove_mapper.addRemove(td);
         return false;
       }
      return true;
    }
   
   @Override public boolean visit(MethodDeclaration md) {
      JcompSymbol js = JcompAst.getDefinition(md);
      if (js != null && !used_items.contains(js)) {
         remove_mapper.addRemove(md);
         return false;
       }
      return true;
    }
   
   @Override public boolean visit(FieldDeclaration fd) {
      boolean keep = false;
      for (Object o : fd.fragments()) {
         VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
         JcompSymbol js = JcompAst.getDefinition(vdf);
         if (used_items.contains(js)) keep = true;
         else remove_mapper.addRemove(vdf);
       }
      if (!keep) {
         remove_mapper.addRemove(fd);
         for (Object o : fd.fragments()) {
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
            remove_mapper.removeRemove(vdf);
          }
       }
      return true;
    }
   
}       // end of inner class UnusedVisitor




/********************************************************************************/
/*                                                                              */
/*      Actual mapping                                                          */
/*                                                                              */
/********************************************************************************/

private class RemoveMapper extends EtchMapper {

   private Set<ASTNode> to_remove;
   
   RemoveMapper() {
      super(EtchTransformRemoveUnused.this);
      to_remove = new HashSet<>();
    }
   
   void addRemove(ASTNode n) {
      to_remove.add(n);
    }
   
   void removeRemove(ASTNode n) {
      to_remove.remove(n);
    }
   
   boolean isEmpty()                            { return to_remove.isEmpty(); }
   
   @Override void rewriteTree(ASTNode orig,ASTRewrite rw) {
      if (to_remove.contains(orig)) {
         rw.remove(orig,null);
       }
    }
   
}       // end of inner class RemoveMapper




}       // end of class EtchTransformRemoveUnused




/* end of EtchTransformRemoveUnused.java */

