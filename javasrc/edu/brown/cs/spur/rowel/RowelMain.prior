/********************************************************************************/
/*										*/
/*		RowelMain.java							*/
/*										*/
/*	Main program for using test cases as a basis for search 		*/
/*										*/
/********************************************************************************/
/*	Copyright 2016 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2016, Brown University, Providence, RI.				 *
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




package edu.brown.cs.spur.rowel;

import java.util.ArrayList;
import java.util.List;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.s6.common.S6Request;
import edu.brown.cs.s6.common.S6SolutionSet;
import edu.brown.cs.s6.engine.EngineMain;

import org.w3c.dom.Element;


public class RowelMain
{



/********************************************************************************/
/*										*/
/*	Main program								*/
/*										*/
/********************************************************************************/

public static void main(String [] args)
{
   RowelMain rm = new RowelMain(args);
   rm.process();
}




/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private String		test_file;
private List<String>	key_words;



/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

RowelMain(String [] args)
{
   test_file = null;
   key_words = new ArrayList<String>();
   scanArgs(args);
}





/********************************************************************************/
/*										*/
/*	Argument processing							*/
/*										*/
/********************************************************************************/

private void scanArgs(String [] args)
{
   test_file = null;

   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-t") && i+1 < args.length) {              // -t <testfile>
	 if (test_file == null) test_file = args[++i];
	 else badArgs();
       }
      else if (args[i].startsWith("-")) {
	 badArgs();
       }
      else {
	 key_words.add(args[i]);
       }
    }
}




private void badArgs()
{
   System.err.println("SPUR: rowel -t <test file> key key ...");
   System.exit(1);
}



/********************************************************************************/
/*										*/
/*	Processing methods							*/
/*										*/
/********************************************************************************/

private void process()
{
   String cnts = buildRequest();
   Element xml = IvyXml.convertStringToXml(cnts);
   EngineMain eng = new EngineMain(new String[] { "-DEBUG", "-thread", "1" } );
   try {
      S6Request.Search srch = eng.getFactory().createSearchRequest(eng,xml);
      S6SolutionSet sset = eng.getFactory().createSolutionSet(srch);
      eng.getFactory().getInitialSolutions(sset);
      IvyXmlWriter xw = new IvyXmlWriter("/research/people/spr/spur/samples/out.xml");
      sset.output(xw);
      xw.close();
    }
   catch (Throwable t) {
      System.err.println("SPUR: Problem processing request: " + t);
      t.printStackTrace();
    }
}



private String buildRequest()
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("SEARCH");
   xw.field("WHAT","PACKAGE");
   xw.field("FORMAT","NONE");
   xw.field("SCOPE","PACKAGE");
   xw.field("LOCAL",false);
   xw.field("REMOTE",true);
   xw.field("GITZIP",true);
   xw.begin("SIGNATURE");
   xw.begin("PACKAGE");
   xw.field("NAME","edu.brown.cs.spur.testing");
   xw.begin("CLASS");
   xw.field("NAME","TestingClass");
   xw.end("CLASS");
   xw.end("PACKAGE");
   xw.end("SIGNATURE");
   xw.begin("TESTS");
   xw.begin("TESTCASE");
   xw.field("NAME","testDummy");
   xw.field("TYPE","USERCODE");
   xw.cdataElement("CODE","{ }");
   xw.end("TESTCASE");
   xw.end("TESTS");
   xw.begin("KEYWORDS");
   for (String s : key_words) {
      xw.textElement("KEYWORD",s);
    }
   xw.textElement("KEYWORD","org.junit");
   xw.textElement("KEYWORD","test");
   xw.end("KEYWORDS");

   if ("x".equals("y")) {
      xw.begin("SOURCES");
      xw.begin("SOURCE");
      xw.field("USE",true);
      xw.text("GITZIP:http://GITZIP/leobispo/tinyhttp#/src/test/java/br/com/is/http/server/HTTPTest.java");
      xw.end("SOURCE");
      xw.end("SOURCES");
    }



	 xw.end("SEARCH");
   xw.close();

   return xw.toString();
}

}	// end of class RowelMain




/* end of RowelMain.java */
