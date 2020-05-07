/********************************************************************************/
/*                                                                              */
/*              SumpElementAttribute.java                                       */
/*                                                                              */
/*      Attribute (field) implementation                                        */
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

import java.io.PrintWriter;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.brown.cs.ivy.file.IvyStringDiff;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.spur.rowel.RowelConstants.RowelMatch;
import edu.brown.cs.spur.rowel.RowelConstants.RowelMatchType;
import edu.brown.cs.spur.sump.SumpConstants.SumpAttribute;

class SumpElementAttribute extends SumpElementBase implements SumpAttribute
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SumpDataType data_type;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SumpElementAttribute(SumpModelBase mdl,JcompSymbol fld,ASTNode n)
{
   super(mdl);
   JcompType typ = fld.getType();
   data_type = new SumpDataType(typ,n);
   setAccess(fld.getModifiers());
   setName(fld.getName());
   setFullName(fld.getFullName());
   addCommentsFor(n);
}


/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/



@Override public ElementType getElementType()
{
   return ElementType.ATTRIBUTE;
}






@Override public SumpDataType getDataType()
{
   return data_type;
}



/********************************************************************************/
/*                                                                              */
/*      Matching methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public double getMatchScore(RowelMatch rm,RowelMatchType mt)
{
   if (rm instanceof SumpElementAttribute) {
      SumpElementAttribute att = (SumpElementAttribute) rm;
      if (!SumpMatcher.matchType(getDataType(),att.getDataType())) return 0;
      return 1 + IvyStringDiff.normalizedStringDiff(getName(),att.getName());
    }
   
   return 0;
}




/********************************************************************************/
/*                                                                              */
/*      Output Methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override void outputXml(IvyXmlWriter xw)
{
   xw.begin("ATTRIBUTE");
   basicXml(xw);
   if (data_type != null) data_type.outputXml("TYPE",xw);
   xw.end("ATTRIBUTE");
}


@Override void outputJava(PrintWriter pw)
{
   outputComment(pw);
   
   String acc = "public";
   if (getAccess() != null) acc = getAccess().toString().toLowerCase();
   pw.print("   " + acc + " ");
   
   if (data_type != null) data_type.outputJava(pw);
   else pw.print("Object");
   
   pw.println(" " + getName() + ";");
}


@Override void setupJava()
{
   if (data_type != null) data_type.setupJava(getData());
}



}       // end of class SumpElementAttribute




/* end of SumpElementAttribute.java */

