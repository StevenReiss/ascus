/********************************************************************************/
/*                                                                              */
/*              SumpElementDependency.java                                      */
/*                                                                              */
/*      Class to class dependency for UML                                       */
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

import java.awt.Point;

import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.spur.sump.SumpConstants.SumpDependency;

class SumpElementDependency extends SumpElementBase implements SumpDependency
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SumpClass       from_class;
private SumpClass       to_class;
private DependArity     from_arity;
private DependArity     to_arity;
private String          from_label;
private String          to_label;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SumpElementDependency(SumpModelBase mdl,SumpClass frm,SumpClass to)
{
   super(mdl);
   
   from_class = frm;
   to_class = to;
   from_arity = DependArity.ANY;
   to_arity = DependArity.ANY;
   from_label = null;
   to_label = null;
}



/********************************************************************************/
/*                                                                              */
/*      Acess methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public ElementType getElementType()
{
   return ElementType.DEPENDENCY;
}


@Override public SumpClass getFromClass()               { return from_class; }

@Override public SumpClass getToClass()                 { return to_class; }

@Override public DependArity getFromArity()             { return from_arity; }

@Override public DependArity getToArity()               { return to_arity; }

@Override public String getFromLabel()                  { return from_label; }

@Override public String getToLabel()                    { return to_label; }




/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override void outputXml(IvyXmlWriter xw)
{
   xw.begin("ASSOCIATION");
   xw.field("CLASS1",from_class.getName());
   xw.field("CLASS2",to_class.getName());
   if (from_label != null) xw.field("ROLE1",from_label);
   if (to_label != null) xw.field("ROLE2",to_label);
   if (from_arity != DependArity.ANY) xw.field("CARDINALITY1",from_arity);
   if (to_arity != DependArity.ANY) xw.field("CARDINALITY2",to_arity);
   xw.end("ASSOCIATION");
}


void generateUXF(IvyXmlWriter xw,SumpLayout layout)
{
   // might need to create a separate element for each segement of the arc
   // need to determine what the panel attributes mean
   // need to determine what the additional attributes mean
   xw.begin("element");
   xw.textElement("id","Relation");
   Point [] pts = layout.getPoints(this);
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
   xw.begin("coordinates");
   xw.textElement("x",minx);
   xw.textElement("y",miny);
   xw.textElement("w",maxx-minx);
   xw.textElement("h",maxy-miny);
   xw.end("coordinates");
   xw.textElement("panel_attributes","lt=<-");
   xw.textElement("additional_attributes","10;10;10;" + (maxy-miny-20));
   xw.end("element");
}


void generateXMI(IvyXmlWriter xw,SumpLayout layout)
{
   if (layout == null) {                // model
       xw.begin("UML:Association");
       xw.field("namespace","Logical_View");
       xw.field("isSpecification",false);
       xw.field("xmi.id",getXmiId());
       xw.field("visibility","public");
       xw.field("name","");
       xw.begin("UML:AssociationEnd");
       xw.field("changeability","changeable");
       xw.field("isNavigable","false");
       xw.field("isSpecification",false);
       xw.field("aggregation","none");
       xw.field("type",((SumpElementClass) from_class).getXmiId());
       xw.field("visibility","public");
       xw.field("name","");
       xw.end("UML:AssociationEnd");
       xw.begin("UML:AssociationEnd");
       xw.field("changeability","changeable");
       xw.field("isNavigable","true");
       xw.field("isSpecification",false);
       xw.field("aggregation","none");
       xw.field("type",((SumpElementClass) to_class).getXmiId());
       xw.field("visibility","public");
       xw.field("name","");
       xw.end("UML:AssociationEnd");
    }
   else {                               // diagram
    }
}

@Override public String toString()
{
   return from_class + "=>" + to_class;
}



}       // end of class SumpElementDependency




/* end of SumpElementDependency.java */

