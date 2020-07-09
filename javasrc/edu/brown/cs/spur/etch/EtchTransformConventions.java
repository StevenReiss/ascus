/********************************************************************************/
/*                                                                              */
/*              EtchTransformConventions.java                                   */
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.spur.sump.SumpParameters;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;
import edu.brown.cs.spur.swift.SwiftIdfBuilder;
import edu.brown.cs.spur.sump.SumpConstants.NameType;


class EtchTransformConventions extends EtchTransform 
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,String>      name_map;
private Map<NameType,String>    naming_conventions;
private String                  cur_package;
private String                  cur_type;

private String [] prefix_names = {
      "my", "the", "f", "get", "set", "is"
};

private static Set<String> word_set;

static {
   word_set = Collections.synchronizedSet(new HashSet<String>());
   word_set.addAll(SwiftIdfBuilder.getEnglishWords());
   word_set.add("boolean");
   word_set.add("int");
}






/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

EtchTransformConventions(Map<String,String> namemap)
{
   super("Conventions");
   name_map = namemap;
   naming_conventions = new HashMap<>();
   cur_package = null;
   cur_type = null;
}



/********************************************************************************/
/*                                                                              */
/*      Transformation methods                                                  */
/*                                                                              */
/********************************************************************************/

@Override protected EtchMemo applyTransform(ASTNode n,SumpModel src,SumpModel tgt)
{
   loadConventions(tgt);
   if (naming_conventions.isEmpty()) return null;
   
   NameMapper mapper = findMappings(n);
   if (mapper == null) return null;
   
   EtchMemo memo = mapper.getMapMemo(n);
   
   return memo;
}


/********************************************************************************/
/*                                                                              */
/*      Find needed name mappings                                               */
/*                                                                              */
/********************************************************************************/

private NameMapper findMappings(ASTNode n)
{
   NameMapper nm = new NameMapper();
   
   NameVisitor nv = new NameVisitor(nm);
   n.accept(nv);
   
   if (nm.isEmpty()) return null;
   
   return nm;
}



/********************************************************************************/
/*                                                                              */
/*      Get the developers conventions                                          */
/*                                                                              */
/********************************************************************************/

private void loadConventions(SumpModel mdl)
{
   SumpParameters sp = mdl.getModelData().getParameters();
   for (NameType nt : NameType.values()) {
      String s = sp.getNamingConventions(nt);
      if (s != null) naming_conventions.put(nt,s);
    }
}



String convertName(String nm,String st)
{
   if (st == null) return nm;
   
   List<String> wds = splitName(nm);
   StringTokenizer stk = new StringTokenizer(st,"LUMFSIECP",true);
   boolean alllower = false;
   boolean allupper = false;
   boolean firstlower = false;
   boolean sepus = false;
   int initct = -1;
   int endct = -1;
   int frstct = -1;
   int pfx = -1;
   while (stk.hasMoreTokens()) {
      String k = stk.nextToken();
      switch (k.charAt(0)) {
         case 'L' :
            alllower = true;
            break;
         case 'U' :
            allupper = true;
            break;
         case 'M' :
            break;
         case 'F' :
            firstlower = true;
            break;
         case 'S' :
            sepus = true;
            break;
         case 'I' :
            initct = Integer.parseInt(stk.nextToken());
            break;
         case 'E' :
            endct = Integer.parseInt(stk.nextToken());
            break;
         case 'C' :
            frstct = Integer.parseInt(stk.nextToken());
            break;
         case 'P' :
            pfx = Integer.parseInt(stk.nextToken());
            break;
       }
    }
   
   StringBuffer buf = new StringBuffer();
   for (int i = 0; i < frstct; ++i) buf.append("_");
   if (wds.size() == 1 && pfx >= 0) {
      switch (pfx) {
         case 0 :
            if (cur_package != null) wds.add(0,cur_package);
            break;
         case 1 :
            if (cur_type != null) wds.add(0,cur_type);
            break;
         default :
            wds.add(0,prefix_names[pfx-2]);
            break;
       }
    }
   
   for (int i = 0; i < wds.size(); ++i) {
      String wd = wds.get(i);
      if (allupper) wd = wd.toUpperCase();
      else if (!alllower) {
         if (i != 0 || !firstlower) wd = startUpper(wd);
       }
      if (i != 0) {
         if (i == 1) {
            for (int j = 0; j < initct; ++j) buf.append("_");
          }
         else if (sepus) buf.append("_");
       }
      buf.append(wd);
    }
   for (int i = 0; i < endct; ++i) buf.append("_");
   
   return buf.toString();
}



private String startUpper(String wd)
{
   StringBuffer buf = new StringBuffer();
   buf.append(Character.toUpperCase(wd.charAt(0)));
   if (wd.length() > 1) buf.append(wd.substring(1));
   return buf.toString();
}


private List<String> splitName(String nm)
{
   int idx0 = 0;
   int ln = nm.length();
   boolean lastupper = true;
   List<String> wds = new ArrayList<>();
   
   for (int i = 0; i < ln; ++i) {
      char c = nm.charAt(i);
      boolean newword = false;
      if (c == '_') newword = true;
      else if (i == 0) newword = true;
      else {
         boolean ufg = Character.isUpperCase(c);
         if (ufg && !lastupper) newword = true;
         lastupper = ufg;
       }
      if (newword) {
         if (idx0 < i) {
            String wd = nm.substring(idx0,i).toLowerCase();
            wds.add(wd);
          }
         if (c == '_') idx0 = i+1;
         else idx0 = i;
         lastupper = true;
       }
    }
   if (idx0 > 0) {
      nm = nm.substring(idx0);
      idx0 = 0;
    }
   if (wds.isEmpty() && ln > 4) {
      idx0 = splitWords(nm,wds);
    }
   if (idx0 >=0 && idx0 < ln) {
      String wd = nm.substring(idx0).toLowerCase();
      wds.add(wd);
    }
   if (wds.size() == 0) {
      wds.add(nm);
    }
   
   for (String s : wds) {
      if (s.length() > 3) word_set.add(s);
    }
   
   return wds;
}



private int splitWords(String nm,List<String> pfx)
{
   if (word_set.contains(nm.toLowerCase())) return -1;
   
   int ln = nm.length();
   for (int i = 3; i < ln-3; ++i) {
      String w0 = nm.substring(0,i).toLowerCase();
      if (word_set.contains(w0)) {
         String w = nm.substring(i).toLowerCase();
         if (word_set.contains(w)) {
            pfx.add(w0);
            return i;
          }
         int wln = pfx.size();
         pfx.add(w0);
         int i0 = splitWords(w,pfx);
         if (i0 > 0) return i0 + w0.length();
         pfx.remove(wln);
       }
    }
   
   return -1;
}



/********************************************************************************/
/*                                                                              */
/*      Visitor to find names to map                                            */
/*                                                                              */
/********************************************************************************/

private class NameVisitor extends ASTVisitor {

   private NameMapper name_mapper;
   
   NameVisitor(NameMapper nm) {
      name_mapper = nm;
    }
   
   @Override public void postVisit(ASTNode n) {
      JcompSymbol js = JcompAst.getDefinition(n);
      if (js == null) return;
      NameType nt = null;
      switch (js.getSymbolKind()) {
         case ANNOTATION :
         case ANNOTATION_MEMBER :
         case NONE :
         case PACKAGE :
            break;
         case CLASS :
         case INTERFACE :
         case ENUM :
         case CONSTRUCTOR :
            nt = NameType.TYPE;
            break;
         case FIELD :
            nt = NameType.FIELD;
            if (js.isStatic() && js.isFinal()) nt = NameType.CONSTANT;
            break;
         case LOCAL :
            if (n.getParent() instanceof MethodDeclaration) nt = NameType.PARAMETER;
            else nt = NameType.LOCAL;
            break;
         case METHOD :
            nt = NameType.METHOD;
            break;
       }
      if (nt == null) return;
      String conv = naming_conventions.get(nt);
      if (conv == null) return;
      String nam = convertName(js.getName(),conv);
      if (nam == null || nam.equals(js.getName())) return;
      name_mapper.addMapping(js,nam);
      if (js.isTypeSymbol()) {
         JcompType jt = js.getDeclaredType();
         name_mapper.addMapping(jt,nam);
       }
      String s = js.getFullName();
      for (String k : name_map.keySet()) {
         String name = name_map.get(k);
          if (name.equals(s)) {
             int idx = s.lastIndexOf(".");
             String ns = s.substring(0,idx) + "." + nam;
             name_map.put(k,ns);
             break;
           }
       }
    }
   
}       // end of inner class NameVisitor




/********************************************************************************/
/*                                                                              */
/*      Actual mapping code                                                     */
/*                                                                              */
/********************************************************************************/

private class NameMapper extends EtchMapper {

   private Map<JcompSymbol,String> sym_mapping;
   private Map<JcompType,String> type_mapping;
   
   NameMapper() {
      super(EtchTransformConventions.this);
      sym_mapping = new HashMap<>();
      type_mapping = new HashMap<>();
    }
   
   void addMapping(JcompSymbol js,String to) {
      sym_mapping.put(js,to);
    }
   
   void addMapping(JcompType jt,String to) {
      type_mapping.put(jt,to);
    }
   
   boolean isEmpty() {
      return sym_mapping.isEmpty() && type_mapping.isEmpty();
    }
   
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



}       // end of class EtchTransformConventions




/* end of EtchTransformConventions.java */

