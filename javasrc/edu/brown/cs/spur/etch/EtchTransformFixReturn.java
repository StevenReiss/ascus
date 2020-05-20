/********************************************************************************/
/*                                                                              */
/*              EtchTransformFixReturn.java                                     */
/*                                                                              */
/*      Fix return type differences between code and model                      */
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

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.spur.sump.SumpDataType;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;
import edu.brown.cs.spur.sump.SumpConstants.SumpOperation;

class EtchTransformFixReturn extends EtchTransform
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

EtchTransformFixReturn(Map<String,String> namemap)
{
   super("FixReturn");
   name_map = namemap;
}



/********************************************************************************/
/*                                                                              */
/*      Apply the transform                                                     */
/*                                                                              */
/********************************************************************************/

@Override protected EtchMemo applyTransform(ASTNode n,SumpModel target)
{
   FixReturnMapper frm = new FixReturnMapper();
   
   findReturnsToFix(frm,n,target);
   
   if (frm.isEmpty()) return null;
   
   EtchMemo memo = frm.getMapMemo(n);
   
   return memo;
}



/********************************************************************************/
/*                                                                              */
/*      Find returns that need fixing                                           */
/*                                                                              */
/********************************************************************************/

private void findReturnsToFix(FixReturnMapper frm,ASTNode n,SumpModel target)
{
   ReturnFinder rf = new ReturnFinder(frm,target);
   n.accept(rf);
}



private class ReturnFinder extends ASTVisitor {

   private FixReturnMapper return_mapper;
   private SumpModel target_model;
   
   ReturnFinder(FixReturnMapper frm,SumpModel tgt) {
      return_mapper = frm;
      target_model = tgt;
    }
   
   @Override public boolean visit(MethodDeclaration md) {
      if (md.isConstructor()) return false;
      JcompSymbol js = JcompAst.getDefinition(md);
      String mnm = js.getFullName();
      String mdlnm = name_map.get(mnm);
      if (mdlnm == null) return false;
      SumpOperation op = findOperation(mdlnm,target_model);
      SumpDataType mdt = op.getReturnType();
      JcompTyper typer = JcompAst.getTyper(md);
      JcompType mrt = typer.findType(mdt.getName());
      JcompType rt = js.getType().getBaseType();
      if (rt == mrt) return false;
      return_mapper.addReturn(js,mdt);
      return true;
    }
   
}       // end of inner class ReturnFinder




/********************************************************************************/
/*                                                                              */
/*      Actual mapper                                                           */
/*                                                                              */
/********************************************************************************/

private class FixReturnMapper extends EtchMapper {

   private Map<JcompSymbol,SumpDataType> fix_returns;
   private JcompType orig_type;
   private SumpDataType return_type;
   private Stack<SumpDataType> return_stack;
   private Stack<JcompType> orig_stack;
   
   FixReturnMapper() {
      super(EtchTransformFixReturn.this);
      fix_returns = new HashMap<>();
      return_type = null;
      orig_type = null;
      return_stack = new Stack<>();
      orig_stack = new Stack<>();
    }
   
   void addReturn(JcompSymbol js,SumpDataType rt) {
      fix_returns.put(js,rt);
    }
   
   boolean isEmpty()                    { return fix_returns.isEmpty(); }
   
   @Override void preVisit(ASTNode orig) {
      if (orig instanceof MethodDeclaration) {
         MethodDeclaration md = (MethodDeclaration) orig;
         JcompSymbol ms = JcompAst.getDefinition(md);
         return_stack.push(return_type);
         orig_stack.push(orig_type);
         return_type = fix_returns.get(ms);
         orig_type = ms.getType().getBaseType();
       }
    }
   
   @Override void rewriteTree(ASTNode orig,ASTRewrite rw) {
      if (orig instanceof MethodDeclaration) {
         MethodDeclaration md = (MethodDeclaration) orig;
         if (return_type != null) {
            JcompSymbol ms = JcompAst.getDefinition(md);
            if (ms.getType().getBaseType().isVoidType()) {
               Block b = md.getBody();
               if (b != null) {
                  boolean needreturn = false;
                  if (b.statements().size() == 0) needreturn = true;
                  else {
                     ASTNode last = (ASTNode) b.statements().get(b.statements().size()-1);
                     if (!JcompAst.checkCanReturn(last)) needreturn = true;
                   }
                  if (needreturn) {
                     ReturnStatement r = rw.getAST().newReturnStatement();
                     Expression ex = return_type.getBaseType().createDefaultValue(rw.getAST());
                     if (ex != null) r.setExpression(ex);
                     ListRewrite lrw = rw.getListRewrite(b,Block.STATEMENTS_PROPERTY);
                     lrw.insertLast(r,null);
                   }
                }
             }
            Type t =  createTypeNode(return_type,rw.getAST());
            rw.set(md,MethodDeclaration.RETURN_TYPE2_PROPERTY,t,null);
          }
         orig_type = orig_stack.pop();
         return_type = return_stack.pop();
       }
      else if (orig instanceof ReturnStatement && return_type != null) {
         ReturnStatement rst = (ReturnStatement) orig;
         if (orig_type.isVoidType()) {
            Expression ex = return_type.getBaseType().createDefaultValue(rw.getAST());
            rw.set(orig,ReturnStatement.EXPRESSION_PROPERTY,ex,null);
          }
         else if (return_type.getBaseType().isVoidType()) {
            rw.set(orig,ReturnStatement.EXPRESSION_PROPERTY,null,null);
          }
         else {
            Expression old = (Expression) rw.createCopyTarget(rst.getExpression());
            CastExpression cst = rw.getAST().newCastExpression();
            cst.setExpression(old);
            Type st = createTypeNode(return_type,rw.getAST());
            cst.setType(st);
            rw.set(rst,ReturnStatement.EXPRESSION_PROPERTY,cst,null);
          }
       }
    }
   
}       // end of inner class FixReturnMapper




}       // end of class EtchTransformFixReturn




/* end of EtchTransformFixReturn.java */

