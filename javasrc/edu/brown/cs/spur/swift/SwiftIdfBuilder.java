/********************************************************************************/
/*                                                                              */
/*              SwiftIdfBuilder.java                                            */
/*                                                                              */
/*      Search word inverse frequency tool IDF builder                          */
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



package edu.brown.cs.spur.swift;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarInputStream;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;

public class SwiftIdfBuilder implements SwiftConstants
{


/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   SwiftIdfBuilder sid = new SwiftIdfBuilder(args);
   sid.generateIdfOutput();
}

/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Set<String>     english_words;
private JcompControl    jcomp_control;
private Map<String,Integer> document_counts;
private Map<String,Integer> kgram_counts;
private int             total_documents;
private int             total_kdocuments;
private List<File>      root_files;
private File            output_file;

private static Pattern package_pattern =
   Pattern.compile("^\\s*package\\s+([A-Za-z_0-9]+(\\s*\\.\\s*[A-Za-z_0-9]+)*)\\s*\\;",
      Pattern.MULTILINE);

         


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SwiftIdfBuilder(String [] args)
{
   this();
   
   scanArgs(args);
} 

   
SwiftIdfBuilder(String input)
{
   this();
   
   if (input == null) input = OUTPUT_FILE;
   
   getValidWords();
   loadInput(input);
}
   

private SwiftIdfBuilder()
{
   english_words = null;
   document_counts = null;
   kgram_counts = null;
   total_documents = 0;
   total_kdocuments = 0;
   root_files = new ArrayList<>();
   output_file = null;
}




/********************************************************************************/
/*                                                                              */
/*      TF-IDF computation                                                      */
/*                                                                              */
/********************************************************************************/

SwiftScoredSet getTfIdf(String text,boolean kgram)
{
   Map<String,Integer> words = null;
   Map<String,Integer> totals = null;
   double tot = 0;
   if (kgram){
      words = buildKgramCounts(text);
      totals = kgram_counts;
      tot = total_kdocuments;
    }  
   else {
      words = handleDocumentText(text);
      totals = document_counts;
      tot = total_documents;
    }
   double termtot = 1;
   
   for (Integer iv : words.values()) {
      termtot += iv;
    }
   
   SwiftScoredSet rslt = new SwiftScoredSet();
   for (Map.Entry<String,Integer> ent : words.entrySet()) {
      String wd = ent.getKey();
      double ct = ent.getValue();
      Integer docs = totals.get(wd);
      double docct = 0;
      if (docs != null) docct = docs;
      double idf = Math.log(tot/(1+docct));
      double tf = ct/termtot;
      double tfidf = tf*idf;
      rslt.add(wd,tfidf);
    }
   rslt.normalize();
   
   return rslt;
}




/********************************************************************************/
/*                                                                              */
/*      Argument scanning methods                                               */
/*                                                                              */
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
         
       }
      else { 
         root_files.add(new File(args[i]));
       }
    }
   
   if (root_files.isEmpty()) {
      root_files.add(new File(DEFAULT_FILE));
    }
   if (output_file == null) output_file = new File(OUTPUT_FILE);
}




/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

private void generateIdfOutput()
{
   getValidWords();
   scanDocuments();
   outputResults();
}
 
   

/********************************************************************************/
/*                                                                              */
/*      <comment here>                                                          */
/*                                                                              */
/********************************************************************************/

private void getValidWords()
{
   english_words = new HashSet<>();
  
   try (BufferedReader br = new BufferedReader(new FileReader(WORDS_FILE))) {
      for ( ; ; ) {
         String ln = br.readLine();
         if (ln == null) break;
         if (ln.length() < 3) continue;      // skip short words                   
         english_words.add(ln.trim());
       }
    }
   catch (IOException e) {
      IvyLog.logE("SWIFT","Problem processing words file",e);
      System.exit(1);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

private void outputResults()
{
   try {
      PrintWriter pw = new PrintWriter(new FileWriter(output_file));
      pw.println(total_documents);
      for (Map.Entry<String,Integer> ent : document_counts.entrySet()) {
         String wd = ent.getKey();
         int ct = ent.getValue();
         pw.println(ct + " " + wd);
       }
      pw.println(START_KGRAMS);
      pw.println(total_kdocuments);
      for (Map.Entry<String,Integer> ent : kgram_counts.entrySet()) {
         String wd = ent.getKey();
         int ct = ent.getValue();
         pw.println(ct + " " + wd);
       } 
      pw.close();
    }
   catch (IOException e) {
      IvyLog.logE("SWIFT","Problem generating output",e);
    }
}



private void loadInput(String file)
{
   try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      document_counts = new HashMap<>();
      kgram_counts = new HashMap<>();
      boolean dokgrams = false;
      String tot = br.readLine();
      total_documents = Integer.parseInt(tot);
      for (  ; ; ) {
         String ln = br.readLine();
         if (ln == null) break;
         if (ln.equals(START_KGRAMS)) {
            dokgrams = true;
            ln = br.readLine();
            if (ln == null) break;
            total_kdocuments = Integer.parseInt(tot);
            continue;
          }
         int idx = ln.indexOf(" ");
         int ct = Integer.parseInt(ln.substring(0,idx));
         String wd = ln.substring(idx+1).trim();
         if (dokgrams) kgram_counts.put(wd,ct);
         else document_counts.put(wd,ct);
       }
    }
   catch (IOException e) { 
      IvyLog.logE("SWIFT","Problem reading saved IDF file",e);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Top level document scanning code                                        */
/*                                                                              */
/********************************************************************************/

private void scanDocuments()
{
   document_counts = new HashMap<>();
   kgram_counts = new HashMap<>();
   for (File f : root_files) {
      addDocuments(f);
    }
}



private void addDocuments(File f)
{
   try {
      if (f.isDirectory()) {
         for (File child : f.listFiles()) {
            addDocuments(child);
          }
       }
      else if (isRelevant(f.getName())) {
         FileInputStream fis = new FileInputStream(f);
         if (f.getName().endsWith(".jar") || f.getName().endsWith(".zip")) {
            addZipFile(f.getName(),fis);
          }
         else if (f.getName().endsWith(".tar")) {
            addTarFile(f.getName(),fis);
          }
         else if (f.getName().endsWith(".java")) {
            addDocument(f.getName(),fis);
          }
         fis.close();
       }
    }
   catch (IOException e) {
      IvyLog.logI("Problem reading java file " + f + ": " + e);
    }
}



private void addDocument(String name,InputStream ins)
{
   if (!isRelevant(name)) return;
   if (name.endsWith(".java")) {
      handleDocument(name,ins);
    }
   else if (name.endsWith(".zip") || name.endsWith(".jar")) {
      addZipFile(name,ins);
    }
   else if (name.endsWith(".tar")) {
      addTarFile(name,ins);
    }
}



private void addZipFile(String name,InputStream ins)
{
   try { 
      ZipInputStream zis = new ZipInputStream(ins);
      for ( ; ; ) {
         ZipEntry zent = zis.getNextEntry();
         if (zent == null) break;
         if (zent.isDirectory()) continue;
         if (isRelevant(zent.getName())) {
            addDocument(zent.getName(),zis);
          }
       }
    }
   catch (Throwable e) { 
      IvyLog.logI("Problem reading jar file " + name + ": " + e);
    }
}



private void addTarFile(String name,InputStream ins)
{
   try {
      TarInputStream tis = new TarInputStream(ins);
      for ( ; ; ) {
         TarEntry tent = tis.getNextEntry();
         if (tent == null) break;
         if (tent.isDirectory()) continue;
         if (isRelevant(tent.getName())) {
            addDocument(tent.getName(),tis);
          }
       }
    }
   catch (Throwable e) { 
      IvyLog.logI("Problem reading tar file " + name + ": " + e);
    }
}


private boolean isRelevant(String name)
{
   if (name.endsWith(".zip") || name.endsWith(".jar") || name.endsWith(".java") ||
         name.endsWith(".tar")) {

      return true;
    }
   
   return false;
}




/********************************************************************************/
/*                                                                              */
/*      Actual scanning methods                                                 */
/*                                                                              */
/********************************************************************************/

private void handleDocument(String name,InputStream ins)
{
   Map<String,Integer> words = new HashMap<>();
   Map<String,Integer> kgrams = new HashMap<>();
   
   try {
      String cnts = IvyFile.loadFile(ins);
      words = handleDocumentText(cnts);
      kgrams = buildKgramCounts(cnts);
    }
   catch (IOException e) {
      IvyLog.logI("Problem reading document file " + name + ": " + e);
    }
   
   if (words.size() > 0) {
      ++total_documents;
      for (String s : words.keySet()) {
         Integer v = document_counts.get(s);
         if (v == null) document_counts.put(s,1);
         else document_counts.put(s,v+1);
       }
    }
   if (kgrams.size() > 0) {
      ++total_kdocuments;
      for (String s : kgrams.keySet()) {
         Integer v = kgram_counts.get(s);
         if (v == null) kgram_counts.put(s,1);
         else kgram_counts.put(s,v+1);
       }
    }
}



private Map<String,Integer> handleDocumentText(String cnts)
{
   Map<String,Integer> words = new HashMap<>();
   
   int start = 0;
   Matcher mat = package_pattern.matcher(cnts);
   if (mat.find()) {
     start = mat.start(); 
    }
   
   StringBuffer buf = new StringBuffer();
   for (int i = start; i < cnts.length(); ++i) {
      char c = cnts.charAt(i);
      if (Character.isAlphabetic(c)) {
         buf.append(c);
       }
      else {
         if (buf.length() > 0) {
            addWord(buf.toString(),words);
          }
         buf.setLength(0);
       }
    }
   
   return words;
}


private Map<String,Integer> buildKgramCounts(String cnts)
{
   Map<String,Integer> kgrams = new HashMap<>(); 
   
   if (jcomp_control == null) jcomp_control = new JcompControl();
   
   CompilationUnit cu = JcompAst.parseSourceFile(cnts);
   JcompProject jp = JcompAst.getResolvedAst(jcomp_control,cu);
   
   if (jp == null) return kgrams;
   SwiftKgramGenerator kgg = new SwiftKgramGenerator();
   List<SwiftKGram> kgl = kgg.getTokens(cu);
   
   for (SwiftKGram kg : kgl) {
      addSingle(kg.getText(),kgrams);
    }
   
   jcomp_control.freeProject(jp);
   
   return kgrams;
}



private void addWord(String wd,Map<String,Integer> words)
{
   String fwd = wd.toLowerCase();
   if (english_words.contains(fwd)) {
      addSingle(fwd,words);
    }
   else if (!wd.equals(fwd)) {
      StringBuffer buf = new StringBuffer();
      boolean lastupper = true;
      boolean havesplit = false;
      for (int i = 0; i < wd.length(); ++i) {
         char ch = wd.charAt(i);
         if (Character.isUpperCase(ch)) {
            if (!lastupper) {
               if (buf.length() > 0) {
                  addWord(buf.toString(),words);
                  havesplit = true;
                }
               buf.setLength(0);
             }
            lastupper = true;
            buf.append(Character.toLowerCase(ch));
          }
         else if (Character.isLowerCase(ch)) {
            buf.append(ch);
            lastupper = false;
          }
       }
      if (havesplit && buf.length() > 0) addWord(buf.toString(),words);
    }
}



private void addSingle(String wd,Map<String,Integer> words)
{
   Integer ct = words.get(wd);
   if (ct == null) words.put(wd,1);
   else words.put(wd,ct+1);
}


}       // end of class SwiftIdfBuilder




/* end of SwiftIdfBuilder.java */

