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

enum ParameterName {
   MIN_FIELDS, MIN_METHODS, MIN_TYPES, MAX_TYPES, MIN_PACKAGE_TERM_MATCH,
      USE_PACKAGE_FIELDS,
      FIELD_MIN_MATCH, FIELD_MAX_MATCH,
      CONSTRUCTOR_MIN_MATCH, CONSTRUCTOR_MAX_MATCH,
      METHOD_MIN_MATCH, METHOD_MAX_MATCH,
      ENUM_MATCH,
      CLASS_MIN_MATCH, CLASS_MAX_MATCH,
      CLASS_CUTOFF, ATTR_CUTOFF, METHOD_CUTOFF, DEPEND_CUTOFF, SCORE_CUTOFF,
      INTERFACE_FRACTION, ENUM_FRACTION,WORD_FRACTION,
      CONVENTION_TYPE, CONVENTION_LOCAL, CONVENTION_PARAMETER, CONVENTION_FIELD,
      CONVENTION_CONSTANT, CONVENTION_METHOD,
      REMOVE_UNUSED, TIGHT_FILTER
}

private Map<ParameterName,Object>      param_values;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SumpParameters() 
{
   param_values = new EnumMap<>(ParameterName.class);
   param_values.put(ParameterName.MIN_FIELDS,5);
   param_values.put(ParameterName.MIN_METHODS,10);
   param_values.put(ParameterName.MIN_TYPES,3);
   param_values.put(ParameterName.MAX_TYPES,200);
   param_values.put(ParameterName.MIN_PACKAGE_TERM_MATCH,25);
   param_values.put(ParameterName.USE_PACKAGE_FIELDS,true);
   param_values.put(ParameterName.FIELD_MIN_MATCH,0.5);
   param_values.put(ParameterName.FIELD_MAX_MATCH,0.75);
   param_values.put(ParameterName.CONSTRUCTOR_MIN_MATCH,0.1);
   param_values.put(ParameterName.CONSTRUCTOR_MAX_MATCH,0.25);
   param_values.put(ParameterName.METHOD_MIN_MATCH,0.5);
   param_values.put(ParameterName.METHOD_MAX_MATCH,0.75);
   param_values.put(ParameterName.ENUM_MATCH,0.75);
   param_values.put(ParameterName.CLASS_MIN_MATCH,0.60);
   param_values.put(ParameterName.CLASS_MAX_MATCH,0.75);
   param_values.put(ParameterName.CLASS_CUTOFF,0.5);
   param_values.put(ParameterName.ATTR_CUTOFF,0.5);
   param_values.put(ParameterName.METHOD_CUTOFF,0.5);
   param_values.put(ParameterName.DEPEND_CUTOFF,0.33);
   param_values.put(ParameterName.SCORE_CUTOFF,0.50);
   param_values.put(ParameterName.INTERFACE_FRACTION,0.25);
   param_values.put(ParameterName.ENUM_FRACTION,0.75);
   param_values.put(ParameterName.WORD_FRACTION,0.20);
   param_values.put(ParameterName.TIGHT_FILTER,true);
}



/********************************************************************************/
/*                                                                              */
/*      Filtering parameters                                                    */
/*                                                                              */
/********************************************************************************/

public int getMinFields()
{
   return getInt(ParameterName.MIN_FIELDS);
}


public int getMinMethods() 
{
   return getInt(ParameterName.MIN_METHODS);
}


public int getMinTypes() 
{
   return getInt(ParameterName.MIN_TYPES);
}


public int getMaxTypes()
{
   return getInt(ParameterName.MAX_TYPES);
}


public int getMinPackageTermMatches()
{
   return getInt(ParameterName.MIN_PACKAGE_TERM_MATCH);
}


public boolean usePackageFields()
{
   return getBoolean(ParameterName.USE_PACKAGE_FIELDS);
}

public double getFieldMinMatch()
{
   return getDouble(ParameterName.FIELD_MIN_MATCH);
}

public double getFieldMaxMatch()
{
   return getDouble(ParameterName.FIELD_MAX_MATCH);
}

public double getConstructorMinMatch()
{
   return getDouble(ParameterName.CONSTRUCTOR_MIN_MATCH);
}

public double getConstructorMaxMatch()
{
   return getDouble(ParameterName.CONSTRUCTOR_MAX_MATCH);
}

public double getMethodMinMatch()
{
   return getDouble(ParameterName.METHOD_MIN_MATCH);
}

public double getMethodMaxMatch()
{
   return getDouble(ParameterName.METHOD_MAX_MATCH);
}

public double getEnumMatch()
{
   return getDouble(ParameterName.ENUM_MATCH);
}

public double getClassMinMatch()
{
   return getDouble(ParameterName.CLASS_MIN_MATCH); 
}

public double getClassMaxMatch()
{
   return getDouble(ParameterName.CLASS_MAX_MATCH); 
}


public double getClassCutoff()
{
   return getDouble(ParameterName.CLASS_CUTOFF);
}

public double getAttrCutoff()
{
   return getDouble(ParameterName.ATTR_CUTOFF);
}

public double getMethodCutoff()
{
   return getDouble(ParameterName.METHOD_CUTOFF);
}

public double getDependCutoff()
{
   return getDouble(ParameterName.DEPEND_CUTOFF);
}

public double getScoreCutoff()
{
   return getDouble(ParameterName.SCORE_CUTOFF);
}

public double getInterfaceFraction()
{
   return getDouble(ParameterName.INTERFACE_FRACTION);
}

public double getEnumFraction()
{
   return getDouble(ParameterName.ENUM_FRACTION);
}


public double getWordFraction()
{
   return getDouble(ParameterName.WORD_FRACTION);
}



public String getNamingConventions(NameType nt) 
{
   String id = "CONVENTION_" + nt.toString();
   try {
      ParameterName p = ParameterName.valueOf(id);
      return getString(p);
    }
   catch (IllegalArgumentException e) {
      return null;
    }
}



public boolean getRemoveUnused()
{
   return getBoolean(ParameterName.REMOVE_UNUSED);
}


public boolean useTightFiltering()
{
   return getBoolean(ParameterName.TIGHT_FILTER);
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
   
   for (ParameterName p : ParameterName.values()) {
      if (param_values.get(p) == null) continue;
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
      ParameterName p = ParameterName.valueOf(nm);
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

private int getInt(ParameterName p)
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


private double getDouble(ParameterName p)
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


private String getString(ParameterName p)
{
   Object o = param_values.get(p);
   if (o == null) return null;
   return o.toString();
}

@SuppressWarnings({ "unchecked", "unused" })
private <T extends Enum<T>> T getEnum(ParameterName p,T dflt)
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


private boolean getBoolean(ParameterName p)
{
   Object o = param_values.get(p);
   if (o == null) return false;
   if (o instanceof Boolean) return ((Boolean) o).booleanValue();
   if (o instanceof Number) {
      Number n = (Number) o;
      return n.doubleValue() != 0;
    }
   String sv = o.toString().toLowerCase();
   if (sv.startsWith("1") || sv.startsWith("t") || sv.startsWith("y")) return true;
   return false;
}

}       // end of class SumpParameters




/* end of SumpParameters.java */

