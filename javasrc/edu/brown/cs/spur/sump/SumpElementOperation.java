/********************************************************************************/
/*                                                                              */
/*              SumpElementOperation.java                                       */
/*                                                                              */
/*      Implementation of a UML Operation for SUMP                              */
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import edu.brown.cs.ivy.file.IvyStringDiff;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.spur.rowel.RowelConstants.RowelMatch;
import edu.brown.cs.spur.rowel.RowelConstants.RowelMatchType;
import edu.brown.cs.spur.sump.SumpConstants.SumpOperation;

class SumpElementOperation extends SumpElementBase implements SumpOperation
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SumpDataType return_type;
private List<SumpElementParameter> param_values;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SumpElementOperation(SumpModelBase mdl,JcompSymbol js,ASTNode n)
{
   super(mdl);
   setAccess(js.getModifiers());
   setName(js.getName());
   setFullName(js.getCompleteName());
   return_type = new SumpDataType(js.getType().getBaseType(),n);
   param_values = null;
   MethodDeclaration md = (MethodDeclaration) js.getDefinitionNode();
   if (md != null) {
      param_values = new ArrayList<>();
      for (Object o : md.parameters()) {
         SingleVariableDeclaration svd = (SingleVariableDeclaration) o;
         JcompSymbol s = JcompAst.getDefinition(svd);
         SumpElementParameter sp = new SumpElementParameter(mdl,s,svd);
         param_values.add(sp);
       }
    }
   addCommentsFor(n);
}



/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/



@Override public ElementType getElementType()
{
   return ElementType.OPERATION;
}



@Override public SumpDataType getReturnType()
{
   return return_type;
}



@Override public Collection<SumpParameter> getParameters()
{
   return new ArrayList<>(param_values);
}



@Override public int getParameterIndex(SumpParameter sp)
{
   if (param_values == null) return -1;
   return param_values.indexOf(sp);
}



/********************************************************************************/
/*                                                                              */
/*      Matching methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public double getMatchScore(RowelMatch rm,RowelMatchType mt)
{
   if (rm instanceof SumpElementOperation) {
      SumpElementOperation op = (SumpElementOperation) rm;
      if (!matchOperation(op)) return 0;
      return 1 + IvyStringDiff.normalizedStringDiff(getName(),op.getName());
    }
   
   return 0;
}


private boolean matchOperation(SumpElementOperation op)
{
   SumpDataType bdt = getReturnType();
   SumpDataType pdt = op.getReturnType();
   
   if (!SumpMatcher.matchType(bdt,pdt)) return false;
   
   Collection<SumpParameter> bsps = getParameters();
   Collection<SumpParameter> psps = op.getParameters();
   if (bsps.size() != psps.size()) return false;
   
   Set<SumpParameter> done = new HashSet<>();
   for (SumpParameter base : bsps) {
      SumpDataType bt = base.getDataType();
      boolean fnd = false;
      for (SumpParameter pat : psps) {
         if (done.contains(pat)) continue;
         SumpDataType pt = pat.getDataType();
         if (SumpMatcher.matchType(bt,pt)) {
            done.add(pat);
            fnd = true;
            break;
          }
       }
      if (!fnd) return false;
    }
   
   return true;
}




/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override void outputXml(IvyXmlWriter xw) 
{
   xw.begin("OPERATION");
   basicXml(xw);
   if (return_type != null) return_type.outputXml("RETURN",xw);
   if (param_values != null) {
      for (SumpElementParameter sp : param_values) {
         sp.outputXml(xw);
       }
    }
   xw.end("OPERATION");
}



@Override void outputJava(PrintWriter pw)
{
   outputComment(pw);
   
   pw.print("   ");
   if (!getData().isCurrentInterface()) {
      String acc = "public";
      if (getAccess() != null) acc = getAccess().toString().toLowerCase();
      pw.print(acc + " ");
    }
   
   boolean isconst = false;
   String nm = getName();
   if (nm.equals("<init>")) {
      nm = getData().getCurrentType();
      isconst = true;
    }   
   else {
      if (!getData().isCurrentInterface()) pw.print("abstract ");
      if (return_type != null) return_type.outputJava(pw);
      else pw.print("void");
      pw.print(" ");
    }
   
   pw.print(nm + "(");
   
   if (param_values != null) {
      int ct = 0;
      for (SumpElementParameter sp : param_values) {
         if (ct++ > 0) pw.print(",");
         sp.outputJava(pw);
       }
    }
   pw.print(")");
   
   if (isconst) pw.println(" { }");
   else pw.println(";");
}


@Override void setupJava()
{
   if (return_type != null) return_type.setupJava(getData());
   if (param_values !=  null) {
      for (SumpElementParameter sp : param_values) {
         sp.setupJava();
       }
    }
}

}       // end of class SumpElementOperation




/* end of SumpElementOperation.java */

