/********************************************************************************/
/*                                                                              */
/*              ScrapTestFinder.java                                            */
/*                                                                              */
/*      Search for test cases                                                   */
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



package edu.brown.cs.spur.scrap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.brown.cs.cose.cosecommon.CoseMaster;
import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseScopeType;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseSearchEngine;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseSearchType;
import edu.brown.cs.ivy.file.IvyLog;

class ScrapTestFinder implements ScrapConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CoseResult      base_result;
private CoseRequest     base_request;
private CoseRequest     test_request;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ScrapTestFinder(CoseRequest req,CoseResult base)
{
   base_request = req;
   base_result = base;
   
   test_request = setupTestRequest();
}



/********************************************************************************/
/*                                                                              */
/*      Find the set of related (unfiltered) tests                              */
/*                                                                              */
/********************************************************************************/

CoseRequest getTestRequest()
{
   return test_request;
}



List<CoseResult> getTestResults() 
{
   
   CoseMaster master = CoseMaster.createMaster(test_request);
   ScrapResultSet tests = new ScrapResultSet();
   try {
      master.computeSearchResults(tests);
    }
   catch (Throwable t) {
      IvyLog.logE("PROBLEM GETTING TEST RESULTS: " + t,t);
    }
   
   List<CoseResult> rslt = tests.getResults();
   for (Iterator<CoseResult> it = rslt.iterator(); it.hasNext(); ) {
      CoseResult cr = it.next();
      String txt = cr.getEditText();
      boolean match = false;
      for (String s : base_result.getPackages()) {
         if (txt.contains("package " + s) || txt.contains("import " + s)) {
            match = true;
            break;
          }
       }
      if (!match) it.remove();
    }
   
   return rslt;
}



private CoseRequest setupTestRequest()
{
   ScrapRequest req = new ScrapRequest();
   for (CoseSearchEngine seng : base_request.getEngines()) {
      if (seng == CoseSearchEngine.GITREPO) seng = CoseSearchEngine.GITHUB;
      req.setSearchEngine(seng);
    }
   req.setCoseSearchType(CoseSearchType.TESTCLASS);
   req.setCoseScopeType(CoseScopeType.FILE);
   req.setProjectId(base_result.getSource().getProjectId());
   Set<String> done = new HashSet<>();
   for (String s : base_result.getPackages()) {
      if (done.add(s)) {
         req.addKeywordSet("package " + s,"junit","test");
         req.addKeywordSet("import " + s,"junit","test"); 
       }
    }
  
   req.setDoDebug(true);
   int n = done.size()*50;
   n = Math.min(n,req.getNumberOfResults());
   req.setNumberOfResults(n);
 
   return req;
}




}       // end of class ScrapTestFinder




/* end of ScrapTestFinder.java */

