/********************************************************************************/
/*                                                                              */
/*              EtchTransformFixReturns.java                                    */
/*                                                                              */
/*      Handle return type differences between models                           */
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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.spur.sump.SumpConstants.SumpAttribute;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;
import edu.brown.cs.spur.sump.SumpConstants.SumpOperation;

class EtchTransformFixReturns extends EtchTransform
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,String> name_map;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

EtchTransformFixReturns(Map<String,String> namemap)
{
   super("FixReturns");
   name_map = namemap;
}


/********************************************************************************/
/*                                                                              */
/*      Apply the transform                                                     */
/*                                                                              */
/********************************************************************************/

@Override protected EtchMemo applyTransform(ASTNode n,SumpModel src,SumpModel target)
{
   ReturnMapper mapper =  findMappings(n,src,target);
   if (mapper == null) return null;
   
   EtchMemo memo =  mapper.getMapMemo(n);
   
   return memo;
}


/********************************************************************************/
/*                                                                              */
/*      Identify all parameters to update                                       */
/*                                                                              */
/********************************************************************************/

private ReturnMapper findMappings(ASTNode cu,SumpModel source,SumpModel target)
{
   ReturnMapper mapper = new ReturnMapper();
   
   findMatchings(cu,target,mapper);
   
   if (mapper.isEmpty()) return null;
   
   return mapper;
}



private void findMatchings(ASTNode cu,SumpModel target,ReturnMapper mapper)
{
   ReturnVisitor sp = new ReturnVisitor(target,mapper);
   cu.accept(sp);
}



private class ReturnVisitor extends ASTVisitor {

   private SumpModel target_model;
   private ReturnMapper return_mapper;
   
   ReturnVisitor(SumpModel t,ReturnMapper pm) {
      target_model = t;
      return_mapper = pm;
    }
   
   @Override public boolean visit(MethodDeclaration md) {
      JcompSymbol jm = JcompAst.getDefinition(md);
      String mnm = getMapName(jm);
      String tnm = name_map.get(mnm);
      if (tnm == null) return false;
      Type t = md.getReturnType2();
      if (t == null) return false;              // constructor
      SumpOperation op = findOperation(tnm,target_model);
      if (op == null) return false;
      String rtnm = "void";
      JcompType jt = JcompAst.getJavaType(t);
      if (jt == null) rtnm = t.toString();
      else rtnm = jt.getName();
      String rtyp = op.getReturnType().getName();
      if (rtnm.equals(rtyp)) return false;
      return_mapper.addMapping(jm,rtyp);
      
      return false;
    }
   
   @Override public boolean visit(FieldDeclaration fd) {
      for (Object o : fd.fragments()) {
         VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
         JcompSymbol jm = JcompAst.getDefinition(vdf);
         String mnm = getMapName(jm);
         String tnm = name_map.get(mnm);
         if (tnm == null) continue;
         Type t = fd.getType();
         JcompType jt = JcompAst.getJavaType(t);
         SumpAttribute at = findAttribute(tnm,target_model);
         if (at == null) continue;
         String atyp = at.getDataType().getName();
         if (jt.getName().equals(atyp)) continue;
         return_mapper.addMapping(jm,atyp);
       }
      return false;
    }
   
}       // end of inner class ReturnVisitor





/********************************************************************************/
/*                                                                              */
/*      Actual mapping transform                                                */
/*                                                                              */
/********************************************************************************/

private class ReturnMapper extends EtchMapper {

   private Map<JcompSymbol,String> return_type;
   private JcompType current_type;
   
   ReturnMapper() {
      super(EtchTransformFixReturns.this);
      return_type = new HashMap<>();
      current_type = null;
    }
   
   boolean isEmpty()                    { return return_type.isEmpty(); }
   
   void addMapping(JcompSymbol js,String typ) {
      return_type.put(js,typ);
    }
   
   @Override boolean preVisit(ASTNode orig,ASTRewrite rw) {
      if (orig instanceof MethodDeclaration) {
         JcompSymbol jm = JcompAst.getDefinition(orig);
         if (jm != null && current_type == null && return_type.containsKey(jm)) {
            JcompTyper typer = JcompAst.getTyper(orig);
            String tnm = return_type.get(jm);
            current_type = typer.findSystemType(tnm);
            return true;
          }
       }
      if (orig instanceof FieldDeclaration) {
         FieldDeclaration fd = (FieldDeclaration) orig;
         int ct = 0;
         boolean use = false;
         for (Object o : fd.fragments()) {
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
            ++ct;
            JcompSymbol jm = JcompAst.getDefinition(vdf);
            if (jm != null && return_type.containsKey(jm)) {     
               use = true;
             }
          }
         if (!use) return false;
         if (ct > 1) {
            
          }
         else {
            
          }
       }
      return false;
    }
   
   @Override void rewriteTree(ASTNode orig,ASTRewrite rw) {
      if (orig instanceof ReturnStatement && current_type != null) {
         ReturnStatement rs = (ReturnStatement) orig;
         if (current_type.isVoidType()) {
            rw.remove(rs.getExpression(),null);
          }
         else {
            Expression newex = null;
            if (rs.getExpression() == null) {
               newex = current_type.createDefaultValue(rw.getAST());
             }
            else {
               JcompType jt = JcompAst.getExprType(rs.getExpression());
               Expression oex = (Expression) copyAst(rs.getExpression());
               newex = createCastExpr(rw.getAST(),current_type,oex,jt);
             }
            rw.set(rs,ReturnStatement.EXPRESSION_PROPERTY,newex,null);
          }
       }
      else if (orig instanceof FieldDeclaration) {
        
       }
      else if (orig instanceof VariableDeclarationFragment) {
         ASTNode par = orig.getParent();
         if (par instanceof FieldDeclaration) {
            // need to isolate this declaration
            // need to add cast to initialization expression
            FieldDeclaration fd = (FieldDeclaration) par;
            JcompSymbol jm = JcompAst.getDefinition(orig);
            if (jm != null && return_type.containsKey(jm)) {
               JcompTyper typer = JcompAst.getTyper(orig);
               String tnm = return_type.get(jm);
               JcompType jt = typer.findSystemType(tnm);
             }
          }
       }
    }
   
}       // end of inner class ReturnMapper




}       // end of class EtchTransformFixReturns




/* end of EtchTransformFixReturns.java */

