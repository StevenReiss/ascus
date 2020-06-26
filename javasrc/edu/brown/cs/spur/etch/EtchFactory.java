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
   
   EtchTransformFixPackage fixpackage = new EtchTransformFixPackage(work,namemap);
   
   work = fixpackage.transform(work,srcmdl,target_model);
   
   EtchTransformInnerClassStatic innerstatic = new EtchTransformInnerClassStatic(namemap);
   
   work = innerstatic.transform(work,srcmdl,target_model);
   
   EtchTransformInnerClass inner = new EtchTransformInnerClass(namemap);
   
   work = inner.transform(work,srcmdl,target_model);
   
   EtchTransformRename renamer = new EtchTransformRename(namemap);
   
   work = renamer.transform(work,srcmdl,target_model);
   
   // need to handle changes to field types
   
   EtchTransformFixReturns returnfix = new EtchTransformFixReturns(namemap);
   work = returnfix.transform(work,srcmdl,target_model);
   
   EtchTransformFixParameters paramfix = new EtchTransformFixParameters(namemap);
   work = paramfix.transform(work,srcmdl,target_model);
   
   EtchTransformFixCalls callfix = new EtchTransformFixCalls(namemap);
   work = callfix.transform(work,srcmdl,target_model);
   
   EtchTransformAddMissing addmissing = new EtchTransformAddMissing(namemap);
   work = addmissing.transform(work,srcmdl,target_model);
   
   return work;
}



/********************************************************************************/
/*                                                                              */
/*      Adapt test code files                                                   */
/*                                                                              */
/********************************************************************************/

public CoseResult fixTests(CoseResult orig,SumpModel srcmdl,Map<String,String> namemap)
{
   CoseResult work = orig;
   
   EtchTransformFixPackage fixpackage = new EtchTransformFixPackage(work,namemap);
   
   work = fixpackage.transform(work,srcmdl,target_model);
  
   // need to reorder parameters in calls
   // need to handle changes to field types
   // need to handle changes to parameter types
   
   EtchTransformRename renamer = new EtchTransformRename(namemap);
   
   work = renamer.transform(work,srcmdl,target_model);
   
   EtchTransformFixCalls callfix = new EtchTransformFixCalls(namemap);
   work = callfix.transform(work,srcmdl,target_model);
   
   // need to remove code that references undefined items
   
   return work;
}






}       // end of class EtchFactory




/* end of EtchFactory.java */

