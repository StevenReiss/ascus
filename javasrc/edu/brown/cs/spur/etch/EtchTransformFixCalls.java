/********************************************************************************/
/*                                                                              */
/*              EtchTransformFixCalls.java                                      */
/*                                                                              */
/*      description of class                                                    */
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.spur.sump.SumpConstants.SumpClass;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;
import edu.brown.cs.spur.sump.SumpConstants.SumpOperation;
import edu.brown.cs.spur.sump.SumpConstants.SumpParameter;


class EtchTransformFixCalls extends EtchTransform
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

EtchTransformFixCalls(Map<String,String> namemap)
{
   super("FixCalls");
   name_map = namemap;
}


/********************************************************************************/
/*                                                                              */
/*      Apply the transform                                                     */
/*                                                                              */
/********************************************************************************/

@Override protected EtchMemo applyTransform(ASTNode n,SumpModel src,SumpModel target)
{ 
   CallMapper mapper = findMappings(n,src,target);
   if (mapper == null) return null;
   
   EtchMemo memo =  mapper.getMapMemo(n);
   
   return memo;
}



/********************************************************************************/
/*                                                                              */
/*      Find changed calls                                                      */
/*                                                                              */
/********************************************************************************/

private CallMapper findMappings(ASTNode cu,SumpModel src,SumpModel tgt)
{
   // src = user model
   // tgt = retrieved model
   
   CallMapper mapper = new CallMapper(tgt);
   
   for (SumpClass tgtcls : tgt.getPackage().getClasses()) {
      String rslt = name_map.get(tgtcls.getFullName());
      if (rslt == null) continue;
      for (SumpOperation tgtop : tgtcls.getOperations()) {
         String oprslt = name_map.get(tgtop.getMapName());
         if (oprslt == null) continue;
         SumpOperation srcop = findOperation(oprslt,src);
         CallFix cf = null;
         if (tgtop.getReturnType() != null) {
            String tgtrt = tgtop.getReturnType().getName();
            String srcrt = srcop.getReturnType().getName();
            if (!tgtrt.equals(srcrt)) {
               if (cf == null) {
                  cf = new CallFix();
                  mapper.addCallFix(tgtop,cf);
                }
               cf.setReturnType(srcrt);
             }
          }
         int tgtct = 0;
         for (SumpParameter tgtp : tgtop.getParameters()) {
            String pnm = tgtp.getFullName();
            String match = name_map.get(pnm);
            if (match != null) {
               SumpParameter usesp = null;
               int srcct = 0;
               for (SumpParameter srcp : srcop.getParameters()) {
                  if (srcp.getFullName().equals(match)) {
                     usesp = srcp;
                     break;
                   }
                  ++srcct;
                }
               if (usesp != null) {
                  String ptypnm = usesp.getDataType().getBaseType().getName();
                  String btypnm = usesp.getDataType().getName();
                  boolean samety = ptypnm.equals(btypnm);
                  boolean sameord = srcct == tgtct;
                  if (!samety || !sameord) {
                     if (cf == null) {
                        cf = new CallFix();
                        mapper.addCallFix(tgtop,cf);
                      }
                     if (!samety) cf.setParameterType(tgtct,ptypnm);
                     if (!sameord) cf.setParameterOrder(tgtct,srcct);
                   }
                }
             }
            ++tgtct;
          }
       }
    }
   
   if (mapper.isEmpty()) return null;
   
   return mapper;
}

















/********************************************************************************/
/*                                                                              */
/*      Actual mapping transform                                                */
/*                                                                              */
/********************************************************************************/

private class CallMapper extends EtchMapper {

   private Map<SumpOperation,CallFix> op_fixes;
   private SumpModel target_model;
   
   CallMapper(SumpModel tgt) {
      super(EtchTransformFixCalls.this);
      op_fixes = new HashMap<>();
      target_model = tgt;
    }
   
   boolean isEmpty()                            { return op_fixes.isEmpty(); }
   
   void addCallFix(SumpOperation op,CallFix cf) {
      op_fixes.put(op,cf);
    }
   
   @Override void rewriteTree(ASTNode orig,ASTRewrite rw) {
      CallFix cf = null;
      if (orig instanceof MethodInvocation || orig instanceof ClassInstanceCreation) {
         JcompSymbol js = JcompAst.getReference(orig);
         JcompType jt = JcompAst.getExprType(orig);
         if (js == null || jt.isErrorType()) {
            JcompType ctyp = null;
            String mnm = null;
            if (orig instanceof MethodInvocation) {
               MethodInvocation mi = (MethodInvocation) orig;
               if (mi.getExpression() != null) {
                  ctyp = JcompAst.getExprType(mi.getExpression());
                }
               else {
                  for (ASTNode n = orig; n != null; n = n.getParent()) {
                     if (n instanceof AbstractTypeDeclaration) {
                        ctyp = JcompAst.getJavaType(n);
                        break;
                      }
                   }
                }
               mnm = mi.getName().getIdentifier();
             }
            else if (orig instanceof ClassInstanceCreation) {
               ClassInstanceCreation ci = (ClassInstanceCreation) orig;
               ctyp = JcompAst.getJavaType(ci.getType());
               mnm = "<init>";
             }
            if (ctyp == null) return;
            SumpClass tcls = findClass(ctyp.getName(),target_model);
            if (tcls == null) return;
            SumpOperation useop = null;
            for (SumpOperation op : tcls.getOperations()) {
               if (op.getName().equals(mnm)) {
                  // match parameters if there is more than one
                  useop = op;
                  break;
                }
             }
            if (useop != null) cf = op_fixes.get(useop);
          }
       }
      if (cf != null) {
         if (orig instanceof MethodInvocation) {
            MethodInvocation mi = (MethodInvocation) orig;
            ListRewrite lrw = rw.getListRewrite(mi,MethodInvocation.ARGUMENTS_PROPERTY);
            fixParameters(cf,mi,lrw,mi.arguments());
            fixCallReturn(cf,mi,rw);
          }
         else if (orig instanceof ClassInstanceCreation) {
            ClassInstanceCreation ci = (ClassInstanceCreation) orig;
            ListRewrite lrw = rw.getListRewrite(ci,ClassInstanceCreation.ARGUMENTS_PROPERTY);
            fixParameters(cf,ci,lrw,ci.arguments());
          }
       }
    }
   
   private void fixParameters(CallFix pf,ASTNode n,ListRewrite lrw,List<?> args) {
      AST ast = lrw.getASTRewrite().getAST();
      Map<Integer,Integer> order = pf.getOrderMap();
      Map<Integer,String> types = pf.getTypeMap();
      JcompTyper typer = JcompAst.getTyper(n);
      if (order.isEmpty()) {
         for (int i = 0; i < args.size(); ++i) {
            Expression ex = (Expression) args.get(i);
            String typ = types.get(i);
            if (typ != null) {
               JcompType jt = typer.findSystemType(typ);
               JcompType otyp = JcompAst.getExprType(ex);
               Expression oldex = (Expression) copyAst(ex);
               Expression newex = createCastExpr(ast,jt,oldex,otyp);
               lrw.replace(ex,newex,null);
             }
          }
       }
      else {
         List<Expression> orig = new ArrayList<>();
         for (int i = 0; i < args.size(); ++i) {
            Expression ex1 = (Expression) args.get(i);
            orig.add((Expression) copyAst(ex1));
          }
         for (int i = 0; i < args.size(); ++i) {
            Expression ex3 = (Expression) args.get(i);
            int j = i;
            for (Map.Entry<Integer,Integer> ent : order.entrySet()) {
               if (ent.getValue() == i) {
                  j = ent.getKey();
                  break;
                }
             }
            String typ = types.get(j);
            Expression ex2 = orig.get(i);
            if (typ != null) {
               JcompType jt = typer.findSystemType(typ);
               JcompType otyp = JcompAst.getExprType(ex3);
               ex2 = createCastExpr(ast,jt,ex2,otyp);
             }
            lrw.replace(ex3,ex2,null);
          }
       }
    }
   
   private void fixCallReturn(CallFix cf,MethodInvocation mi,ASTRewrite rw) {
      String tnm = cf.getReturnType();
      if (tnm == null) return;
      MethodInvocation nmi = (MethodInvocation) rw.createCopyTarget(mi);
      JcompTyper typer = JcompAst.getTyper(mi);
      JcompType jt = typer.findSystemType(tnm);
      JcompType oty = JcompAst.getExprType(mi);
      Expression rs = createCastExpr(rw.getAST(),jt,nmi,oty);
      rw.replace(mi,rs,null);
    }
   
}       // end of inner class CallMapper




/********************************************************************************/
/*                                                                              */
/*      Class to describe parameter fixes                                       */
/*                                                                              */
/********************************************************************************/

private static class CallFix {

   private Map<Integer,String> fix_types;
   private Map<Integer,Integer> fix_order;
   private String return_type;
   
   CallFix() {
      fix_types = new HashMap<>();
      fix_order = new HashMap<>();
      return_type = null;
    }
   
   Map<Integer,Integer> getOrderMap()       { return fix_order; }
   Map<Integer,String> getTypeMap()         { return fix_types; }
   String getReturnType()                   { return return_type; }
   
   void setParameterType(int idx,String typ) {
      fix_types.put(idx,typ);
    }
   
   void setParameterOrder(int idx,int ord) {
      fix_order.put(idx,ord);
    }

   void setReturnType(String typ) {
      return_type = typ;
    }
   
}       // end of inner class ParamFix



}       // end of class EtchTransformFixCalls




/* end of EtchTransformFixCalls.java */

