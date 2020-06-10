/********************************************************************************/
/*                                                                              */
/*              SumpParameters.java                                             */
/*                                                                              */
/*      User settable parameters for absraction and matching                    */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/

/* SVN: $Id$ */



package edu.brown.cs.spur.sump;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import edu.brown.cs.ivy.file.IvyLog;

public class SumpParameters implements SumpConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

enum Parameters {
   MIN_FIELDS, MIN_METHODS, MIN_TYPES, MAX_TYPES, MIN_PACKAGE_TERM_MATCH,
}

private Map<Parameters,Double>      param_values;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SumpParameters() 
{
   param_values = new EnumMap<>(Parameters.class);
   for (Parameters p : Parameters.values()) {
      param_values.put(p,0.0);
    }
   param_values.put(Parameters.MIN_FIELDS,5.0);
   param_values.put(Parameters.MIN_METHODS,10.0);
   param_values.put(Parameters.MIN_TYPES,3.0);
   param_values.put(Parameters.MAX_TYPES,200.0);
   param_values.put(Parameters.MIN_PACKAGE_TERM_MATCH,25.0);
}



/********************************************************************************/
/*                                                                              */
/*      Filtering parameters                                                    */
/*                                                                              */
/********************************************************************************/

public int getMinFields()
{
   return getInt(Parameters.MIN_FIELDS);
}


public int getMinMethods() 
{
   return getInt(Parameters.MIN_METHODS);
}


public int getMinTypes() 
{
   return getInt(Parameters.MIN_TYPES);
}


public int getMaxTypes()
{
   return getInt(Parameters.MAX_TYPES);
}


public int getMinPackageTermMatches()
{
   return getInt(Parameters.MIN_PACKAGE_TERM_MATCH);
}




/********************************************************************************/
/*                                                                              */
/*      General methods                                                         */
/*                                                                              */
/********************************************************************************/

Map<String,Double> getNonDefaults()
{
   Map<String,Double> rslt = new HashMap<>();
   SumpParameters dflt = new SumpParameters();
   
   for (Parameters p : Parameters.values()) {
      if (!param_values.get(p).equals(dflt.param_values.get(p))) {
         rslt.put(p.toString(),param_values.get(p));
       }
    }
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Setter methods                                                          */
/*                                                                              */
/********************************************************************************/

public void set(String nm,double v)
{
   try {
      Parameters p = Parameters.valueOf(nm);
      param_values.put(p,v);
    }
   catch (IllegalArgumentException e) {
      IvyLog.logE("SUMP","Bad parameter name " + nm);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Worker methods                                                          */
/*                                                                              */
/********************************************************************************/

private int getInt(Parameters p)
{
   Double d = param_values.get(p);
   if (d == null) return 0;
   return d.intValue();
}

private double getDouble(Parameters p)
{
   Double d = param_values.get(p);
   if (d == null) return 0;
   return d.doubleValue();
}



}       // end of class SumpParameters




/* end of SumpParameters.java */

