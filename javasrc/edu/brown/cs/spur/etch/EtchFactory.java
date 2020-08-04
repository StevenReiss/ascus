/********************************************************************************/
/*                                                                              */
/*              EtchFactory.java                                                */
/*                                                                              */
/*      Main entry point to apply gransformations                               */
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



package edu.brown.cs.spur.etch;

import java.util.Map;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;
import edu.brown.cs.spur.sump.SumpParameters;


public class EtchFactory implements EtchConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SumpModel target_model;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public EtchFactory(SumpModel target)
{
   target_model = target;
}



/********************************************************************************/
/*                                                                              */
/*      Adapt code files                                                        */
/*                                                                              */
/********************************************************************************/

public CoseResult fixCode(CoseResult orig,SumpModel srcmdl,Map<String,String> namemap)
{
   CoseResult work = orig;
   SumpParameters sp = target_model.getModelData().getParameters();
   
   EtchTransformAddMissing fixmissing = new EtchTransformAddMissing(namemap); 
   fixmissing.updateMappings(work,srcmdl,target_model);
   
   EtchTransformFixPackage fixpackage = new EtchTransformFixPackage(work,namemap);
   
   work = fixpackage.transform(work,null,srcmdl,target_model);
   
   EtchTransformInnerStatic innerstatic = new EtchTransformInnerStatic(namemap);
   
   work = innerstatic.transform(work,null,srcmdl,target_model);
   
   EtchTransformInnerClass inner = new EtchTransformInnerClass(namemap);
   
   work = inner.transform(work,null,srcmdl,target_model);
   
   EtchTransformRename renamer = new EtchTransformRename(namemap);
   
   work = renamer.transform(work,null,srcmdl,target_model);
   
   EtchTransformFieldFix fieldfix = new EtchTransformFieldFix(namemap);
   work = fieldfix.transform(work,null,srcmdl,target_model);
   
   EtchTransformReturnFix returnfix = new EtchTransformReturnFix(namemap);
   work = returnfix.transform(work,null,srcmdl,target_model);
   
   EtchTransformParameterFix paramfix = new EtchTransformParameterFix(namemap);
   work = paramfix.transform(work,null,srcmdl,target_model);
   
   EtchTransformCallFix callfix = new EtchTransformCallFix(namemap);
   work = callfix.transform(work,null,srcmdl,target_model);
   
   EtchTransformFieldUseFix fieldusefix = new EtchTransformFieldUseFix(namemap);
   work = fieldusefix.transform(work,null,srcmdl,target_model);
   
   EtchTransformAddMissing addmissing = new EtchTransformAddMissing(namemap);
   work = addmissing.transform(work,null,srcmdl,target_model);
   
   if (sp.getRemoveUnused()) {
      EtchTransformRemoveUnused unused = new EtchTransformRemoveUnused(namemap,false);
      work = unused.transform(work,null,srcmdl,target_model);    
    }
   
   EtchTransformConventions conventions = new EtchTransformConventions(namemap);
   work = conventions.transform(work,null,srcmdl,target_model);
   
   return work;
}



/********************************************************************************/
/*                                                                              */
/*      Adapt test code files                                                   */
/*                                                                              */
/********************************************************************************/

public CoseResult fixLocalTests(CoseResult test,CoseResult base,
      SumpModel srcmdl,Map<String,String> namemap)
{
   CoseResult work = test;
   
   EtchTransformFixPackage fixpackage = new EtchTransformFixPackage(work,namemap);
   work = fixpackage.transform(work,base,srcmdl,target_model); 
  
   EtchTransformPostRename renamer = new EtchTransformPostRename(namemap);
   work = renamer.transform(work,base,srcmdl,target_model); 
   
   EtchTransformCallFix callfix = new EtchTransformCallFix(namemap);
   work = callfix.transform(work,base,srcmdl,target_model);
   
   EtchTransformFieldUseFix fieldusefix = new EtchTransformFieldUseFix(namemap);
   work = fieldusefix.transform(work,base,srcmdl,target_model);
   
   EtchTransformRemoveUndef undef = new EtchTransformRemoveUndef(namemap);
   work = undef.transform(work,base,srcmdl,target_model);  
   
   EtchTransformRemoveUnused unused = new EtchTransformRemoveUnused(namemap,true);
   work = unused.transform(work,base,srcmdl,target_model);  
   
   EtchTransformConventions conventions = new EtchTransformConventions(namemap);
   work = conventions.transform(work,base,srcmdl,target_model); 
   
   return work;
}



public CoseResult fixGlobalTests(CoseResult test,CoseResult base,SumpModel srcmdl,Map<String,String> namemap)
{
   CoseResult work = test;
   
   EtchTransformFixPackage fixpackage = new EtchTransformFixPackage(work,namemap);
   
   work = fixpackage.transform(work,base,srcmdl,target_model); 
   
   EtchTransformPostRename renamer = new EtchTransformPostRename(namemap);
   work = renamer.transform(work,base,srcmdl,target_model); 
   
   EtchTransformCallFix callfix = new EtchTransformCallFix(namemap);
   work = callfix.transform(work,base,srcmdl,target_model);
   
   EtchTransformFieldUseFix fieldusefix = new EtchTransformFieldUseFix(namemap);
   work = fieldusefix.transform(work,base,srcmdl,target_model);
   
   EtchTransformRemoveUndef undef = new EtchTransformRemoveUndef(namemap);
   work = undef.transform(work,base,srcmdl,target_model);  
   
   EtchTransformRemoveLocal locals = new EtchTransformRemoveLocal(namemap,base);
   work = locals.transform(work,base,srcmdl,target_model);  
   
   EtchTransformRemoveUnused unused = new EtchTransformRemoveUnused(namemap,true);
   work = unused.transform(work,base,srcmdl,target_model);  
   
   EtchTransformConventions conventions = new EtchTransformConventions(namemap);
   work = conventions.transform(work,base,srcmdl,target_model); 
   
   return work;
}






}       // end of class EtchFactory




/* end of EtchFactory.java */

