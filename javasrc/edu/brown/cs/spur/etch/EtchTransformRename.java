/********************************************************************************/
/*                                                                              */
/*              EtchTransformRename.java                                        */
/*                                                                              */
/*      Rename based on matching map transformation                             */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2013 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.spur.etch;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;


class EtchTransformRename extends EtchTransform
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

EtchTransformRename(Map<String,String> namemap)
{
   super("Rename");
   name_map = new HashMap<>(namemap);
   for (Iterator<String> it = name_map.keySet().iterator(); it.hasNext(); ) {
      String k = it.next();
      String v = name_map.get(k);
      if (k.equals(v)) it.remove();
    }
}



/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override protected EtchMemo applyTransform(ASTNode n,SumpModel target)
{
   NameMapper mapper =  findMappings(n,target);
   if (mapper == null) return null;
   
   EtchMemo memo =  mapper.getMapMemo(n);
   
   return memo;
}


/********************************************************************************/
/*                                                                              */
/*      Get mappings based on symbols                                           */
/*                                                                              */
/********************************************************************************/

private NameMapper findMappings(ASTNode cu,SumpModel target)
{
   NameMapper mapper = new NameMapper(target);
   FindNameVisitor fnv = new FindNameVisitor(mapper);
   cu.accept(fnv);
   if (mapper.isEmpty()) return null;
   return mapper;
}



private class FindNameVisitor extends ASTVisitor {

   private NameMapper name_handler;
   
   FindNameVisitor(NameMapper nm) {
      name_handler = nm;
    }

   @Override public void postVisit(ASTNode n) {
      JcompSymbol js = JcompAst.getDefinition(n);
      if (js != null) {
         String jnm = js.getFullName();
         String rnm = name_map.get(jnm);
         if (rnm != null) {
            String jsnm = jnm;
            String fpkg = name_handler.getFromPrefix();
            if (jnm.startsWith(fpkg)) {
               jsnm = jsnm.substring(fpkg.length()+1);
             }
            String rsnm = rnm;
            String tpkg = name_handler.getToPrefix();
            if (rnm.startsWith(tpkg)) {
               rsnm = rsnm.substring(tpkg.length()+1);
             }
            if (!rsnm.equals(jsnm)) {   
               name_handler.addMapping(js,rnm);
             }  
          }
       }
    }
   
   @Override public boolean visit(PackageDeclaration pd) {
      String pid = pd.getName().getFullyQualifiedName();
      String rnm = name_map.get(pid);
      if (rnm != null) name_handler.setPrefixMap(pid,rnm);
      return false;
    }
}


/********************************************************************************/
/*                                                                              */
/*      Actual mapper                                                           */
/*                                                                              */
/********************************************************************************/

private class NameMapper extends EtchMapper {
 
   private SumpModel target_model;
   private Map<JcompSymbol,String> sym_mapping;
   private String from_prefix;
   private String to_prefix;
   
   NameMapper(SumpModel tgt) {
      super(EtchTransformRename.this);
      target_model = tgt;
      sym_mapping = new HashMap<>();
      from_prefix = null;
      to_prefix = null;
    }
   
   void addMapping(JcompSymbol js,String to) {
      sym_mapping.put(js,to);
    }
   
   void setPrefixMap(String frm,String to) {
      from_prefix = frm;
      to_prefix = to;
    }
   
   boolean isEmpty()                    { return sym_mapping.isEmpty(); }
   
   String getFromPrefix()              { return from_prefix; }
   String getToPrefix()                { return to_prefix; }
   
   @Override void rewriteTree(ASTNode orig,ASTRewrite rw) {
      JcompSymbol js = JcompAst.getDefinition(orig);
      if (js != null) {
         String newname = sym_mapping.get(js);
         if (newname == null && js.getName().equals("<init>")) {
            for (ASTNode p = orig; p != null; p = p.getParent()) {
               if (p instanceof TypeDeclaration) {
                  JcompSymbol tjs = JcompAst.getDefinition(p);
                  newname = sym_mapping.get(tjs);
                  break;
                }
             }
          }
         if (newname != null) {
            rewriteName(orig,rw,newname);
          }
       }
      js = JcompAst.getReference(orig);
      if (js != null) {
         String newname = sym_mapping.get(js);
         if (newname != null) {
            rewriteName(orig,rw,newname);
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
   }
   
   private void rewriteName(ASTNode nd,ASTRewrite rw,String name) {
      if (nd instanceof SimpleName) {
         String nm0 = name;
         int idx1 = nm0.lastIndexOf(".");
         if (idx1 > 0) nm0 = nm0.substring(idx1+1);
         try {
            rw.set(nd,SimpleName.IDENTIFIER_PROPERTY,nm0,null);
          }
         catch (IllegalArgumentException e) {
            IvyLog.logE("JAVA","Problem with new transform name " + name + ": " + e);
          }
       }
    }
   
}       // end of inner class NameMapper



}       // end of class EtchTransformRename




/* end of EtchTransformRename.java */

