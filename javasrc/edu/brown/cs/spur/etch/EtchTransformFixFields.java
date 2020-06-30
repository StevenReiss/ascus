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

import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.spur.sump.SumpConstants.SumpAttribute;
import edu.brown.cs.spur.sump.SumpConstants.SumpClass;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;


class EtchTransformFixFields extends EtchTransform
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

EtchTransformFixFields(Map<String,String> namemap)
{
   super("FixFields");
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

   FieldMapper() {
      super(EtchTransformFixFields.this);
    }
   
   void addMapping(SumpAttribute att,String typ) {
      
    }
   
   boolean isEmpty()                            { return true; }
   
   @Override public void rewriteTree(ASTNode orig,ASTRewrite rw) {
      
    }
   
}       // end of inner class FieldMapper



}       // end of class EtchTransformFixFields




/* end of EtchTransformFixFields.java */

