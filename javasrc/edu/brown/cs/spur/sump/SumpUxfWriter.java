/********************************************************************************/
/*                                                                              */
/*              SumpUxfWriter.java                                              */
/*                                                                              */
/*      Handle writing UXF files for UMLet                                      */
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

import java.awt.Point;
import java.awt.Rectangle;
import java.io.Writer;

import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.spur.lids.LidsConstants.LidsLibrary;

class SumpUxfWriter implements SumpConstants
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

SumpUxfWriter(Writer w)
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

void generateUxf(SumpModel mdl)
{
   OutputUxf ox = new OutputUxf();
   mdl.accept(ox);
}



/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/


/********************************************************************************/
/*                                                                              */
/*      Visitor to do output                                                    */
/*                                                                              */
/********************************************************************************/

private class OutputUxf extends SumpVisitor {

   private SumpLayout cur_layout;
   
   OutputUxf() {
      cur_layout = null;
    }

   @Override public boolean visit(SumpModel mdl) {
      cur_layout = mdl.computeLayout();
      xml_writer.begin("diagram");
      xml_writer.field("program","umlet");
      xml_writer.field("version","13.3");
      xml_writer.textElement("zoom_level",10);
      mdl.getPackage().accept(this);
      xml_writer.end("diagram");
      xml_writer.close();
      xml_writer.begin("element");
      xml_writer.textElement("id","UMLNote");
      StringBuffer buf = new StringBuffer();
      buf.append("NAME: " + getData().getName() + ";\n");
      for (String s : getData().getImports()) {
         buf.append("IMPORT: " + s + ":\n");
       }
      if (getData().getContextPath() != null) {
         buf.append("CONTEXT: " + getData().getContextPath() + ":\n");
       }
      for (LidsLibrary s : getData().getLibraries()) {
         buf.append("LIBRARY: " + s.getFullId() + ";\n");
       }
      for (String imp : getData().getMissingImports()) {
         buf.append("MISSING: " + imp + ";\n");
       }
      for (String s : getData().getSources()) {
         buf.append("SOURCE: " + s + ";\n");
       }
      for (String s : getData().getSuggestedWords()) {
         buf.append("SUGGEST: " + s + ";\n");
       }
      xml_writer.textElement("panel_attributes",buf.toString());
      xml_writer.end("element"); 
      return false;
    }
   
   @Override public boolean visit(SumpClass c) {
      xml_writer.begin("element");
      xml_writer.textElement("id","UMLClass");
      xml_writer.begin("coordinates");
      Rectangle r = cur_layout.getBounds(c);
      xml_writer.textElement("x",r.x);
      xml_writer.textElement("y",r.y);
      xml_writer.textElement("w",r.width);
      xml_writer.textElement("h",r.height);
      xml_writer.end("coordinates");
      StringBuffer buf = new StringBuffer();
      if (c.getEnumConstants() != null) buf.append("<<enumeration>>");
      buf.append(c.getJavaOutputName() + "\n-\n");
      if (c.getEnumConstants() != null) {
         for (String s : c.getEnumConstants()) {
            buf.append(s + "\n");
          }
       }
      for (SumpAttribute at : c.getAttributes()) {
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
         buf.append(getUmlTypeName(at.getDataType()));
         buf.append("\n");
       }
      if (!c.getOperations().isEmpty()) {
         buf.append("-\n");
         for (SumpOperation op : c.getOperations()) {
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
               buf.append(getUmlTypeName(ep.getDataType()));
             }
            buf.append(")");
            if (op.getReturnType() != null) {
               buf.append(": ");
               buf.append(getUmlTypeName(op.getReturnType()));
             }
            buf.append("\n");
          }
       }
      xml_writer.textElement("panel_attributes",buf.toString());
      xml_writer.end("element");
      return false;
    }
   
   @Override public boolean visit(SumpDependency sd) {
      // might need to create a separate element for each segement of the arc
      // need to determine what the panel attributes mean
      // need to determine what the additional attributes mean
      xml_writer.begin("element");
      xml_writer.textElement("id","Relation");
      Point [] pts = cur_layout.getPoints(sd);
      int minx = -1;
      int maxx = -1;
      int miny = -1;
      int maxy = -1;
      for (Point pt : pts) {
         if (minx < 0 || minx > pt.x) minx = pt.x;
         if (maxx < 0 || maxx < pt.x) maxx = pt.x;
         if (miny < 0 || miny > pt.y) miny = pt.y;
         if (maxy < 0 || maxy < pt.y) maxy = pt.y;
       }
      xml_writer.begin("coordinates");
      xml_writer.textElement("x",minx);
      xml_writer.textElement("y",miny);
      xml_writer.textElement("w",maxx-minx);
      xml_writer.textElement("h",maxy-miny);
      xml_writer.end("coordinates");
      xml_writer.textElement("panel_attributes","lt=<-");
      xml_writer.textElement("additional_attributes","10;10;10;" + (maxy-miny-20));
      xml_writer.end("element");
      return false;
    }

   private String getUmlTypeName(SumpDataType dt) {
      return getJavaTypeName(dt);
    }
   
}



}       // end of class SumpUxfWriter




/* end of SumpUxfWriter.java */

