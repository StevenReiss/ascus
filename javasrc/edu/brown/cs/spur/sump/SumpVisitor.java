/********************************************************************************/
/*                                                                              */
/*              SumpElementVisitor.java                                         */
/*                                                                              */
/*      Visit the model in order                                                */
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

import java.io.PrintWriter;
import java.io.StringWriter;

import edu.brown.cs.ivy.jcomp.JcompType;

/**
 *      This visits the model as:
 *          Model ::    Package+
 *          Package ::  Class* Dependency* 
 *          Class ::    Attribute* Operation*
 *          Attribute :: DataType
 *          Operation :: ReturntType Parameter* 
 *          Parameter :: ParamType
 **/


public class SumpVisitor implements SumpConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private         SumpModel       sump_model;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SumpVisitor()            
{
   this(null);
}

public SumpVisitor(SumpModel mdl)            
{
   sump_model = mdl;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

protected SumpModel getModel()          { return sump_model; }

protected SumpData getData()            { return sump_model.getModelData(); }

void setModel(SumpModel m)              { sump_model = m; }



/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

protected String getOutputName(String orignm) {
   SumpClass sc = getModel().getClassForName(orignm);
   String nm = orignm;
   if (sc != null) {
      SumpElementClass sec = (SumpElementClass) sc;
      nm = sec.getJavaOutputName();
    }
   else {
      int idx1 = nm.indexOf("<");
      if (idx1 < 0) {
         int idx2 = nm.lastIndexOf(".");
         if (idx2 > 0) nm = nm.substring(idx2+1);
       }
      else {
         int idx2 = nm.lastIndexOf(".",idx1);
         if (idx2 > 0) nm = nm.substring(idx2+1);
         // need to fix parameters as well 
       }
    }
   return nm;
}



protected String getJavaTypeName(SumpDataType dt) 
{
   if (dt.getBaseType() == null) return getOutputName(dt.getName());
   StringWriter sw = new StringWriter();
   PrintWriter pw = new PrintWriter(sw);
   outputJavaType(dt.getBaseType(),pw);
   return sw.toString();
}


private void outputJavaType(JcompType jt,PrintWriter pw) {
   if (jt.isPrimitiveType()) pw.print(jt.getName());
   else if (jt.isTypeVariable() || jt.isWildcardType()) {
      pw.print("Object");
    }
   else if (jt.isArrayType()) {
      outputJavaType(jt.getBaseType(),pw);
      pw.print("[]");
    }
   else if (jt.isParameterizedType()) {
      outputJavaType(jt.getBaseType(),pw);
      pw.print("<");
      int ct = 0;
      for (JcompType pt : jt.getComponents()) {
         if (ct++ > 0) pw.print(",");
         outputJavaType(pt,pw);
       }
      pw.print(">");
    }
   else if (jt.isMethodType()) {
      pw.print("(");
      int ct = 0;
      for (JcompType pt : jt.getComponents()) {
         if (ct++ > 0) pw.print(",");
         outputJavaType(pt,pw);
       }
      pw.print(")");
      outputJavaType(jt.getBaseType(),pw);
    }
   else if (jt.isUnionType()) {
      int ct = 0;
      for (JcompType pt : jt.getComponents()) {
         if (ct++ > 0) pw.print("|");
         outputJavaType(pt,pw);
       }
    }
   else if (jt.isIntersectionType()) {
      int ct = 0;
      for (JcompType pt : jt.getComponents()) {
         if (ct++ > 0) pw.print("&");
         outputJavaType(pt,pw);
       }
    }
   else {
      String nm = getOutputName(jt.getName());
      pw.print(nm);
    }
}




/********************************************************************************/
/*                                                                              */
/*      Basic visitation methods                                                */
/*                                                                              */
/********************************************************************************/

public boolean visit(SumpModel m)                       { return true; }
public void endVisit(SumpModel m)                       { }

public boolean visit(SumpPackage p)                     { return true; }
public void endVisit(SumpPackage p)                     { }

public boolean visit(SumpClass c)                       { return true; }
public void endVisit(SumpClass c)                       { }

public boolean visit(SumpAttribute a)                   { return true; }
public void endVisit(SumpAttribute a)                   { }

public boolean visit(SumpOperation o)                   { return true; }
public void endVisit(SumpOperation o)                   { }

public boolean visit(SumpParameter p)                   { return true; }
public void endVisit(SumpParameter p)                   { }

public boolean visit(SumpDependency d)                  { return true; }
public void endVisit(SumpDependency d)                  { }

public boolean visit(SumpDataType t)                    { return true; }
public void endVisit(SumpDataType t)                    { }

public boolean preVisit(SumpElement e)                  { return true; }
public void postVisit(SumpElement e)                    { }


}       // end of class SumpElementVisitor




/* end of SumpElementVisitor.java */

