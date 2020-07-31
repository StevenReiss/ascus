/********************************************************************************/
/*                                                                              */
/*              EtchTransformAddMissing.java                                    */
/*                                                                              */
/*      Transforms to add missing classes and methods                           */
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.spur.sump.SumpConstants.SumpClass;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;
import edu.brown.cs.spur.sump.SumpConstants.SumpOperation;

class EtchTransformAddMissing extends EtchTransform
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

EtchTransformAddMissing(Map<String,String> namemap)
{
   super("AddMissing");
   name_map = namemap;
}



/********************************************************************************/
/*                                                                              */
/*      Apply the transform                                                     */
/*                                                                              */
/********************************************************************************/

@Override protected EtchMemo applyTransform(ASTNode n,SumpModel src,SumpModel target)
{
   AddMissingMapper amm = new AddMissingMapper();
   
   findMissingItems(amm,target);
   
   if (amm.isEmpty()) return null;
   
   EtchMemo memo = amm.getMapMemo(n);
   
   return memo;
}



void updateMappings(CoseResult cr,SumpModel src,SumpModel target)
{
   AddMissingMapper amm = new AddMissingMapper();
   
   findMissingItems(amm,target);
   
   if (amm.isEmpty()) return;
   
   List<SumpClass> toadd = amm.getAddedClasses();
   
   Set<String> known = new HashSet<>();
   CompilationUnit cu = (CompilationUnit) cr.getStructure();
   for (Object o : cu.types()) {
      AbstractTypeDeclaration atd = (AbstractTypeDeclaration) o;
      String nnm = atd.getName().getIdentifier();
      known.add(nnm);
    }
   
   for (SumpClass sc : toadd) {
      if (known.contains(sc.getName())) {
         System.err.println("CHECK HERE");
         // add mapping of atd with that name to new name
       }
    }
   
   
}


/********************************************************************************/
/*                                                                              */
/*      Determine what needs to be added                                        */
/*                                                                              */
/********************************************************************************/

private void findMissingItems(AddMissingMapper amm,SumpModel target)
{
   Map<String,String> revname = new HashMap<>();
   for (Map.Entry<String,String> ent : name_map.entrySet()) {
      revname.put(ent.getValue(),ent.getKey());
    }
   
   for (SumpClass sc : target.getPackage().getClasses()) {
      String orig = revname.get(sc.getFullName());
      if (orig != null) {
         for (SumpOperation op : sc.getOperations()) {
            if (!revname.containsKey(op.getFullName())) {
               amm.addMethod(orig,op);
             }
          }
       }
      else amm.addClass(sc);
    }
}


/*************************************************************  *******************/
/*                                                                              */
/*      Mapper to add missing items                                             */
/*                                                                              */
/********************************************************************************/

private class AddMissingMapper extends EtchMapper {

   private List<SumpClass> add_classes;
   private Map<String,List<SumpOperation>> add_methods;
   
   AddMissingMapper() {
      super(EtchTransformAddMissing.this);
      add_classes = new ArrayList<>();
      add_methods = new HashMap<>();
    }
   
   void addClass(SumpClass sc) {
      add_classes.add(sc);
    }
   
   List<SumpClass> getAddedClasses()                    { return add_classes; }
   
   void addMethod(String cls,SumpOperation op) {
      List<SumpOperation> newops = add_methods.get(cls);
      if (newops == null) {
         newops = new ArrayList<>();
         add_methods.put(cls,newops);
       }
      newops.add(op);
    }
   
   boolean isEmpty() {
      return add_classes.isEmpty() && add_methods.isEmpty();
    }
   
   @Override void rewriteTree(ASTNode orig,ASTRewrite rw) {
      if (orig instanceof TypeDeclaration) {
         TypeDeclaration td = (TypeDeclaration) orig;
         JcompSymbol js = JcompAst.getDefinition(td);
         List<SumpOperation> ops = add_methods.get(js.getFullName());
         if (ops != null) {
            ListRewrite lrw = rw.getListRewrite(td,TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
            for (SumpOperation op : ops) {
               MethodDeclaration md = createDummyMethod(rw.getAST(),op);
               if (md != null) {
                  lrw.insertLast(md,null);
                }
             }
          }
       }
      else if (orig instanceof CompilationUnit && !add_classes.isEmpty()) {
         CompilationUnit cu = (CompilationUnit) orig;
         ListRewrite lrw = rw.getListRewrite(cu,CompilationUnit.TYPES_PROPERTY);
         for (SumpClass sc : add_classes) {
            TypeDeclaration td = createDummyClass(rw.getAST(),sc,orig);
            lrw.insertLast(td,null);
          }
       }
    }
}

}       // end of class EtchTransformAddMissing




/* end of EtchTransformAddMissing.java */

