/********************************************************************************/
/*										*/
/*		ScrapPackageAbstraction.java					*/
/*										*/
/*	description of class							*/
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

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import edu.brown.cs.cose.cosecommon.CoseConstants;
import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseScores;
import edu.brown.cs.spur.lids.LidsFinder;
import edu.brown.cs.spur.lids.LidsConstants.LidsLibrary;
import edu.brown.cs.spur.sump.SumpConstants;
import edu.brown.cs.spur.sump.SumpData;
import edu.brown.cs.spur.swift.SwiftScorer;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class ScrapPackageAbstraction extends ScrapAbstraction implements SumpConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private List<PackageClass> all_classes;
private List<PackageClass> use_classes;
private List<SumpModel>    uml_models;
private List<LidsLibrary>  use_libraries;
private Collection<String> missing_imports;





/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

ScrapPackageAbstraction(ScrapAbstractor abs,CoseResult cr,CompilationUnit cu)
{
   super(abs,cr,cu);
   all_classes = new ArrayList<>();
   uml_models = null;
   use_libraries = null;
   missing_imports = null;
  
   initialize(cr,cu);
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

@Override AbstractionType getAbstractionType()	{ return AbstractionType.PACKAGE; }




/********************************************************************************/
/*										*/
/*	Merge methods								*/
/*										*/
/********************************************************************************/

@Override public boolean mergeWith(ScrapAbstraction abs)
{
   if (!(abs instanceof ScrapPackageAbstraction)) return false;

   ScrapPackageAbstraction spa = (ScrapPackageAbstraction) abs;
   if (spa == this) return true;

   ScrapMergeData md = new ScrapMergeData();
   
   RowelMatcher<PackageClass> rm = new RowelMatcher<>(use_classes,spa.use_classes);
   
   matchItems(use_classes,spa.use_classes,md,true,false);
   matchItems(use_classes,spa.use_classes,md,false,false);
   matchItems(all_classes,spa.all_classes,md,false,false);
   matchItems(use_classes,spa.use_classes,md,false,true);
   
   if (!checkForMatch(md,spa)) return false;
   
   String p1 = spa.getCoseResult().getSource().getProjectId();
   String p2 = abs.getCoseResult().getSource().getProjectId();
   if (p1.equals(p2)) {
      String t1 = spa.getCoseResult().getEditText();
      String t2 = abs.getCoseResult().getEditText();
      SwiftScorer scorer = new SwiftScorer(t1,false);
      double v = scorer.getScore(t2);
      if (v > 0.99) {
         return true;
       }
    }
   
   if (!actualMergeWith(md,spa)) return false;
   
   return true;
}



private boolean checkForMatch(ScrapMergeData md,ScrapPackageAbstraction spa)
{
   if (!checkMatch(md,use_classes,spa.use_classes,0.6,0.75,0.0)) return false;
   return true;
}



private boolean actualMergeWith(ScrapMergeData md,ScrapPackageAbstraction spa)
{
   for (PackageClass pc : spa.use_classes) {
      PackageClass npc = (PackageClass) md.getMapping(pc);
      if (npc != null) npc.mergeWith(pc);
      else if (md.isAdded(pc)) {
         use_classes.add(pc);
       }
    }
   
   superMergeWith(spa);
   
   return true;
}


void applyClassMapping(Map<ScrapClassAbstraction,ScrapClassAbstraction> absmap)
{
}




/********************************************************************************/
/*										*/
/*	Setup methods								*/
/*										*/
/********************************************************************************/

private void initialize(CoseResult cr,CompilationUnit cu)
{
   if (cu == null) cu = (CompilationUnit) cr.getStructure();

   JcompProject oproj = JcompAst.getProject(cu);
   JcompProject proj = oproj;
   if (oproj == null) {
      proj = getResolvedAst(cu);
      JcompAst.setProject(cu,proj);
      if (proj == null) return;
    }

   try {
      PackageDeclaration pd = cu.getPackage();
      if (pd == null) name_set.add("<DEFAULT>");
      else name_set.add(pd.getName().getFullyQualifiedName());

      findAllElements(cr,cu);
      findComponents();

      for (PackageClass pc : all_classes) pc.clearData();
    }
   finally {
      if (oproj == null) {
	 jcomp_main.freeProject(proj);
	 JcompAst.setProject(cu,null);
       }
    }
}



private void findAllElements(CoseResult cr,CompilationUnit cu)
{
   all_classes = new ArrayList<>();

   for (Object o : cu.types()) {
      AbstractTypeDeclaration atd = (AbstractTypeDeclaration) o;
      addClass(cr,atd);
    }
}


private void addClass(CoseResult cr,AbstractTypeDeclaration atd)
{
   PackageClass pc = new PackageClass(scrap_abstractor,cr,atd);
   if (pc.getAbstraction() == null) {
      // enums come here
      return;
    }
   all_classes.add(pc);

   for (Object o : atd.bodyDeclarations()) {
      if (o instanceof AbstractTypeDeclaration) {
	 AbstractTypeDeclaration inner = (AbstractTypeDeclaration) o;
	 addClass(cr,inner);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Methods to isolate relevant components					*/
/*										*/
/********************************************************************************/

private void findComponents()
{
   Set<PackageClass> remove = new HashSet<>();
   List<PackageClass> use = new ArrayList<>();

   for (PackageClass pc : all_classes) {
      switch (pc.getAbstraction().getArgType()) {
	 case USERTYPE :
	 case THISTYPE :
            if (isClassIgnorable(pc)) continue;
	    break;
	 case EXCEPTION :
	 case NUMBER :
	 case STRING :
	 default :
	    // remove exceptions, enums (if there), instances of standard classes
	    continue;
       }
      
      use.add(pc);
    }

   for (int i = 0; i < use.size(); ++i) {
      PackageClass pc1 = use.get(i);
      for (int j = i+1; j < use.size(); ++j) {
	 PackageClass pc2 = use.get(j);
	 if (pc1.isSubtypeOf(pc2)) {
	    remove.add(pc1);
	  }
	 if (pc2.isSubtypeOf(pc1)) {
	    remove.add(pc2);
	  }
       }
    }

   use.removeAll(remove);

   if (use.isEmpty())
      use = all_classes;

   use_classes = new ArrayList<>(use);
}



private boolean isClassIgnorable(PackageClass pc)
{
   AbstractTypeDeclaration atd = pc.getAstNode();
   if (Modifier.isPrivate(atd.getModifiers())) return true;
   boolean iface = false;
   boolean abst = false;
   if (atd instanceof TypeDeclaration) {
      TypeDeclaration td = (TypeDeclaration) atd;
      if (td.isInterface()) iface = true;
      else if (Modifier.isAbstract(atd.getModifiers())) abst = true;
    }
   else if (atd instanceof EnumDeclaration) return true;
   else return true;
   
   int methods = 0; 
   int smethods = 0;
   int cnsts = 0;
   for (Object o : atd.bodyDeclarations()) {
      BodyDeclaration bd = (BodyDeclaration) o;
      if (bd instanceof MethodDeclaration) {
         MethodDeclaration md = (MethodDeclaration) bd;
         if (Modifier.isStatic(bd.getModifiers())) ++smethods;
         else if (md.isConstructor()) ++cnsts;
         else ++methods;
       }
    }
   
   if (methods == 0 && (iface || abst)) return true;
   if (methods == 0 && smethods == 0) return true;
   if (methods == 0 && cnsts == 0) return true;
   if (methods == 0) return true;
   
   CoseScores cs = pc.getScores();
   if (cs.getBoolean("TESTCASE")) return true;
      
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      UML methods                                                             */
/*                                                                              */
/********************************************************************************/

@Override List<SumpModel> getUmlModels() 
{
   if (uml_models == null) buildUmlModels();
   
   return uml_models;
}


@Override void buildUmlModels()
{
   uml_models = new ArrayList<>();
   Map<String,SumpClass> cmap = new HashMap<>();
   
   for (CoseResult cr : getAllResults()) {
      CompilationUnit cu = (CompilationUnit) cr.getStructure();
      SumpData sd = new SumpData(scrap_abstractor.getRequest(),cr);
      for (LidsLibrary ll : getReferencedLibraries()) {
         sd.addLibrary(ll.getId());
       }
      SumpModel mdl = SumpFactory.createModel(sd);
      SumpPackage spe = mdl.setPackage("Default");
      for (PackageClass pc : use_classes) {
         pc.addToUmlPackage(spe,cu,cmap);
       }
      for (PackageClass pc : use_classes) {
         pc.addDependencies(spe,cu,cmap);
       }
      mdl.computeLayout();
      uml_models.add(mdl);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Library methods                                                         */
/*                                                                              */
/********************************************************************************/

List<LidsLibrary> getReferencedLibraries()
{
   if (use_libraries == null) {
      use_libraries = new ArrayList<>();
      Set<String> imports = new HashSet<>();
      
      for (CoseResult cr : getAllResults()) {
         CompilationUnit cu = (CompilationUnit) cr.getStructure();
         PackageDeclaration pd = cu.getPackage();
         String pnm = null;
         if (pd != null) pnm = pd.getName().getFullyQualifiedName();
         for (Object o : cu.imports()) {
            ImportDeclaration id = (ImportDeclaration) o;
            String idnm = id.getName().getFullyQualifiedName();
            if (CoseConstants.isRelatedPackage(pnm,idnm)) continue;
            if (CoseConstants.isStandardJavaLibrary(idnm)) continue;
            if (id.isOnDemand()) idnm += ".*";
            imports.add(idnm);
          }
       }
      
      LidsFinder fndr = new LidsFinder();
      for (String s : imports) fndr.addImportPath(s);
      use_libraries = fndr.findLibraries();
      missing_imports = fndr.getMissiingImports();
    }
   return use_libraries;
}

/********************************************************************************/
/*										*/
/*	Output Methods								*/
/*										*/
/********************************************************************************/

@Override void outputAbstraction()
{
   System.err.println("PACKAGE ABSTRACTION FOR " + getResultCount() + " Results");
   for (CoseResult cr : getAllResults()) {
      System.err.println("   SOURCE: " + cr.getSource().getName());
    }
   for (String s : name_set) System.err.println("\t: " + s);

   System.err.println("--------KEY CLASSES");
   for (PackageClass pc : use_classes) {
      pc.outputClass();
    }
   
   System.err.println("--------OTHER CLASSES");
   for (PackageClass pc : all_classes) {
      if (use_classes.contains(pc)) continue;
      pc.outputClass();
    }
   
   System.err.println("---------LIBRARIES");
   for (LidsLibrary lib : use_libraries) {
      System.err.println("\t" + lib.getId());
    }
   System.err.println("---------MISSING IMPORTS");
   for (String imp : missing_imports) {
      System.err.println("\t" + imp);
    }
   
   System.err.println("--------UML MODELS");
   for (SumpModel mdl : uml_models) {
      IvyXmlWriter xw = new IvyXmlWriter();
      mdl.outputXml(xw);
      System.err.print(xw.toString());
      xw.close();
      Writer w = new StringWriter();
      mdl.outputJava(w);
      System.err.print(w.toString());
      break;            // only output one model for now
    }
   
   System.err.println();
}



/********************************************************************************/
/*										*/
/*	PackageClass -- holder of class information				*/
/*										*/
/********************************************************************************/

private static class PackageClass extends ScrapComponent {

   private ScrapClassAbstraction class_abstraction;
   private List<AbstractTypeDeclaration> type_decls;

   PackageClass(ScrapAbstractor abs,CoseResult cr,AbstractTypeDeclaration atd) {
      class_abstraction = (ScrapClassAbstraction) abs.addToAbstractor(cr,atd);
      type_decls = new ArrayList<>();
      type_decls.add(atd);
      component_modifiers = atd.getModifiers();
      component_scores = cr.getScores(abs.getRequest(),atd);
      addName(atd.getName().getIdentifier());
    }

   @Override ComponentType getComponentType()	{ return ComponentType.CLASS; } 

   ScrapClassAbstraction getAbstraction()	{ return class_abstraction; }
   
   AbstractTypeDeclaration getAstNode()         { return type_decls.get(0); }

   @Override boolean isCompatibleWith(ScrapComponent sc) {
      if (sc instanceof PackageClass) {
         PackageClass pc = (PackageClass) sc;
         if (class_abstraction != pc.class_abstraction) return false;
         return true;
       }
      return false;
    }
   
   @Override void mergeWith(ScrapComponent sc) {
      PackageClass pc = (PackageClass) sc;
      type_decls.addAll(pc.type_decls);
      super.mergeWith(sc);
    }
   
   boolean isSubtypeOf(PackageClass pc) {
      for (AbstractTypeDeclaration atd : type_decls) {
         for (AbstractTypeDeclaration atd1 : pc.type_decls) {
            if (atd.getAST() == atd1.getAST()) {
               JcompType jt = JcompAst.getJavaType(atd);
               JcompType jt1 = JcompAst.getJavaType(atd1);
               if (jt.isCompatibleWith(jt1)) return true;
             }
          }
       }
      return false;
    }

   void outputClass() {
      System.err.println("CLASS----" + getNames());
      if (class_abstraction == null)
         System.err.println("\tNO ABSTRACTION");
      else
         class_abstraction.outputShortAbstraction();
    }
   
   void addToUmlPackage(SumpPackage pkg,CompilationUnit cu,Map<String,SumpClass> cmap) { 
      for (AbstractTypeDeclaration atd : type_decls) {
         if (atd.getAST() == cu.getAST()) {
            SumpClass sc = pkg.addClass(atd);
            class_abstraction.addToUmlClass(sc,atd);
            String nm = atd.getName().getIdentifier();
            cmap.put(nm,sc);
          }
       }
    }
   
   
   void addDependencies(SumpPackage pkg,CompilationUnit cu,Map<String,SumpClass> cmap)
   {
      for (AbstractTypeDeclaration atd : type_decls) {
         if (atd.getAST() == cu.getAST()) {
            pkg.addDependencies(atd,cmap);
          }
       }
   }
   
}	// end of inner class PackageClass;






}	// end of class ScrapPackageAbstraction




/* end of ScrapPackageAbstraction.java */





















































































































































































































































































































































