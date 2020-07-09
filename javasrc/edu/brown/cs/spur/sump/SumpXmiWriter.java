/********************************************************************************/
/*                                                                              */
/*              SumpXmiWriter.java                                              */
/*                                                                              */
/*      Aid for writing out XMI files                                           */
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

import java.awt.Rectangle;
import java.io.Writer;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

class SumpXmiWriter implements SumpConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private IvyXmlWriter    xml_writer;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SumpXmiWriter(Writer w)
{
   xml_writer = null;
   if (w instanceof IvyXmlWriter) xml_writer = (IvyXmlWriter) w;
   else xml_writer = new IvyXmlWriter(w);
}


/********************************************************************************/
/*                                                                              */
/*      Generation methods                                                      */
/*                                                                              */
/********************************************************************************/

void generateXmi(SumpModel mdl)
{
   OutputXmi ox = new OutputXmi();
   mdl.accept(ox);
}




/********************************************************************************/
/*                                                                              */
/*      Ouptut helpers : start and end                                          */
/*                                                                              */
/********************************************************************************/

private void outputXmiStart()
{
   xml_writer.outputHeader();
   
   xml_writer.begin("XMI");
   xml_writer.field("xmi.version","1.2");
   xml_writer.field("verified",false);
   xml_writer.field("xmlns","http://schema.omg.org/spec/UML/1.4");
   
   outputXmiHeader();     
   
   xml_writer.begin("XMI:Content");
}


private void outputXmiHeader()
{
   xml_writer.begin("XMI.header");
   xml_writer.begin("XMI.documentation");
   xml_writer.end("XMI.documentation");
   xml_writer.begin("XMI.metamodel");
   xml_writer.field("xmi.version","1.4");
   xml_writer.field("xmi.name","UML");
   xml_writer.field("href","UML.xml");
   xml_writer.end("XMI.metamodel");
   xml_writer.end("XMI.header"); 
}



private void outputXmiEnd()
{
   xml_writer.end("XMI:Content");
   
   // ouptut XMI.extensions for umbrello
   
   xml_writer.end("XMI");
}


/********************************************************************************/
/*                                                                              */
/*      Element helpers                                                         */
/*                                                                              */
/********************************************************************************/

private void beginXmiElement(String elt,String name,Object id,String namespace)
{
   xml_writer.begin(elt);
   
   xml_writer.field("isRoot",false);
   xml_writer.field("isAbstract",false);
   xml_writer.field("isSpecification",false);
   xml_writer.field("isLeaf",false);
   
   switch (elt) {
      case "UML:Stereotype" :
      case "UML:Model" :
      case "UML:Package" :
      case "UML:Class" :
      case "UML:Association" :
         xml_writer.field("visibility","public");
         break;
      case "UML:Parameter" :
         xml_writer.field("visibility","private");
         break;
      case "UML:Attribute" :
         SumpAttribute satt = (SumpAttribute) id;
         outputVisibility(satt.getAccess());
         break;
      case "UML:Operation" :
         SumpOperation sop = (SumpOperation) id;
         outputVisibility(sop.getAccess());
         break;
    }
         
   if (name != null) xml_writer.field("name",name);
   if (id == null) ;
   else if (id instanceof String) xml_writer.field("xml.id",id);
   else if (id instanceof Number) {
      Number n = (Number) id;
      xml_writer.field("xml.id","u" + Integer.toHexString(n.intValue())); 
    }
   else if (id instanceof SumpElement) {
      SumpElement seb = (SumpElement) id;
      xml_writer.field("xml.id",getXmiId(seb));
    }
   else {
      int v = id.hashCode();
      xml_writer.field("xml.id","u" + Integer.toHexString(v)); 
    }
}



private void outputVisibility(ElementAccess acc)
{
   if (acc == null) return;
   xml_writer.field("visibility",acc.toString().toLowerCase());
}


private void endXmiElement(String elt)
{
   xml_writer.end(elt);
}



private void outputAssociationEnd(SumpClass sc,boolean to)
{
   xml_writer.begin("UML:AssociationEnd");
   xml_writer.field("changeability","changeable");
   xml_writer.field("isNavigable","false");
   xml_writer.field("isSpecification",to);
   xml_writer.field("aggregation","none");
   xml_writer.field("type",getXmiId(sc));
   xml_writer.field("visibility","public");
   xml_writer.field("name","");
   xml_writer.end("UML:AssociationEnd"); 
}



private String getXmiId(SumpElement e)
{
   int x = hashCode();
   return "u" + Integer.toHexString(x);
}



/********************************************************************************/
/*                                                                              */
/*      Visitor to do the output                                                */
/*                                                                              */
/********************************************************************************/

private class OutputXmi extends SumpVisitor {

   private SumpLayout cur_layout;
   private String type_field;
   
   OutputXmi() {
      cur_layout = null;
      type_field = null;
    }
   
   @Override public boolean visit(SumpModel mdl) {
      outputXmiStart();
      
      beginXmiElement("UML:Model",getData().getName(),"m1",null);
      
      xml_writer.begin("UML:Namespace.ownedElement");
      
      beginXmiElement("UML:Stereotype","folder","folder","m1");
      endXmiElement("UML:Stereotype");
      
      beginXmiElement("UML:Model","Logical View","Logical_View","m1");
      
      xml_writer.begin("UML:Namespace.ownedElement");
      mdl.getPackage().accept(this);
      xml_writer.end("UML:Namespace.ownedElement");
      
      xml_writer.begin("XMI.extension");
      xml_writer.field("xmi.extender","umbrello");
      xml_writer.begin("diagrams");
      xml_writer.field("resolution",96);
      xml_writer.begin("diagram");
      xml_writer.field("name","class diagram");
      
      cur_layout = mdl.computeLayout();
      mdl.getPackage().accept(this);
      xml_writer.end("diagram");
      xml_writer.end("diagrams");
      xml_writer.end("XMI.extension");
      
      endXmiElement("UML:Model");
      
      xml_writer.end("UML:Namespace.ownedElement");  
      
      endXmiElement("UML:Model");
      
      outputXmiEnd();
      
      return false;
    }
   
   @Override public boolean visit(SumpPackage p) {
      if (cur_layout == null) {
         beginXmiElement("UML:Package","DataTypes","DataTypes","Logical_View");
         xml_writer.field("stereotype","folder");
         xml_writer.begin("UML:Namespace.ownedElement");
         // output all used datatypes here
         xml_writer.end("UML:Namespace.ownedElement");
         endXmiElement("UML:Package");
         return true;
       }
      else {
         xml_writer.begin("notewidget");
         xml_writer.field("text","Note text goes here");
         // output note info here
         xml_writer.end("notewidget");
         xml_writer.begin("widgets");
         for (SumpClass sc : p.getClasses()) {
            sc.accept(this);
          }
         xml_writer.end("widgets");
         xml_writer.begin("associations");
         for (SumpDependency sd : p.getDependencies()) {
            sd.accept(this);
          }
         xml_writer.end("associations");
         return false;
       }
    }
   
   @Override public boolean visit(SumpClass c) {
      if (cur_layout == null) {                // model
         beginXmiElement("UML:Class",c.getJavaOutputName(),c,"Logical_View");
         return true;
       }
      else {                               // widgets
         Rectangle r = cur_layout.getBounds(c);
         xml_writer.begin("classwidget");
         xml_writer.field("x",r.x);
         xml_writer.field("y",r.y);
         xml_writer.field("width",r.width);
         xml_writer.field("height",r.height);
         xml_writer.field("showoperations",1);
         xml_writer.field("showattributes",1);
         xml_writer.field("showscope",1);
         xml_writer.field("showpubliconly",1); 
         xml_writer.field("showstereotype",1);
         xml_writer.field("showpsigs",601);
         xml_writer.field("showattsigs",601);
         xml_writer.field("showpackage",1);
         xml_writer.field("autoresize",1);
         xml_writer.field("localId","x" + getXmiId(c));
         xml_writer.field("xmi.id",getXmiId(c));
         xml_writer.end("classwidget");
         return false;
       }
    }
   @Override public void endVisit(SumpClass c) {
      endXmiElement("UML:Class");
    }
   
   @Override public boolean visit(SumpAttribute a) {
      beginXmiElement("UML:Attribute",a.getName(),a,null);
      type_field = "type";
      return true;
    }
   @Override public void endVisit(SumpAttribute a) {
      type_field = null;
      endXmiElement("UML:Attribute");
    }
   
   @Override public boolean visit(SumpOperation op) {
      beginXmiElement("UML:Operation",op.getName(),op,null);
      type_field = "returnType";
      op.getReturnType().accept(this);
      xml_writer.begin("UML:BehavioralFeature.parameter");
      for (SumpParameter sp : op.getParameters()) {
         sp.accept(this);
       }
      xml_writer.end("UML:BehavioralFeature.parameter");
      endXmiElement("UML:Operation");
      return false;
    }
   
   @Override public boolean visit(SumpParameter p) {
      beginXmiElement("UML:Parameter",p.getName(),p,null);
      type_field = "type";
      p.getDataType().accept(this);
      endXmiElement("UML:Parameter");
      return false;
    }
   
   @Override public boolean visit(SumpDependency sd) {
      if (cur_layout == null) {                // model
         beginXmiElement("UML:Association","",sd,"Logical_View");
         outputAssociationEnd(sd.getFromClass(),false);
         outputAssociationEnd(sd.getToClass(),true);
         endXmiElement("UML:Association");
       }
      else {                               // diagram
         // need to do output here
       }
      return false;
    }
   
   @Override public boolean visit(SumpDataType dt) {
      // should output xmiId of the data type
      xml_writer.field(type_field,dt.getName());
      return false;
    }
   
}       // end of inner class OutputXmi


}       // end of class SumpXmiWriter




/* end of SumpXmiWriter.java */

