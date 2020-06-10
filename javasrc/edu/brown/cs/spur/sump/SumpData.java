/********************************************************************************/
/*                                                                              */
/*              SumpData.java                                                   */
/*                                                                              */
/*      Data for output Java and UML                                            */
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseSource;
import edu.brown.cs.spur.lids.LidsFinder;
import edu.brown.cs.spur.lids.LidsConstants.LidsLibrary;

public class SumpData implements SumpConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          model_name;
private String          model_source;
private Set<LidsLibrary> library_set;
private Set<String>     source_set;
private Set<String>     import_set;
private Stack<String>   cur_class;
private Stack<Boolean>  is_interface;
private CoseRequest     cose_request;
private Set<String>     missing_imports;
private SumpParameters  sump_parameters;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SumpData(CoseRequest req,CoseResult rslt,SumpParameters sp)
{
   model_name = null;
   model_source = null;
   library_set = new HashSet<>();
   source_set = new HashSet<>();
   missing_imports = new HashSet<>();
   import_set = new TreeSet<>();
   cur_class = new Stack<>();
   is_interface = new Stack<>();
   cose_request = req;
   sump_parameters = sp;
   if (sp == null) sump_parameters = new SumpParameters();
   
   if (rslt != null) {
      addSource(rslt.getSource());
      String nm = rslt.getSource().getDisplayName();
      int idx = nm.lastIndexOf("/");
      if (idx > 0) nm = nm.substring(idx+1);
      idx = nm.lastIndexOf(".");
      if (idx > 0) nm = nm.substring(0,idx);
      model_name = nm;
    }
}






/********************************************************************************/
/*                                                                              */
/*      Public Setup Methods                                                    */
/*                                                                              */
/********************************************************************************/

public void addLibrary(LidsLibrary lib)
{
   if (lib == null) return;
   
   library_set.add(lib);
}


public void addLibrary(String lib)
{
   addLibrary(LidsFinder.createLibrary(lib));
}


public void addSource(CoseSource src)
{
   source_set.add(src.getName());
}


public void addSource(String src)
{
   source_set.add(src);
}


public void addMissingImport(String imp)
{
   missing_imports.add(imp);
}


public void setName(String name)
{
   model_name = name;
}


public void setSource(String source)
{
   model_source = source;
}



/********************************************************************************/
/*                                                                              */
/*      Internal access methods                                                 */
/*                                                                              */
/********************************************************************************/

public Collection<LidsLibrary> getLibraries()   { return library_set; }

public Collection<String> getSources()          { return source_set; }

public Collection<String> getMissingImports()   { return missing_imports; }

public CoseRequest getCoseRequest()             { return cose_request; }

public String getName()                         { return model_name; }

public String getSource()                       { return model_source; }

public SumpParameters getParameters()           { return sump_parameters; }


void pushType(String cls,boolean iface) 
{ 
   cur_class.push(cls); 
   is_interface.push(iface);
}

void popType()                                 
{ 
   cur_class.pop();
   is_interface.pop();
}

String getCurrentType()         
{
   if (cur_class.isEmpty()) return null;
   return cur_class.peek();
}

boolean isCurrentInterface()
{
   if (is_interface.isEmpty()) return false;
   return is_interface.peek();
}



/********************************************************************************/
/*                                                                              */
/*      Import managment                                                        */
/*                                                                              */
/********************************************************************************/

void beginImport()
{
   import_set.clear();
}

void addImport(String typ)
{
   // if (CoseConstants.isStandardJavaLibrary(typ)) return;
   int idx = typ.lastIndexOf(".");
   if (idx > 0) {
      String pkg = typ.substring(0,idx);
      if (pkg.equals("java.lang")) return;
    }
   import_set.add(typ);
}


Collection<String> getImports()                 { return import_set; }



}       // end of class SumpData




/* end of SumpData.java */

