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
import edu.brown.cs.ivy.jcomp.JcompType;
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
      int idx = k.indexOf("(");
      if (k.equals(v)) it.remove();
      else if (idx > 0 && k.substring(0,idx).equals(v)) it.remove(); 
    }
}



/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override protected EtchMemo applyTransform(ASTNode n,SumpModel target)
{
   NameMapper mapper = findMappings(n,target);
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
   // if (mapper.isEmpty()) return null;        // package name needs to be considered
   return mapper;
}



private class FindNameVisitor extends ASTVisitor {

   private NameMapper name_handler;
   
   FindNameVisitor(NameMapper nm) {
      name_handler = nm;
    }

   @Override public void postVisit(ASTNode n) {
      JcompSymbol js = JcompAst.getDefinition(n);
      JcompType jt = JcompAst.getJavaType(n);
      if (js != null) {
         String jnm = getMapName(js);
         String rnm = name_map.get(jnm);
         if (rnm != null) {
            String jsnm = jnm;
            String fpkg = name_handler.getFromPrefix();
            if (fpkg != null && jnm.startsWith(fpkg)) {
               jsnm = jsnm.substring(fpkg.length()+1);
             }
            int idx = jsnm.indexOf("(");
            if (idx > 0) jsnm = jsnm.substring(0,idx);
            String rsnm = rnm;
            String tpkg = name_handler.getToPrefix();
            if (tpkg != null && rnm.startsWith(tpkg)) {
               rsnm = rsnm.substring(tpkg.length()+1);
             }
            if (!rsnm.equals(jsnm)) {   
               name_handler.addMapping(js,rnm);
             }  
          }
       }
      else if (jt != null) {
         String tnm = jt.getName();
         String rnm = name_map.get(tnm);
         if (rnm != null) {
            String pfx = name_handler.getToPrefix();
            if (rnm.startsWith(pfx + ".")) {
               int ln = pfx.length();
               int idx = pfx.lastIndexOf(".");
               String npfx = pfx.substring(0,idx);
               rnm = npfx + rnm.substring(ln);
             }
            name_handler.addMapping(jt,rnm);
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
   private Map<JcompType,String> type_mapping;
   private String from_prefix;
   private String to_prefix;
   
   NameMapper(SumpModel tgt) {
      super(EtchTransformRename.this);
      target_model = tgt;
      sym_mapping = new HashMap<>();
      type_mapping = new HashMap<>();
      from_prefix = null;
      to_prefix = null;
    }
   
   void addMapping(JcompSymbol js,String to) {
      sym_mapping.put(js,to);
    }
   
   void addMapping(JcompType jt,String to) {
      type_mapping.put(jt,to);
    }
   
   void setPrefixMap(String frm,String to) {
      from_prefix = frm;
      to_prefix = to;
    }
   
   String getFromPrefix()              { return from_prefix; }
   String getToPrefix()                { return to_prefix; }
   
   @Override void rewriteTree(ASTNode orig,ASTRewrite rw) {
      JcompSymbol jsd = JcompAst.getDefinition(orig);
      JcompSymbol jsr = JcompAst.getReference(orig);
      JcompType jt = JcompAst.getJavaType(orig);
      if (jsd != null || jsr != null) {
         handleName(orig,rw);
       }
      else if (jt != null) {
         handleType(orig,rw);
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
   
   private boolean handleName(ASTNode orig,ASTRewrite rw) {
      if (orig instanceof SimpleName) {
         JcompSymbol jsd = JcompAst.getDefinition(orig);
         JcompSymbol jsr = JcompAst.getReference(orig);
         if (jsd != null) {
            String newname = sym_mapping.get(jsd);
            if (jsd.isConstructorSymbol()) {
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
               return true;
             }
          }
         else if (jsr != null) {
            String newname = sym_mapping.get(jsr);
            if (newname != null) {
               rewriteName(orig,rw,newname);
               return true;
             }
          }
       }
      return false;
    }
   
   private boolean handleType(ASTNode orig,ASTRewrite rw) {
      JcompType jt = JcompAst.getJavaType(orig);
      String newname = type_mapping.get(jt);
      return rewriteType(orig,rw,newname);
    }
   
}       // end of inner class NameMapper



}       // end of class EtchTransformRename




/* end of EtchTransformRename.java */

