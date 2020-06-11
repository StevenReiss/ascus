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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.brown.cs.cose.cosecommon.CoseConstants;
import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.keysearch.KeySearchCache;
import edu.brown.cs.ivy.file.IvyLog;

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
private CoseResult      base_result;

private static KeySearchCache cose_cache = KeySearchCache.getCache();



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public LidsFinder(CoseResult cr)
{
   check_imports = new HashSet<>();
   done_imports = new HashSet<>();
   missing_imports = new HashSet<>();
   maven_finder = new LidsMavenFinder();
   base_result = cr;
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
   if (CoseConstants.isStandardJavaLibrary(path)) return false;
   // if (JcompTyper.isSystemType(path)) return false;
   
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
   findMavenFiles();
   findGradleFiles();
   
   Map<LidsLibrary,Set<String>> covered = findMavenLibraries();
   
   // now choose the smallest covering set -- do it greedily
   
   List<LidsLibrary> rslt = new ArrayList<>();
   Set<String> missing = new HashSet<>(check_imports);
   
   while (!covered.isEmpty()) {
      LidsLibrary use = null;
      int max = 0;
      // choose set that covers the most elements
      for (Map.Entry<LidsLibrary,Set<String>> ent : covered.entrySet()) {
         if (use == null || ent.getValue().size() > max) {
            max = ent.getValue().size();
            use = ent.getKey();
          }
         else if (ent.getValue().size() == max && 
               ent.getKey().getName().compareTo(use.getName()) > 0) {
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



public Collection<String> getMissingImports()
{
   return missing_imports;
}


/********************************************************************************/
/*                                                                              */
/*      Worker methods                                                          */
/*                                                                              */
/********************************************************************************/

private Map<LidsLibrary,Set<String>> findMavenLibraries()
{
   
   Map<LidsLibrary,Set<String>> covered = new HashMap<>();
   
   for (String s : check_imports) {
      IvyLog.logD("LIDS","Look for library for " + s);
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
   
   return covered;
}



private void findMavenFiles()
{
   if (base_result == null) return;
   if (!base_result.getSource().getName().startsWith("GIT")) return;
   if (base_result.getSource().getProjectId() == null) return;
   
   String q = "repo:" + base_result.getSource().getProjectId();
   q += " filename:pom.xml language:\"Maven POM\"";
   List<URI> rslt = getGithubResult(q);
   if (rslt == null) return;
}




private void findGradleFiles()
{
   if (base_result == null) return;
   if (!base_result.getSource().getName().startsWith("GIT")) return;
   if (base_result.getSource().getProjectId() == null) return;
   
   String q = "repo:" + base_result.getSource().getProjectId();
   q += " filename:build.gradle language:Gradle";
   List<URI> rslt = getGithubResult(q);
   if (rslt == null) return;
}




private List<URI> getGithubResult(String q) 
{
   URL url = null;
   try { 
      URI uri = new URI("https","api.githb.com","/search/code",q,null);
      url = uri.toURL();
    }
   catch (URISyntaxException e) { }
   catch (MalformedURLException e) { }
   if (url == null) return null;
   
   ByteArrayOutputStream baos = new ByteArrayOutputStream();
   
   long delay = 30000;
   for ( ; ; ) {
      try {
         InputStream br = cose_cache.getInputStream(url,true,false);
         byte [] buf = new byte[8192]; 
         for ( ; ; ) {
            int ln = br.read(buf);
            if (ln <= 0) break;
            baos.write(buf,0,ln);
          }
         br.close();
         String rslt = baos.toString("UTF-8");
         if (!rslt.endsWith("\n")) rslt += "\n";
         return decodeGithubResults(rslt);
       }
      catch (Exception e) {
         if (delay < 30000 && e.toString().contains(" 504 ")) {
            try {
               Thread.sleep(delay);
             }
            catch (InterruptedException ex) { }
            delay = 2*delay;
            continue;
          }
         IvyLog.logE("LIDS","Problem getting MAVEN data: " + e);

         break;
       }
    }
   
   return null;
}


private List<URI> decodeGithubResults(String cnts)
{
   List<URI> rslt = new ArrayList<URI>();
   try {
      JSONArray jarr = null;
      if (cnts.startsWith("{")) {
         JSONObject jobj = new JSONObject(cnts);
         jarr = jobj.getJSONArray("items");
       }
      else if (cnts.startsWith("[")) {
         jarr = new JSONArray(cnts);
       }
      else jarr = new JSONArray();
      for (int i = 0; i < jarr.length(); ++i) {
         JSONObject jobj = jarr.getJSONObject(i);
         URI uri2 = convertGithubSearchResults(jobj);
         if (uri2 != null) rslt.add(uri2);
       }
    }
   catch (Exception e) {
      IvyLog.logE("LIDS","Problem parsing github json return",e);
    }
   
   return rslt;
}



protected URI convertGithubSearchResults(JSONObject jobj)
{
   try {
      URI uri2 = new URI(jobj.getString("html_url"));
      return uri2;
    }
   catch (URISyntaxException e) {
      IvyLog.logE("LIDS","BAD URI: " + e);
    }
   catch (JSONException e) {
      IvyLog.logE("LIDS","BAD JSON: " + e);
    }
   
   return null;
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

