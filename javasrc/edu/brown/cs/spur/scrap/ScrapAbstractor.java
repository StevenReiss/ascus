/********************************************************************************/
/*                                                                              */
/*              ScrapAbstractor.java                                            */
/*                                                                              */
/*      Basic abstraction methods                                               */
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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseScores;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompExtendedSource;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompSource;
import edu.brown.cs.spur.sump.SumpParameters;

public class ScrapAbstractor implements ScrapConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private CoseRequest     base_request;
private Map<AbstractionType,Collection<ScrapAbstraction>> abstraction_map;
private Map<AbstractionType,Collection<ScrapAbstraction>> original_abstractions;
private int             num_inputs;
private JcompControl    jcomp_control;
private SumpParameters  search_params;



/**********************************t**********************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public ScrapAbstractor(CoseRequest rq,SumpParameters sp)
{
   base_request = rq;
   search_params = sp;
   abstraction_map = new HashMap<>();
   for (AbstractionType at : AbstractionType.values()) {
      abstraction_map.put(at,new HashSet<>());
    }
   original_abstractions = new HashMap<>();
   num_inputs = 0;
   jcomp_control = new JcompControl();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

CoseRequest getRequest()                        { return base_request; }

SumpParameters getParameters()                  { return search_params; }


JcompControl getJcompControl()  
{
   return jcomp_control;
}


Collection<ScrapAbstraction> getAbstractions(AbstractionType typ)
{
   return abstraction_map.get(typ);
}


Collection<ScrapAbstraction> getOriginalAbstractions(AbstractionType typ)
{
   return original_abstractions.get(typ);
}



/********************************************************************************/
/*                                                                              */
/*     Add new result to the abstractor                                         */
/*                                                                              */
/********************************************************************************/


public ScrapClassAbstraction addToAbstractor(CoseResult cr,AbstractTypeDeclaration td)
{
   return (ScrapClassAbstraction) getAbstraction(cr,((ASTNode) td));
}


public ScrapMethodAbstraction addToAbstractor(CoseResult cr,MethodDeclaration md)
{
   return (ScrapMethodAbstraction) getAbstraction(cr,((ASTNode) md));
}

public ScrapMethodAbstraction addToAbstraction(CoseResult cr,ScrapTypeAbstraction ret,
      Set<ScrapTypeAbstraction> args,boolean stat)
{
   ScrapMethodAbstraction sma = new ScrapMethodAbstraction(this,cr,ret,args,stat);
   sma = (ScrapMethodAbstraction) getAbstraction(sma);
   return sma;
}

public ScrapFieldAbstraction addToAbstractor(CoseResult cr,String name,ScrapTypeAbstraction typ,boolean stat)
{
   ScrapFieldAbstraction sfa = new ScrapFieldAbstraction(this,cr,name,typ,stat);
   sfa = (ScrapFieldAbstraction) getAbstraction(sfa);
   return sfa;
}


public ScrapFieldAbstraction addToAbstractor(CoseResult cr,VariableDeclarationFragment vd)
{
   return (ScrapFieldAbstraction) getAbstraction(cr,((ASTNode) vd));
}

public ScrapAbstraction addToAbstractor(CoseResult cr)
{
   return getAbstraction(cr,null);
}


private ScrapAbstraction getAbstraction(CoseResult cr,ASTNode n)
{
   ++num_inputs;
   
   ScrapAbstraction ourabs = createAbstraction(cr,n);
   
   return getAbstraction(ourabs);
}



private ScrapAbstraction getAbstraction(ScrapAbstraction ourabs)
{
   if (ourabs == null) return null;
   
   Collection<ScrapAbstraction> absset = abstraction_map.get(ourabs.getAbstractionType());
   for (ScrapAbstraction abs : absset) {
      if (abs.mergeWith(ourabs)) {
         return abs;
       }   
    }
   
   absset.add(ourabs);
   
   return ourabs;
}



private ScrapAbstraction  createAbstraction(CoseResult cr,ASTNode n)
{
   ScrapAbstraction rslt = null;
   
   if (n == null) n = (ASTNode) cr.getStructure();
   
   switch (n.getNodeType()) {
      case ASTNode.TYPE_DECLARATION :
      case ASTNode.ENUM_DECLARATION :
         rslt = new ScrapClassAbstraction(this,cr,(AbstractTypeDeclaration) n); 
         break;
      case ASTNode.METHOD_DECLARATION :
         rslt = new ScrapMethodAbstraction(this,cr,(MethodDeclaration) n);
         break;
      case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
         rslt = new ScrapFieldAbstraction(this,cr,(VariableDeclarationFragment) n);
         break;
      case ASTNode.COMPILATION_UNIT :
         rslt = new ScrapPackageAbstraction(this,cr,(CompilationUnit) n);
         break;
      default :
         break;
    }
   
   return rslt;
}




/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

JcompProject getResolvedAst(ASTNode an)
{
   if (an == null) return null;
   
   List<JcompSource> srcs = new ArrayList<>();
   JcompSource src = new ResultSource(an);
   srcs.add(src);
   List<String> jars = new ArrayList<>();
   
   JcompProject proj = jcomp_control.getProject(jars,srcs,false);
   try {
      ASTNode root = an.getRoot();
      synchronized (root) {
         proj.resolve();
       }
    }
   catch (Throwable t) {
      t.printStackTrace();
      jcomp_control.freeProject(proj);
      return null;
    }
   
   return proj;
}



/********************************************************************************/
/*                                                                              */
/*      Jcomp Source for result                                                 */
/*                                                                              */
/********************************************************************************/

private static class ResultSource implements JcompExtendedSource {

   private ASTNode root_result;
   
   ResultSource(ASTNode nd) {
      root_result = nd.getRoot();
    }
   
   @Override public String getFileContents() {
      return root_result.toString();
    }
   
   @Override public String getFileName() {
      return "*SCRAP*";
    }
   
   @Override public ASTNode getAstRootNode() {
      return root_result;
    }

}       // end of inner class ResultSource



/********************************************************************************/
/*                                                                              */
/*      Pruning methods                                                         */
/*                                                                              */
/********************************************************************************/

public void orderAndPrune()
{
   checkClasses();
   
   int max = 0;
   int maxm = 0;
   int maxf = 0;
   int maxt = 0;
   int maxp = 0;
   double maxscore = 0;
   for (Collection<ScrapAbstraction> absset : abstraction_map.values()) {
      for (ScrapAbstraction sa : absset) { 
         int ct = sa.getUseCount();
         if (sa instanceof ScrapClassAbstraction) maxt = Math.max(maxt,ct);
         else if (sa instanceof ScrapMethodAbstraction) maxm = Math.max(maxm,ct);
         else if (sa instanceof ScrapFieldAbstraction) maxf = Math.max(maxf,ct);
         else if (sa instanceof ScrapPackageAbstraction) maxp = Math.max(maxp,ct);
         max = Math.max(max,ct);
         double scr = getScore(sa);
         if (scr > maxscore) maxscore = scr;
       }
    }
   
   double cutoff = 0;
   double scutoff = 0;
   switch (base_request.getCoseSearchType()) {
      case CLASS :
         cutoff = 0.2;
         break;
      case METHOD :
         cutoff = 0.5;
         break;
      case PACKAGE :
         cutoff = 0;
         break;
      default :
         break;
    }
   
   double thresholdm = Math.min(maxm*cutoff,num_inputs/4);
   double thresholdf = Math.min(maxf*cutoff,num_inputs/4);
   double thresholdt = Math.min(maxt*cutoff,num_inputs/4);
   double thresholdp = Math.min(maxp*cutoff,num_inputs/4);
   double thresholds = maxscore*scutoff;
   
   for (AbstractionType aty : abstraction_map.keySet()) {
      Set<ScrapAbstraction> sorter = new TreeSet<>(new AbsComparator());
      Collection<ScrapAbstraction> absset = abstraction_map.get(aty);
      if (original_abstractions.get(aty) == null) {
         original_abstractions.put(aty,absset);
       }

      for (ScrapAbstraction sa : absset) { 
         int ct = sa.getUseCount();
         if (sa instanceof ScrapMethodAbstraction) {
            if (ct < thresholdm) continue;
          }
         else if (sa instanceof ScrapFieldAbstraction) {
            if (ct < thresholdf) continue;
          }
         else if (sa instanceof ScrapClassAbstraction) {
            if (ct < thresholdt) continue;
          }
         else if (sa instanceof ScrapPackageAbstraction) {
            if (ct < thresholdp) continue;
          }
         if (getScore(sa) < thresholds) continue;
         
         sorter.add(sa);
         // if (aty == AbstractionType.PACKAGE) {
            // System.err.println("ADD PACKAGE");
            // for (CoseResult cr : sa.getAllResults()) {
               // CoseScores cs = cr.getScores(base_request);
               // double x = cs.getDouble("TERMMATCHES");
               // x /= cs.getDouble("NODES");
               // System.err.println("    " + cr.getSource());
               // System.err.println("    " + cr.getBasePackage());
               // System.err.println("    " + x + " " + cs.getInt("TERMMATCHES") + " " + cs.getInt("NODES"));
               // for (Map.Entry<String,Object> ent : cs.entrySet()) {
                  // System.err.println("\t" + ent.getKey() + " = " + ent.getValue());
                // }
             // }
          // }
       }
      abstraction_map.put(aty,new ArrayList<>(sorter));
    }
}



private class AbsComparator implements Comparator<ScrapAbstraction> {

   @Override public int compare(ScrapAbstraction a1,ScrapAbstraction a2) {
      if (a1 == a2) return 0;
      int ct1 = a1.getUseCount();
      int ct2 = a2.getUseCount();
      if (ct1 > ct2) return -1;
      if (ct2 > ct1) return 1;
      double cs1 = getScore(a1);
      double cs2 = getScore(a2);
      if (cs1 > cs2) return -1;
      if (cs2 > cs1) return 1;
      return a1.toString().compareTo(a2.toString());
    }
   
}       // end of AbsComparator



private double getScore(ScrapAbstraction abs) {
   double tot = 0;
   for (CoseResult cr : abs.getAllResults()) {
      CoseScores cs = cr.getScores(base_request);
      double x = 0;
      switch (abs.getAbstractionType()) {
         case TYPE :
            break;
         case CLASS :
         case FIELD :
         case METHOD :
            x = cs.getDouble("TERMMATCH");
            tot += x;
            break;
         case PACKAGE :
            x = cs.getDouble("TERMDENSITY");
            tot = Math.max(tot,x);
            break;
       }
    }
   return tot;
}



private void checkClasses()
{
   List<ScrapClassAbstraction> clss = new ArrayList<>();
   Collection<ScrapAbstraction> cabs = abstraction_map.get(AbstractionType.CLASS);
   Map<ScrapClassAbstraction,ScrapClassAbstraction> absmap = new HashMap<>();
   
   for (ScrapAbstraction sa : cabs) {
      if (sa instanceof ScrapClassAbstraction) {
         ScrapClassAbstraction ca = (ScrapClassAbstraction) sa;
         clss.add(ca);
       }
    }
   
   for (int i = 0; i < clss.size(); ++i) {
      ScrapClassAbstraction ca1 = clss.get(i);
      for (int j = i+1; j < clss.size(); ++j) {
         ScrapClassAbstraction ca2 = clss.get(j);
         if (ca1.mergeWith(ca2)) {
            absmap.put(ca2,ca1);
            cabs.remove(ca2);
          }
       }
    }
   
   if (absmap.size() == 0) return;
   
   Collection<ScrapAbstraction> pabs = abstraction_map.get(AbstractionType.PACKAGE);
   List<ScrapPackageAbstraction> pkgs = new ArrayList<>();
   for (ScrapAbstraction sa : pabs) {
      if (sa instanceof ScrapPackageAbstraction) {
         ScrapPackageAbstraction pa = (ScrapPackageAbstraction) sa;
         pkgs.add(pa);
         pa.applyClassMapping(absmap);
       }
    }
   
   for (int i = 0; i < pkgs.size(); ++i) {
      ScrapPackageAbstraction pa1 = pkgs.get(i);
      for (int j = i+1; j < pkgs.size(); ++j) {
         ScrapPackageAbstraction pa2 = pkgs.get(j);
         if (pa1.mergeWith(pa2)) {
            pabs.remove(pa2);
          }
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Check for textually related abstractions                                */
/*                                                                              */
/********************************************************************************/

public void findRelatedAbstractions(ScrapAbstraction abs)
{
   
}




/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

void outputAbstractor(AbstractionType at)
{
   Collection<ScrapAbstraction> absset = abstraction_map.get(at);
   System.err.println("ABSTRACTOR: " + absset.size());
   for (ScrapAbstraction sa : absset) {
      if (sa.getAbstractionType() == at) {
         sa.buildUmlModels();
         sa.getReferencedLibraries();
         sa.outputAbstraction();
       }
    }
   System.err.println("\n\n=========================================\n");
}


/********************************************************************************/

}       // end of class ScrapAbstractor




/* end of ScrapAbstractor.java */

