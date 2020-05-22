/********************************************************************************/
/*                                                                              */
/*              EtchTransformFixParameters.java                                 */
/*                                                                              */
/*      Make parameter order and type match the model                           */
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



package edu.brown.cs.spur.etch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.ivy.jcomp.JcompTyper;
import edu.brown.cs.spur.sump.SumpConstants.SumpModel;
import edu.brown.cs.spur.sump.SumpConstants.SumpParameter;
import edu.brown.cs.spur.sump.SumpConstants.SumpOperation;

class EtchTransformFixParameters extends EtchTransform
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,String> name_map;

private static final String    LOCAL_SUFFIX = "_local";


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

EtchTransformFixParameters(Map<String,String> namemap)
{
   super("FixParameters");
   name_map = namemap;
}


/********************************************************************************/
/*                                                                              */
/*      Apply the transform                                                     */
/*                                                                              */
/********************************************************************************/

@Override protected EtchMemo applyTransform(ASTNode n,SumpModel target)
{
   ParamMapper mapper =  findMappings(n,target);
   if (mapper == null) return null;
   
   EtchMemo memo =  mapper.getMapMemo(n);
   
   return memo;
}



/********************************************************************************/
/*                                                                              */
/*      Find parameter mappings                                                 */
/*                                                                              */
/********************************************************************************/

private ParamMapper findMappings(ASTNode cu,SumpModel target)
{
   ParamMapper mapper = new ParamMapper();
   
   findMatchings(cu,target,mapper);
   
   if (mapper.isEmpty()) return null;
   
   return mapper;
}




/********************************************************************************/
/*                                                                              */
/*      Identify all parameters to update                                       */
/*                                                                              */
/********************************************************************************/

private void findMatchings(ASTNode cu,SumpModel target,ParamMapper mapper)
{
   ParamVisitor sp = new ParamVisitor(target,mapper);
   cu.accept(sp);
}






private class ParamVisitor extends ASTVisitor {

   private SumpModel target_model;
   private ParamMapper param_mapper;
   
   ParamVisitor(SumpModel t,ParamMapper pm) {
      target_model = t;
      param_mapper = pm;
    }
   
   @Override public boolean visit(MethodDeclaration md) {
      JcompSymbol jm = JcompAst.getDefinition(md);
      String mnm = getMapName(jm);
      ParamFix pf = null;
      int ct = 0;
      for (Object o : md.parameters()) {
         SingleVariableDeclaration svd = (SingleVariableDeclaration) o;
         JcompSymbol pm = JcompAst.getDefinition(svd.getName());
         String pnm = mnm + "." + pm.getName();
         String match = name_map.get(pnm);
         if (match != null) {
            SumpOperation op = findOperation(match,target_model);
            int pct = 0;
            SumpParameter usesp = null;
            for (SumpParameter sp : op.getParameters()) {
               if (sp.getFullName().equals(match)) {
                  usesp = sp;
                  break;
                }
               ++pct;
             }
            if (usesp != null) {
               boolean samenm = pm.getName().equals(usesp.getName());
               String ptypnm = usesp.getDataType().getBaseType().getName();
               String btypnm = name_map.get(pm.getType().getName());
               if (btypnm == null) btypnm = pm.getType().getName();
               boolean samety = ptypnm.equals(btypnm);
               boolean sameord = ct == pct;
               if (!samenm || !samety || !sameord) {
                  if (pf == null) {
                     pf = new ParamFix();
                     param_mapper.addParameterFix(jm,pf);
                   }
                  if (!samety) pf.setParameterType(pm,ptypnm);
                  if (!samenm) pf.setParameterName(pm,usesp.getName());
                  if (!sameord) pf.setParameterOrder(pm,pct);
                }
             }
          }
         ++ct;
       }
      
      return false;
    }
   
}       // end of inner class ParamVisitor




/********************************************************************************/
/*                                                                              */
/*      Actual mapping transform                                                */
/*                                                                              */
/********************************************************************************/

private class ParamMapper extends EtchMapper {

   private Map<JcompSymbol,ParamFix> fix_set;
   private Map<JcompSymbol,String> param_remap; 
   
   ParamMapper() {
      super(EtchTransformFixParameters.this);
      fix_set = new HashMap<>();
      param_remap = new HashMap<>();
    }
   
   boolean isEmpty()                            { return fix_set.isEmpty(); }
   
   void addParameterFix(JcompSymbol m,ParamFix pf) {
      fix_set.put(m,pf);
    }
   
   @Override void preVisit(ASTNode orig) {
      if (orig instanceof MethodDeclaration) {
         JcompSymbol jm = JcompAst.getDefinition(orig);
         ParamFix pf = fix_set.get(jm);
         if (pf != null) {
            for (Map.Entry<JcompSymbol,String> ent : pf.getNameMap().entrySet()) {
               JcompSymbol js = ent.getKey();
               String nm = ent.getValue();
               String tnm = pf.getTypeMap().get(js);
               if (tnm != null) nm += LOCAL_SUFFIX;
               param_remap.put(js,nm);
             }
          }
       }
    }
   
   @Override void rewriteTree(ASTNode orig,ASTRewrite rw) {
      if (orig instanceof MethodDeclaration) {
         JcompSymbol jm = JcompAst.getDefinition(orig);
         ParamFix pf = fix_set.get(jm);
         if (pf != null) {
            fixParameters((MethodDeclaration) orig,rw,pf);
          }
         param_remap.clear();
       }
      else if (orig instanceof SimpleName) {
         JcompSymbol js = JcompAst.getReference(orig);
         if (js != null && param_remap != null) {
            String newnm = param_remap.get(js);
            if (newnm != null) {
               rw.set(orig,SimpleName.IDENTIFIER_PROPERTY,newnm,null);
             }
          }
       }
    }
   
   private void fixParameters(MethodDeclaration md,ASTRewrite rw,ParamFix pf) {
      Map<JcompSymbol,String> names = pf.getNameMap();
      Map<JcompSymbol,Integer> order = pf.getOrderMap();
      Map<JcompSymbol,String> types = pf.getTypeMap();
      List<JcompSymbol> params = new ArrayList<>();
      Map<JcompSymbol,SingleVariableDeclaration> origmap = new HashMap<>();
      List<SingleVariableDeclaration> origlist = new ArrayList<>();
      for (Object o : md.parameters()) {
         SingleVariableDeclaration svd = (SingleVariableDeclaration) o;
         JcompSymbol js = JcompAst.getDefinition(svd.getName());
         params.add(js);
         origmap.put(js,svd);
         origlist.add(svd);
       }
      List<JcompSymbol> orderp = new ArrayList<>(params);
      for (Map.Entry<JcompSymbol,Integer> ent : order.entrySet()) {
         int v = ent.getValue();
         orderp.add(v,ent.getKey());
       }
      ListRewrite lrw = rw.getListRewrite(md,MethodDeclaration.PARAMETERS_PROPERTY);
      for (int i = 0; i < params.size(); ++i) {
         JcompSymbol js = params.get(i);
         if (orderp.get(i) != js || types.get(js) != null || names.get(js) != null) {
            SingleVariableDeclaration osvd = origmap.get(js);
            SingleVariableDeclaration rsvd = origlist.get(i);
            SingleVariableDeclaration svd = rw.getAST().newSingleVariableDeclaration();
            String nm = names.get(js);
            if (nm == null) nm = js.getName();
            Type typast = null;
            if (types.get(js) != null) {
               JcompTyper typer = JcompAst.getTyper(md);
               JcompType jt = typer.findSystemType(types.get(js));
               typast = jt.createAstNode(rw.getAST());
             }
            else {
               typast = (Type) dupNode(rw.getAST(),osvd.getType());
             }
            svd.setType(typast);
            SimpleName sn = JcompAst.getSimpleName(rw.getAST(),nm);
            svd.setName(sn);
            lrw.replace(rsvd,svd,null);
          }
       }
      if (types.size() > 0) {
         int ct = 0;
         Block body = md.getBody();
         for (Object o : body.statements()) {
            if (o instanceof SuperConstructorInvocation) ct = 1;
            else if (o instanceof ConstructorInvocation) ct = 1;
            break;
          }
         ListRewrite slrw = rw.getListRewrite(body,Block.STATEMENTS_PROPERTY);
         for (Map.Entry<JcompSymbol,String> ent : types.entrySet()) {
            JcompTyper typer = JcompAst.getTyper(md);
            JcompType jt = typer.findSystemType(ent.getValue());
            JcompSymbol js = ent.getKey();
            String nm = names.get(js);
            if (nm == null) nm = js.getName();
            Statement st = createCast(rw.getAST(),nm + LOCAL_SUFFIX,js.getType(),nm,jt);
            slrw.insertAt(st,ct++,null);
          }
       }
      
    }
   
}       // end of inner class ParamMapper



/********************************************************************************/
/*                                                                              */
/*      Class to describe parameter fixes                                       */
/*                                                                              */
/********************************************************************************/

private static class ParamFix {

   private Map<JcompSymbol,String> fix_types;
   private Map<JcompSymbol,String> fix_names;
   private Map<JcompSymbol,Integer> fix_order;
   
   ParamFix() {
      fix_types = new HashMap<>();
      fix_names = new HashMap<>();
      fix_order = new HashMap<>();
    }
   
   Map<JcompSymbol,String> getNameMap()         { return fix_names; }
   Map<JcompSymbol,Integer> getOrderMap()       { return fix_order; }
   Map<JcompSymbol,String> getTypeMap()         { return fix_types; }
   
   void setParameterType(JcompSymbol pm,String typ) {
      fix_types.put(pm,typ);
    }
   
   void setParameterName(JcompSymbol pm,String nm) {
      fix_names.put(pm,nm);
    }
   
   void setParameterOrder(JcompSymbol pm,int ord) {
      fix_order.put(pm,ord);
    }
   
}       // end of inner class ParamFix



}       // end of class EtchTransformFixParameters




/* end of EtchTransformFixParameters.java */

