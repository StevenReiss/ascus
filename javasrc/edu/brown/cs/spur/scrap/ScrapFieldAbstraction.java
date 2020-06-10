/********************************************************************************/
/*                                                                              */
/*              ScrapFieldAbstraction.java                                      */
/*                                                                              */
/*      description of class                                                    */
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

import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompSymbol;

class ScrapFieldAbstraction extends ScrapAbstraction
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ScrapTypeAbstraction    field_type;
private boolean                 is_static;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ScrapFieldAbstraction(ScrapAbstractor abs,CoseResult cr,VariableDeclarationFragment vdf)
{
   super(abs,cr,vdf);
   field_type = null;
   initialize(cr,vdf);
}


ScrapFieldAbstraction(ScrapAbstractor abs,CoseResult cr,String name,ScrapTypeAbstraction typ,boolean st)
{
   super(abs,cr,null);
   field_type = typ;
   is_static = st;
   name_set.add(name);
}


private void initialize(CoseResult cr,VariableDeclarationFragment vdf)
{
   JcompProject oproj = JcompAst.getProject(vdf);
   JcompProject proj = oproj;
   if (proj == null) {
      proj = getResolvedAst(vdf);
      JcompAst.setProject(vdf,proj);
      if (proj == null) return;
    }
   
   try {
      JcompSymbol js = JcompAst.getDefinition(vdf);
      if (!js.isFieldSymbol()) return;
      field_type = new ScrapTypeAbstraction(proj,js.getType(),vdf);
      name_set.add(js.getName());
      is_static = js.isStatic();
    }
   finally {
      if (oproj == null) {
         jcomp_main.freeProject(proj);
         JcompAst.setProject(vdf,null);
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override AbstractionType getAbstractionType()
{
   return AbstractionType.FIELD;
}


ScrapTypeAbstraction getFieldType()
{
   return field_type;
}



/********************************************************************************/
/*                                                                              */
/*      Merge methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public boolean mergeWith(ScrapAbstraction abs)
{
   if (!(abs instanceof ScrapFieldAbstraction)) return false;
   
   ScrapFieldAbstraction sfa = (ScrapFieldAbstraction) abs;
   if (!sfa.field_type.isEquivalent(field_type)) return false;
   if (is_static != sfa.is_static) return false;
   
   field_type.mergeWith(sfa.field_type);
   superMergeWith(sfa);
   return true;
}




/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/



@Override void outputAbstraction()
{
   System.err.println("FIELD ABSTRACTION FOR " + getResultCount() + " Results");
   System.err.println("    TYPE:");
   field_type.outputArg("\t");
}





}       // end of class ScrapFieldAbstraction




/* end of ScrapFieldAbstraction.java */

