/********************************************************************************/
/*                                                                              */
/*              EtchTransformRemoveUndef.java                                   */
/*                                                                              */
/*      Remove code with undefined references                                   */
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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;

class EtchTransformRemoveUndef extends EtchTransform
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static Set<StructuralPropertyDescriptor> delete_froms;

static {
   delete_froms = new HashSet<StructuralPropertyDescriptor>();
   delete_froms.add(Block.STATEMENTS_PROPERTY);
   delete_froms.add(MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY);
   delete_froms.add(MethodDeclaration.MODIFIERS2_PROPERTY);
   delete_froms.add(FieldDeclaration.MODIFIERS2_PROPERTY);
   delete_froms.add(FieldDeclaration.FRAGMENTS_PROPERTY);
   delete_froms.add(TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
   delete_froms.add(TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY);
   delete_froms.add(TypeDeclaration.SUPERCLASS_TYPE_PROPERTY);
   delete_froms.add(TypeDeclaration.MODIFIERS2_PROPERTY);
   delete_froms.add(CompilationUnit.IMPORTS_PROPERTY);
   delete_froms.add(CompilationUnit.TYPES_PROPERTY);
}




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

EtchTransformRemoveUndef(Map<String,String> namemap)
{
   super("RemoveUndef");
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
   Set<ASTNode> undefs = null;
   UndefFinder ufn = new UndefFinder();
   for ( ; ; ) {
      n.accept(ufn);
      undefs = ufn.getUndefined();
      if (!ufn.hasChanged()) break;
    }
   if (undefs == null || undefs.size() == 0) return null;
   
   RemoveMapper rm = new RemoveMapper(undefs,ufn.getCheckSyms());
   
   return rm;
}


/********************************************************************************/
/*										*/
/*	Class to find package elements that contain undefined symbols		*/
/*										*/
/********************************************************************************/

private static class UndefFinder extends ASTVisitor {

   private Set<ASTNode> undef_items;
   private boolean has_undef;
   private boolean return_undef;
   private boolean ignore_undef;
   private boolean force_undef;
   private Set<JcompSymbol> undef_syms;
   private Set<JcompType> undef_types;
   private Set<JcompSymbol> check_syms;
   private Stack<Boolean> undef_stack;
   private boolean has_changed;
   
   UndefFinder() {
      undef_items = new HashSet<ASTNode>();
      undef_stack = new Stack<Boolean>();
      has_undef = false;
      return_undef = false;
      ignore_undef = false;
      force_undef = false;
      undef_syms = new HashSet<JcompSymbol>();
      undef_types = new HashSet<JcompType>();
      check_syms = new HashSet<JcompSymbol>();
      has_changed = false;
    }
   
   Set<ASTNode> getUndefined()			{ return undef_items; }
   Set<JcompSymbol> getCheckSyms()		{ return check_syms; }
   
   boolean hasChanged() {
      boolean fg = has_changed;
      has_changed = false;
      return fg;
    }
   
   @Override public void preVisit(ASTNode n) {
      undef_stack.push(has_undef);
      undef_stack.push(return_undef);
      undef_stack.push(force_undef);
      has_undef = false;
      return_undef = false;
      force_undef = false;
      if (!(n instanceof Expression)) {
         ignore_undef = false;
         force_undef = false;
       }
    }
   
   @Override public void postVisit(ASTNode node) {
      JcompType jt = JcompAst.getExprType(node);
      if (jt != null && jt.isErrorType() && !has_undef) {
         has_undef = true;
       }
      if (jt != null && undef_types.contains(jt) && !has_undef) {
         has_undef = true;
       }
      
      JcompSymbol jd = JcompAst.getDefinition(node);
      JcompSymbol js = JcompAst.getReference(node);
      if (jd == null && js != null && undef_syms.contains(js)) {
         force_undef = true;
       }
      if (node instanceof Expression) {
         if (ignore_undef)
            has_undef = false;
       }
      else {
         has_undef |= force_undef;
         force_undef = false;
         ignore_undef = false;
       }
      if (force_undef) {
         has_undef = true;
       }
      force_undef |= has_undef;
      
      if (has_undef) {
         if (jd != null) {
            if (undef_syms.add(jd)) {
               has_changed = true;
             }
            if (jd.isTypeSymbol()) {
               if (undef_types.add(jd.getType())) {
                  has_changed = true;
                }
             }
          }
         
         if (!return_undef && JcompAst.checkHasReturn(node))
            return_undef = true;
         
         StructuralPropertyDescriptor spd = node.getLocationInParent();
         if (delete_froms.contains(spd)) {
            if (undef_items.add(node)) {
               has_changed = true;
             }
            ASTNode par = node.getParent();
            switch (par.getNodeType()) {
               case ASTNode.TYPE_DECLARATION :
               case ASTNode.METHOD_DECLARATION :
                  return_undef = false;
                  has_undef = false;
                  force_undef = false;
                  break;
               default :
                  has_undef = return_undef;
                  force_undef = false;
                  break;
             }
          }
       }
      
      force_undef |= undef_stack.pop();
      return_undef |= undef_stack.pop();
      has_undef |= undef_stack.pop();
    }
   
   @Override public boolean visit(TryStatement n) {
      safeVisit(n.getBody());
      safeVisit(n.catchClauses());
      safeVisit(n.getFinally());
      return false;
    }
   
   @Override public boolean visit(CatchClause n) {
      safeVisit(n.getException());
      safeVisit(n.getBody());
      return false;
    }
   
   @Override public void endVisit(TryStatement ts) {
      // remove empty try statements
      if (isEmptyTryStatement(ts.getBody())) has_undef = true;
    }
   
   @Override public boolean visit(MethodDeclaration md) {
      safeVisit(md.modifiers());
      safeVisit(md.typeParameters());
      safeVisit(md.getReturnType2());
      safeVisit(md.getName());
      safeVisit(md.parameters());
      safeVisit(md.thrownExceptionTypes());
      boolean fg = has_undef;
      safeVisit(md.getBody());
      fg = (!fg && has_undef);
      
      // remove empty or unused private methods
      if (Modifier.isPrivate(md.getModifiers())) {
         if (isEmptyStatement(md.getBody())) has_undef = true;
         else {
            JcompSymbol js = JcompAst.getDefinition(md);
            if (js != null && !js.isUsed()) {
               has_undef = true;
             }
          }
       }
      else if (fg && Modifier.isPublic(md.getModifiers())) {
         JcompSymbol js = JcompAst.getDefinition(md);
         if (js != null) check_syms.add(js);
         has_undef = false;
         force_undef = false;
       }
      
      return false;
    }
   
   @Override public boolean visit(Initializer n) {
      safeVisit(n.getBody());
      if (isEmptyStatement(n.getBody())) {
         has_undef = true;
       }
      return false;
    }
   
   @Override public void endVisit(Block b) {
      // remove empty blocks in statement lists
      if (isEmptyStatement(b)) {
         ASTNode par = b.getParent();
         if (par.getNodeType() == ASTNode.BLOCK) has_undef = true;
       }
    }
   
   @Override public boolean visit(IfStatement n) {
      safeVisit(n.getExpression());
      boolean rtn = return_undef;
      safeVisit(n.getThenStatement());
      safeVisit(n.getElseStatement());
      return_undef = rtn;
      return false;
    }
   
   @Override public boolean visit(AnonymousClassDeclaration n) {
      boolean rtn = return_undef;
      safeVisit(n.bodyDeclarations());
      return_undef = rtn;
      return false;
    }
   
   @Override public void endVisit(IfStatement n) {
      // remove empty else clause
      // remove if statement if not else or then
      boolean thenempty = isEmptyStatement(n.getThenStatement());
      Statement elsestmt = n.getElseStatement();
      boolean elseempty = isEmptyStatement(elsestmt);
      if (thenempty && elseempty) has_undef = true;
      else if (elseempty && !has_undef && elsestmt != null) {
         undef_items.add(elsestmt);
       }
    }
   
   @Override public void endVisit(EnhancedForStatement n) {
      // remove empty for statements
      if (isEmptyStatement(n.getBody())) has_undef = true;
    }
   
   @Override public void endVisit(ForStatement n) {
      // remove empty for statements
      if (isEmptyStatement(n.getBody())) {
         boolean ok = false;
         List<?> inits = n.initializers();
         if (inits.size() == 0) ok = true;
         if (inits.size() == 1) {
            ASTNode sn = (ASTNode) inits.get(0);
            if (sn.getNodeType() == ASTNode.VARIABLE_DECLARATION_EXPRESSION) ok = true;
          }
         List<?> upds = n.updaters();
         if (upds.size() != 1) ok = false;
         if (ok) has_undef = true;
       }
    }
   
   @Override public void endVisit(WhileStatement n) {
      // remove empty while statements (might want to take condition into account)
      if (isEmptyStatement(n.getBody())) {
         has_undef = true;
       }
    }
   
   @Override public boolean visit(TypeDeclaration td) {
      safeVisit(td.modifiers());
      safeVisit(td.typeParameters());
      safeVisit(td.getName());
      safeVisit(td.getSuperclassType());
      safeVisit(td.superInterfaceTypes());
      safeVisit(td.bodyDeclarations());
      
      JcompType jt = JcompAst.getJavaType(td);
      if (jt == null) return false;
      if (jt.isAbstract() || jt.isInterfaceType()) return false;
      if (td.getParent() instanceof CompilationUnit) return false;
      JcompTyper typer = JcompAst.getTyper(td);
      if (jt.definesAllNeededMethods(typer)) return false;
      has_undef = true;
      
      return false;
    }
   
   @Override public void endVisit(VariableDeclarationFragment vdf) {
      // remove unused static or private fields
      JcompSymbol js = JcompAst.getDefinition(vdf);
      if (js == null) return;
      if (js.getType().isUndefined())
         has_undef = true;
      if (!js.isFieldSymbol()) return;
      if (!js.isUsed() && !js.isBinarySymbol()) {
         if (js.isStatic() || js.isPrivate()) {
            has_undef = true;
          }
       }
      else if (!js.isRead() && !js.isBinarySymbol()) {
         Expression exp = vdf.getInitializer();
         if (exp != null) {
            if (exp.getNodeType() == ASTNode.METHOD_INVOCATION) return;
            if (exp.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION) return;
          }
         if (js.isStatic() || js.isPrivate()) {
            if (js.getName().equals("serialVersionUID")) return;
            // System.err.println("FIELD " + js + " NOT READ");
            has_undef = true;
          }
       }
    }
   
   @Override public void endVisit(FieldDeclaration fd) {
      int ctr = 0;
      for (Object o : fd.fragments()) {
         VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
         if (!undef_items.contains(vdf)) ++ctr;
       }
      if (ctr == 0) {
         has_undef = true;
       }
    }
   
   @Override public void endVisit(VariableDeclarationStatement vd) {
      int ctr = 0;
      for (Object o : vd.fragments()) {
         VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
         if (!undef_items.contains(vdf)) ++ctr;
       }
      if (ctr == 0) {
         has_undef = true;
       }
    }
   
   @Override public void endVisit(SimpleType n) {
      JcompType jt = JcompAst.getJavaType(n);
      if (jt != null && jt.isUndefined()) {
         has_undef = true;
       }
    }
   
   @Override public void endVisit(Assignment n) {
      if (has_undef) {
         Expression ex = n.getLeftHandSide();
         JcompSymbol js = JcompAst.getReference(ex);
         if (js != null && !js.isFieldSymbol()) check_syms.add(js);
       }
    }
   
   @Override public void endVisit(SingleMemberAnnotation n) {
      JcompType jt = JcompAst.getJavaType(n.getTypeName());
      if (jt == null || jt.isUndefined()) {
         has_undef = true;
         force_undef = true;
       }
      return;
    }
   
   
   
   @Override public void endVisit(NormalAnnotation n) {
      JcompType jt = JcompAst.getJavaType(n.getTypeName());
      if (jt == null || jt.isUndefined()) {
         has_undef = true;
         force_undef = true;
       }
      return;
    }
   
   @Override public void endVisit(MarkerAnnotation n) {
      JcompType jt = JcompAst.getJavaType(n.getTypeName());
      if (jt == null || jt.isUndefined()) {
         has_undef = true;
       }
      return;
    }
   
   
   private void safeVisit(ASTNode n) {
      if (n == null) return;
      boolean udf = has_undef;
      boolean rtn = return_undef;
      boolean fudf = force_undef;
      has_undef = false;
      force_undef = false;
      return_undef = false;
      n.accept(this);
      has_undef |= udf;
      return_undef |= rtn;
      force_undef |= fudf;
    }
   
   private void safeVisit(List<?> nl) {
      if (nl == null) return;
      for (Object o : nl) {
         ASTNode n = (ASTNode) o;
         safeVisit(n);
       }
    }
   
   private boolean isEmptyStatement(ASTNode st) {
      if (st == null) return true;
      switch (st.getNodeType()) {
         case ASTNode.EMPTY_STATEMENT :
            return true;
         case ASTNode.BLOCK :
            Block thb = (Block) st;
            int ctr = 0;
            for (Object o : thb.statements()) {
               ASTNode n = (ASTNode) o;
               if (!undef_items.contains(n)) {
                  if (ctr > 0) ++ctr;
                  else if (!doesNothing(n)) ++ctr;
                }
             }
            if (ctr == 0) return true;
            break;
       }
      return false;
    }
   
   
   private boolean isEmptyTryStatement(ASTNode st) {
      if (st == null) return true;
      switch (st.getNodeType()) {
         case ASTNode.EMPTY_STATEMENT :
            return true;
         case ASTNode.BLOCK :
            Block thb = (Block) st;
            int ctr = 0;
            for (Object o : thb.statements()) {
               ASTNode n = (ASTNode) o;
               if (!undef_items.contains(n)) {
                  if (ctr > 0) ++ctr;
                  else if (n instanceof ThrowStatement) ;
                  else if (!doesNothing(n)) ++ctr;
                }
             }
            if (ctr == 0) return true;
            break;
       }
      return false;
    }
   
   
   private boolean doesNothing(ASTNode n)
   {
      switch (n.getNodeType()) {
         case ASTNode.EXPRESSION_STATEMENT :
            return doesNothing(((ExpressionStatement) n).getExpression());
         case ASTNode.VARIABLE_DECLARATION_STATEMENT :
            VariableDeclarationStatement vds = (VariableDeclarationStatement) n;
            for (Object o : vds.fragments()) {
               VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
               if (vdf.getInitializer() != null) return false;
             }
            return true;
         case ASTNode.EMPTY_STATEMENT :
            return true;
         case ASTNode.BOOLEAN_LITERAL :
         case ASTNode.CHARACTER_LITERAL :
         case ASTNode.NULL_LITERAL :
         case ASTNode.NUMBER_LITERAL :
         case ASTNode.TYPE_LITERAL :
            return true;
         case ASTNode.PARENTHESIZED_EXPRESSION :
            return doesNothing(((ParenthesizedExpression) n).getExpression());
         case ASTNode.METHOD_INVOCATION :
            MethodInvocation mi = (MethodInvocation) n;
            JcompSymbol js = JcompAst.getReference(mi.getName());
            if (js != null) {
               String nm = js.getFullName();
               switch (nm) {
                  case "edu.brown.cs.s6.runner.RunnerAssert.fail" :
                     ASTNode nx = mi.getParent().getParent().getParent();
                     if (nx.getNodeType() == ASTNode.TRY_STATEMENT)
                        return true;
                     break;
                  default :
                     break;
                }
             }
            break;
         default :
            break;
       }
      return false;
    }
   
}	// end of subclass UndefFinder







/********************************************************************************/
/*                                                                              */
/*      Actual mapping                                                          */
/*                                                                              */
/********************************************************************************/

private class RemoveMapper extends EtchMapper {

   private Set<ASTNode> to_remove;
   private Set<JcompSymbol> check_syms;
   private boolean fix_override;
   
   RemoveMapper(Collection<ASTNode> rems,Set<JcompSymbol> checks) {
      super(EtchTransformRemoveUndef.this);
      to_remove = new HashSet<>(rems);
      check_syms = checks;
      fix_override = false;
      for (ASTNode n : rems) {
	 if (n.getLocationInParent() == TypeDeclaration.SUPERCLASS_TYPE_PROPERTY ||
               n.getLocationInParent() == TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY)
	    fix_override = true;
	 else if (n.getNodeType() == ASTNode.METHOD_DECLARATION) {
	    // might want to restrict to methods with this name
	    fix_override = true;
	  }
       }
    }

   @Override void rewriteTree(ASTNode orig,ASTRewrite rw) {
      if (to_remove.contains(orig)) {
	 StructuralPropertyDescriptor spd = orig.getLocationInParent();
	 ASTNode par = orig.getParent();
	 if (delete_froms.contains(spd)) {
	    if (spd.isChildListProperty()) {
	       ListRewrite lrw = rw.getListRewrite(par,(ChildListPropertyDescriptor) spd);
	       lrw.remove(orig,null);
	     }
	    else if (spd.isChildProperty()) {
	       rw.set(par,spd,null,null);
	     }
	  }
	 else if (spd == IfStatement.THEN_STATEMENT_PROPERTY) {
	    EmptyStatement es = rw.getAST().newEmptyStatement();
	    rw.set(par,spd,es,null);
	  }
	 else if (spd == IfStatement.ELSE_STATEMENT_PROPERTY) {
	    rw.set(par,spd,null,null);
	  }
	 else  {
	    // replace with something appropriate
	  }
       }
      else if (orig.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
	 VariableDeclarationFragment vdf = (VariableDeclarationFragment) orig;
	 JcompSymbol js = JcompAst.getDefinition(orig);
	 if (js != null && check_syms != null && check_syms.contains(js)) {
	    JcompType jt = js.getType();
	    if (vdf.getInitializer() == null) {
	       Expression ex = jt.createDefaultValue(rw.getAST());
	       rw.set(orig,VariableDeclarationFragment.INITIALIZER_PROPERTY,ex,null);
	     }
	  }
       }
      else if (orig.getNodeType() == ASTNode.METHOD_DECLARATION) {
	 MethodDeclaration md = (MethodDeclaration) orig;
	 JcompSymbol js = JcompAst.getDefinition(orig);
	 if (js != null && check_syms != null && check_syms.contains(js)) {
	    Block b = md.getBody();
	    JcompType jt = js.getType();
	    JcompType rt = jt.getBaseType();
	    if (b != null && rt != null && !rt.isVoidType()) {
	       if (needsReturn(b)) {
		  AST ast = rw.getAST();
		  ReturnStatement rs = ast.newReturnStatement();
		  Expression ex = rt.createDefaultValue(ast);
		  rs.setExpression(ex);
		  ListRewrite lrw = rw.getListRewrite(b,Block.STATEMENTS_PROPERTY);
		  lrw.insertLast(rs,null);
		}
	     }
	  }
	 if (fix_override) {
	    if (Modifier.isPublic(md.getModifiers()) || Modifier.isProtected(md.getModifiers())) {
	       for (Object o : md.modifiers()) {
		  IExtendedModifier iem = (IExtendedModifier) o;
		  if (iem.isAnnotation()) {
		     Annotation ann = (Annotation) iem;
		     String tnm = ann.getTypeName().getFullyQualifiedName();
		     if (tnm.contains("Override")) {
			ListRewrite lrw = rw.getListRewrite(md,MethodDeclaration.MODIFIERS2_PROPERTY);
			lrw.remove((ASTNode) o,null);
			break;
		      }
		   }
		}
	     }
	  }
       }
    }
   
   private boolean needsReturn(Block b) {
      if (JcompAst.checkHasReturn(b)) return false;
      List<?> stmts = b.statements();
      int ln = stmts.size();
      if (ln == 0) return true;
      ASTNode n = (ASTNode) stmts.get(ln-1);
      if (n.getNodeType() == ASTNode.THROW_STATEMENT) return false;
      return true;
    }
   
}       // end of inner class RemoveMapper



}       // end of class EtchTransformRemoveUndef




/* end of EtchTransformRemoveUndef.java */

