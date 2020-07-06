/********************************************************************************/
/*                                                                              */
/*              SumpElementParameter.java                                       */
/*                                                                              */
/*      UML parameter representation                                            */
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

import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.file.IvyStringDiff;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.spur.rowel.RowelConstants.RowelMatch;
import edu.brown.cs.spur.rowel.RowelConstants.RowelMatchType;
import edu.brown.cs.spur.sump.SumpConstants.SumpParameter;

class SumpElementParameter extends SumpElementBase implements SumpParameter, RowelMatch
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SumpDataType    param_type;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SumpElementParameter(SumpModelBase mdl,JcompSymbol js,SingleVariableDeclaration n)
{
   super(mdl);
   setAccess(js.getModifiers());
   setName(js.getName());
   setFullName(js.getCompleteName());
   param_type = new SumpDataType(js.getType(),n);
   addCommentsFor(n.getName());
   addCommentsFor(n);
}



/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public ElementType getElementType()
{
   return ElementType.PARAMETER;
}



@Override public SumpDataType getDataType()
{
   return param_type;
}

/********************************************************************************/
/*                                                                              */
/*      Match methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public double getMatchScore(RowelMatch rm,RowelMatchType mt)
{
   if (rm instanceof SumpElementParameter) {
      SumpElementParameter sep = (SumpElementParameter) rm;
      if (!SumpMatcher.matchType(getDataType(),sep.getDataType())) return 0;
      return 1 + IvyStringDiff.normalizedStringDiff(getName(),sep.getName());  
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
   xw.begin("PARAMETER");
   basicXml(xw);
   if (param_type != null) param_type.outputXml("TYPE",xw);
   xw.end("PARAMETER");
}


@Override void outputJava(PrintWriter pw)
{
   outputComment(pw);
   
   if (param_type != null) param_type.outputJava(sump_model,pw);
   else pw.print("Object");
   pw.print(" " + getName());
}


@Override void setupJava()
{
   if (param_type != null) {
      param_type.setupJava(getData());
    }
}


/********************************************************************************/
/*                                                                              */
/*      UML diagram output methods                                              */
/*                                                                              */
/********************************************************************************/

void generateXMI(SumpXmiWriter xw)
{
   xw.beginXmiElement("UML:Parameter",getName(),this,null);
   xw.field("type",param_type.getName());
   xw.endXmiElement("UML:Parameter");
}




}       // end of class SumpElementParameter




/* end of SumpElementParameter.java */

