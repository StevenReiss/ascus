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
/*      Visitation methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public void accept(SumpVisitor sev)
{
   if (!sev.preVisit(this)) return;
   if (!sev.visit(this)) return;
   sev.endVisit(this);
   sev.postVisit(this);
}



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







@Override public String toString()
{
   return from_class + "=>" + to_class;
}



}       // end of class SumpElementDependency




/* end of SumpElementDependency.java */

