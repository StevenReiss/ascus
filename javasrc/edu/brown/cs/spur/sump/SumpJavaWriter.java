/********************************************************************************/
/*                                                                              */
/*              SumpJavaWriter.java                                             */
/*                                                                              */
/*      Handle outputing a model as a Java interface                            */
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



package edu.brown.cs.spur.sump;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseSearchEngine;
import edu.brown.cs.cose.cosecommon.CoseRequest.CoseKeywordSet;
import edu.brown.cs.spur.lids.LidsConstants.LidsLibrary;

class SumpJavaWriter implements SumpConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private PrintWriter     print_writer;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SumpJavaWriter(Writer w) 
{
   print_writer = null;
   if (w instanceof PrintWriter) print_writer = (PrintWriter) w;
   else print_writer = new PrintWriter(w);
}


/********************************************************************************/
/*                                                                              */
/*      Visitor method                                                          */
/*                                                                              */
/********************************************************************************/

void generateCode(SumpModel mdl)
{
   OutputJava oj = new OutputJava();
   mdl.accept(oj);
}




/********************************************************************************/
/*                                                                              */
/*      Visitor to handle java output                                           */
/*                                                                              */
/********************************************************************************/

private class OutputJava extends SumpVisitor {

   private Stack<String> cur_class;
   private Stack<Boolean> is_interface;
   
   OutputJava() {
      cur_class = new Stack<>();
      is_interface = new Stack<>();
    }
   
   @Override public boolean visit(SumpModel mdl) {
      String nm = mdl.getModelData().getName();
      
      for (String src : mdl.getModelData().getSources()) {
         print_writer.println("@Ascus(source=\"" + src + "\")");
       }
      if (mdl.getModelData().getContextPath() != null) {
         print_writer.println("@Ascus(context=\"" + mdl.getModelData().getContextPath() + "\")");
       }
      for (LidsLibrary lib : mdl.getModelData().getLibraries()) {
         print_writer.println("@Ascus(library=\"" + lib.getFullId() + "\")");
       }   
      for (String imp : mdl.getModelData().getMissingImports()) {
         print_writer.println("@Ascus(missing=\"" + imp + "\")");
       }  
      CoseRequest cr = mdl.getModelData().getCoseRequest();
      if (cr != null) {
         StringBuffer buf = new StringBuffer();
         buf.append(cr.getCoseSearchType());
         buf.append(",");
         buf.append(cr.getCoseScopeType());
         buf.append(",");
         buf.append(cr.getNumberOfResults());
         for (CoseSearchEngine se : cr.getEngines()) {
            buf.append(",");
            buf.append(se);
          }
         print_writer.println("@Ascus(search=\"" + buf.toString() + "\")");
         for (CoseKeywordSet cks : cr.getCoseKeywordSets()) {
            print_writer.print("@Ascus(keywords={");
            int ct = 0;
            for (String s : cks.getWords()) {
               if (ct++ > 0) print_writer.print(",");
               print_writer.print("\"" + s + "\"");
             }
            print_writer.println("})");
          }
         if (cr.getKeyTerms().size() > 0) {
            print_writer.print("@Ascus(keyterms={");
            int ct = 0;
            for (String s : cr.getKeyTerms()) {
               if (ct++ > 0) print_writer.print(",");
               print_writer.print("\"" + s + "\"");
             }
            print_writer.println("})");
          }
         Map<String,Object> pmap = mdl.getModelData().getParameters().getNonDefaults();
         for (Map.Entry<String,Object> ent : pmap.entrySet()) {
            print_writer.println("@Ascus(parameter=\"" + ent.getKey() + "=" + ent.getValue() + "\";");
          }
         List<String> sug = mdl.getModelData().getSuggestedWords();
         if (sug.size() > 0) {
            print_writer.print("@Ascus(suggestedTerms={");
            int ct = 0;
            for (String s : sug) {
               if (ct++ > 0) print_writer.print(",");
               print_writer.print("\"" + s + "\"");
             }
            print_writer.println("})");
          }
       }
      if (mdl.getModelData().getModelScore() > 0) {
         print_writer.println("@Ascus(score=" + mdl.getModelData().getModelScore() + ")");
       }
      
      print_writer.println("package edu.brown.cs.SAMPLE;");
      
      print_writer.println();
      print_writer.println("import edu.brown.cs.sump.annot.Ascus;");
      print_writer.println("import edu.brown.cs.sump.annot.AscusPackage;");
      print_writer.println("import edu.brown.cs.sump.annot.AscusClass;");
      for (String imp : mdl.getModelData().getImports()) {
         print_writer.println("import " + imp + ";");
       }
      print_writer.println();
      
      print_writer.println("@AscusPackage");
      print_writer.println("public interface " + nm + " {");
      
      print_writer.println();
      
      return true;
    }
   @Override public void endVisit(SumpModel m) {
      print_writer.println();
      print_writer.println("}");
    }
   
   @Override public boolean visit(SumpPackage p) {
      outputComment(p);
      return true;
    }
   
   @Override public boolean visit(SumpClass c) {
      print_writer.println();
      cur_class.push(c.getName());
      is_interface.push(c.isInterface());
      outputComment(c);
      Collection<SumpClass> uses = getModel().findUsedClasses(c);
      if (uses == null || uses.size() == 0) print_writer.println("@AscusClass ");
      else {
         print_writer.print("@AscusClass(uses={");
         int ct = 0;
         for (SumpClass cls : uses) {
            if (ct++ > 0) print_writer.print(",");
            print_writer.print(cls.getJavaOutputName());
            print_writer.print(".class");
          }
         print_writer.println("})");
       }
      String pfx = null;
      if (c.isInterface()) pfx = "interface";
      else if (c.getEnumConstants() != null) pfx = "enum";
      else if (c.getOperations().size() == 0) pfx = "class";
      else pfx = "abstract class";
      
      print_writer.print(pfx + " " + c.getJavaOutputName());
      if (c.getSuperClassName() != null) {
         String nm = getOutputName(c.getSuperClassName());
         print_writer.print(" extends " + nm);
       }   
      if (c.getInterfaceNames() != null && !c.getInterfaceNames().isEmpty()) {
         if (c.isInterface()) print_writer.print(" extends ");
         else print_writer.print(" implements ");
         int ct = 0;
         for (String s : c.getInterfaceNames()) {
            if (ct++ > 0) print_writer.print(", ");
            String nm = getOutputName(s);
            print_writer.print(nm);
          }
       }
      print_writer.println(" {");
      if (c.getEnumConstants() != null) {
         int ct = 0;
         for (String s : c.getEnumConstants()) {
            if (ct > 0) {
               print_writer.print(", ");
               if (ct%5 == 0) print_writer.println();
             }
            ++ct;
            print_writer.print(s);
          }
         if (!c.getAttributes().isEmpty() || !c.getOperations().isEmpty()) {
            print_writer.println(";");
          }
         else print_writer.println();
       }
      return true;
    }
   @Override public void endVisit(SumpClass c) {
      print_writer.println("}");
      cur_class.pop();
      is_interface.pop();
    }
   
   @Override public boolean visit(SumpAttribute a) {
      outputComment(a);
      
      String acc = "public";
      if (a.getAccess() != null) acc = a.getAccess().toString().toLowerCase();
      print_writer.print("   " + acc + " ");
      
      if (a.getDataType() == null) print_writer.print("Object");
      
      return true;
    }
   @Override public void endVisit(SumpAttribute a) {
      print_writer.println(" " + a.getName() + ";");
    }
   
   @Override public boolean visit(SumpOperation op) {
      outputComment(op);
      
      print_writer.print("   ");
      if (!is_interface.peek()) {
         String acc = "public";
         if (op.getAccess() != null) acc = op.getAccess().toString().toLowerCase();
         print_writer.print(acc + " ");
       }
      
      boolean isconst = false;
      String nm = op.getName();
      if (nm.equals("<init>")) {
         nm = cur_class.peek();
         isconst = true;
       }   
      else {
         if (!is_interface.peek()) print_writer.print("abstract ");
         if (op.getReturnType() != null) {
            op.getReturnType().accept(this);
          }
         else print_writer.print("void");
         print_writer.print(" ");
       }
      
      print_writer.print(nm + "(");
      
      if (op.getParameters() != null) {
         int ct = 0;
         for (SumpParameter sp : op.getParameters()) {
            if (ct++ > 0) print_writer.print(",");
            sp.accept(this);
          }
       }
      print_writer.print(")");
      
      if (isconst) print_writer.println(" { }");
      else print_writer.println(";");
      return false;
    }
   
   @Override public boolean visit(SumpParameter p) {
      outputComment(p);
      if (p.getDataType() != null) {
         p.getDataType().accept(this);
       }      
      else print_writer.print("Object");
      print_writer.print(" " + p.getName());
      return false;
    }
   
   @Override public boolean visit(SumpDataType dt) {
      print_writer.print(getJavaTypeName(dt));
      return false;
    }
   
   
   private void outputComment(SumpElement e) {
      String c = e.getComment();
      if (c != null) {
         print_writer.print(c);
         if (!c.endsWith("\n")) print_writer.println();
       }
    }
  
   
   
   
   
}       // end of inner class OuptutJava



}       // end of class SumpJavaWriter




/* end of SumpJavaWriter.java */

