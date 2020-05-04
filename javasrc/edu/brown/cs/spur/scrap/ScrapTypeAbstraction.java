/********************************************************************************/
/*                                                                              */
/*              ScrapTypeAbstraction.java                                       */
/*                                                                              */
/*      Abstraction representing a type (argument, return, ...)                 */
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



package edu.brown.cs.spur.scrap;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;

import edu.brown.cs.spur.sump.SumpArgType;

class ScrapTypeAbstraction implements ScrapConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SumpArgType arg_type; 
private Map<String,JcompType> base_types;
private int use_count;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ScrapTypeAbstraction(JcompProject proj,JcompType jt,ASTNode n)
 {
   JcompTyper typer = proj.getResolveTyper();
   if (jt == null) jt = typer.findSystemType("void");
   arg_type = SumpArgType.computeArgType(typer,jt,n);
   base_types = new HashMap<>();
   base_types.put(jt.getName(),jt);
   use_count = 1;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

SumpArgType getArgType()                         { return arg_type; }



boolean isEquivalent(ScrapTypeAbstraction sta)
{
   // can be more sophisticated, especially for OTHER
   
   return arg_type == sta.arg_type;
}



/********************************************************************************/
/*                                                                              */
/*      Merge methods                                                           */
/*                                                                              */
/********************************************************************************/

void mergeWith(ScrapTypeAbstraction ma) 
{
   use_count += ma.use_count;
   base_types.putAll(ma.base_types);
}


/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

void outputArg(String pfx) 
{
   System.err.println(pfx + "TYPE: " + arg_type);
   System.err.println(pfx + "COUNT: " + use_count);
   System.err.println(pfx + "TYPES:");
   for (String s : base_types.keySet()) {
      System.err.println(pfx + "    " + s);
    }
}


@Override public String toString() {
   return arg_type.toString();
}



}       // end of class ScrapTypeAbstraction




/* end of ScrapTypeAbstraction.java */

