/********************************************************************************/
/*                                                                              */
/*              EtchTransformFixPackage.java                                    */
/*                                                                              */
/*      Handle qualified package names that should be removed                   */
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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;


class EtchTransformFixPackage extends EtchTransform
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Set<String>             package_names;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

EtchTransformFixPackage(CoseResult cr,Map<String,String> namemap)
{
   super("FixPackage");
   package_names = new HashSet<>(cr.getPackages());
}



/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override protected EtchMemo applyTransform(ASTNode n,SumpModel target)
{
   PackageMapper mapper = findMappings(n,target);
   if (mapper == null) return null;
   
   EtchMemo memo = mapper.getMapMemo(n);
   
   return memo;
}




/********************************************************************************/
/*                                                                              */
/*      Find possible mappings                                                  */
/*                                                                              */
/********************************************************************************/

private PackageMapper findMappings(ASTNode cu,SumpModel target)
{
   PackageMapper mapper = new PackageMapper(target);
   FindPackageVisitor fpv = new FindPackageVisitor(mapper);
   cu.accept(fpv);
   if (mapper.isEmpty()) return null;
   return mapper;
}



private class FindPackageVisitor extends ASTVisitor {

   private PackageMapper name_handler;
   
   FindPackageVisitor(PackageMapper pm) {
      name_handler = pm;
    }
   
   @Override public boolean visit(QualifiedName qn) {
      String s = qn.getQualifier().getFullyQualifiedName();
      if (package_names.contains(s)) {
         name_handler.addNode(qn);
         return false;
       }
      return true;
    }
   
   @Override public boolean visit(PackageDeclaration n) {
      return false;
   }
   
   @Override public boolean visit(ImportDeclaration n) {
      return false;
    }
   
}       // end of inner class FindPackageVisitor




/********************************************************************************/
/*                                                                              */
/*      Actual mapper                                                           */
/*                                                                              */
/********************************************************************************/

private class PackageMapper extends EtchMapper {

   private Set<ASTNode> fix_names;
   
   PackageMapper(SumpModel target) {
      super(EtchTransformFixPackage.this);
      fix_names = new HashSet<>();
    }
   
   boolean isEmpty()            { return fix_names.isEmpty(); }
   
   void addNode(ASTNode n)      { fix_names.add(n); }
   
   @Override void rewriteTree(ASTNode orig,ASTRewrite rw) {
      if (fix_names.contains(orig)) {
         QualifiedName qn = (QualifiedName) orig;
         SimpleName sn = JcompAst.getSimpleName(rw.getAST(),qn.getName().getIdentifier());
         rw.replace(orig,sn,null);
       }
    }
   
}       // end of inner class PackageMapper


}       // end of class EtchTransformFixPackage




/* end of EtchTransformFixPackage.java */

