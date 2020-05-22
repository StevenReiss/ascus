/********************************************************************************/
/*										*/
/*		SumpElement.java						*/
/*										*/
/*	description of class							*/
/*										*/
/********************************************************************************/



package edu.brown.cs.spur.sump;

import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.spur.sump.SumpConstants.SumpElement;

import java.io.PrintWriter;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.Modifier;

public abstract class SumpElementBase implements SumpConstants, SumpElement
{



/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

protected SumpModelBase sump_model;
private String	element_name;
private String  full_name;
private ElementAccess element_access;
private String  element_comment;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

protected SumpElementBase(SumpModelBase mdl)
{
   this(mdl,null);
}


protected SumpElementBase(SumpModelBase mdl,String name)
{
   sump_model = mdl;
   element_name = name;
   full_name = name;
   element_access = ElementAccess.PUBLIC;
   element_comment = null;
}



/********************************************************************************/
/*										*/
/*	Public access methods							*/
/*										*/
/********************************************************************************/

abstract public ElementType getElementType();

public String getName() 			{ return element_name; }

@Override public String getFullName()           { return full_name; }

@Override public String getMapName()            { return getFullName(); }


public ElementAccess getAccess()		{ return element_access; }



public SumpDataType getDataType() throws SumpException		
{
   throw new SumpException("Element has no data type");
}


/********************************************************************************/
/*										*/
/*	Local access methods            					*/
/*										*/
/********************************************************************************/

void setAccess(int mods)
{
   ElementAccess acc = null;
   if (Modifier.isPublic(mods)) acc = ElementAccess.PUBLIC;
   else if (Modifier.isPrivate(mods)) acc = ElementAccess.PRIVATE;
   element_access = acc;
}

void setName(String nm)
{
   element_name = nm;
}

void setFullName(String nm)
{
   full_name = nm;
}

void setComment(String cmmt)
{
   element_comment = cmmt;
}



SumpData getData()      
{
   return sump_model.getModelData();
}


/********************************************************************************/
/*                                                                              */
/*      Comment management                                                      */
/*                                                                              */
/********************************************************************************/

void addComment(String cmmt)
{
   if (element_comment == null || element_comment.equals("")) element_comment = cmmt;
   else {
      if (!element_comment.endsWith("\n")) element_comment += "\n";
      element_comment += cmmt;
    }
}

String getComment()                     { return element_comment; }


void addCommentsFor(ASTNode n)
{
   if (n == null) return;
   
   BodyDeclaration bd = null;
   if (n instanceof BodyDeclaration) bd = (BodyDeclaration) n;
   else if (n.getParent() instanceof FieldDeclaration) {
      FieldDeclaration fd = (FieldDeclaration) n.getParent();
      if (fd.fragments().size() == 1) bd = fd;
    }
   if (bd != null) {
      Javadoc jd = bd.getJavadoc();
      if (jd != null)
         addComment(jd);
    }
   
   CompilationUnit cu = (CompilationUnit) n.getRoot();
   int idx0 = cu.firstLeadingCommentIndex(n);
   int idx1 = cu.lastTrailingCommentIndex(n);
   if (idx0 > 0 && idx1 > 0) {
      List<?> cmmts = cu.getCommentList();
      for (int i = idx0; i <= idx1; ++i) {
         Comment c = (Comment) cmmts.get(i);
         if (!c.isDocComment()) addComment(c);
       }
    }
}


void addComment(Comment c)
{
   if (c == null) return;
   String cs = c.toString();
   addComment(cs);
}



/********************************************************************************/
/*										*/
/*	Output methods								*/
/*										*/
/********************************************************************************/

void outputXml(IvyXmlWriter xw) 
{
   xw.begin(getElementType().toString());
   xw.field("NAME",getName());
   xw.end(getElementType().toString());
}


protected void basicXml(IvyXmlWriter xw)
{
   xw.field("NAME",getName());
   if (getAccess() != null) xw.field("ACCESS",getAccess());
   if (element_comment != null) {
      xw.cdataElement("COMMENT",element_comment);
    }
}


void outputJava(PrintWriter pw)                 { }


protected void outputComment(PrintWriter pw)
{
   if (element_comment == null) return;
   pw.print(element_comment);
   if (!element_comment.endsWith("\n")) pw.println();
}

void setupJava()                                { }



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public String toString()
{
   return full_name;
}


}	// end of class SumpElement




/* end of SumpElement.java */
