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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap; 
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;

import edu.brown.cs.cose.cosecommon.CoseConstants;
import edu.brown.cs.cose.cosecommon.CoseDefaultRequest;
import edu.brown.cs.cose.cosecommon.CoseDefaultResultSet;
import edu.brown.cs.cose.cosecommon.CoseMaster;
import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseScopeType;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseSearchEngine;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseSearchLanguage;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseSearchType;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.xml.IvyXml;

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




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public LidsFinder(CoseResult cr)
{
   check_imports = new TreeSet<>();
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
   Set<LidsLibrary> l1 = findMavenFiles();
   Set<LidsLibrary> l2 = findGradleFiles();
   if (l1 == null) l1 = l2;
   else if (l2 != null) l1.addAll(l2);
   
   List<LidsLibrary> rslt = new ArrayList<>();
   if (l1 != null) {
      addProjectLibraries(l1,rslt);
    }
   
   Map<LidsLibrary,Set<String>> covered = findMavenLibraries(l1);
   Set<String> done = new HashSet<>();
   
   // next if we have specific libraries, use them and removed from needed set
   
   Set<String> missing = new HashSet<>(check_imports);
   
   if (l1 != null && !l1.isEmpty()) {
      for (Iterator<LidsLibrary> it = covered.keySet().iterator(); it.hasNext(); ) {
         LidsLibrary klib = it.next();
         for (LidsLibrary mlib : l1) {
            if (mlib.getId().equals(klib.getId())) {
               rslt.add(mlib);
               Set<String> cov = covered.get(klib);
               it.remove();
               missing.removeAll(cov);
               done.addAll(cov);
               break;
             }
          } 
       }
      
      for (LidsLibrary mlib : l1) {
         List<LidsLibrary> libs = new ArrayList<>(covered.keySet());
         for (LidsLibrary klib : libs) {
            if (mlib.getGroup().equals(klib.getGroup())) {
               if (maven_finder.checkLibraryExists(klib,mlib.getVersion())) {
                  Set<String> cov = covered.remove(klib);
                  klib.setVersion(mlib.getVersion());
                  covered.put(klib,cov);
                }
             }
          }
       }
    }
   
   cleanLibrarys(covered,done);
   
   boolean chng = true;
   while (chng) {
      chng = false;
      for (String s : missing) {
         int bestct = 10;
         int i1 = s.indexOf(".");
         int i2 = s.indexOf(".",i1+1);
         if (i2 > 0) bestct = i2;
         LidsLibrary best = null;
         for (LidsLibrary ll : covered.keySet()) {
            Set<String> use = covered.get(ll);
            if (use.contains(s)) {
               String pfx = ll.getId();
               if (pfx.startsWith("com.google.android") && s.startsWith("android")) {
                  pfx = pfx.substring(11);
                }
               int front = initialMatch(pfx,s);
               if (front > bestct) {
                  bestct = front;
                  best = ll;
                }
             }
          }
         if (best != null) {
            Set<String> cov = covered.get(best);
            done.addAll(cov);
            missing.removeAll(cov);
            rslt.add(best);
            chng = true;
            break;
          }
       }
      if (chng) cleanLibrarys(covered,done);
    }
   
   // now choose the smallest covering set -- do it greedily
   
   while (!covered.isEmpty()) {
      LidsLibrary use = null;
      int max = 0;
      
      // choose set that covers the most elements
      for (Map.Entry<LidsLibrary,Set<String>> ent : covered.entrySet()) {
         if (use == null) {
            max = ent.getValue().size();
            use = ent.getKey();
          }
         else {
            if (ent.getValue().size() > max) {
               max = ent.getValue().size();
               use = ent.getKey();
             }
            else if (ent.getValue().size() == max && 
                  ent.getKey().getName().compareTo(use.getName()) > 0) {
               use = ent.getKey();
             }
          }
       }
      if (use == null) break;
      rslt.add(use);
      Set<String> cov = covered.remove(use);
      missing.removeAll(cov);
      done.addAll(cov);
      cleanLibrarys(covered,done);
    }
   
   missing_imports.addAll(missing);
   
   check_imports.clear();
   done_imports.addAll(done);
   
   return rslt;
}



private void cleanLibrarys(Map<LidsLibrary,Set<String>> covered,Set<String> done) 
{
   for (Iterator<LidsLibrary> it = covered.keySet().iterator(); it.hasNext(); ) {
      LidsLibrary lib = it.next();
      Set<String> vals = covered.get(lib);
      vals.removeAll(done);
      if (vals.isEmpty()) it.remove();
    }
}



private int initialMatch(String s1,String s2) 
{
   int max = Math.min(s1.length(),s2.length());
   for (int i = 0; i < max; ++i) {
      if (s1.charAt(i) != s2.charAt(i)) return i;
    }
   return max;
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

private Map<LidsLibrary,Set<String>> findMavenLibraries(Set<LidsLibrary> userlibs)
{
   Map<LidsLibrary,Set<String>> covered = new LinkedHashMap<>();
   Set<String> grps = null;
   if (userlibs != null) {
      grps = new HashSet<>();
      for (LidsLibrary ll : userlibs) {
         grps.add(ll.getGroup());
         grps.add(ll.getName());
         grps.add(ll.getId());
       }
    }
   
   for (String s : check_imports) {
      IvyLog.logD("LIDS","Look for library for " + s);
      List<LidsLibrary> libs = maven_finder.findLibrariesForImport(s,grps);
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



private Set<LidsLibrary> findMavenFiles()
{
   if (base_result == null) return null;
   if (!base_result.getSource().getName().startsWith("GIT")) return null;
   if (base_result.getSource().getProjectId() == null) return null;
   
   String q1 = "repo:" + base_result.getSource().getProjectId();
   String q2 = "filename:pom.xml";
   String q3 = "language:\"Maven POM\"";
   List<CoseResult> rslt = getGithubResult(q1,q2,q3);
   if (rslt == null) return null;
   Set<LidsLibrary> libs = new HashSet<>();
   for (CoseResult cr : rslt) {
      Element xml = IvyXml.convertStringToXml(cr.getText());
      for (Element deps : IvyXml.children(xml,"dependencies")) {
         for (Element dep : IvyXml.children(deps,"dependency")) {
            String grp = IvyXml.getTextElement(dep,"groupId");
            String aid = IvyXml.getTextElement(dep,"artifactId");
            String ver = IvyXml.getTextElement(dep,"version");
            if (ver == null || ver.startsWith("$")) ver = "LATEST";
            String lnm = grp + ":" + aid + ":" + ver;
            LidsLibrary lib = createLibrary(lnm);
            libs.add(lib);
          }
       }
    }
   return libs;
}




private Set<LidsLibrary> findGradleFiles()
{
   if (base_result == null) return null;
   if (!base_result.getSource().getName().startsWith("GIT")) return null;
   if (base_result.getSource().getProjectId() == null) return null;
   
   String q1 = "repo:" + base_result.getSource().getProjectId();
   String q2 = "filename:build.gradle";
   String q3 = "language:Gradle";
   List<CoseResult> rslt = getGithubResult(q1,q2,q3);
   if (rslt == null) return null;
   Set<String> alllibs = new HashSet<>();
   for (CoseResult cr : rslt) {
      Set<String> libs = getGradleLibraries(cr.getText());
      if (libs != null) alllibs.addAll(libs);
    }
   Set<LidsLibrary> libs = new HashSet<>();
   for (String s : alllibs) {
      LidsLibrary ll = createLibrary(s);
      if (ll.getId() == null) continue;
      libs.add(ll);
    }
   return libs;
}



private Set<String> getGradleLibraries(String text)
{
   // this should use the gradel tooling api
   Set<String> rslt = new HashSet<>();
   try (BufferedReader br = new BufferedReader(new StringReader(text))) {
      boolean indeps = false;
      for ( ; ; ) {
         String ln = br.readLine();
         if (ln == null) break;
         ln = ln.trim();
         if (ln.length() == 0) continue;
         if (ln.startsWith("dependencies")) indeps = true;
         else if (indeps) {
            if (ln.startsWith("}") || ln.endsWith("}")) {
               indeps = false;
             }
            StringTokenizer tok = new StringTokenizer(ln);
            if (tok.hasMoreTokens()) {
               String key = tok.nextToken();
               if ((key.equals("compile") || key.equals("compileOnly") ||
                       key.equals("testCompile")) && 
                     tok.hasMoreTokens()) {
                  String val = tok.nextToken("").trim();
                  if (val.startsWith("'")) {
                     int idx = val.lastIndexOf("'");
                     val = val.substring(1,idx);
                   }
                  if (!val.startsWith(":") && !val.startsWith("project") && 
                        !val.startsWith("group:")) {
                     rslt.add(val);
                   }
                }
             }
          }
       }
    }
   catch (IOException e) { }
   
   return rslt;
}


private List<CoseResult> getGithubResult(String ... query) 
{
   CoseDefaultRequest req = new CoseDefaultRequest();
   req.setSearchEngine(CoseSearchEngine.GITHUB);
   req.setDoDebug(true);
   req.setCoseScopeType(CoseScopeType.FILE);
   req.setCoseSearchType(CoseSearchType.FILE);
   req.setSearchLanguage(CoseSearchLanguage.OTHER);
   for (String s : query) { 
      req.addKeyword(s);
    }

   CoseMaster master = CoseMaster.createMaster(req);
   CoseDefaultResultSet rslts = new CoseDefaultResultSet();
   master.computeSearchResults(rslts);
   return rslts.getResults();
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
/*      Check project libraries                                                 */
/*                                                                              */
/********************************************************************************/

private void addProjectLibraries(Collection<LidsLibrary> chk,List<LidsLibrary> rslt)
{
   for (LidsLibrary ll : chk) {
      Set<String> used = new HashSet<>();
      for (String s : check_imports) {
         if (maven_finder.libraryWorksFor(ll,s)) {
            used.add(s);
          }
       }
      if (!used.isEmpty()) {
         check_imports.removeAll(used);
         done_imports.addAll(used);
         rslt.add(ll);
       }
    }
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



private static class StringLibrary extends LidsLibrary {

   private String lib_id;
   private String lib_name;
   private String lib_version;
   
   StringLibrary(String id) {
      String [] split = id.split(":");
      if (split.length != 3) {
         System.err.println("BAD LIBRARY " + id);
         return;
       }
      lib_id = split[0] + ":" + split[1];
      lib_name = split[1];
      lib_version = split[2];
    }
   
   @Override public String getName()            { return lib_name; }
   @Override public String getVersion()         { return lib_version; }
   @Override public String getId()              { return lib_id; }
   @Override public String getFullId()          { return lib_id + ":" + lib_version; }
   
   @Override public void setVersion(String s) {
      lib_version = s;
    }
   
}       // end of inner class StringLibrary




}       // end of class LidsFinder




/* end of LidsFinder.java */

