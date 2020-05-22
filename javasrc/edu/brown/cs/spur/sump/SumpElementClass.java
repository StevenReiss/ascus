/********************************************************************************/
/*                                                                              */
/*              SumpElementClass.java                                           */
/*                                                                              */
/*      UML class representation                                                */
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



package edu.brown.cs.spur.sump;

import java.awt.Rectangle;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import edu.brown.cs.ivy.file.IvyStringDiff;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.spur.rowel.RowelConstants.RowelMatch;
import edu.brown.cs.spur.rowel.RowelConstants.RowelMatchType;
import edu.brown.cs.spur.sump.SumpConstants.SumpClass;

class SumpElementClass extends SumpElementBase implements SumpClass
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<SumpElementAttribute> attribute_list;
private List<SumpElementOperation> operation_list;
private Set<String>         enum_constants;
private String              super_name;
private List<String>        iface_names;
private boolean             is_interface;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SumpElementClass(SumpModelBase mdl,AbstractTypeDeclaration atd)
{ 
   super(mdl);
   attribute_list = new ArrayList<>();
   operation_list = new ArrayList<>();
   super_name = null;
   iface_names = new ArrayList<>();
   is_interface = false;
   enum_constants = null;
   
   initialize(atd);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public ElementType getElementType()
{
   return ElementType.CLASS;
}



@Override public boolean isInterface()
{
   return is_interface;
}



@Override public SumpClass getSuperClass()
{
   return null;
}


@Override public Collection<SumpClass> getInterfaces()
{
   if (iface_names.isEmpty()) return null;
   return null;
}



@Override public Collection<SumpAttribute> getAttributes()
{
   return new ArrayList<>(attribute_list);
}



@Override public Collection<SumpOperation> getOperations()
{
   return new ArrayList<>(operation_list);
}


@Override public Collection<String> getEnumConstants()
{
   return enum_constants;
}


/********************************************************************************/
/*                                                                              */
/*      Matching methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public double getMatchScore(RowelMatch rm,RowelMatchType mt)
{
   if (rm instanceof SumpElementClass) {
      SumpElementClass sc = (SumpElementClass) rm;
      double score = SumpMatcher.computeClassMatchScore(this,sc,null);
      if (score < SumpMatcher.CLASS_CUTOFF) return 0;
      double nscore = IvyStringDiff.normalizedStringDiff(getName(),sc.getName());
      return 0.8*score + 0.2*nscore;
    }
   
   return 0;
}



/********************************************************************************/
/*                                                                              */
/*      Creation methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public void addAttribute(JcompSymbol js,ASTNode n)
{
   SumpElementAttribute att = new SumpElementAttribute(sump_model,js,n);
   attribute_list.add(att);
}


@Override public void addOperation(JcompSymbol js,ASTNode n)
{
   SumpElementOperation op = new SumpElementOperation(sump_model,js,n);
   operation_list.add(op);
}


@Override public void addEnumConstant(JcompSymbol js,ASTNode n)
{
   if (enum_constants == null) enum_constants = new LinkedHashSet<>();
   String s = js.getName();
   if (!enum_constants.contains(s)) enum_constants.add(s);
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

private void initialize(AbstractTypeDeclaration atd)
{
   String nm = atd.getName().getIdentifier();
   setName(nm);
   setFullName(JcompAst.getJavaType(atd).getName());
   
   if (atd instanceof TypeDeclaration) {
      TypeDeclaration td = (TypeDeclaration) atd;
      if (td.isInterface()) is_interface = true;
      Type t = td.getSuperclassType();
      if (t != null) {
         JcompType jt = JcompAst.getJavaType(t);
         if (!jt.isUndefined()) super_name = jt.getName();
       }
      for (Object o : td.superInterfaceTypes()) {
         Type t1 = (Type) o;
         JcompType jt = JcompAst.getJavaType(t1);
         if (!jt.isUndefined()) {
            String inm = jt.getName();
            iface_names.add(inm);
          }
       }
    }
   else if (atd instanceof EnumDeclaration) {
      EnumDeclaration ed = (EnumDeclaration) atd;
      enum_constants = new LinkedHashSet<>();
      for (Object o : ed.enumConstants()) {
         EnumConstantDeclaration ecd = (EnumConstantDeclaration) o;
         String s = ecd.getName().getIdentifier();
         enum_constants.add(s);
       }
    }
   
   addCommentsFor(atd);
}



/********************************************************************************/
/*                                                                              */
/*      Output Methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override void outputXml(IvyXmlWriter xw)
{
   String what = "CLASS";
   if (is_interface) what = "INTERFACE";
   else if (enum_constants != null) what = "ENUM";
   
   xw.begin(what);
   basicXml(xw);
   Rectangle bnds = sump_model.getBounds(this);
   if (bnds != null) {
      xw.begin("LOCATION");
      xw.field("X",bnds.x);
      xw.field("Y",bnds.y);
      xw.field("WIDTH",bnds.width);
      xw.field("HEIGHT",bnds.height);
      xw.end("LOCATION");
    }
   if (super_name != null) {
      xw.begin("INHERITS");
      xw.field("NAME",super_name);
      xw.end("INHERITS");
    }
   if (iface_names != null) {
      for (String s : iface_names) {
         xw.begin("INHERITS");
         xw.field("NAME",s);
         xw.end("INHERITS");
       }
    }
   for (SumpElementAttribute att : attribute_list) {
      att.outputXml(xw);
    }
   for (SumpElementOperation op : operation_list) {
      op.outputXml(xw);
    }
   if (enum_constants != null) {
      for (String s : enum_constants) {
         xw.textElement("VALUE",s);
       }
    }
   xw.end(what);
}



@Override void outputJava(PrintWriter pw)
{
   getData().pushType(getName(),is_interface);
   
   outputComment(pw);
   
   Set<SumpElementClass> uses = sump_model.findUsedClasses(this);
   if (uses == null || uses.size() == 0) pw.println("@AscusClass ");
   else {
      pw.print("@AscusClass(uses={");
      int ct = 0;
      for (SumpElementClass cls : uses) {
         if (ct++ > 0) pw.print(",");
         pw.print(cls.getName());
         pw.print(".class");
       }
      pw.println("})");
    }
   String pfx = null;
   if (is_interface) pfx = "interface";
   else if (enum_constants != null) pfx = "enum";
   else if (operation_list.size() == 0) pfx = "class";
   else pfx = "abstract class";

   pw.print(pfx + " " + getName());
   if (super_name != null) pw.print(" extends " + super_name);
   if (iface_names != null && !iface_names.isEmpty()) {
      if (is_interface) pw.print(" extends ");
      else pw.print(" implements ");
      int ct = 0;
      for (String s : iface_names) {
         if (ct++ > 0) pw.print(", ");
         pw.print(s);
       }
    }
   pw.println(" {");
   if (enum_constants != null) {
      int ct = 0;
      for (String s : enum_constants) {
         if (ct > 0) {
            pw.print(", ");
            if (ct%5 == 0) pw.println();
          }
         ++ct;
         pw.print(s);
       }
      if (!attribute_list.isEmpty() || !operation_list.isEmpty()) {
         pw.println(";");
       }
      else pw.println();
    }
   
   for (SumpElementAttribute att : attribute_list) {
      att.outputJava(pw);
    }
   for (SumpElementOperation op : operation_list) {
      op.outputJava(pw);
    }
   
   pw.println("}");
   
   getData().popType();
}


@Override void setupJava()
{
   for (SumpElementAttribute att : attribute_list) {
      att.setupJava();
    }
   
   for (SumpElementOperation op : operation_list) {
      op.setupJava();
    }
}


void generateUXF(IvyXmlWriter xw,SumpLayout layout)
{
   xw.begin("element");
   xw.textElement("id","UMLClass");
   xw.begin("coordinates");
   Rectangle r = layout.getBounds(this);
   xw.textElement("x",r.x);
   xw.textElement("y",r.y);
   xw.textElement("w",r.width);
   xw.textElement("h",r.height);
   xw.end("coordinates");
   StringBuffer buf = new StringBuffer();
   if (enum_constants != null) buf.append("<<enumeration>>");
   buf.append(getName() + "\n-\n");
   if (enum_constants != null) {
      for (String s : enum_constants) {
         buf.append(s + "\n");
       }
    }
   for (SumpElementAttribute at : attribute_list) {
      if (at.getAccess() != null) {
         switch (at.getAccess()) {
            case PRIVATE : 
               buf.append("#");
               break;
            case PUBLIC :
               buf.append("+");
               break;
            default  :
               break;
          }
       }
      buf.append(at.getName());
      buf.append(": ");
      buf.append(at.getDataType().getName());
      buf.append("\n");
    }
   if (!operation_list.isEmpty()) {
      buf.append("-\n");
      for (SumpElementOperation op : operation_list) {
         if (op.getAccess() != null) {
            switch (op.getAccess()) {
               case PRIVATE : 
                  buf.append("#");
                  break;
               case PUBLIC :
                  buf.append("+");
                  break;
               default :
                  break;
             }
          }
         buf.append(op.getName());
         buf.append("(");
         int ct = 0;
         for (SumpParameter ep : op.getParameters()) {
            if (ct++ > 0) buf.append(", ");
            buf.append(ep.getName());
            buf.append(": ");
            buf.append(ep.getDataType().getName());
          }
         buf.append(")");
         if (op.getReturnType() != null) {
            buf.append(": ");
            buf.append(op.getReturnType().getName());
          }
         buf.append("\n");
       }
    }
   xw.textElement("panel_attributes",buf.toString());
   xw.end("element");
}



}       // end of class SumpElementClass




/* end of SumpElementClass.java */

