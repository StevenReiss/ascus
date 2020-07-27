/********************************************************************************/
/*										*/
/*		ScrapTest.java							*/
/*										*/
/*	Test cases for search-based abstraction 				*/
/*										*/
/********************************************************************************/
/*	Copyright 2013 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/



package edu.brown.cs.spur.scrap;

import java.io.File;

import org.junit.Test;

import edu.brown.cs.cose.cosecommon.CoseDefaultRequest;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseScopeType;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseSearchEngine;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseSearchType;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.spur.sump.SumpData;
import edu.brown.cs.spur.sump.SumpConstants.SumpFactory;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;

public class ScrapTest implements ScrapConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

public ScrapTest()
{ }



/********************************************************************************/
/*										*/
/*	Basic Method Tests							 */
/*										*/
/********************************************************************************/

@Test
public void methodTest01()
{
   ScrapDriver sd1 = new ScrapDriver("-m","-nr","500","roman","numeral");
   sd1.processAbstractor();
   ScrapDriver sd2 = new ScrapDriver("-m","-nr","500","robots.txt");
   sd2.processAbstractor();
   ScrapDriver sd3 = new ScrapDriver("-m","-nr","500","median","int","array");
   sd3.processAbstractor();
}



/********************************************************************************/
/*										*/
/*	Basic class tests							*/
/*										*/
/********************************************************************************/

@Test
public void classTest01()
{
   ScrapDriver sd4 = new ScrapDriver("-c","-nr","500","complex","number","imaginary");
   sd4.processAbstractor();

   ScrapDriver sd3 = new ScrapDriver("-c","-nr","500","union","find","disjoint",
	 "-t","lookup");
   sd3.processAbstractor();

   ScrapDriver sd1 = new ScrapDriver("-c","-nr","500","web","server","embedded",
	"-t","url","uri","application","property","port","http","https","ftp","routing",
	    "callback");
   sd1.processAbstractor();

   ScrapDriver sd2 = new ScrapDriver("-c","-nr","500","robots.txt","robots",
	"-t","url","uri","robots");
   sd2.processAbstractor();

   ScrapDriver sd5 = new ScrapDriver("-c","-nr","500","pie","chart","swing",
	 "-t","graphics","jpanel","jcomponent");
   sd5.processAbstractor();
}



/********************************************************************************/
/*										*/
/*	Basic Package Tests							*/
/*										*/
/********************************************************************************/

@Test
public void packageTest01()
{
   ScrapDriver sd1 = new ScrapDriver("-pu","-nr","500","-nt","8",
	 "-REPO","@embedded","@web","server",
	 "-t","url","uri","application","property","port","http","https","ftp","routing",
	 "callback","request","response");

   sd1.processAbstractor();

   ScrapDriver sd2 = new ScrapDriver("-puembe","-nr","500","-nt","8",
	 "-REPO","contact","@management",                                       "-t","address","name","phone","mail");
   sd2.processAbstractor();
}


@Test
public void packageTest02()
{
   ScrapDriver sd3 = new ScrapDriver("-pu","-nr","500","-REPO","checkers","game","player",
	 "-t","swing","ai","heuristics","board","red","black","king","jump","move");

   sd3.processAbstractor();
}


@Test
public void packageTest03()
{
   ScrapDriver sd1 = new ScrapDriver("-pu","-nr","500","-nt","8",
	 "-source",
	 "GITREPO:https://github.com/zuojisi/SimpleWebServer/blob/7a5209a86b6c8a13c7d5dc1370c9f16462d5d5b5/SimpleWebServer/src/com/pupfly/SimpleWebServer/SimpleWebServerActivity.java",
	 "-REPO","@embedded","@web","server",
	 "-t","url","uri","application","property","port","http","https","ftp","routing",
	 "callback","request","response");

   sd1.processAbstractor();
}



@Test
public void packageTest04()
{
   ScrapDriver sd1 = new ScrapDriver("-pu","-nr","500","-nt","8",
	 "-GITHUB","@embedded","@web","server",
	 "-t","url","uri","application","property","port","http","https","ftp","routing",
	 "callback","request","response");

   sd1.processAbstractor();
}



/********************************************************************************/
/*										*/
/*	Loading tests								*/
/*										*/
/********************************************************************************/

@Test
public void loadTest01()
{
   File f = new File("/research/people/spr/spur/scrap/src/test01.ascus");
   SumpModel mdl = SumpFactory.loadModel(f);
   IvyXmlWriter xw = new IvyXmlWriter();
   mdl.outputXml(xw);
   System.err.println("RESULT:\n" + xw.toString());

   f = new File("/research/people/spr/spur/scrap/src/test02.ascus");
   mdl = SumpFactory.loadModel(f);
   xw = new IvyXmlWriter();
   mdl.outputXml(xw);
   System.err.println("RESULT:\n" + xw.toString());
}



/********************************************************************************/
/*										*/
/*	Candidate building tests						*/
/*										*/
/********************************************************************************/

@Test
public void candidateTest01()
{
   candidateTest("test04.ascus");
   candidateTest("test05.ascus");
   candidateTest("test06.ascus");
   candidateTest("test07.ascus");
   candidateTest("test10.ascus");
   candidateTest("test11.ascus");
   candidateTest("test12.ascus");
   candidateTest("test13.ascus");
   candidateTest("test14.ascus");
   candidateTest("test15.ascus");
   candidateTest("test16.ascus");
   candidateTest("test17.ascus");
   candidateTest("test18.ascus");
}



@Test
public void candidateTest02()
{
   File f = new File("/research/people/spr/spur/scrap/src/test14.ascus");
   System.err.println("==============================");
   System.err.println("START WORK ON " + f);
   System.err.println("==============================");
   SumpModel mdl = SumpFactory.loadModel(f);
   SumpData sd = mdl.getModelData();
   CoseDefaultRequest cdr = (CoseDefaultRequest) sd.getCoseRequest();
   cdr.setCoseScopeType(CoseScopeType.PACKAGE_USED);
   cdr.setCoseSearchType(CoseSearchType.PACKAGE);
   cdr.setSearchEngine(CoseSearchEngine.GITREPO);
   // cdr.setMaxPackageFiles(500);
   cdr.setNumberOfResults(500);
   cdr.setNumberOfThreads(8);

   cdr.setNumberOfThreads(1);
   cdr.addSpecificSource(sd.getSources());

   // cdr.addSpecificSource(
	 // "GITREPO:https://github.com/kasra-sh/PicoHTTPd/blob/d1f5426a1cde4857fde7247ed77a680567b1e135/src/main/java/ir/kasra_sh/picohttpd/http/response/ResponseString.java");

   ScrapDriver driver = new ScrapDriver(sd);
   driver.processBuildCandidates(mdl);
}



private void candidateTest(String name)
{
   try {
      File f = new File("/research/people/spr/spur/scrap/src/" + name);
      System.err.println("==============================");
      System.err.println("START WORK ON " + f);
      System.err.println("==============================");
      SumpModel mdl = SumpFactory.loadModel(f);
      SumpData sd = mdl.getModelData();
      CoseDefaultRequest cdr = (CoseDefaultRequest) sd.getCoseRequest();
      cdr.setCoseScopeType(CoseScopeType.PACKAGE_USED);
      cdr.setCoseSearchType(CoseSearchType.PACKAGE);
      cdr.setSearchEngine(CoseSearchEngine.GITREPO);
      cdr.setNumberOfResults(500);
      cdr.setNumberOfThreads(8);
      // cdr.setNumberOfThreads(1);

      ScrapDriver driver = new ScrapDriver(sd);
      driver.processBuildCandidates(mdl);
    }
   catch (Throwable t) {
      t.printStackTrace();
      System.err.println("Problem running candidate test: " + t);
    }
}



}	// end of class ScrapTest




/* end of ScrapTest.java */

