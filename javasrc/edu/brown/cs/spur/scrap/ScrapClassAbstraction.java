/********************************************************************************/
/*                                                                              */
/*              ScrapClassAbstraction.java                                      */
/*                                                                              */
/*      description of class                                                    */
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



package edu.brown.cs.spur.scrap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseScores;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.spur.rowel.RowelConstants.RowelMatch;
import edu.brown.cs.spur.rowel.RowelMatcher;
import edu.brown.cs.spur.sump.SumpArgType;
import edu.brown.cs.spur.sump.SumpConstants.SumpClass;

class ScrapClassAbstraction extends ScrapAbstraction
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

enum ComponentType { FIELD, METHOD };

private List<ClassMethod>       all_methods;
private List<ClassField>        all_fields;
private List<ClassField>        use_fields;
private List<ClassMethod>       use_constructors;
private List<ClassMethod>       use_methods;
private ScrapTypeAbstraction    void_type;
private SumpArgType             arg_type;

private static Set<String>      standard_names;

static {
   standard_names = new HashSet<>();
   standard_names.add("hashCode");
   standard_names.add("equals");
   standard_names.add("toString");
   standard_names.add("clone");
}


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ScrapClassAbstraction(ScrapAbstractor abs,CoseResult cr,AbstractTypeDeclaration atd)
{
   super(abs,cr,atd);
   all_methods = null;
   all_fields = null;
   initialize(cr,atd);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override AbstractionType getAbstractionType()
{
   return AbstractionType.CLASS;
}


SumpArgType getArgType()               { return arg_type; }



/********************************************************************************/
/*                                                                              */
/*      Update methods                                                          */
/*                                                                              */
/********************************************************************************/

void addUsedField(ClassField cf)
{
   use_fields.add(cf);
}


void addUsedMethod(ClassMethod cm)
{
   if (cm.isConstructor()) {
      use_constructors.add(cm);
    }
   else {
      use_methods.add(cm);
    }
}





/********************************************************************************/
/*                                                                              */
/*      Merge methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public boolean mergeWith(ScrapAbstraction abs)
{
   if (!(abs instanceof ScrapClassAbstraction)) return false;
   ScrapClassAbstraction sca = (ScrapClassAbstraction) abs;
   if (arg_type != sca.arg_type) return false;
   
   ScrapMergeData md = new ScrapMergeData();
   
   matchFields(md,sca);
   matchConstructors(md,sca);
   matchMethods(md,sca);
   
   eliminateFields(md,sca);
   eliminateConstructors(md,sca);
   eliminateMethods(md,sca);
   
   otherFields(md,sca);
   otherMethods(md,sca);
   
   if (!checkForMatch(md,sca)) return false;
   
   if (!actualMergeWith(md,sca)) return false;

   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Field merge methods                                                     */
/*                                                                              */
/********************************************************************************/

private void matchFields(ScrapMergeData md,ScrapClassAbstraction sca)
{  
   RowelMatcher<ClassField> rm = new RowelMatcher<>(use_fields,sca.use_fields);
   Map<ClassField,ClassField> map = rm.bestMatch(MatchType.MATCH_EXACT);
   for (Map.Entry<ClassField,ClassField> ent : map.entrySet()) {
      md.addMapping(ent.getKey(),ent.getValue());
    }
}



private void eliminateFields(ScrapMergeData md,ScrapClassAbstraction sca)
{
   for (ClassField cf : use_fields) {
      if (md.isUsed(cf)) continue;
      ClassField bestf = null;
      double bestv = -1;
      for (ClassField nf : sca.all_fields) {
         if (md.isUsed(nf)) continue;
         if (!cf.isCompatibleWith(nf)) continue;
         double v = cf.getNameMatch(nf);
         if (v > bestv) {
            bestv = v;
            bestf = nf;
          }
       }
      if (bestf == null) continue;
      md.addAssociation(cf,bestf);
    }
   
   for (ClassField cf : use_fields) {
      if (md.isUsed(cf)) continue;
      for (ClassMethod cm : sca.use_methods) {
         if (md.isUsed(cm)) continue;
         if (cm.canBeAccessMethod(cf) || cm.canBeSetterMethod(cf)) {
            md.addAssociation(cm,cf);
            break;
          }
       }
    }
}



private void otherFields(ScrapMergeData md,ScrapClassAbstraction sca)
{
   for (ClassField cf : sca.all_fields) {
      if (md.isUsed(cf) || sca.use_fields.contains(cf)) continue;
      ClassField bestf = null;
      double bestv = -1;
      for (ClassField nf : all_fields) {
         if (md.isUsed(nf)) continue;
         if (!cf.isCompatibleWith(nf)) continue;
         double v = cf.getNameMatch(nf);
         if (v > bestv) {
            bestv = v;
            bestf = nf;
          }
       }
      if (bestf != null) { 
         md.addMapping(cf,bestf);
       }
    }
}



 /********************************************************************************/
/*                                                                              */
/*      Constructor Merge methods                                               */
/*                                                                              */
/********************************************************************************/

private void matchConstructors(ScrapMergeData md,ScrapClassAbstraction sca)
{
   RowelMatcher<ClassMethod> rm = new RowelMatcher<>(use_constructors,sca.use_constructors);
   Map<ClassMethod,ClassMethod> map = rm.bestMatch(MatchType.MATCH_EXACT);
   for (Map.Entry<ClassMethod,ClassMethod> ent : map.entrySet()) {
      md.addMapping(ent.getKey(),ent.getValue());
    }
   
   // for (ClassMethod cm : use_constructors) {
      // if (md.isUsed(cm)) continue;
      // for (ClassMethod nm : sca.use_constructors) {
         // if (md.isUsed(nm)) continue;
         // if (!cm.isCompatibleWith(nm)) continue;
         // md.addMapping(cm,nm);
         // break;
       // }
    // }
}


private void eliminateConstructors(ScrapMergeData md,ScrapClassAbstraction sca)
{
   for (ClassMethod cm : use_constructors) {
      if (md.isUsed(cm)) continue;
      int nconst = 0;
      boolean havedflt = false;
      for (ClassMethod nm : sca.all_methods) {
         if (!nm.isConstructor()) continue;
         ++nconst;
         if (nm.getAbstraction().getParameterTypes().isEmpty()) havedflt = true;
       }
      if (nconst == 0) {
         havedflt = true;
         Set<ScrapTypeAbstraction> args = new HashSet<>();
         ScrapMethodAbstraction dflt = scrap_abstractor.addToAbstraction(getCoseResult(),void_type,args);
         if (cm.isCompatibleWith(dflt)) {
            md.addAssociation(cm,null);
            continue;
          }
       }
      if (havedflt) {
         for (ClassMethod nm : sca.use_methods) {
            if (md.isUsed(nm)) continue;
            if (nm.canBeInitializeMethod(cm)) {
               md.addAssociation(cm,null);
               md.addAssociation(nm,null);
               break;
             }
          }
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Method merge methods                                                    */
/*                                                                              */
/********************************************************************************/

private void matchMethods(ScrapMergeData md,ScrapClassAbstraction sca)
{
   RowelMatcher<ClassMethod> rm = new RowelMatcher<>(use_methods,sca.use_methods);
   Map<ClassMethod,ClassMethod> map = rm.bestMatch(MatchType.MATCH_EXACT);
   for (Map.Entry<ClassMethod,ClassMethod> ent : map.entrySet()) {
      md.addMapping(ent.getKey(),ent.getValue());
    }
}


private void eliminateMethods(ScrapMergeData md,ScrapClassAbstraction sca)
{
   for (ClassMethod cm : use_methods) {
      if (md.isUsed(cm)) continue;
      ClassMethod bestm = null;
      double bestv = -1;
      for (ClassMethod nm: sca.use_methods) {
         if (md.isUsed(nm)) continue;
         if (!cm.isCompatibleWith(nm)) continue;
         double v = cm.getNameMatch(nm);
         if (v > bestv) {
            bestv = v;
            bestm = nm;
          }
       }
      if (bestm == null) continue;
      md.addAssociation(cm,bestm);
    }
}



private void otherMethods(ScrapMergeData md,ScrapClassAbstraction sca)
{
   for (ClassMethod cm : sca.all_methods) {
      if (md.isUsed(cm) || sca.use_methods.contains(cm)) continue;
      ClassMethod bestm = null;
      double bestv = -1;
      for (ClassMethod nm : all_methods) {
         if (md.isUsed(nm)) continue;
         if (cm.isConstructor() != nm.isConstructor()) continue;
         if (!cm.isCompatibleWith(nm)) continue;
         double v = cm.getNameMatch(nm);
         if (v > bestv) {
            bestv = v;
            bestm = nm;
          }
       }
      if (bestm != null) {
         md.addMapping(cm,bestm);
         continue;
       }
      if (cm.containsName("clone") || cm.containsName("equals") ||
            cm.containsName("hashCode") || cm.containsName("toString") ||
            cm.containsName("compareTo") || cm.containsName("compare") ||
            cm.containsName("finalize")) {
         md.addAssociation(cm,null);
       }
    }
   
   matchItems(use_methods,sca.use_methods,md,false,true);
}




/********************************************************************************/
/*                                                                              */
/*      Merge checking methods                                                  */
/*                                                                              */
/********************************************************************************/

private boolean checkForMatch(ScrapMergeData md,ScrapClassAbstraction sca)
{
   double f1 = 0.5;
   double f2 = 0.75;
   double f3 = 0.1;
   double f4 = 0.25;
   double f5 = 0.25;
   double f6 = 0.50;
   
   switch (scrap_abstractor.getRequest().getCoseSearchType()) {
      case PACKAGE :
         f5 = 0.5;
         f6 = 0.75;
         break;
      default :
         break;
    }
   
   if (!checkMatch(md,use_fields,sca.use_fields,f1,f2,0.0)) return false;   
   if (!checkMatch(md,use_constructors,sca.use_constructors,f3,f4,0.0)) return false;
   if (!checkMatch(md,use_methods,sca.use_methods,f5,f6,0.0)) return false;
   // System.err.println("MATCH SUCCESSFUL");
   return true;
}







/********************************************************************************/
/*                                                                              */
/*      Actual merging methods                                                  */
/*                                                                              */
/********************************************************************************/

private boolean actualMergeWith(ScrapMergeData md,ScrapClassAbstraction sca)
{
   for (ClassField cf : sca.use_fields) {
       ClassField nf = (ClassField) md.getMapping(cf);
       if (nf != null) nf.mergeWith(cf);
       else if (md.isAdded(cf)) {
          addUsedField(cf);
        }
    }
   
   for (ClassMethod cm : sca.use_constructors) {
      ClassMethod nm = (ClassMethod) md.getMapping(cm);
      if (nm != null) nm.mergeWith(cm);
      else if (md.isAdded(cm)) {
         addUsedMethod(cm);
       }
    }
   
   for (ClassMethod cm : sca.use_methods) {
      ClassMethod nm = (ClassMethod) md.getMapping(cm);
      if (nm != null) nm.mergeWith(cm);
      else if (md.isAdded(cm)) {
         addUsedMethod(cm);
       }
    }
   
   // should also add to all_fields and all_methods
   superMergeWith(sca);
   
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

private void initialize(CoseResult cr,ASTNode an)
{
   if (an == null) an = (ASTNode) cr.getStructure();
   JcompProject oproj = JcompAst.getProject(an);
   JcompProject proj = oproj;
   if (oproj == null) {
      proj = getResolvedAst(an);
      JcompAst.setProject(an,proj);
      if (proj == null) return;
    }
   
   try {
      JcompType vtyp = proj.getResolveTyper().findSystemType("void");
      void_type = new ScrapTypeAbstraction(proj,vtyp,null);
      AbstractTypeDeclaration atd = (AbstractTypeDeclaration) an; 
      JcompType typ = JcompAst.getJavaType(atd);
      name_set.add(typ.getName());
      arg_type = SumpArgType.computeArgType(typ,atd);
      findAllElements(cr,atd);
      findComponents(atd);
   
      if (typ.getOuterType() != null) {
         ScrapTypeAbstraction outer = new ScrapTypeAbstraction(proj,typ.getOuterType(),null);
         ClassField cf = new ClassField(scrap_abstractor,cr,"<OUTER>",outer);
         all_fields.add(cf);
       }
      
      for (ClassField cf : all_fields) cf.clearData();
      for (ClassMethod cm : all_methods) cm.clearData();
    }
   finally {
      if (oproj == null) {
         jcomp_main.freeProject(proj);
         JcompAst.setProject(an,null);
       }
    }
}


private void findAllElements(CoseResult cr,AbstractTypeDeclaration atd)
{
   all_methods = new ArrayList<>();
   all_fields = new ArrayList<>();
   
   for (Object o : atd.bodyDeclarations()) {
      if (o instanceof MethodDeclaration) {
         MethodDeclaration md = (MethodDeclaration) o;
         if (!isRelevant(md)) continue;
         ClassMethod cm = new ClassMethod(scrap_abstractor,cr,md);
         all_methods.add(cm);
       }
      else if (o instanceof FieldDeclaration) {
         FieldDeclaration fd = (FieldDeclaration) o;
         if (!isRelevant(fd)) continue;
         for (Object o1 : fd.fragments()) {
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) o1;
            ClassField cf = new ClassField(scrap_abstractor,cr,vdf);
            all_fields.add(cf);
          }
       }
    }
}



private boolean isRelevant(MethodDeclaration md)
{
   switch (scrap_abstractor.getRequest().getCoseSearchType()) {
      case CLASS :
         if (Modifier.isAbstract(md.getModifiers())) return false;
         break;
      case PACKAGE :
         if (Modifier.isPrivate(md.getModifiers())) return false;
         break;
      default :
         break;
    }
   
   return true;
}


private boolean isRelevant(FieldDeclaration fd)
{
   if (Modifier.isStatic(fd.getModifiers())) return false;
   if (Modifier.isFinal(fd.getModifiers())) return false;
   
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Find important fields and methods                                       */
/*                                                                              */
/********************************************************************************/

private void findComponents(AbstractTypeDeclaration atd)
{
   Set<JcompSymbol> symbols = findSymbols(atd);
   List<ClassMethod> consts = new ArrayList<>();
   List<ClassMethod> relevant = new ArrayList<>();
   Set<ClassField> fields = new HashSet<>();
   for (ClassField cf : all_fields) {
      if (cf.isAccessible() && !cf.isStatic()) {
         fields.add(cf);
       }
    }
   for (ClassMethod cm : all_methods) {
      if (!cm.isAccessible()) continue;
      if (cm.isStatic()) continue;
      JcompSymbol acc = cm.getAccessField();
      if (acc != null) {
         if (cm.isPublic()) {
            boolean fnd = false;
            for (ClassField cf : all_fields) {
               if (cf.getDefinition() == acc) {
                  fields.add(cf);
                  fnd = true;
                  break;
                }
             }
            if (fnd) continue;
          }
       }
      if (cm.isConstructor() && cm.getStructure().parameters().size() == 0 &&
            cm.isPublic()) {
         consts.add(cm);
         continue;
       }
      
      JcompSymbol js = cm.getDefinition();
      if (!symbols.contains(js)) continue;
      else if (cm.isConstructor()) consts.add(cm);
      else relevant.add(cm);
    }
   
   use_fields = new ArrayList<>(fields);
   use_constructors = new ArrayList<>(consts);
   use_methods = new ArrayList<>(relevant);
   
}



/********************************************************************************/
/*                                                                              */
/*      Check for (recursive) use of symbols                                    */
/*                                                                              */
/********************************************************************************/

private Set<JcompSymbol> findSymbols(AbstractTypeDeclaration atd)
{
   JcompType classtyp = JcompAst.getJavaType(atd);
   Set<JcompSymbol> symbols = new HashSet<>();
   double sumv = 0;
   double ctv = 0;
   List<ClassMethod> chkmthds = new ArrayList<>();
   for (ClassMethod cm : all_methods) {
      CoseScores cs = cm.getScores();
      switch (scrap_abstractor.getRequest().getCoseSearchType()) {
         case CLASS:
            if (cs.getBoolean("TRIVIAL")) continue;
            break;
         default :
            break;
       }
      if (cs.getBoolean("TESTCASE")) continue;
      double v = cs.getDouble("TERMMATCH");
      chkmthds.add(cm);
      sumv += v;
      ctv += 1.0;
    }
   double avg = sumv/ctv;
   avg = avg * 1.00;
   if (avg > 0.5) avg = 0.5;
   avg -= 0.01;
   
   for (ClassMethod cm : chkmthds) {
      CoseScores cs = cm.getScores();
      JcompSymbol js = cm.getDefinition();
      switch (scrap_abstractor.getRequest().getCoseSearchType()) {
         case CLASS :
            break;
         case PACKAGE :
            break;
         default :
            break;
       }
      
      if (classtyp.isInterfaceType()) {
         symbols.add(js);
       }    
      else if (cs.getDouble("TERMMATCH") >= avg || cs.getInt("TERMTITLEMATCH") >= 1) {
         if (js != null) symbols.add(js);
       }
      else if (cm.isConstructor() && cm.isAccessible()) {
         symbols.add(js);
       }   
    }
    
   for (ClassField cf : all_fields) {
      CoseScores cs = cf.getScores();
      if (cs.getDouble("TERMMATCH") > 0) {
         JcompSymbol js = cf.getDefinition();
         if (js != null) {
            symbols.add(js);
          }
       }
    }
   computeSymbols(symbols);
   return symbols;
}



private void computeSymbols(Set<JcompSymbol> syms)
{
   boolean chng = true;
   while (chng) {
      chng = false;
      for (ClassMethod cm : all_methods) {
         JcompSymbol js = cm.getDefinition();
         if (js == null || syms.contains(js)) continue;
         UseChecker uc = new UseChecker(syms);
         cm.getStructure().accept(uc);
         if (uc.useFound()) {
            syms.add(js);
            chng = true;
          }
       }
    }
}



private static class UseChecker extends ASTVisitor {

   private Set<JcompSymbol> base_symbols;
   private boolean use_found;
   
   UseChecker(Set<JcompSymbol> syms) {
      base_symbols = syms;
      use_found = false;
    }
   
   boolean useFound()                           { return use_found; }
   
   @Override public void postVisit(ASTNode n) {
      if (use_found) return;
      JcompSymbol js = JcompAst.getReference(n);
      if (js != null) {
         if (base_symbols.contains(js)) use_found = true;
         if (js.getName().equals("start")) { 
            String pfx = null;
            String ttl = js.getFullName();
            int idx = ttl.lastIndexOf(".");
            if (idx > 0) {
               ttl = ttl.substring(0,idx);
               if (!ttl.equals("java.lang.Thread")) pfx = ttl;
             }
            for (JcompSymbol jsx : base_symbols) {
               if (jsx.isMethodSymbol() && jsx.getName().equals("run")) {
                   if (pfx == null) use_found = true;
                   else if (jsx.getFullName().equals(pfx + ".run")) use_found = true;
                }
             }
          }
       }
    }
   
   @Override public boolean preVisit2(ASTNode n) {
      if (use_found) return false;
      return true;
    }
   
}


/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override void outputAbstraction()
{
   System.err.println("CLASS ABSTRACTION FOR " + getResultCount() + " Results");
   for (CoseResult cr : getAllResults()) {
      System.err.println("   SOURCE: " + cr.getSource().getName());
    }
   for (String s : name_set) System.err.println("\t: " + s);
   System.err.println("ACCESS: ");
   for (ClassField cf : use_fields) {
      cf.outputField();
    }
   System.err.println("CONSTRUCTORS: ");
   for (ClassMethod cm : use_constructors) {
      cm.outputMethod();
    }
   System.err.println("METHODS: ");
   for (ClassMethod cm : use_methods) {
      cm.outputMethod();
    }
}



@Override void outputShortAbstraction()
{
   for (ClassField cf : use_fields) {
      cf.outputShortField();
    }
   for (ClassMethod cm : use_constructors) {
      cm.outputShortMethod();
    }
   for (ClassMethod cm : use_methods) {
      cm.outputShortMethod();
    }  
}


void addToUmlClass(SumpClass scls,AbstractTypeDeclaration atd)
{
   for (ClassField cf : use_fields) {
      cf.addToUmlClass(scls,atd);
    }
   for (ClassMethod cm : use_constructors) {
      cm.addToUmlClass(scls,atd);
    }
   for (ClassMethod cm : use_methods) {
      cm.addToUmlClass(scls,atd);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Analysis methods                                                        */
/*                                                                              */
/********************************************************************************/

private static JcompSymbol getAccessField(MethodDeclaration md)
{
   if (md.isConstructor()) return null;
   if (md.parameters().size() > 0) return checkSetMethod(md);
   else {
      Type t = md.getReturnType2();
      if (t == null) return null;
      if (t.isPrimitiveType()) {
         PrimitiveType pt = (PrimitiveType) t;
         if (pt.getPrimitiveTypeCode() == PrimitiveType.VOID) return null;
       }
      return checkGetMethod(md);
    }
}




private static JcompSymbol checkGetMethod(MethodDeclaration md)
{
   Block b = md.getBody();
   if (b == null) return null;
   if (b.statements().size() != 1) return null;
   ASTNode n = (ASTNode) b.statements().get(0);
   if (n.getNodeType() != ASTNode.RETURN_STATEMENT) return null;
   ReturnStatement rs = (ReturnStatement) n;
   if (rs.getExpression() == null) return null;
   RefFinder rf = new RefFinder();
   rs.accept(rf);
   Set<JcompSymbol> refs = rf.getReferences();
   JcompSymbol rslt = null;
   for (Iterator<JcompSymbol> it = refs.iterator(); it.hasNext(); ) {
      JcompSymbol js = it.next();
      if (!js.isFieldSymbol()) it.remove();
      else rslt = js;
    }
   if (refs.isEmpty() || refs.size() > 1) return null;
   return rslt;
}




private static JcompSymbol checkSetMethod(MethodDeclaration md)
{
   int np = md.parameters().size();
   Block b = md.getBody();
   if (b == null) return null;
   if (b.statements().size() != np) return null;
   for (Object o : b.statements()) {
      Statement s = (Statement) o;
      if (s.getNodeType() != ASTNode.EXPRESSION_STATEMENT) return null;
      ExpressionStatement es = (ExpressionStatement) s;
      if (es.getExpression().getNodeType() != ASTNode.ASSIGNMENT) return null;
      Assignment as = (Assignment) es.getExpression();
      JcompSymbol js = JcompAst.getReference(as.getLeftHandSide());
      if (js != null && js.isFieldSymbol()) return js;
    }
   return null;
}



private static class RefFinder extends ASTVisitor {

   private Set<JcompSymbol> all_refs;
   
   RefFinder() {
      all_refs = new HashSet<>();
    }
   
   Set<JcompSymbol> getReferences()             { return all_refs; }
   
   @Override public void postVisit(ASTNode n) {
      JcompSymbol js = JcompAst.getReference(n);
      if (js != null) all_refs.add(js);
    }
   
}       // end of inner class RefFinder



/********************************************************************************/
/*                                                                              */
/*      Method information                                                      */
/*                                                                              */
/********************************************************************************/

private static class ClassMethod extends ScrapComponent implements RowelMatch {

   private ScrapMethodAbstraction method_abstraction;
   private List<MethodDeclaration> method_decls;
   private boolean is_constructor;
   
   ClassMethod(ScrapAbstractor abs,CoseResult cr,MethodDeclaration md) {
      method_abstraction = (ScrapMethodAbstraction) abs.addToAbstractor(cr,md);
      method_decls = new ArrayList<>();
      method_decls.add(md);
      component_modifiers = md.getModifiers();
      component_scores = cr.getScores(abs.getRequest(),md);
      if (md.isConstructor()) addName("<init>");
      else addName(md.getName().getIdentifier());
      is_constructor = md.isConstructor();
    }
   
   @Override ComponentType getComponentType()   { return ComponentType.METHOD; }    
   
   boolean isAccessible() {
      if (Modifier.isAbstract(component_modifiers)) return false;
      if (Modifier.isPublic(component_modifiers)) return true;
      if (Modifier.isPrivate(component_modifiers)) return false;
      if (Modifier.isProtected(component_modifiers)) return false;
      // package protected
      return true;
    }
   
   boolean isConstructor() {
      return is_constructor;
    }
   
   ScrapMethodAbstraction getAbstraction()      { return method_abstraction; }
   
   JcompSymbol getDefinition() {
      if (method_decls.isEmpty()) return null;
      return JcompAst.getDefinition(method_decls.get(0));
    }
   
   MethodDeclaration getStructure() { 
      if (method_decls.isEmpty()) return null;
      return method_decls.get(0);
    }
   
   JcompSymbol getAccessField() {
      return ScrapClassAbstraction.getAccessField(method_decls.get(0));
    }
   
   void clearData() {
      // method_decls.clear();
      super.clearData();
    }
   
   @Override boolean isCompatibleWith(ScrapComponent sc) {
      if (sc instanceof ClassMethod) {
         ClassMethod cm = (ClassMethod) sc;
         if (isCompatibleWith(cm.method_abstraction)) return true;
       }
      return false;
    }
   
   @Override  boolean isComparableTo(ScrapComponent sc) {
      if (sc instanceof ClassMethod) {
         ClassMethod cm = (ClassMethod) sc;
         return method_abstraction.isComparable(cm.method_abstraction);
       }
      return false;
   }
   
   
   boolean isCompatibleWith(ScrapMethodAbstraction sma)  {
      if (method_abstraction != sma) return false;
      return true;
   }
   
   boolean canBeAccessMethod(ClassField cf) {
      if (isConstructor()) return false;
      ScrapTypeAbstraction ftyp = cf.getFieldType();
      if (method_abstraction.getReturnType() != ftyp) return false;
      if (method_abstraction.getParameterTypes().size() > 1) return false;
      
      Set<String> words = getNameWords(getNames());
      for (String s : component_names.keySet()) {
         Set<String> cwords = getNameWords(s);
         boolean havepfx = false;
         for (Iterator<String> it = cwords.iterator(); it.hasNext(); ) {
            String swd = it.next();
            if (swd.equals("get") || swd.equals("is")) {
               it.remove();
               havepfx = true;
             }
          }
         int count = 0;
         for (String s1 : words) {
            if (cwords.contains(s1)) ++count;
          }
         if (count == words.size()) return true;
         if (havepfx && count > words.size() / 2) return true;
       }
      return false;
    }
   
   boolean canBeSetterMethod(ClassField cf) {
      if (isConstructor()) return false;
      ScrapTypeAbstraction ftyp = cf.getFieldType();
      if (method_abstraction.getReturnType().getArgType() != SumpArgType.VOID) return false;
      if (method_abstraction.getParameterTypes().size() != 1) return false;
      for (ScrapTypeAbstraction sta : method_abstraction.getParameterTypes()) {
         if (sta != ftyp) return false;
       }
      
      Set<String> words = getNameWords(getNames());
      for (String s : component_names.keySet()) {
         Set<String> cwords = getNameWords(s);
         boolean havepfx = false;
         for (Iterator<String> it = cwords.iterator(); it.hasNext(); ) {
            String swd = it.next();
            if (swd.equals("set")) {
               it.remove();
               havepfx = true;
             }
          }
         int count = 0;
         for (String s1 : words) {
            if (cwords.contains(s1)) ++count;
          }
         if (count == words.size()) return true;
         if (havepfx && count > words.size() / 2) return true;
       }
      return false;
   }
   
   boolean canBeInitializeMethod(ClassMethod cm)
   {
      if (isConstructor()) return false;
      if (!isCompatibleWith(cm)) return false;
      for (String s : component_names.keySet()) {
         Set<String> cwords = getNameWords(s);
         if (cwords.contains("initialize") || cwords.contains("init")) return true;
         if (cwords.contains("set")) return true;
       }
      return false;
   }
   
   void mergeWith(ClassMethod cm) {
      method_decls.addAll(cm.method_decls);
      super.mergeWith(cm);
    }
   
   void outputMethod() {
      System.err.println("MTHD----" + getNames());
      ScrapTypeAbstraction rtyp = method_abstraction.getReturnType();
      System.err.println("   RETURN");
      rtyp.outputArg("\t");
      System.err.println("    ARGS:");
      for (ScrapTypeAbstraction ma : method_abstraction.getParameterTypes()) {
         ma.outputArg("\t");
       } 
    }
   
   
   void outputShortMethod() {
      System.err.print("\t(");
      int ct = 0;
      for (ScrapTypeAbstraction ma : method_abstraction.getParameterTypes()) {
         if (ct++ > 0) System.err.print(",");
         System.err.print(ma.getArgType());
       }
      System.err.print(")");
      System.err.print(method_abstraction.getReturnType().getArgType());
      System.err.print(" : ");
      if (isConstructor()) System.err.println("<INIT>");
      else System.err.println(getNames());
   }
   
   void addToUmlClass(SumpClass scls,AbstractTypeDeclaration atd) {
      for (MethodDeclaration md : method_decls) {
         if (isChild(atd,md)) {
            JcompSymbol msym = JcompAst.getDefinition(md);
            if (msym != null) scls.addOperation(msym,atd);
          }
       }
    }
   
   
   
   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("METHOD ");
      buf.append(getNames());
      buf.append(" : ");
      buf.append(method_abstraction.getReturnType().toString());
      buf.append("(");
      int i = 0;
      for (ScrapTypeAbstraction pa : method_abstraction.getParameterTypes()) {
         if (i++ > 0) buf.append(",");
         buf.append(pa.toString());
       }
      buf.append(")");
      return buf.toString();
    }
   
}       // end of inner class ClassMethod



/********************************************************************************/
/*                                                                              */
/*      Field information                                                       */
/*                                                                              */
/********************************************************************************/

private static class ClassField extends ScrapComponent {

   private ScrapFieldAbstraction field_abstraction;
   private List<VariableDeclarationFragment> field_asts;
   
   ClassField(ScrapAbstractor abs,CoseResult cr,VariableDeclarationFragment vdf) {
      field_abstraction = (ScrapFieldAbstraction) abs.addToAbstractor(cr,vdf);
      addName(vdf.getName().getIdentifier());
      FieldDeclaration fd = (FieldDeclaration) vdf.getParent();
      component_modifiers = fd.getModifiers();
      component_scores = cr.getScores(abs.getRequest(),vdf.getParent());
      field_asts = new ArrayList<>();
      field_asts.add(vdf);
    }
   
   ClassField(ScrapAbstractor abs,CoseResult cr,String name,ScrapTypeAbstraction outer) {
       addName(name);
       component_modifiers = 0;
       component_scores = null;
       field_asts = new ArrayList<>();
       field_abstraction = abs.addToAbstractor(cr,name,outer);
    }
   
   ComponentType getComponentType()             { return ComponentType.FIELD; }
   
   boolean isAccessible() {
      if (Modifier.isPublic(component_modifiers)) return true;
      return false;
    }
   
   JcompSymbol getDefinition() {
      if (field_asts.isEmpty()) return null;
      return JcompAst.getDefinition(field_asts.get(0).getName());
    }
   
   ScrapTypeAbstraction getFieldType() {
      return field_abstraction.getFieldType();
    }
   
   void clearData() {
     // field_asts.clear();
      component_scores = null;
   }
   
   @Override boolean isCompatibleWith(ScrapComponent sc) {
      if (sc instanceof ClassField) {
         ClassField cf = (ClassField) sc;
         if (field_abstraction != cf.field_abstraction) {
            return false;
          }
         return true;
       }
      return false;
    }
   
   void mergeWith(ClassField cf) {
      field_asts.addAll(cf.field_asts);
      super.mergeWith(cf);
    }
   
   private void outputField() {
      System.err.println("FLD-----" + getNames());
      ScrapTypeAbstraction typ = getFieldType();
      typ.outputArg("\t");
    }
   
   private void outputShortField() {
      ScrapTypeAbstraction typ = getFieldType();
      System.err.println("\t" + typ.getArgType() + " : " + getNames());
   }
   
   void addToUmlClass(SumpClass scls,AbstractTypeDeclaration atd) {
      for (VariableDeclarationFragment vdf : field_asts) {
         if (isChild(atd,vdf)) {
            JcompSymbol js = JcompAst.getDefinition(vdf);
            scls.addAttribute(js,vdf);
            break;
          }
       }
    }
   
   @Override public String toString() {
      return "FIELD " + getNames() + " : " + getFieldType().toString();
    }
   
}       // end of inner class ClassField




}       // end of class ScrapClassAbstraction




/* end of ScrapClassAbstraction.java */

