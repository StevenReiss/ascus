/********************************************************************************/
/*                                                                              */
/*              LidsFinder.java                                                 */
/*                                                                              */
/*      LIbrary Discovery vis Search top level methods                          */
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



package edu.brown.cs.spur.lids;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.ivy.jcomp.JcompTyper;

public class LidsFinder implements LidsConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Set<String> check_imports;
private Set<String> done_imports;
private Set<String> missing_imports;
private LidsMavenFinder maven_finder;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public LidsFinder()
{
   check_imports = new HashSet<>();
   done_imports = new HashSet<>();
   missing_imports = new HashSet<>();
   maven_finder = new LidsMavenFinder();
}


/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/


public boolean addImportPath(String path)
{
   if (path == null) return false;
   if (done_imports.contains(path) || missing_imports.contains(path)) return false;
   if (JcompTyper.isSystemType(path)) return false;
   
   return check_imports.add(path);
}



public void addImportPaths(Collection<String> paths)
{
   for (String s : paths) addImportPath(s);
}



/********************************************************************************/
/*                                                                              */
/*      Working methods                                                         */
/*                                                                              */
/********************************************************************************/

public List<LidsLibrary> findLibraries()
{
   Map<LidsLibrary,Set<String>> covered = new HashMap<>();
   
   for (String s : check_imports) {
      List<LidsLibrary> libs = maven_finder.findLibrariesForImport(s);
      if (libs != null) {
         for (LidsLibrary lib : libs) {
            Set<String> rslt = covered.get(lib);
            if (rslt == null) {
               rslt = new HashSet<>();
               covered.put(lib,rslt);
             }
            rslt.add(s);
          }
       }
    }
 
   // now choose the smallest covering set -- do it greedily
   
   List<LidsLibrary> rslt = new ArrayList<>();
   Set<String> missing = new HashSet<>(check_imports);
   
   while (!covered.isEmpty()) {
      LidsLibrary use = null;
      int max = 0;
      for (Map.Entry<LidsLibrary,Set<String>> ent : covered.entrySet()) {
         if (use == null || ent.getValue().size() > max) {
            max = ent.getValue().size();
            use = ent.getKey();
          }
       }
      if (use == null) break;
      rslt.add(use);
      Set<String> cov = covered.remove(use);
      missing.removeAll(cov);
      done_imports.addAll(cov);
      for (Iterator<LidsLibrary> it = covered.keySet().iterator(); it.hasNext(); ) {
         LidsLibrary lib = it.next();
         Set<String> vals = covered.get(lib);
         vals.removeAll(cov);
         if (vals.isEmpty()) it.remove();
       }
    }
   
   missing_imports.addAll(missing);
   
   check_imports.clear();
   
   return rslt;
}



public Collection<String> getMissiingImports()
{
   return missing_imports;
}


/********************************************************************************/
/*                                                                              */
/*      Create a library from a string                                          */
/*                                                                              */
/********************************************************************************/

public static LidsLibrary createLibrary(String fid)
{
   return new StringLibrary(fid);
}



private static class StringLibrary implements LidsLibrary {

   private String lib_id;
   private String lib_name;
   private String lib_version;
   
   StringLibrary(String id) {
      String [] split = id.split(":");
      lib_id = split[0] + ":" + split[1];
      lib_name = split[1];
      lib_version = split[2];
    }
   
   @Override public String getName()            { return lib_name; }
   @Override public String getVersion()         { return lib_version; }
   @Override public String getId()              { return lib_id; }
   @Override public String getFullId()          { return lib_id + ":" + lib_version; }
   
   @Override public String toString() {
      return lib_id + "@" + lib_name + "@" + lib_version;
    }

}       // end of inner class StringLibrary




}       // end of class LidsFinder




/* end of LidsFinder.java */

