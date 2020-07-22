/********************************************************************************/
/*										*/
/*		LidsMavenFinder.java						*/
/*										*/
/*	Maven search interface for Lids 					*/
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



package edu.brown.cs.spur.lids;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
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
import edu.brown.cs.cose.keysearch.KeySearchCache;
import edu.brown.cs.ivy.file.IvyLog;

class LidsMavenFinder implements LidsConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private static Map<String,List<LidsLibrary>> found_libs = new HashMap<>();
private static Map<String,MavenLibrary> known_libs = new HashMap<>();
private static KeySearchCache cose_cache = KeySearchCache.getCache();
private static Map<String,List<LidsLibrary>> lib_byname = new HashMap<>();

private static final int MAX_RESULTS = 4000;

private static String SEARCH_PFX =
   "https://search.maven.org/solrsearch/select?rows=100&wt=json&q=";





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

LidsMavenFinder()
{
}



/********************************************************************************/
/*										*/
/*	Find library for a path 						*/
/*										*/
/********************************************************************************/

List<LidsLibrary> findLibrariesForImport(String imp,Set<String> grps)
{
   for (int i = 0; ; ++i) {
      if (!imp.contains(".")) break;
      List<LidsLibrary> libs = null;
      if (i == 0) {
	 if (!imp.endsWith(".*")) libs = checkLibrary(imp,true,grps);
       }
      else libs = checkLibrary(imp,false,grps);
      if (libs != null) return libs;
      int idx = imp.lastIndexOf(".");
      if (idx < 0) break;
      imp = imp.substring(0,idx);
    }

   return null;
}



private synchronized List<LidsLibrary> checkLibrary(String path,boolean iscls,Set<String> grps)
{
   if (found_libs.containsKey(path)) return found_libs.get(path);

   // check for related class path and use it if appropriate
   if (iscls) {
      int idx = path.lastIndexOf(".");
      if (idx > 0) {
	 String pkg = path.substring(0,idx+1);
	 String pkg0 = path.substring(0,idx);
	 if (found_libs.containsKey(pkg0)) return found_libs.get(pkg0);

	 for (Map.Entry<String,List<LidsLibrary>> ent : found_libs.entrySet()) {
	    String s = ent.getKey();
	    if (s.startsWith(pkg)) {
	       int idx1 = s.lastIndexOf(".");
	       if (idx1 == idx) {
		  found_libs.put(path,ent.getValue());
		  found_libs.put(pkg0,ent.getValue());
		  return ent.getValue();
		}
	     }
	  }
       }
    }

   List<LidsLibrary> rslt = doMavenSearch(path,grps);

   if (rslt != null && rslt.size() == 0) rslt = null;

   found_libs.put(path,rslt);

   return rslt;
}



private List<LidsLibrary> doMavenSearch(String path,Set<String> grps)
{
   String q = SEARCH_PFX + "fc:%22" + path + "%22";
   q += "%20AND%20p:%22jar%22";

   Map<String,Set<String>> artifacts = new HashMap<>();
   Set<String> related = new HashSet<>();
   Set<String> userrelated = new HashSet<>();
   int nrslt =	1;
   int start = 0;
   while (start < nrslt) {
      String q1 = q;
      if (start > 0)
	 q1 = q1 + "&start=" + start;
      String rslt1 = getMavenResult(q1);
      if (rslt1 == null) break;
      try {
	 JSONObject top = new JSONObject(rslt1);
	 JSONObject resp = top.getJSONObject("response");
	 JSONArray arr = resp.getJSONArray("docs");
	 if (start == 0) {
	    nrslt = resp.getInt("numFound");
	    if (nrslt > MAX_RESULTS) nrslt = MAX_RESULTS;
	  }
	 start += arr.length();
	 if (arr != null) {
	    for (int i = 0; i < arr.length(); ++i) {
	       JSONObject doc = arr.getJSONObject(i);
	       String type = doc.getString("p");
	       if (type == null || !type.equals("jar")) continue;
	       String art = doc.getString("a");
	       String grp = doc.getString("g");
	       // String ver = doc.getString("v");
	       if (CoseConstants.isRelatedPackage(grp,path)) related.add(grp);
               if (grps != null && grps.contains(grp)) userrelated.add(grp);
	       Set<String> al = artifacts.get(grp);
	       if (al == null) {
		  al = new HashSet<>();
		  artifacts.put(grp,al);
		}
	       al.add(art);
	     }
	  }
       }
      catch (JSONException e) {
	 IvyLog.logE("Problem with maven result: " + e);
       }
    }
   List<LidsLibrary> rslt = new ArrayList<>();
   if (!userrelated.isEmpty()) {
      related = userrelated;
    }
   if (!related.isEmpty()) {
      for (Iterator<String> it = artifacts.keySet().iterator(); it.hasNext(); ) {
	 String art = it.next();
	 if (!related.contains(art)) it.remove();
       }
    }

   for (Map.Entry<String,Set<String>> ent : artifacts.entrySet()) {
      List<LidsLibrary> alllibs = getLibraryFromName(ent.getKey());
      for (LidsLibrary ll : alllibs) {
	 if (ent.getValue().contains(ll.getName())) {
	    rslt.add(ll);
	  }
       }
    }

   if (rslt.isEmpty()) rslt = null;

   return rslt;
}



private List<LidsLibrary> getLibraryFromName(String name)
{
   List<LidsLibrary> rslt = lib_byname.get(name);
   if (rslt != null) return rslt;

   rslt = new ArrayList<>();
   String q1 = SEARCH_PFX + "g:%22" + name + "%22";
   String rslt2 = getMavenResult(q1);
   if (rslt2 !=  null) {
      JSONObject top = new JSONObject(rslt2);
      JSONObject resp = top.getJSONObject("response");
      JSONArray arr = resp.optJSONArray("docs");
      if (arr != null) {
	 for (int i = 0; i < arr.length(); ++i) {
	    JSONObject doc = arr.getJSONObject(i);
	    String type = doc.getString("p");
	    if (type == null || !type.equals("jar")) continue;
	    String id = doc.getString("id");
	    MavenLibrary fnd = known_libs.get(id);
	    if (fnd == null) {
	       fnd = new MavenLibrary(doc);
	       known_libs.put(id,fnd);
	     }
	    rslt.add(fnd);
	  }
       }
    }

   lib_byname.put(name,rslt);

   return rslt;
}



private String getMavenResult(String q)
{
   ByteArrayOutputStream baos = new ByteArrayOutputStream();
   URL url = null;

   long delay = 30000;
   for ( ; ; ) {
      try {
	 url = new URL(q);
	 InputStream br = cose_cache.getInputStream(url,true,false);
	 byte [] buf = new byte[8192];
	 for ( ; ; ) {
	    int ln = br.read(buf);
	    if (ln <= 0) break;
	    baos.write(buf,0,ln);
	  }
	 br.close();
	 String rslt = baos.toString("UTF-8");
	 if (rslt.length() == 0) return null;
	 if (!rslt.endsWith("\n")) rslt += "\n";
	 if (rslt.length() < 10) return null;
	 return rslt;
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
	 cose_cache.setCacheContents(url,"\n");
	 IvyLog.logI("LIDS","Problem getting MAVEN data: " + e);
	 break;
       }
    }

   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Check a library for an import                                           */
/*                                                                              */
/********************************************************************************/

boolean libraryWorksFor(LidsLibrary ll,String imp)
{
   if (imp.endsWith(".*")) {
      int idx = imp.lastIndexOf(".");
      imp = imp.substring(0,idx);
    }
         
   for ( ; ; ) {
      if (!imp.contains(".")) break;
      boolean rslt = checkWorks(ll,imp);
      if (rslt) return true;
      
      // handle inner class by going to outer class
      int idx = imp.lastIndexOf(".");
      if (idx < 0) break;
      imp = imp.substring(0,idx);
      int idx1 = imp.lastIndexOf(".");
      if (idx1 < 0) break;
      if (!Character.isUpperCase(imp.charAt(idx1+1))) break;
    }
   
   return false;
}


private boolean checkWorks(LidsLibrary ll,String path)
{
   String q = SEARCH_PFX + "fc:%22" + path + "%22";
   q += "%20AND%20p:%22jar%22";
   q += "%20AND%20g:%22" + ll.getGroup() + "%22";
   q += "%20AND%20a:%22" + ll.getName() + "%22";
   q += "%20AND%20v:%22" + ll.getVersion() + "%22"; 
   String rslt = getMavenResult(q);
   if (rslt == null) return false;
   try {
      JSONObject top = new JSONObject(rslt);
      JSONObject resp = top.getJSONObject("response");
      int fnd = resp.getInt("numFound");
      if (fnd > 0) return true;
    }
   catch (JSONException e) {
      IvyLog.logE("Problem with maven result: " + e);
    } 
      
   return false;
}

/********************************************************************************/
/*										*/
/*	Library implementation for Maven results				*/
/*										*/
/********************************************************************************/

private static class MavenLibrary extends  LidsLibrary {

   private String lib_id;
   private String lib_name;
   private long lib_timestamp;
   private String lib_version;

   MavenLibrary(JSONObject obj) {
      lib_id = obj.getString("id");
      lib_name = obj.getString("a");
      lib_timestamp = obj.getLong("timestamp");
      lib_version = obj.optString("v",null);
      lib_version = obj.optString("latestVersion",lib_version);
    }

   @Override public String getName()		{ return lib_name; }
   @Override public String getVersion() 	{ return lib_version; }
   @Override public String getId()		{ return lib_id; }
   @Override public String getFullId()		{ return lib_id + ":" + lib_version; }
   
   @Override public void setVersion(String v)   { lib_version = v; }

   @Override public String toString() {
      return lib_id + "@" + lib_name + "@" + lib_timestamp + "@" + lib_version;
    }

}	// end of inner class MavenLibrary



}	// end of class LidsMavenFinder




/* end of LidsMavenFinder.java */

