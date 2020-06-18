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
import edu.brown.cs.spur.rowel.RowelConstants;

public class SumpParameters implements SumpConstants, RowelConstants.RowelMatchType 
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

enum Parameters {
   MIN_FIELDS, MIN_METHODS, MIN_TYPES, MAX_TYPES, MIN_PACKAGE_TERM_MATCH,
      USE_PACKAGE_FIELDS,
      FIELD_MIN_MATCH, FIELD_MAX_MATCH,
      CONSTRUCTOR_MIN_MATCH, CONSTRUCTOR_MAX_MATCH,
      METHOD_MIN_MATCH, METHOD_MAX_MATCH,
      ENUM_MATCH,
      CLASS_MIN_MATCH, CLASS_MAX_MATCH,
      CLASS_CUTOFF, ATTR_CUTOFF, METHOD_CUTOFF, DEPEND_CUTOFF, SCORE_CUTOFF,
      INTERFACE_FRACTION, ENUM_FRACTION,WORD_FRACTION
}

private Map<Parameters,Object>      param_values;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SumpParameters() 
{
   param_values = new EnumMap<>(Parameters.class);
   for (Parameters p : Parameters.values()) {
      param_values.put(p,0);
    }
   param_values.put(Parameters.MIN_FIELDS,5);
   param_values.put(Parameters.MIN_METHODS,10);
   param_values.put(Parameters.MIN_TYPES,3);
   param_values.put(Parameters.MAX_TYPES,200);
   param_values.put(Parameters.MIN_PACKAGE_TERM_MATCH,25);
   param_values.put(Parameters.USE_PACKAGE_FIELDS,true);
   param_values.put(Parameters.FIELD_MIN_MATCH,0.5);
   param_values.put(Parameters.FIELD_MAX_MATCH,0.75);
   param_values.put(Parameters.CONSTRUCTOR_MIN_MATCH,0.1);
   param_values.put(Parameters.CONSTRUCTOR_MAX_MATCH,0.25);
   param_values.put(Parameters.METHOD_MIN_MATCH,0.5);
   param_values.put(Parameters.METHOD_MAX_MATCH,0.75);
   param_values.put(Parameters.ENUM_MATCH,0.75);
   param_values.put(Parameters.CLASS_MIN_MATCH,0.60);
   param_values.put(Parameters.CLASS_MAX_MATCH,0.75);
   param_values.put(Parameters.CLASS_CUTOFF,0.5);
   param_values.put(Parameters.ATTR_CUTOFF,0.5);
   param_values.put(Parameters.METHOD_CUTOFF,0.5);
   param_values.put(Parameters.DEPEND_CUTOFF,0.33);
   param_values.put(Parameters.SCORE_CUTOFF,0.50);
   param_values.put(Parameters.INTERFACE_FRACTION,0.25);
   param_values.put(Parameters.ENUM_FRACTION,0.75);
   param_values.put(Parameters.WORD_FRACTION,0.20);
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


public boolean usePackageFields()
{
   return getBoolean(Parameters.USE_PACKAGE_FIELDS);
}

public double getFieldMinMatch()
{
   return getDouble(Parameters.FIELD_MIN_MATCH);
}

public double getFieldMaxMatch()
{
   return getDouble(Parameters.FIELD_MAX_MATCH);
}

public double getConstructorMinMatch()
{
   return getDouble(Parameters.CONSTRUCTOR_MIN_MATCH);
}

public double getConstructorMaxMatch()
{
   return getDouble(Parameters.CONSTRUCTOR_MAX_MATCH);
}

public double getMethodMinMatch()
{
   return getDouble(Parameters.METHOD_MIN_MATCH);
}

public double getMethodMaxMatch()
{
   return getDouble(Parameters.METHOD_MAX_MATCH);
}

public double getEnumMatch()
{
   return getDouble(Parameters.ENUM_MATCH);
}

public double getClassMinMatch()
{
   return getDouble(Parameters.CLASS_MIN_MATCH); 
}

public double getClassMaxMatch()
{
   return getDouble(Parameters.CLASS_MAX_MATCH); 
}


public double getClassCutoff()
{
   return getDouble(Parameters.CLASS_CUTOFF);
}

public double getAttrCutoff()
{
   return getDouble(Parameters.ATTR_CUTOFF);
}

public double getMethodCutoff()
{
   return getDouble(Parameters.METHOD_CUTOFF);
}

public double getDependCutoff()
{
   return getDouble(Parameters.DEPEND_CUTOFF);
}

public double getScoreCutoff()
{
   return getDouble(Parameters.SCORE_CUTOFF);
}

public double getInterfaceFraction()
{
   return getDouble(Parameters.INTERFACE_FRACTION);
}

public double getEnumFraction()
{
   return getDouble(Parameters.ENUM_FRACTION);
}


public double getWordFraction()
{
   return getDouble(Parameters.WORD_FRACTION);
}




/********************************************************************************/
/*                                                                              */
/*      General methods                                                         */
/*                                                                              */
/********************************************************************************/

Map<String,Object> getNonDefaults()
{
   Map<String,Object> rslt = new HashMap<>();
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
   Object o = param_values.get(p);
   if (o == null) return 0;
   if (o instanceof Number) return ((Number) o).intValue();
   String sv = o.toString();
   try {
      return Integer.valueOf(sv);
    }
   catch (NumberFormatException e) { }
   return 0;
}


private double getDouble(Parameters p)
{
   Object o = param_values.get(p);
   if (o == null) return 0;
   if (o instanceof Number) return ((Number) o).doubleValue();
   String sv = o.toString();
   try {
      return Double.valueOf(sv);
    }
   catch (NumberFormatException e) { }
   return 0;
} 


@SuppressWarnings("unchecked")
private <T extends Enum<T>> T getEnum(Parameters p,T dflt)
{
   Object o = param_values.get(p);
   if (o == null) return dflt;
   if (o.getClass() == dflt.getClass()) return dflt;
   for (Object e : dflt.getClass().getEnumConstants()) {
      if (e.toString().equals(o.toString())) {
	  return (T) e;
      }
    }
   return dflt;
}


private boolean getBoolean(Parameters p)
{
   Object o = param_values.get(p);
   if (o == null) return false;
   if (o instanceof Boolean) return ((Boolean) o).booleanValue();
   String sv = o.toString();
   try {
      return Boolean.valueOf(sv);
    }
   catch (NumberFormatException e) { }
   sv = sv.toLowerCase();
   if (sv.startsWith("1") || sv.startsWith("t") || sv.startsWith("y")) return true;
   return false;
}

}       // end of class SumpParameters




/* end of SumpParameters.java */

