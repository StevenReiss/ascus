/********************************************************************************/
/*                                                                              */
/*              EtchTransformRemoveLocal.java                                   */
/*                                                                              */
/*      Remove any tests that use non-model methods                             */
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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;

class EtchTransformRemoveLocal extends EtchTransform
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CompilationUnit         base_ast;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

EtchTransformRemoveLocal(Map<String,String> namemap,CoseResult base)
{
   super("RemoveLocal");
   
   base_ast = (CompilationUnit) base.getStructure();
}




/********************************************************************************/
/*                                                                              */
/*      Apply the Transform                                                     */
/*                                                                              */
/********************************************************************************/

@Override protected EtchMemo applyTransform(ASTNode n,SumpModel src,SumpModel tgt)
{
   RemoveMapper mapper = findMappings(n,src,tgt);
   if (mapper == null) return null;
   
   EtchMemo memo = mapper.getMapMemo(n);
   
   return memo;
}



/********************************************************************************/
/*                                                                              */
/*      Find items to remove                                                    */
/*                                                                              */
/********************************************************************************/

private RemoveMapper findMappings(ASTNode n,SumpModel src,SumpModel tgt)
{
   RemoveMapper mapper = new RemoveMapper();
   
   Set<JcompSymbol> locals = findLocalMethods(n);
   if (locals == null || locals.isEmpty()) return null;
   
   findRemoveMethods(mapper,n,locals);
   
   if (mapper.isEmpty()) return null;
   
   return mapper;
}



/********************************************************************************/
/*                                                                              */
/*      Find all methods that reference non-model methods                       */
/*                                                                              */
/********************************************************************************/

private Set<JcompSymbol> findLocalMethods(ASTNode n)
{
   LocalFinder lf = new LocalFinder();
   for ( ; ; ) {
      n.accept(lf);
      if (!lf.hasChanged()) break;
    }
   
   return lf.getLocals();
}



private class LocalFinder extends ASTVisitor {

   private Set<JcompSymbol> local_syms;
   private boolean has_changed;
   private Stack<JcompSymbol> method_stack;
   
   LocalFinder() {
      local_syms = new HashSet<>();
      has_changed = false;
      method_stack = new Stack<>();
    }
   
   boolean hasChanged() {
      boolean fg = has_changed;
      has_changed = false;
      return fg;
    }
   
   Set<JcompSymbol> getLocals()                 { return local_syms; }
   
   @Override public boolean visit(MethodDeclaration md) {
      JcompSymbol js = JcompAst.getDefinition(md);
      if (local_syms.contains(js)) return false;
      method_stack.push(js);
      return true;
    }
   @Override public void endVisit(MethodDeclaration md) {
      method_stack.pop();
    }
   
   @Override public void postVisit(ASTNode n) {
      JcompSymbol js = JcompAst.getReference(n);
      if (js != null && isLocalSymbol(js) && !method_stack.isEmpty()) {
         JcompSymbol mthd = method_stack.peek();
         if (local_syms.add(mthd)) has_changed = true;
       }
    }
   
   private boolean isLocalSymbol(JcompSymbol js) {
      if (local_syms.contains(js)) return true;
      if (js.isBinarySymbol()) return false;
      ASTNode n = js.getDefinitionNode();
      if (n == null) return false;
      ASTNode root = n.getRoot();
      if (root == base_ast) return true;
      return false;
    }
   
}       // end of inner class LocalFinder




/********************************************************************************/
/*                                                                              */
/*      Find methods to remove                                                  */
/*                                                                              */
/********************************************************************************/

private void findRemoveMethods(RemoveMapper mapper,ASTNode n,Set<JcompSymbol> locals)
{
   RemoveFinder rf = new RemoveFinder(mapper,locals);
   n.accept(rf);
}


private class RemoveFinder extends ASTVisitor {

   private Set<JcompSymbol> local_syms;
   private RemoveMapper remove_mapper;
   
   RemoveFinder(RemoveMapper mapper,Set<JcompSymbol> locals) {
      local_syms = locals;
      remove_mapper = mapper;
    }
   
   @Override public boolean visit(MethodDeclaration md) {
       JcompSymbol js = JcompAst.getDefinition(md);
       if (js != null && local_syms.contains(js)) {
          remove_mapper.addRemove(md);
          return false;
        }
       return true;
    }
   
}       // end of inner class RemoveFinder



/********************************************************************************/
/*                                                                              */
/*      Actual mapping                                                          */
/*                                                                              */
/********************************************************************************/

private class RemoveMapper extends EtchMapper {

   private Set<ASTNode> to_remove;
   
   RemoveMapper() {
      super(EtchTransformRemoveLocal.this);
      to_remove = new HashSet<>();
    }
   
   void addRemove(ASTNode n) {
      to_remove.add(n);
    }
   
   boolean isEmpty()                            { return to_remove.isEmpty(); }
   
   @Override void rewriteTree(ASTNode orig,ASTRewrite rw) {
      if (to_remove.contains(orig)) {
         rw.remove(orig,null);
       }
    }

}       // end of inner class RemoveMapper
}       // end of class EtchTransformRemoveLocal




/* end of EtchTransformRemoveLocal.java */

