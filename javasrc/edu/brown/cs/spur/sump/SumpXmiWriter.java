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

import java.io.Writer;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

class SumpXmiWriter extends IvyXmlWriter implements SumpConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SumpXmiWriter()
{ }



SumpXmiWriter(Writer w)
{
   super(w);
}



/********************************************************************************/
/*                                                                              */
/*      Ouptut helpers : start and end                                          */
/*                                                                              */
/********************************************************************************/

void outputXmiStart()
{
   outputHeader();
   
   begin("XMI");
   field("xmi.version","1.2");
   field("verified",false);
   field("xmlns","http://schema.omg.org/spec/UML/1.4");
   
   outputXmiHeader();     
}


void outputXmiHeader()
{
   begin("XMI.header");
   begin("XMI.documentation");
   end("XMI.documentation");
   begin("XMI.metamodel");
   field("xmi.version","1.4");
   field("xmi.name","UML");
   field("href","UML.xml");
   end("XMI.header"); 
}



void outputXmiEnd()
{
   // ouptut XMI.extensions for umbrello
   
   end("XMI");
}


/********************************************************************************/
/*                                                                              */
/*      Element helpers                                                         */
/*                                                                              */
/********************************************************************************/

void beginXmiElement(String elt,String name,Object id,String namespace)
{
   begin(elt);
   
   field("isRoot",false);
   field("isAbstract",false);
   field("isSpecification",false);
   field("isLeaf",false);
   
   switch (elt) {
      case "UML:Stereotype" :
      case "UML:Model" :
      case "UML:Package" :
      case "UML:Class" :
      case "UML:Association" :
         field("visibility","public");
         break;
      case "UML:Parameter" :
         field("visibility","private");
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
         
   if (name != null) field("name",name);
   if (id == null) ;
   else if (id instanceof String) field("xml.id",id);
   else if (id instanceof Number) {
      Number n = (Number) id;
      field("xml.id","u" + Integer.toHexString(n.intValue())); 
    }
   else if (id instanceof SumpElementBase) {
      SumpElementBase seb = (SumpElementBase) id;
      field("xml.id",seb.getXmiId());
    }
   else {
      int v = id.hashCode();
      field("xml.id","u" + Integer.toHexString(v)); 
    }
}



private void outputVisibility(ElementAccess acc)
{
   switch (acc) {
      case PUBLIC :
         field("visibility","public");
         break;
      case PRIVATE :
         field("visibility","private");
         break;
    }
}


void endXmiElement(String elt)
{
   end(elt);
}



void outputAssociationEnd(SumpClass sc,boolean to)
{
   begin("UML:AssociationEnd");
   field("changeability","changeable");
   field("isNavigable","false");
   field("isSpecification",to);
   field("aggregation","none");
   field("type",((SumpElementClass) sc).getXmiId());
   field("visibility","public");
   field("name","");
   end("UML:AssociationEnd"); 
}



}       // end of class SumpXmiWriter




/* end of SumpXmiWriter.java */

