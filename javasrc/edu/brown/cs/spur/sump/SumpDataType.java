/********************************************************************************/
/*                                                                              */
/*              SumpDataType.java                                               */
/*                                                                              */
/*      Representation of a data type                                           */
/*                                                                              */
/********************************************************************************/



package edu.brown.cs.spur.sump;

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
/*      Visitation methods                                                      */
/*                                                                              */
/********************************************************************************/

public void accept(SumpVisitor sev)
{
   if (!sev.visit(this)) return;
   sev.endVisit(this);
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
