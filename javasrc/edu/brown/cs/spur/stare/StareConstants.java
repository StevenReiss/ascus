/********************************************************************************/
/*                                                                              */
/*              StareConstants.java                                             */
/*                                                                              */
/*      Search Transformations for Automatic Repair and Editing constants       */
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



package edu.brown.cs.spur.stare;

import java.util.Map;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;

public interface StareConstants
{

String TEST_BASE = "/ws/volfred/tmp/ascus";
String TEST_DIRECTORY = "Ascus_#";

String TEMPLATE_FILE = "$(SPUR)/resources/stare.pom.vel";
String TEMPLATE_RESOURCE = "resources/stare.pom.vel";
      





/********************************************************************************/
/*                                                                              */
/*      Representation of a solution                                            */
/*                                                                              */
/********************************************************************************/

interface StareCandidateSolution {

   SumpModel getModel();
   SumpModel getPatternModel();
   CoseResult getCoseResult();
   Map<String,String> getNameMap();
   CoseResult getLocalTestResult();
   CoseResult getGlobalTestResult();

}       // end of inner interface StareSolution


interface StareSolution extends StareCandidateSolution {

   boolean generateCode();
   
}




}       // end of interface StareConstants




/* end of StareConstants.java */

