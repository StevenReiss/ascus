/********************************************************************************/
/*                                                                              */
/*              EtchTransformFieldFix.java                                      */
/*                                                                              */
/*      Handle changes to field types                                           */
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
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.spur.sump.SumpConstants.SumpAttribute;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;

class EtchTransformFieldFix extends EtchTransform
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

EtchTransformFieldFix(Map<String,String> namemap)
{
   super("FieldFix");
   name_map = namemap;
}


/********************************************************************************/
/*                                                                              */
/*      Apply the transform                                                     */
/*                                                                              */
/********************************************************************************/

@Override protected EtchMemo applyTransform(ASTNode n,SumpModel src,SumpModel target)
{
   FieldMapper mapper =  findMappings(n,src,target);
   if (mapper == null) return null;
   
   EtchMemo memo =  mapper.getMapMemo(n);
   
   return memo;
}


/********************************************************************************/
/*                                                                              */
/*      Identify all parameters to update                                       */
/*                                                                              */
/********************************************************************************/

private FieldMapper findMappings(ASTNode cu,SumpModel source,SumpModel target)
{
   FieldMapper mapper = new FieldMapper();
   
   FieldVisitor sp = new FieldVisitor(target,mapper);
   cu.accept(sp); 
   
   if (mapper.isEmpty()) return null;
   
   return mapper;
}



private class FieldVisitor extends ASTVisitor {

   private SumpModel target_model;
   private FieldMapper field_mapper;
   
   FieldVisitor(SumpModel t,FieldMapper pm) {
      target_model = t;
      field_mapper = pm;
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
         field_mapper.addMapping(jm,atyp);
       }
      return false;
    }
   
}       // end of inner class FieldVisitor





/********************************************************************************/
/*                                                                              */
/*      Actual mapping transform                                                */
/*                                                                              */
/********************************************************************************/

private class FieldMapper extends EtchMapper {

   private Map<JcompSymbol,String> field_type;
   
   FieldMapper() {
      super(EtchTransformFieldFix.this);
      field_type = new HashMap<>();
    }
   
   boolean isEmpty()                    { return field_type.isEmpty(); }
   
   void addMapping(JcompSymbol js,String typ) {
      field_type.put(js,typ);
    }
   
   @SuppressWarnings("unchecked")
   @Override void rewriteTree(ASTNode orig,ASTRewrite rw) {
      if (orig instanceof FieldDeclaration) {
         FieldDeclaration fd = (FieldDeclaration) orig;
         int ct = 0;
         int samct = 0;
         String typ = null;
         for (Object o : fd.fragments()) {
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
            ++ct;
            JcompSymbol jm = JcompAst.getDefinition(vdf);
            String ntyp = field_type.get(jm);
            if (jm != null && ntyp != null) {     
               if (typ == null || typ.equals(ntyp)) {
                  typ = ntyp;
                  ++samct;
                }
             }
          }
         if (typ == null) return;
         if (ct > 1 && ct != samct) {
            AbstractTypeDeclaration par = (AbstractTypeDeclaration) fd.getParent();
            ListRewrite lrw = rw.getListRewrite(par,par.getBodyDeclarationsProperty());
            ASTNode after = null;
            for (Object o : fd.fragments()) {
               VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
               JcompSymbol jm = JcompAst.getDefinition(vdf);
               String ntyp = field_type.get(jm);
               VariableDeclarationFragment nvdf = null;
               Type t = null;
               nvdf = (VariableDeclarationFragment) copyAst(vdf);
               if (ntyp == null || vdf.getInitializer() == null) {
                  t = (Type) copyAst(fd.getType());
                }
               else {
                  JcompType otyp = JcompAst.getExprType(vdf.getInitializer());
                  JcompType njt = JcompAst.getTyper(orig).findSystemType(ntyp);
                  t = njt.createAstNode(rw.getAST());
                  Expression nex = createCastExpr(rw.getAST(),njt,nvdf.getInitializer(),otyp);
                  nvdf.setInitializer(nex);
                }
               
               FieldDeclaration nfld = rw.getAST().newFieldDeclaration(nvdf);
               nfld.setType(t);
               for (Object o1 : fd.modifiers()) {
                  ASTNode em = (ASTNode) o1;
                  ASTNode nem = copyAst(em);
                  nfld.modifiers().add(nem);
                }
               if (after == null) {
                  lrw.replace(fd,nfld,null);
                }
               else {
                  lrw.insertAfter(nfld,after,null);
                }
               after = nfld;
             }
          }
         else {
            JcompType jt = JcompAst.getTyper(orig).findSystemType(typ);
            Type t = jt.createAstNode(rw.getAST());
            rw.set(fd,FieldDeclaration.TYPE_PROPERTY,t,null);
            for (Object o : fd.fragments()) {
               VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
               if (vdf.getInitializer() != null) {
                  JcompType otyp = JcompAst.getExprType(vdf.getInitializer());
                  Expression oex = (Expression) copyAst(vdf.getInitializer());
                  Expression nex = createCastExpr(rw.getAST(),jt,oex,otyp);
                  rw.set(vdf,VariableDeclarationFragment.INITIALIZER_PROPERTY,nex,null);
                }
             }
          }
       }
    }
   
}       // end of inner class FieldMapper




}       // end of class EtchTransformFieldFix




/* end of EtchTransformFieldFix.java */

