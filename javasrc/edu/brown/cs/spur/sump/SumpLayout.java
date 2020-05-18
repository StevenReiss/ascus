/********************************************************************************/
/*                                                                              */
/*              SumpLayout.java                                                 */
/*                                                                              */
/*      Layout the UML class graph so we can generate outside representations   */
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



package edu.brown.cs.spur.sump;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import edu.brown.cs.ivy.petal.PetalArc;
import edu.brown.cs.ivy.petal.PetalArcDefault;
import edu.brown.cs.ivy.petal.PetalEditor;
import edu.brown.cs.ivy.petal.PetalLayoutMethod;
import edu.brown.cs.ivy.petal.PetalLevelLayout;
import edu.brown.cs.ivy.petal.PetalModelDefault;
import edu.brown.cs.ivy.petal.PetalNode;
import edu.brown.cs.ivy.petal.PetalNodeDefault;
import edu.brown.cs.ivy.swing.SwingGridPanel;

class SumpLayout implements SumpConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private SumpPackage for_package;
private Map<SumpClass,PetalNode> class_components;
private Map<SumpDependency,PetalArc> arc_components;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SumpLayout(SumpPackage pkg)
{
   for_package = pkg;
   class_components = new HashMap<>();
   arc_components = new HashMap<>();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

Rectangle getBounds(SumpClass cls)
{
   PetalNode pn = class_components.get(cls);
   if (pn == null) return null;
   Component c = pn.getComponent();
   return c.getBounds();
}


Point [] getPoints(SumpDependency dep)
{
   PetalArc pa = arc_components.get(dep);
   return pa.getPoints();
}



/********************************************************************************/
/*                                                                              */
/*      Main Layout Method                                                      */
/*                                                                              */
/********************************************************************************/

void process()
{
   PetalModelDefault mdl = new PetalModelDefault();
   
   for (SumpClass sc : for_package.getClasses()) {
      JComponent jc = createClassComponent(sc);
      jc.setSize(jc.getPreferredSize());
      PetalNode pn = new PetalNodeDefault(jc);
      class_components.put(sc,pn);
      mdl.addNode(pn);
    }
   for (SumpDependency sd : for_package.getDependencies()) {
      SumpClass sc1 = sd.getFromClass();
      SumpClass sc2 = sd.getToClass();
      PetalArcDefault pa = new PetalArcDefault(class_components.get(sc1),
            class_components.get(sc2));
      mdl.addArc(pa);
      arc_components.put(sd,pa);
    }
   
   PetalEditor pe = new PetalEditor(mdl);
   pe.setGridSize(10);
   pe.setMinimumSize(new Dimension(100,100));
   pe.setSize(1024,1024);
   pe.update();
   pe.setSize(pe.getPreferredSize());
   PetalLayoutMethod plm = new PetalLevelLayout(pe);
   pe.commandLayout(plm);
   pe.validate();
}



/********************************************************************************/
/*                                                                              */
/*      Create class component                                                  */
/*                                                                              */
/********************************************************************************/

private JComponent createClassComponent(SumpClass sc)
{
   SwingGridPanel pnl = new SwingGridPanel();
   String nm = sc.getName();
   int idx = nm.lastIndexOf(".");
   if (idx > 0) nm = nm.substring(idx+1);
   JLabel ttl = new JLabel(nm);
   int y = 0;
   pnl.addGBComponent(ttl,0,y++,0,1,0,10);
   JSeparator sep = new JSeparator();
   pnl.addGBComponent(sep,0,y++,0,1,0,10);
   for (SumpAttribute sa : sc.getAttributes()) {
      String pfx = " ";
      if (sa.getAccess() == null) pfx = " ";
      else if (sa.getAccess() == ElementAccess.PUBLIC) pfx = "+";
      else if (sa.getAccess() == ElementAccess.PRIVATE) pfx = "-";
      String anm = sa.getName();
      SumpDataType dt = sa.getDataType();
      String tnm = dt.getName();
      if (tnm != null) {
         int idx1 = tnm.lastIndexOf(".");
         if (idx1 > 0) tnm = tnm.substring(idx1+1);
       }
      else tnm = "?";
      String fld = anm + ": " + tnm;
      JLabel l1 = new JLabel(pfx);
      JLabel l2 = new JLabel(fld);
      pnl.addGBComponent(l1,0,y,1,1,0,0);
      pnl.addGBComponent(l2,1,y++,1,1,0,10);
    }
   JSeparator sep1 = new JSeparator();
   pnl.addGBComponent(sep1,0,y++,0,1,0,10);
   for (SumpOperation so : sc.getOperations()) {
      String pfx = " ";
      if (so.getAccess() == null) pfx = " ";
      else if (so.getAccess() == ElementAccess.PUBLIC) pfx = "+";
      else if (so.getAccess() == ElementAccess.PRIVATE) pfx = "-";
      String anm = so.getName();
      SumpDataType dt = so.getReturnType();
      String tnm = dt.getName();
      if (tnm != null) {
         int idx1 = tnm.lastIndexOf(".");
         if (idx1 > 0) tnm = tnm.substring(idx1+1);
       }
      else tnm = "?";
      String fld = anm + "(): " + tnm;
      JLabel l1 = new JLabel(pfx);
      JLabel l2 = new JLabel(fld);
      pnl.addGBComponent(l1,0,y,1,1,0,0);
      pnl.addGBComponent(l2,1,y++,1,1,0,10);
    }   
   return pnl;
}




}       // end of class SumpLayout




/* end of SumpLayout.java */

