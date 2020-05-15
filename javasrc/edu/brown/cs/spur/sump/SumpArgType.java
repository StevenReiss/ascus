/********************************************************************************/
/*                                                                              */
/*              SumpArgType.java                                                */
/*                                                                              */
/*      Characterization of types                                               */
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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;

public enum SumpArgType
{

   VOID, BOOLEAN, NUMBER, STRING, URL, DATE, MAP, COLLECTION, ARRAY, 
   COMPONENT, COLOR, POINT, SHAPE, INPUT, OUTPUT, 
   OBJECT, THISTYPE, USERTYPE, INETADDRESS, ITERATOR,
   GRAPHICS, EVENT, SOCKET, EXCEPTION,
   JAVA, ANDROID, OTHER;
   
   
   
/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static Map<String,SumpArgType> known_map;
private static Map<String,SumpArgType> compat_map;
private static Map<String,SumpArgType> contains_map;  

static {
   known_map = new HashMap<>();
   known_map.put("java.lang.Boolean",SumpArgType.BOOLEAN);
   known_map.put("java.lang.String",SumpArgType.STRING);
   known_map.put("java.io.File",SumpArgType.STRING);
   known_map.put("java.lang.Object",SumpArgType.OBJECT);
   known_map.put("java.util.Date",SumpArgType.DATE);
   known_map.put("java.sql.Date",SumpArgType.DATE);
   known_map.put("java.sql.Timestamp",SumpArgType.DATE);  
   known_map.put("java.net.URL",SumpArgType.URL);
   known_map.put("java.net.URI",SumpArgType.URL);
   known_map.put("java.awt.Color",SumpArgType.COLOR);
   known_map.put("java.awt.Rectangle",SumpArgType.SHAPE);
   
   compat_map = new HashMap<>();
   compat_map.put("java.util.Collection",SumpArgType.COLLECTION);
   compat_map.put("java.util.Iterable",SumpArgType.COLLECTION);
   compat_map.put("java.util.Map",SumpArgType.MAP);
   compat_map.put("java.awt.Component",SumpArgType.COMPONENT);
   compat_map.put("java.awt.geom.Point2D",SumpArgType.POINT);
   compat_map.put("java.awt.Shape",SumpArgType.SHAPE);
   compat_map.put("java.lang.Number",SumpArgType.NUMBER);
   compat_map.put("java.lang.Enum",SumpArgType.NUMBER);
   compat_map.put("java.io.Reader",SumpArgType.INPUT);
   compat_map.put("java.io.Writer",SumpArgType.OUTPUT);
   compat_map.put("java.io.InputStream",SumpArgType.INPUT);
   compat_map.put("java.io.OutputStream",SumpArgType.OUTPUT);
   compat_map.put("java.lang.CharSequence",SumpArgType.STRING);
   compat_map.put("java.net.InetAddres",SumpArgType.INETADDRESS);
   compat_map.put("java.util.Enumeration",SumpArgType.ITERATOR);
   compat_map.put("java.util.Iterator",SumpArgType.ITERATOR);
   compat_map.put("java.awt.Graphics",SumpArgType.GRAPHICS);
   compat_map.put("java.awt.Event",SumpArgType.EVENT);
   compat_map.put("java.util.EventObject",SumpArgType.EVENT);
   compat_map.put("java.net.Socket",SumpArgType.SOCKET);
   compat_map.put("java.net.ServerSocket",SumpArgType.SOCKET);     
   compat_map.put("java.lang.Throwable",SumpArgType.EXCEPTION);
   
   contains_map = new HashMap<>();
   contains_map.put("Color",SumpArgType.COLOR);
   contains_map.put("Point",SumpArgType.POINT);
   contains_map.put("Date",SumpArgType.DATE);
   contains_map.put("Time",SumpArgType.DATE);
   contains_map.put("URI",SumpArgType.URL);
   contains_map.put("URL",SumpArgType.URL);   
}
   

  
/********************************************************************************/
/*                                                                              */
/*      Compute ArgType for a JcompType                                         */
/*                                                                              */
/********************************************************************************/

public static SumpArgType computeArgType(JcompType jt,ASTNode n)
{
   JcompTyper typer = JcompAst.getTyper(n);
   return computeArgType(typer,jt,n);
}


public static SumpArgType computeArgType(JcompTyper typer,JcompType jt,ASTNode n)
{
   if (jt.isPrimitiveType()) {
      if (jt.isVoidType()) return SumpArgType.VOID;
      else if (jt.isBooleanType()) return SumpArgType.BOOLEAN;
      else return SumpArgType.NUMBER;
    }
   else if (jt.isArrayType()) {
      return SumpArgType.ARRAY;
    }
   
   SumpArgType at = known_map.get(jt.getName());
   if (at != null) return at;
   
   for (Map.Entry<String,SumpArgType> ent : compat_map.entrySet()) {
      String typnam = ent.getKey();
      JcompType etype = typer.findSystemType(typnam);
      if (etype != null && jt.isAssignCompatibleWith(etype)) {
         return ent.getValue();
       }
    }
   
   for (Map.Entry<String,SumpArgType> ent : contains_map.entrySet()) {
      String cont = ent.getKey();
      if (jt.getName().contains(cont)) return ent.getValue();
    }
   
   if (n != null) {
      for (ASTNode p = n; p != null; p = p.getParent()) {
         if (p instanceof AbstractTypeDeclaration) {
            AbstractTypeDeclaration atd = (AbstractTypeDeclaration) p;
            JcompType ctype = JcompAst.getJavaType(atd);
            if (jt == ctype) return SumpArgType.THISTYPE;
          }
       }
    }
   
   while (jt.isParameterizedType()) jt = jt.getBaseType();
   
   if (jt.isUndefined()) {
      if (jt.getName().startsWith("javax.")) return JAVA;
      if (jt.getName().startsWith("android.")) return ANDROID;      return SumpArgType.OTHER;
    } 
   
   if (jt.isBinaryType()) {
      if (jt.getName().startsWith("java.")) return JAVA;
      if (jt.getName().startsWith("org.w3c.dom.")) return JAVA;
      if (jt.getName().startsWith("javax.")) return JAVA;
      if (jt.getName().startsWith("javafx.")) return JAVA;
      if (jt.getName().startsWith("android.")) return ANDROID;
      return SumpArgType.JAVA;
    }
   
   return SumpArgType.USERTYPE;
}



}       // end of enum SumpArgType




/* end of SumpArgType.java */

