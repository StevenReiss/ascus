/********************************************************************************/
/*                                                                              */
/*              SumpDataType.java                                               */
/*                                                                              */
/*      Representation of a data type                                           */
/*                                                                              */
/********************************************************************************/



package edu.brown.cs.spur.sump;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;

import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class SumpDataType implements SumpConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private JcompType       base_type;
private String          package_name;
private SumpArgType     arg_type;
private String          type_name;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SumpDataType(JcompType jt,ASTNode n)
{
   base_type = jt;
   arg_type = SumpArgType.computeArgType(jt,n);
   package_name = null;
   if (n != null) {
      CompilationUnit cu = (CompilationUnit) n.getRoot();
      PackageDeclaration pd = cu.getPackage();
      if (pd != null) {
         package_name = pd.getName().getFullyQualifiedName() + ".";
       }
    }
   type_name = computeName(base_type);
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public String getName()                
{
   return type_name;
}

public SumpArgType getArgType()
{
   return arg_type;
}


public JcompType getBaseType()
{
   return base_type; 
}




/********************************************************************************/
/*                                                                              */
/*      Output Methods                                                          */
/*                                                                              */
/********************************************************************************/

void outputXml(String fld,IvyXmlWriter xw)
{
   xw.begin(fld);
   xw.field("ARGTYPE",getArgType());
   xw.text(getName());
   xw.end(fld);
}


void outputJava(SumpModelBase model,PrintWriter pw)
{
   if (base_type == null) {
      String nm = model.getJavaOutputName(getName());
      pw.print(nm);
    }
   else outputJava(base_type,model,pw);
}


String getUmlOutputName(SumpModelBase model)
{
   StringWriter sw = new StringWriter();
   PrintWriter pw = new PrintWriter(sw);
   if (base_type == null) {
      String nm = model.getJavaOutputName(getName());
      pw.print(nm);
    }
   else outputJava(base_type,model,pw);
   return sw.toString();
}



private void outputJava(JcompType jt,SumpModelBase model,PrintWriter pw)
{
   if (jt.isPrimitiveType()) pw.print(jt.getName());
   else if (jt.isTypeVariable() || jt.isWildcardType()) {
      pw.print("Object");
    }
   else if (jt.isArrayType()) {
      outputJava(jt.getBaseType(),model,pw);
      pw.print("[]");
    }
   else if (jt.isParameterizedType()) {
      outputJava(jt.getBaseType(),model,pw);
      pw.print("<");
      int ct = 0;
      for (JcompType pt : jt.getComponents()) {
         if (ct++ > 0) pw.print(",");
         outputJava(pt,model,pw);
       }
      pw.print(">");
    }
   else if (jt.isMethodType()) {
      pw.print("(");
      int ct = 0;
      for (JcompType pt : jt.getComponents()) {
         if (ct++ > 0) pw.print(",");
         outputJava(pt,model,pw);
       }
      pw.print(")");
      outputJava(jt.getBaseType(),model,pw);
    }
   else if (jt.isUnionType()) {
      int ct = 0;
      for (JcompType pt : jt.getComponents()) {
         if (ct++ > 0) pw.print("|");
         outputJava(pt,model,pw);
       }
    }
   else if (jt.isIntersectionType()) {
      int ct = 0;
      for (JcompType pt : jt.getComponents()) {
         if (ct++ > 0) pw.print("&");
         outputJava(pt,model,pw);
       }
    }
   else {
      String nm = model.getJavaOutputName(jt.getName());
      pw.print(nm);
    }
}


void setupJava(SumpData sd)
{
   setupJava(base_type,sd);
}



private void setupJava(JcompType jt,SumpData sd)
{
   if (jt == null) return;
   if (jt.isPrimitiveType()) return;
   if (jt.isCompiledType()) return;
   if (jt.isUndefined()) return; 
   if (jt.isTypeVariable() || jt.isWildcardType()) return;
   if (jt.isArrayType()) {
      setupJava(jt.getBaseType(),sd);
    }
   else if (jt.isParameterizedType() || jt.isMethodType()) {
      setupJava(jt.getBaseType(),sd);
      for (JcompType pt : jt.getComponents()) {
         setupJava(pt,sd);
       }
    }
   else if (jt.isUnionType() || jt.isIntersectionType()) {
      for (JcompType pt : jt.getComponents()) {
         setupJava(pt,sd);
       }
    }
   else {
      sd.addImport(jt.getName());
    }
}
/********************************************************************************/
/*                                                                              */
/*      Get the output name value                                               */
/*                                                                              */
/********************************************************************************/

private String computeName(JcompType base)                
{
   if (base.isParameterizedType()) {
      JcompType jt = base.getBaseType();
      StringBuffer buf = new StringBuffer();
      buf.append(computeName(jt));
      buf.append("<");
      int i = 0;
      for (JcompType ct : base.getComponents()) {
         if (i++ > 0) buf.append(",");
         buf.append(computeName(ct));
       }
      buf.append(">");
      return buf.toString();
    }
   
   String nm = base.getName();
   if (nm != null && package_name != null) {
      if (nm.startsWith(package_name)) nm = nm.substring(package_name.length());
      for (JcompType jt = base; jt.getOuterType() != null; jt = jt.getOuterType()) {
         int idx = nm.indexOf(".");
         if (idx > 0) nm = nm.substring(idx+1);
       }
    }
   
   return nm;
}




/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return getName();
}


}       // end of class SumpDataType




/* end of SumpDataType.java */
