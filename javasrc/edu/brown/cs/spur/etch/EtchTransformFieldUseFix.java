/********************************************************************************/
/*                                                                              */
/*              EtchTransformFixFields.java                                     */
/*                                                                              */
/*      Handle field type changes                                               */
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
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.spur.sump.SumpConstants.SumpAttribute;
import edu.brown.cs.spur.sump.SumpConstants.SumpClass;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;


class EtchTransformFieldUseFix extends EtchTransform
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

EtchTransformFieldUseFix(Map<String,String> namemap)
{
   super("FieldUseFix");
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
   FieldMapper mapper = new FieldMapper(source,target);
   
   for (SumpClass tgtcls : target.getPackage().getClasses()) {
      String rslt = name_map.get(tgtcls.getFullName());
      if (rslt == null) continue;
      for (SumpAttribute tgatt : tgtcls.getAttributes()) {
         String atrslt =  name_map.get(tgatt.getMapName());
         if (atrslt == null) continue;
         SumpAttribute srcatt = findAttribute(atrslt,source);
         String tgttyp = tgatt.getDataType().getName();
         String srctyp = srcatt.getDataType().getName();
         if (!tgttyp.equals(srctyp)) {
            mapper.addMapping(tgatt,srctyp);
          }
       }
    }
   
   if (mapper.isEmpty()) return null;
   
   return mapper;
}



/********************************************************************************/
/*                                                                              */
/*      Actual mapper                                                           */
/*                                                                              */
/********************************************************************************/

private class FieldMapper extends EtchMapper {

   private Map<SumpAttribute,String> map_attrs;
   private SumpModel target_model;
   
   FieldMapper(SumpModel src,SumpModel tgt) {
      super(EtchTransformFieldUseFix.this);
      map_attrs = new HashMap<>(); 
      target_model = tgt;
    }
   
   void addMapping(SumpAttribute att,String typ) {
      map_attrs.put(att,typ);
    }
   
   boolean isEmpty()                            { return map_attrs.isEmpty(); }
   
   @Override public void rewriteTree(ASTNode orig,ASTRewrite rw) {
      if (orig instanceof FieldAccess) {
         FieldAccess facc = (FieldAccess) orig;
         JcompSymbol js = JcompAst.getReference(facc.getName());
         fixReference(facc,js,rw);
       }
      else if (orig instanceof QualifiedName) {
         QualifiedName qnm = (QualifiedName) orig;
         JcompSymbol js = JcompAst.getReference(qnm.getName());
         fixReference(qnm,js,rw);
       }
    }
   
   private void fixReference(ASTNode orig,JcompSymbol js,ASTRewrite rw) {
      if (js == null) return;
      SumpAttribute att = findAttribute(js.getFullName(),target_model); 
      if (att == null) return;
      String ntyp = map_attrs.get(att);
      if (ntyp == null) return;
      JcompTyper typer = JcompAst.getTyper(orig);
      JcompType njtype = typer.findSystemType(ntyp);
      JcompType ojtype = JcompAst.getExprType(orig);
      Expression oex = (Expression) copyAst(orig);
      Expression nex = createCastExpr(rw.getAST(),njtype,oex,ojtype);
      rw.replace(orig,nex,null);
    }
   
}       // end of inner class FieldMapper



}       // end of class EtchTransformFieldUseFix




/* end of EtchTransformFixFields.java */

