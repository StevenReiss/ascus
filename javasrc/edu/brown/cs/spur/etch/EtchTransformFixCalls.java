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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
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
/*      Find parameter mappings                                                 */
/*                                                                              */
/********************************************************************************/

private CallMapper findMappings(ASTNode cu,SumpModel source,SumpModel target)
{
   CallMapper mapper = new CallMapper();
   
   findMatchings(cu,source,target,mapper);
   
   if (mapper.isEmpty()) return null;
   
   return mapper;
}




/********************************************************************************/
/*                                                                              */
/*      Identify all parameters to update                                       */
/*                                                                              */
/********************************************************************************/

private void findMatchings(ASTNode cu,SumpModel src,SumpModel target,CallMapper mapper)
{
   ParamVisitor sp = new ParamVisitor(src,target,mapper);
   cu.accept(sp);
}


private class ParamVisitor extends ASTVisitor {

   private SumpModel source_model;
   private SumpModel target_model;
   private CallMapper param_mapper;
   
   ParamVisitor(SumpModel s,SumpModel t,CallMapper pm) {
      source_model = s;
      target_model = t;
      param_mapper = pm;
    }
   
   @Override public boolean visit(MethodDeclaration md) {
      JcompSymbol jm = JcompAst.getDefinition(md);
      String mnm = getMapName(jm);
      CallFix pf = null;
      SumpOperation sop = findOperation(mnm,source_model);
      if (sop == null) return false;
      
      Type t = md.getReturnType2();
      if (t != null) {           // constructor
         String rtnm = "void";
         JcompType jt = JcompAst.getJavaType(t);
         if (jt == null) rtnm = t.toString();
         else rtnm = jt.getName();
         String rtyp = sop.getReturnType().getName();
         if (!rtnm.equals(rtyp)) {
            if (pf == null) {
               pf = new CallFix();
               param_mapper.addCallFix(jm,pf);
             }  
            pf.setReturnType(rtyp);
          }
       }

      int ct = 0;
      for (SumpParameter srcp : sop.getParameters()) {
          String pnm = srcp.getFullName();
          String match = name_map.get(pnm);
          if (match != null) {
             SumpOperation op = findOperation(match,target_model);
             int pct = 0;
             SumpParameter usesp = null;
             for (SumpParameter sp : op.getParameters()) {
                if (sp.getFullName().equals(match)) {
                   usesp = sp;
                   break;
                 }
                ++pct;
              }
             if (usesp != null) {
                String ptypnm = usesp.getDataType().getBaseType().getName();
                String tnm = usesp.getDataType().getName();
                String btypnm = name_map.get(tnm);
                if (btypnm == null) btypnm = tnm;
                boolean samety = ptypnm.equals(btypnm);
                boolean sameord = ct == pct;
                if (!samety || !sameord) {
                   if (pf == null) {
                      pf = new CallFix();
                      param_mapper.addCallFix(jm,pf);
                    }
                   if (!samety) pf.setParameterType(ct,ptypnm);
                   if (!sameord) pf.setParameterOrder(ct,pct);
                 }
              }
           }
          ++ct;
       }
      
      return false;
    }
   
}       // end of inner class ParamVisitor





/********************************************************************************/
/*                                                                              */
/*      Actual mapping transform                                                */
/*                                                                              */
/********************************************************************************/

private class CallMapper extends EtchMapper {

   private Map<JcompSymbol,CallFix> fix_set;
   
   CallMapper() {
      super(EtchTransformFixCalls.this);
      fix_set = new HashMap<>();
    }
   
   boolean isEmpty()                            { return fix_set.isEmpty(); }
   
   void addCallFix(JcompSymbol m,CallFix pf) {
      fix_set.put(m,pf);
    }
   
   @Override void rewriteTree(ASTNode orig,ASTRewrite rw) {
      JcompSymbol js = JcompAst.getReference(orig);
      CallFix pf = fix_set.get(js);
      if (pf == null) return;
      
      if (orig instanceof MethodInvocation) {
          MethodInvocation mi = (MethodInvocation) orig;
          ListRewrite lrw = rw.getListRewrite(mi,MethodInvocation.ARGUMENTS_PROPERTY);
          fixParameters(pf,mi,lrw,mi.arguments());
          fixCallReturn(pf,mi,rw);
        }
      else if (orig instanceof ClassInstanceCreation) {
         ClassInstanceCreation ci = (ClassInstanceCreation) orig;
         ListRewrite lrw = rw.getListRewrite(ci,ClassInstanceCreation.ARGUMENTS_PROPERTY);
         fixParameters(pf,ci,lrw,ci.arguments());
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

