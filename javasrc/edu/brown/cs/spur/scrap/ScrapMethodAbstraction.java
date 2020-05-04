/********************************************************************************/
/*                                                                              */
/*              ScrapMethodAbstraction.java                                     */
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.spur.sump.SumpArgType;

class ScrapMethodAbstraction extends ScrapAbstraction
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Set<ScrapTypeAbstraction> call_args;
private ScrapTypeAbstraction      return_arg;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ScrapMethodAbstraction(ScrapAbstractor abs,CoseResult cr,MethodDeclaration md)
{
   super(abs,cr,md);
   call_args = null;
   return_arg = null;
   initialize(cr,md);
}


ScrapMethodAbstraction(ScrapAbstractor abs,CoseResult cr,ScrapTypeAbstraction ret,
      Collection<ScrapTypeAbstraction> args)
{
   super(abs,cr,null);
   call_args = new HashSet<>();
   if (args != null) call_args.addAll(args);
   return_arg = ret;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override AbstractionType getAbstractionType()
{
   return AbstractionType.METHOD;
}


Set<ScrapTypeAbstraction> getParameterTypes()           { return call_args; }

ScrapTypeAbstraction getReturnType()                    { return return_arg; }



/********************************************************************************/
/*                                                                              */
/*      Merge methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public boolean mergeWith(ScrapAbstraction abs)
{
   if (!(abs instanceof ScrapMethodAbstraction)) return false;
   
   ScrapMethodAbstraction sma = (ScrapMethodAbstraction) abs;
   
   if (!sma.return_arg.isEquivalent(return_arg)) return false;
   
   Set<ScrapTypeAbstraction> done = new HashSet<>();
   Map<ScrapTypeAbstraction,ScrapTypeAbstraction> match = new HashMap<>();
   
   for (ScrapTypeAbstraction ma : sma.call_args) {
      boolean fnd = false;
      for (ScrapTypeAbstraction ourma : call_args) {
         if (done.contains(ourma)) continue;
         if (ourma.isEquivalent(ma)) { 
            match.put(ma,ourma);
            done.add(ourma);
            fnd = true;
            break;
          }
       }
      if (!fnd) return false;
    }
   for (ScrapTypeAbstraction ourma : call_args) {
      if (!done.contains(ourma)) return false;
    }
   
   for (ScrapTypeAbstraction ma : sma.call_args) {
      ScrapTypeAbstraction ourma = match.get(ma);
      ourma.mergeWith(ma);
    }
   return_arg.mergeWith(sma.return_arg);
   
   superMergeWith(sma);
   
   return true;
}



boolean isComparable(ScrapAbstraction abs)
{
   if (!(abs instanceof ScrapMethodAbstraction)) return false;
   if (abs == this) return true;
   
   ScrapMethodAbstraction sma = (ScrapMethodAbstraction) abs;
   
   if (!sma.return_arg.isEquivalent(return_arg)) {
      if (return_arg.getArgType() != SumpArgType.VOID &&
            sma.return_arg.getArgType() != SumpArgType.VOID) {
         return false;
       }
    }
            
   Set<ScrapTypeAbstraction> done = new HashSet<>();
   Map<ScrapTypeAbstraction,ScrapTypeAbstraction> match = new HashMap<>();
   for (ScrapTypeAbstraction ma : sma.call_args) {
      boolean fnd = false;
      for (ScrapTypeAbstraction ourma : call_args) {
         if (done.contains(ourma)) continue;
         if (ourma.isEquivalent(ma)) { 
            match.put(ma,ourma);
            done.add(ourma);
            fnd = true;
            break;
          }
       }
      if (!fnd) {
         if (isImportant(ma)) return false;
       }
    }
   for (ScrapTypeAbstraction ourma : call_args) {
      if (!done.contains(ourma) && isImportant(ourma)) return false;
    }
   
   return true;
}



private boolean isImportant(ScrapTypeAbstraction ma)
{
   switch (ma.getArgType()) {
      case VOID :
      case BOOLEAN :
         return false;
      default :
         return true;
    }
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
   if (proj == null) {
      proj = getResolvedAst(an);
      JcompAst.setProject(an,proj);
      if (proj == null) return;
    }
   
   try {
      JcompSymbol js = JcompAst.getDefinition(an);
      JcompType mtyp = JcompAst.getJavaType(an);
      call_args = new HashSet<>();
      for (JcompType atyp : mtyp.getComponents()) {
         ScrapTypeAbstraction ma = new ScrapTypeAbstraction(proj,atyp,an);
         call_args.add(ma);
       }
      return_arg = new ScrapTypeAbstraction(proj,mtyp.getBaseType(),an);
      name_set.add(js.getName());
    }
   finally {
      if (oproj == null) {
         jcomp_main.freeProject(proj);
         JcompAst.setProject(an,null);
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override void outputAbstraction()
{
   System.err.println("METHOD ABSTRACTION FOR " + getResultCount() + " Results");
   for (String s : name_set) System.err.println("\t: " + s);
   System.err.println("    RETURN:");
   return_arg.outputArg("\t");
   System.err.println("    ARGS:");
   for (ScrapTypeAbstraction ma : call_args) {
      ma.outputArg("\t");
    }
}



}       // end of class ScrapMethodAbstraction




/* end of ScrapMethodAbstraction.java */

