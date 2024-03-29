/********************************************************************************/
/*                                                                              */
/*              EtchTransformPostRename.java                                    */
/*                                                                              */
/*      Handle renaming of code after main renaming is done (e.g. test cases)   */
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
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.spur.sump.SumpConstants.SumpAttribute;
import edu.brown.cs.spur.sump.SumpConstants.SumpClass;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;
import edu.brown.cs.spur.sump.SumpConstants.SumpOperation;

class EtchTransformPostRename extends EtchTransform
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,String>      name_map;
private CoseResult              base_result;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

EtchTransformPostRename(Map<String,String> namemap,CoseResult base)
{
   super("PostRename");
   name_map = new HashMap<>(namemap);
   base_result = base;
   for (Iterator<String> it = name_map.keySet().iterator(); it.hasNext(); ) {
      String k = it.next();
      String v = name_map.get(k);
      int idx = k.indexOf("(");
      if (k.equals(v)) it.remove();
      else if (idx > 0 && k.substring(0,idx).equals(v)) it.remove(); 
    } 
}



/********************************************************************************/
/*                                                                              */
/*      Transform methods                                                       */
/*                                                                              */
/********************************************************************************/

@Override protected EtchMemo applyTransform(ASTNode n,SumpModel frmmdl,SumpModel tomdl)
{
   PostNameMapper mapper = findMappings(n,frmmdl,tomdl);
   if (mapper == null) return null;
   
   EtchMemo memo =  mapper.getMapMemo(n);
   
   return memo;
}


/********************************************************************************/
/*                                                                              */
/*      Find mappings based on names                                            */
/*                                                                              */
/********************************************************************************/

private PostNameMapper findMappings(ASTNode cu,SumpModel frmmdl,SumpModel tomdl)
{
   PostNameMapper mapper = new PostNameMapper(frmmdl,tomdl);
   FindChangeVisitor fcv = new FindChangeVisitor(mapper,frmmdl,tomdl);
   cu.accept(fcv);
   
   return mapper;
}



/********************************************************************************/
/*                                                                              */
/*      Find all things to change                                               */
/*                                                                              */
/********************************************************************************/

private class FindChangeVisitor extends ASTVisitor {

  private PostNameMapper name_handler;
  private SumpModel from_model;
  private SumpModel to_model;
  
  FindChangeVisitor(PostNameMapper mapper,SumpModel frmmdl,SumpModel tomdl) {
     name_handler = mapper;
     from_model = frmmdl;
     to_model = tomdl;
   }
  
  @Override public boolean visit(PackageDeclaration pd) {
     String pid = pd.getName().getFullyQualifiedName();
     String npid = to_model.getPackage().getFullName();
     if (!pid.equals(npid)) {
        name_handler.addMapping(pd.getName(),npid);
      }
     return false;
   }
  
  @Override public boolean visit(SimpleName n) {
     ASTNode par = n.getParent();
     switch (par.getNodeType()) {
        case ASTNode.METHOD_INVOCATION :
           JcompSymbol js = JcompAst.getReference(par);
           JcompType jt = JcompAst.getExprType(par);
           if (js == null || jt.isErrorType()) {
              SumpOperation useop = findOperation(par,from_model);
              if (useop != null) {
                 String fnm = useop.getFullName();
                 String mnm = name_map.get(fnm);
                 if (mnm != null) {
                    name_handler.addMapping(n,mnm);
                  }
               }
            }
           break;
        case ASTNode.SIMPLE_TYPE :
           jt = JcompAst.getJavaType(n);
           if (jt == null) jt = JcompAst.getJavaType(par);
           if (jt != null) {
              SumpClass sc = findClass(jt.getName(),from_model);
              if (sc != null) {
                 String nnm = name_map.get(sc.getFullName());
                 if (nnm != null) name_handler.addMapping(par,nnm);
               }
            }
           break;
        case ASTNode.FIELD_ACCESS :
           SumpAttribute sat = findAttribute(par,from_model);
           if (sat != null) {
              String mnm = name_map.get(sat.getFullName());
              if (mnm != null) name_handler.addMapping(n,mnm);
            }
           break;
      }
     
     return false;
   }
  
  @Override public boolean visit(QualifiedName n) {
     ASTNode par = n.getParent();
     switch (par.getNodeType()) {
        case ASTNode.SIMPLE_TYPE :
           JcompType jt = JcompAst.getJavaType(n);
           if (jt == null) jt = JcompAst.getJavaType(par);
           if (jt != null) {
              SumpClass sc = findClass(jt.getName(),from_model);
              if (sc != null) {
                 String nnm = name_map.get(sc.getFullName());
                 if (nnm != null) name_handler.addMapping(par,nnm);
               }
            }
           // handle type names
           break;
      }
     
     return true;
   }
  
  
}       // end of inner class FindChangeVisitor



/********************************************************************************/
/*                                                                              */
/*      Actual mapper                                                           */
/*                                                                              */
/********************************************************************************/

private class PostNameMapper extends EtchMapper {

   private SumpModel target_model;
   private Map<ASTNode,String> name_maps;
   
   PostNameMapper(SumpModel frmmdl,SumpModel tomdl) {
      super(EtchTransformPostRename.this);
      name_maps = new HashMap<>();
      target_model = tomdl;
    }
   
   void addMapping(ASTNode n,String nm) {
      name_maps.put(n,nm);
    }
   
   @Override void rewriteTree(ASTNode orig,ASTRewrite rw) {
      String rslt = name_maps.get(orig);
      if (rslt != null) {
         if (orig instanceof SimpleType) {
            rewriteType(orig,rw,rslt);
          }
         else {
            rewriteName(orig,rw,rslt);
          }
       }
      
      if (orig instanceof PackageDeclaration) {
         PackageDeclaration pd = (PackageDeclaration) orig;
         String rnm = target_model.getPackage().getFullName();
         if (rnm != null) {
            Name n = JcompAst.getQualifiedName(rw.getAST(),rnm);
            rw.set(pd,PackageDeclaration.NAME_PROPERTY,n,null);
          }
       }
      else if (orig instanceof ImportDeclaration) {
         ImportDeclaration id = (ImportDeclaration) orig;
         if (checkBaseImport(id,rw)) return;
         else {
            handleImportName(id.getName(),rw);
          }
       }
    }
   
   private boolean handleImportName(Name n,ASTRewrite rw) {
      CompilationUnit cu = (CompilationUnit) n.getRoot();
      PackageDeclaration pd = cu.getPackage();
      String frompfx = pd.getName().getFullyQualifiedName();
      return fixImportName(n,frompfx,rw);
    }
   
   private boolean fixImportName(Name n,String pfx,ASTRewrite rw) {
      String nm = n.getFullyQualifiedName();
      if (nm.equals(pfx)) {
         String rnm = target_model.getPackage().getFullName();
         Name n1 = JcompAst.getQualifiedName(rw.getAST(),rnm);
         rw.replace(n,n1,null);
         return true;
       }
      else if (n instanceof QualifiedName) {
         QualifiedName qn = (QualifiedName) n;
         return fixImportName(qn.getQualifier(),pfx,rw);
       }
      return false;
    }
   
   private boolean checkBaseImport(ImportDeclaration id,ASTRewrite rw)
   {
      if (base_result == null) return false;
      
      String nm = id.getName().getFullyQualifiedName();
      
      String which = null;
      for (String s : base_result.getPackages()) {
         if (nm.startsWith(s)) {
            if (which == null || which.length() < s.length()) which = s;
          }
       }
      if (which == null) return false;
      
      if (!id.isStatic()) {
         String tnm = nm;
         if (!id.isOnDemand()) {
            int idx = tnm.lastIndexOf(".");
            tnm = nm.substring(0,idx);
          }
         if (which.equals(tnm)) {
            rw.remove(id,null);
            return true;
          }
         else {
            return fixImportName(id.getName(),which,rw);
          }
       }
      else {
         return fixImportName(id.getName(),which,rw);
       }
   }
   
   
}       // end of inner class PostNameMapper



}       // end of class EtchTransformPostRename




/* end of EtchTransformPostRename.java */

