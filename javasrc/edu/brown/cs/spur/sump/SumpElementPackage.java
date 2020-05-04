/********************************************************************************/
/*                                                                              */
/*              SumpPackageElement.java                                         */
/*                                                                              */
/*      Implementation of a UML package                                         */
/*                                                                              */
/********************************************************************************/



package edu.brown.cs.spur.sump;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.spur.sump.SumpConstants.SumpPackage;

class SumpElementPackage extends SumpElementBase implements SumpPackage
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,SumpElementClass> class_map;
private List<SumpElementDependency> class_depends;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SumpElementPackage(SumpModelBase mdl,String name)
{
   super(mdl,name);
   class_map = new HashMap<>();
   class_depends = new ArrayList<>();
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public ElementType getElementType()
{
   return ElementType.PACKAGE;
}


@Override public Collection<SumpClass> getClasses()
{
   return new ArrayList<>(class_map.values());
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public SumpClass addClass(AbstractTypeDeclaration atd)
{
   SumpElementClass cls = new SumpElementClass(sump_model,atd);
   
   String nm = cls.getName();
   class_map.put(nm,cls);
   
   return cls;
}



@Override public void addDependencies(AbstractTypeDeclaration atd,Map<String,SumpClass> cmap)
{
   JcompType ourtyp = JcompAst.getJavaType(atd);
   Set<SumpClass> done = new HashSet<>();
   for (Object o : atd.bodyDeclarations()) {
      if (o instanceof FieldDeclaration) {
         FieldDeclaration fd = (FieldDeclaration) o;
         JcompType jt = JcompAst.getJavaType(fd.getType());
         addDependenciesFor(jt,ourtyp,cmap,done);
       }
      else if (o instanceof MethodDeclaration) {
         MethodDeclaration md = (MethodDeclaration) o;
         if (md.isConstructor()) continue;
         JcompType mtyp = JcompAst.getJavaType(md);
         JcompType rtyp = mtyp.getBaseType();
         addDependenciesFor(rtyp,ourtyp,cmap,done);
       }
    }
   if (ourtyp.getOuterType() != null) {
      JcompType jt = ourtyp.getOuterType();
      boolean skip = false;
      if (jt.isInterfaceType()) {
         TypeDeclaration td = (TypeDeclaration) jt.getDefinition().getDefinitionNode();
         for (Object o : td.modifiers()) {
            if (o instanceof Annotation) {
               Annotation an = (Annotation) o;
               if (an.getTypeName().getFullyQualifiedName().contains("AscusPackage")) skip = true;
             }
          }
       }
      if (!skip) addDependenciesFor(jt,ourtyp,cmap,done);
    }
}


@Override public SumpDependency addDependency(SumpClass frm,SumpClass to)
{
   SumpElementDependency sd = new SumpElementDependency(sump_model,frm,to);
   class_depends.add(sd);
   return sd;
}



@Override public Collection<SumpDependency> getDependencies()
{
   return new ArrayList<>(class_depends);
}



/********************************************************************************/
/*                                                                              */
/*      Creation helper methods                                                 */
/*                                                                              */
/********************************************************************************/

private void addDependenciesFor(JcompType jt,JcompType ourtyp,Map<String,SumpClass> cmap,
        Set<SumpClass> done)
{
   if (jt.isPrimitiveType()) return;
   if (jt.isTypeVariable()) return;
   if (jt.isParameterizedType()) {
      addDependenciesFor(jt.getBaseType(),ourtyp,cmap,done);
      for (JcompType pty : jt.getComponents()) {
         addDependenciesFor(pty,ourtyp,cmap,done);
       }
      return;
    }
   if (jt == ourtyp) return;
   if (jt.isBinaryType()) return;
   
   String pfx = null;
   String ourname = ourtyp.getName();
   String onm = ourname;
   int idx = ourname.lastIndexOf(".");
   if (idx >= 0) {
      pfx = onm.substring(0,idx+1);
      onm = onm.substring(idx+1);
    }
   for (JcompType oty = ourtyp.getOuterType(); oty != null; oty = oty.getOuterType()) {
      int idx1 = pfx.lastIndexOf(".",pfx.length()-2);
      if (idx1 > 0) pfx = pfx.substring(0,idx1+1);
    }
   
   String nm = jt.getName();
   if (nm.equals(ourname)) return;
   if (pfx != null && !nm.startsWith(pfx)) return;
   int idx2 = nm.lastIndexOf(".");
   if (idx2 > 0) nm = nm.substring(idx2+1);
   
   if (cmap.get(nm) != null) {
      SumpClass us = cmap.get(onm);
      SumpClass to = cmap.get(nm);
      if (us != null && to != null && us != to) {
         if (done.add(to)) addDependency(us,to);
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override void outputXml(IvyXmlWriter xw)
{
   xw.begin("PACKAGE");
   basicXml(xw);
   for (SumpElementClass sc : class_map.values()) {
      sc.outputXml(xw);
    }
   for (SumpElementDependency sd : class_depends) {
      sd.outputXml(xw);
    }
   xw.end("PACKAGE");
}


@Override void outputJava(PrintWriter pw)
{
   outputComment(pw);
   
   for (SumpElementClass sc : class_map.values()) {
      pw.println();
      sc.outputJava(pw);
    }
}

@Override void setupJava()
{
   for (SumpElementClass sc : class_map.values()) {
      sc.setupJava();
    }
}


}       // end of class SumpPackageElement




/* end of SumpPackageElement.java */
