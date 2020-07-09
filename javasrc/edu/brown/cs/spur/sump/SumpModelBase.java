/********************************************************************************/
/*                                                                              */
/*              SumpModel.java                                                  */
/*                                                                              */
/*      UML model representaiton                                                */
/*                                                                              */
/********************************************************************************/



package edu.brown.cs.spur.sump;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.w3c.dom.Element;

import edu.brown.cs.cose.cosecommon.CoseDefaultRequest;
import edu.brown.cs.cose.cosecommon.CoseRequest;
import edu.brown.cs.cose.cosecommon.CoseConstants.CoseSearchEngine;
import edu.brown.cs.cose.cosecommon.CoseRequest.CoseKeywordSet;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.spur.lids.LidsInstaller;
import edu.brown.cs.spur.lids.LidsConstants.LidsLibrary;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;

public class SumpModelBase implements SumpConstants, SumpModel
{



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SumpElementPackage model_package;
private SumpLayout model_layout;
private SumpData model_data;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SumpModelBase(SumpData data)
{
   model_package = null;
   model_data = data;
}



public SumpModelBase(JcompControl ctrl,File f)
{
   this(new SumpData(new CoseDefaultRequest(),null,null));
   if (f != null && f.exists()) {
      String nm = f.getName();
      model_data.setSource(f.getPath());
      String ext = "";
      int idx = nm.lastIndexOf(".");
      if (idx > 0) ext = nm.substring(idx+1);
      switch (ext) {
         case "java" :
         case "ascus" :
            loadJava(ctrl,f);
            break;
         case "xml" :
            // loadXml(f);
            break;
         case "uxf" :
            loadUxf(ctrl,f);
            break;
         case "xmi" :
            loadXmi(ctrl,f);
            break;
         default :
            break;
       }
    }
}


public SumpModelBase(SumpData data,CompilationUnit cu)
{
   this(data);
   createModel(cu);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public SumpPackage setPackage(String name)
{
   if (model_package != null && model_package.getName().equals(name))
      return model_package;
   
   if (model_package != null) model_package.setName(name);
   else model_package = new SumpElementPackage(this,name);
   
   return model_package;
}

@Override public SumpPackage getPackage()
{
   return model_package;
}


@Override public Collection<SumpClass> findUsedClasses(SumpClass cls)
{
   Set<SumpClass> rslt = new HashSet<>();
   for (SumpDependency dp : model_package.getDependencies()) {
      if (dp.getFromClass() == cls) {
         rslt.add((SumpElementClass) dp.getToClass());
       }
    }
   return rslt;
}


@Override public SumpData getModelData()                { return model_data; }


@Override public Collection<SumpClass> getDependentClasses(SumpClass sc)
{
   Set<SumpClass> rslt = new HashSet<>();
   
   for (SumpDependency sd : getPackage().getDependencies()) {
      if (sd.getFromClass() == sc) {
         rslt.add(sd.getToClass());
       }
      else if (sd.getToClass() == sc) {
         rslt.add(sd.getFromClass());
       }
    }
   
   return rslt;
}


@Override public Collection<SumpClass> getInheritedClasses(SumpClass sc)
{
   Set<SumpClass> rslt = new HashSet<>();
   
   Set<String> names = new HashSet<>();
   String sup = sc.getSuperClassName();
   if (sup != null) names.add(sup);
   Collection<String> ifcs = sc.getInterfaceNames();
   if (ifcs != null) names.addAll(ifcs);
   for (SumpClass cls : model_package.getClasses()) {
      if (names.contains(cls.getName()) || names.contains(cls.getFullName()))
         rslt.add(cls);
    }
      
   return rslt;   
}


@Override public SumpClass getClassForName(String nm)
{
   if (nm == null) return null;
   
   for (SumpClass cls : model_package.getClasses()) {
      if (nm.equals(cls.getName()) || nm.equals(cls.getFullName()))
         return cls;
    }
   
   return null;
}



@Override public JcompProject resolveModel(JcompControl ctrl,CompilationUnit cu)
{
   if (cu == null) return null;
   
   if (ctrl == null) ctrl = new JcompControl();
   
   List<String> jars = new ArrayList<>();
   if (getModelData().getContextPath() != null) {
      jars.add(getModelData().getContextPath());
    }
   LidsInstaller inst = LidsInstaller.getInstaller();
   for (LidsLibrary ll : getModelData().getLibraries()) {
      String cp = inst.getClassPath(ll);
      if (cp != null) jars.add(cp);
    }
   
   if (JcompAst.isResolved(cu)) return null;
   
   JcompProject proj = JcompAst.getResolvedAst(ctrl,cu,jars);
   JcompAst.setProject(cu,proj);
   
   return proj;
}



/********************************************************************************/
/*                                                                              */
/*      Creation methods                                                        */
/*                                                                              */
/********************************************************************************/

private void createModel(CompilationUnit cu)
{
   PackageDeclaration pd = cu.getPackage();
   SumpPackage pkg = null;
   if (pd == null) {
      pkg = setPackage("*DEFAULT*");
    }
   else {
      pkg = setPackage(pd.getName().getFullyQualifiedName());
    }
   for (Object o : pd.annotations()) {
      Annotation an = (Annotation) o;
      String nm = an.getTypeName().getFullyQualifiedName();
      int idx = nm.lastIndexOf(".");
      if (idx > 0) nm = nm.substring(idx+1);
      if (nm.equals("Ascus")) handleAscus(an);
      else if (nm.equals("AscusSet")) handleAscusSet(an);
      else if (nm.equals("AscusTestFile")) handleAscusTestFile(an);
    }
   
   Map<String,SumpClass> cmap = new HashMap<>();
   Map<String,AbstractTypeDeclaration> tmap = new HashMap<>();
   for (Object o : cu.types()) {
      if (o instanceof AbstractTypeDeclaration) {
         AbstractTypeDeclaration td = (AbstractTypeDeclaration) o;
         addTypes(td,pkg,cmap,tmap);
       }
    }
   for (Object o : cu.types()) {
      if (o instanceof AbstractTypeDeclaration) {
         AbstractTypeDeclaration td = (AbstractTypeDeclaration) o;
         addAnnotDepends(td,cmap);
       }
    }
   
   for (String s : cmap.keySet()) {
      SumpClass cls = cmap.get(s);
      AbstractTypeDeclaration td = tmap.get(s);
      addClassData(cls,td);
    }
   
   for (AbstractTypeDeclaration td : tmap.values()) {
      pkg.addDependencies(td,cmap);
    }
}




private void addTypes(AbstractTypeDeclaration td,SumpPackage pkg,Map<String,SumpClass> cmap,
        Map<String,AbstractTypeDeclaration> tmap)
{
   
   boolean skip = false;
   for (Object o : td.modifiers()) {
      if (o instanceof Annotation) {
         Annotation an = (Annotation) o;
         String anm = an.getTypeName().getFullyQualifiedName();
         int idx = anm.lastIndexOf(".");
         if (idx > 0) anm = anm.substring(idx+1);
         if (anm.equals("AscusPackage")) {
            skip = true;
            String nm = td.getName().getIdentifier();
            model_data.setName(nm);
            String pnm = getPackage().getName() + "." + nm;
            setPackage(pnm);
          }
         else if (anm.equals("AscusTest")) {
            handleAscusTest(an);
            skip = true;
          }
       }
    }
   
   if (!skip) {
      SumpClass cls = pkg.addClass(td);
      String nm = cls.getName();
      cmap.put(nm,cls);
      tmap.put(nm,td);
    }
   
   for (Object o : td.bodyDeclarations()) {
      if (o instanceof AbstractTypeDeclaration) {
         AbstractTypeDeclaration inner = (AbstractTypeDeclaration) o;
         addTypes(inner,pkg,cmap,tmap);
       }
    }
}



private void addAnnotDepends(AbstractTypeDeclaration td,Map<String,SumpClass> cmap)
{
   String nm = td.getName().getIdentifier();
   
   for (Object o : td.modifiers()) {
      if (o instanceof Annotation) {
         Annotation an = (Annotation) o;
         String anm = an.getTypeName().getFullyQualifiedName();
         int idx = anm.lastIndexOf(".");
         if (idx > 0) anm = anm.substring(idx+1);
         if (anm.equals("AscusClass")) {
            if (an.isSingleMemberAnnotation()) {
               SingleMemberAnnotation sma = (SingleMemberAnnotation) an;
               Expression exp = sma.getValue();
               addDepends(nm,exp,cmap);
             }
            else if (an.isNormalAnnotation()) {
               NormalAnnotation nan = (NormalAnnotation) an;
               for (Object ov : nan.values()) {
                  MemberValuePair mvp = (MemberValuePair) ov;
                  if (mvp.getName().getIdentifier().equals("uses")) {
                     Expression exp = mvp.getValue();
                     addDepends(nm,exp,cmap);
                   }
                }
             }
          }
       }
    }
   
   for (Object o : td.bodyDeclarations()) {
      if (o instanceof TypeDeclaration) {
         TypeDeclaration inner = (TypeDeclaration) o;
         addAnnotDepends(inner,cmap);
       }
    }
}
  


private void addDepends(String nm,Expression exp,Map<String,SumpClass> cmap)
{
   if (exp instanceof ArrayInitializer) {
      ArrayInitializer arr = (ArrayInitializer) exp;
      for (Object o : arr.expressions()) {
         Expression e = (Expression) o;
         addDepends(nm,e,cmap);
       }
    }
   else if (exp instanceof TypeLiteral) {
      TypeLiteral tl = (TypeLiteral) exp;
      JcompType jt = JcompAst.getJavaType(tl.getType());
      String tnm = null;
      if (jt != null) tnm = jt.getName();
      else tnm = tl.getType().toString();
      int idx1 = tnm.lastIndexOf(".");
      if (idx1 > 0) tnm = tnm.substring(idx1+1);
      idx1 = tnm.lastIndexOf("$");
      if (idx1 > 0) tnm = tnm.substring(idx1+1);
      SumpClass c1 = cmap.get(nm);
      SumpClass c2 = cmap.get(tnm);
      if (c1 != null && c2 != null && c1 != c2) {
         addDepends(c1,c2);
       }
    }
}


private void addDepends(SumpClass c1,SumpClass c2)
{
   for (SumpDependency sd : model_package.getDependencies()) {
      if (sd.getFromClass() == c1 && sd.getToClass() == c2) return;
    }
   
   model_package.addDependency(c1,c2);
}






private void addClassData(SumpClass cls,AbstractTypeDeclaration atd)
{
   for (Object o : atd.bodyDeclarations()) {
      if (o instanceof FieldDeclaration) {
         FieldDeclaration fd = (FieldDeclaration) o;
         for (Object o1 : fd.fragments()) {
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) o1;
            JcompSymbol js = JcompAst.getDefinition(vdf);
            cls.addAttribute(js,vdf);
          }
       }
      else if (o instanceof MethodDeclaration) {
         MethodDeclaration md = (MethodDeclaration) o;
         JcompSymbol ms = JcompAst.getDefinition(md);
         cls.addOperation(ms,md);
       }
    }
   if (atd instanceof EnumDeclaration) {
      for (Object o : ((EnumDeclaration) atd).enumConstants()) {
         EnumConstantDeclaration ecd = (EnumConstantDeclaration) o;
         JcompSymbol js = JcompAst.getDefinition(ecd);
         cls.addEnumConstant(js,ecd);
       }
    }
}



private void handleAscusSet(Annotation an)
{
   Expression ex = null;
   if (an.isSingleMemberAnnotation()) {
      SingleMemberAnnotation sma = (SingleMemberAnnotation) an;
      ex = sma.getValue();
    }
   else if (an.isNormalAnnotation()) {
      NormalAnnotation na = (NormalAnnotation) an;
      na.values();
    }
   else return;
   if (ex == null) return;
}



private void handleAscusTest(Annotation an)
{
   
}



private void handleAscusTestFile(Annotation an)
{
   Expression ex = null;
   if (an.isSingleMemberAnnotation()) {
      SingleMemberAnnotation sma = (SingleMemberAnnotation) an;
      ex = sma.getValue();
    }
   else if (an.isNormalAnnotation()) {
      NormalAnnotation na = (NormalAnnotation) an;
      for (Object o : na.values()) {
         MemberValuePair mvp = (MemberValuePair) o;
         String nm = mvp.getName().getIdentifier();
         Expression val = mvp.getValue();
         switch (nm) {
            case "file" :
               ex = val;
               break;
            default :
               break;
          }
         if (ex != null) break;
       }
    }
   else return;
   if (ex == null) return;
   String s = getStringValue(ex);
   model_data.addTestFile(s);
}


private void handleAscus(Annotation an)
{
   CoseDefaultRequest cdr = (CoseDefaultRequest) model_data.getCoseRequest();
   
   NormalAnnotation na = (NormalAnnotation) an;
   for (Object o : na.values()) {
      MemberValuePair mvp = (MemberValuePair) o;
      String nm = mvp.getName().getIdentifier();
      Expression val = mvp.getValue();
      switch (nm) {
         case "source" :
            model_data.addSource(getStringValue(val));
            break;
         case "library" :
            model_data.addLibrary(getStringValue(val));
            break;
         case "libraries" :
            List<String> l1 = getStringValues(val,null);
            if (l1 != null) {
               for (String s : l1) model_data.addLibrary(s);
             }
            break;
         case "missing" :
            model_data.addMissingImport(getStringValue(val));
            break;
         case "keywords" :
            List<String> k1 = getStringValues(val,null);
            if (k1 != null && cdr != null) cdr.addKeywordSet(k1);
            break;
         case "keyterms" :
            List<String> k2 = getStringValues(val,null);
            if (k2 != null && cdr != null) {
               for (String s : k2) cdr.addKeyTerm(s);
             }
            break;
         case "search" :
            // might set search type, scope, count, engine from string
            break;
         case "parameter" :
            String pv = getStringValue(val);
            int idx = pv.indexOf("=");
            String pnm = pv;
            double vl = 0;
            if (idx > 0) {
               pnm = pv.substring(0,idx).trim();
               String vs = nm.substring(idx+1).trim();
               try {
                  vl = Double.valueOf(vs);
                }
               catch (NumberFormatException e) { }
             }
            else {
               vl = getDoubleValue(val);
             }
            model_data.getParameters().set(pnm,vl);
            break;
         case "context" :
            String cv = getStringValue(val);
            model_data.setContextPath(cv);
            break;
         case "suggestedTerms" :
            List<String> k3 = getStringValues(val,null);
            if (k3 != null) {
                for (String s : k3) model_data.addSuggestedWord(s);
             }
            break;
         default :
            System.err.println("UNKNOWN TAG : " + nm);
            break;
       }
    }
}


private String getStringValue(Expression e)
{
   switch (e.getNodeType()) {
      case ASTNode.STRING_LITERAL :
         StringLiteral sl = (StringLiteral) e;
         return sl.getLiteralValue();
      default :
         return null;
    }
}


private double getDoubleValue(Expression e)
{
   switch (e.getNodeType()) {
      case ASTNode.NUMBER_LITERAL :
         NumberLiteral nl = (NumberLiteral) e;
         return Double.valueOf(nl.getToken());
    }
   return 0.0;
}


private List<String> getStringValues(Expression exp,List<String> rslt)
{
   if (exp instanceof ArrayInitializer) {
      ArrayInitializer arr = (ArrayInitializer) exp;
      for (Object o : arr.expressions()) {
         Expression e = (Expression) o;
         rslt = getStringValues(e,rslt);
       }
    }
   else {
      String s = getStringValue(exp);
      if (s != null) {
         if (rslt == null) rslt = new ArrayList<>();
         rslt.add(s);
       }
    }

   return rslt;
}




/********************************************************************************/
/*                                                                              */
/*      Matching methods                                                        */
/*                                                                              */
/********************************************************************************/

@Override public boolean contains(SumpModel mdl)
{
   SumpMatcher match = new SumpMatcher();
   return match.matchScore(this,mdl,null) > 0;
}


@Override public double matchScore(SumpModel mdl,Map<String,String> namemap)
{
   SumpMatcher match = new SumpMatcher();
   return match.matchScore(this,mdl,namemap);
}


/********************************************************************************/
/*                                                                              */
/*      Layout methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public SumpLayout computeLayout()
{
   if (model_layout == null) {
      model_layout = new SumpLayout(model_package);
      model_layout.process();
    }
   return model_layout;
}


@Override public Rectangle getBounds(SumpClass cls)
{
   if (model_layout == null) return null;
   return model_layout.getBounds(cls);
}


/********************************************************************************/
/*                                                                              */
/*      Visitation methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public void accept(SumpVisitor sev)
{
   sev.setModel(this);
   if (!sev.visit(this)) return;
   model_package.accept(sev);
   sev.endVisit(this);
}





/********************************************************************************/
/*                                                                              */
/*      Generation methods                                                      */
/*                                                                              */
/********************************************************************************/

@Override public void outputXml(IvyXmlWriter xw)
{
   xw.begin("MODEL");
   xw.field("NAME",model_data.getName());
   for (String src : model_data.getSources()) {
      xw.textElement("SOURCE",src);
    }
   if (model_data.getContextPath() != null) {
      xw.textElement("CONTEXT",model_data.getContextPath());
    }
   for (LidsLibrary lib : model_data.getLibraries()) {
      xw.textElement("LIBRARY",lib.getFullId());
    }
   for (String s : model_data.getMissingImports()) {
      xw.textElement("MISSING",s);
    }
   CoseRequest cr = model_data.getCoseRequest();
   if (cr != null) {
      xw.begin("SEARCH");
      xw.field("TYPE",cr.getCoseSearchType());
      xw.field("SCOPE",cr.getCoseScopeType());
      xw.field("RESULTS",cr.getNumberOfResults());
      for (CoseSearchEngine se : cr.getEngines()) {
         xw.textElement("ENGINE",se.toString());
       }
      for (CoseKeywordSet cks : cr.getCoseKeywordSets()) {
         xw.begin("KEYWORDS");
         for (String s : cks.getWords()) {
            xw.textElement("WORD",s);
          }
         xw.end("KEYWORDS");
       }
      for (String s : cr.getKeyTerms()) {
         xw.textElement("TERM",s);
       }
      xw.end("SEARCH");
    }
   model_package.outputXml(xw);
   xw.end("MODEL");
}


@Override public void outputJava(Writer w)
{
   SumpJavaWriter sjw = new SumpJavaWriter(w);
   sjw.generateCode(this);
}


String getJavaOutputName(String orignm)
{
   SumpClass sc = getClassForName(orignm);
   String nm = orignm;
   if (sc != null) {
      SumpElementClass sec = (SumpElementClass) sc;
      nm = sec.getJavaOutputName();
    }
   else {
      int idx1 = nm.indexOf("<");
      if (idx1 < 0) {
         int idx2 = nm.lastIndexOf(".");
         if (idx2 > 0) nm = nm.substring(idx2+1);
       }
      else {
         int idx2 = nm.lastIndexOf(".",idx1);
         if (idx2 > 0) nm = nm.substring(idx2+1);
         // need to fix or remove parameters as well 
       }
    }
   
   return nm;
}



@Override public void save(File f) throws IOException
{
   IvyXmlWriter xw = new IvyXmlWriter(f);
   outputXml(xw);
   xw.close();
}


@Override public void generateXMI(Writer writer)
{
   SumpXmiWriter sx = new SumpXmiWriter(writer);
   sx.generateXmi(this);
}



@Override public void generateUXF(Writer writer)
{
   SumpUxfWriter sx = new SumpUxfWriter(writer);
   sx.generateUxf(this);
}





/********************************************************************************/
/*                                                                              */
/*      Load Sump Model from annotated Java file                                */
/*                                                                              */
/********************************************************************************/

private void loadJava(JcompControl ctrl,File f)
{
   if (ctrl == null) ctrl = new JcompControl();
   try {
      String cnts = IvyFile.loadFile(f);
      CompilationUnit cu = JcompAst.parseSourceFile(cnts);
      JcompProject proj = resolveModel(ctrl,cu);
      createModel(cu);
      ctrl.freeProject(proj);
    }
   catch (IOException e) { }
}



/********************************************************************************/
/*                                                                              */
/*      Load Sump Model from XMI file                                                       */
/*                                                                              */
/********************************************************************************/

private void loadXmi(JcompControl ctrl,File f)
{
   if (ctrl == null) ctrl = new JcompControl();
   
   Element e = IvyXml.loadXmlFromFile(f);
   if (e == null) return;
   
   
}



/********************************************************************************/
/*                                                                              */
/*      Load Sump Model from UXF file                                           */
/*                                                                              */
/********************************************************************************/

private void loadUxf(JcompControl ctrl,File f)
{
   if (ctrl == null) ctrl = new JcompControl();
   
   Element e = IvyXml.loadXmlFromFile(f);
   if (e == null) return;
   
   
}



}       // end of class SumpModel




/* end of SumpModel.java */
