/********************************************************************************/
/*                                                                              */
/*              SumpConstants.java                                              */
/*                                                                              */
/*      Constants for search UML presentation modeling                          */
/*                                                                              */
/********************************************************************************/



package edu.brown.cs.spur.sump;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;

import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.spur.rowel.RowelConstants.RowelMatch;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

public interface SumpConstants
{

enum ElementType {
   PACKAGE,
   CLASS,
   METHOD,
   ATTRIBUTE,
   OPERATION,
   PARAMETER,
   DEPENDENCY,
}


enum ElementAccess {
   PUBLIC,
   PRIVATE,
}


enum DependArity {
   ANY,
   ONE,
   MULTIPLE
}



/********************************************************************************/
/*                                                                              */
/*      Element definitions                                                     */
/*                                                                              */
/********************************************************************************/

interface SumpModel {
  SumpPackage setPackage(String name);
  SumpPackage getPackage();
  SumpData getModelData();
  boolean contains(SumpModel mdl);
  double matchScore(SumpModel mdl,Map<String,String> namemap);
  void outputXml(IvyXmlWriter xw);
  void outputJava(Writer w);
  void save(File file) throws IOException;
  void generateUXF(File file) throws IOException;
  void generateXMI(File file) throws IOException;
  void computeLayout();
  Rectangle getBounds(SumpClass cls);
}



interface SumpElement {
   ElementType getElementType();
   String getName();
   String getFullName();
}       // end of inner interface SumpElement



interface SumpPackage extends SumpElement {

   Collection<SumpClass> getClasses();
   
   SumpClass addClass(AbstractTypeDeclaration atd);
   SumpDependency addDependency(SumpClass frm,SumpClass to);
   Collection<SumpDependency> getDependencies();
   
   void addDependencies(AbstractTypeDeclaration atd,Map<String,SumpClass> cmap);
   
}       // end of inner interface SumpPackageElement


interface SumpClass extends SumpElement {
   boolean isInterface();
   SumpClass getSuperClass();
   Collection<SumpClass> getInterfaces();
   Collection<SumpAttribute> getAttributes();
   Collection<SumpOperation> getOperations();
   
   void addAttribute(JcompSymbol js,ASTNode n);
   void addOperation(JcompSymbol js,ASTNode n);
   

}       // end of inner interface SumpClassElement


interface SumpAttribute extends SumpElement {
   SumpDataType getDataType();
   ElementAccess getAccess();
   
}       // end of inner interface SumpAttribute

interface SumpOperation extends SumpElement {
   SumpDataType getReturnType();
   Collection<SumpParameter> getParameters();
   ElementAccess getAccess();
}       // end of inner interface SumpOperation


interface SumpParameter extends SumpElement, RowelMatch {

   SumpDataType getDataType();
   
}       // end of inner interace SumpParameter


interface SumpDependency extends SumpElement {
   SumpClass getFromClass();
   SumpClass getToClass();
   DependArity getFromArity();
   DependArity getToArity();
   String getFromLabel();
   String getToLabel();
}




/********************************************************************************/
/*                                                                              */
/*      Factory methods                                                         */
/*                                                                              */
/********************************************************************************/

public class SumpFactory {

   public static SumpModel createModel(SumpData data) {
      return new SumpModelBase(data);
    }
   
   public static SumpModel loadModel(File f) {
      return new SumpModelBase(null,f);
    }
   
   public static SumpModel loadModel(JcompControl ctrl,File f) {
      return new SumpModelBase(ctrl,f);
    }
   
   public static SumpModel createModel(SumpData data,CompilationUnit cu) {
      return new SumpModelBase(data,cu);
    }
   
}       // end of inner class SumpFactory



}       // end of interface SumpConstants




/* end of SumpConstants.java */
