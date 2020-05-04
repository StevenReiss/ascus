/********************************************************************************/
/*                                                                              */
/*              ScrapMergeData.java                                             */
/*                                                                              */
/*      Handle merging of classes and packages                                  */
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ScrapMergeData implements ScrapConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<ScrapComponent,ScrapComponent> component_map;
private Set<ScrapComponent> add_components;
private Set<ScrapComponent> used_components;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

ScrapMergeData() 
{
   component_map = new HashMap<>();
   add_components = new HashSet<>();
   used_components = new HashSet<>();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

boolean isUsed(ScrapComponent cc) 
{
   return used_components.contains(cc);
}


<T extends ScrapComponent> List<T> removeUsed(Collection<T> orig)
{
   List<T> rslt =  new ArrayList<>();
   for (T sc : orig) {
      if (!isUsed(sc)) rslt.add(sc);
    }
   
   return rslt;
}



ScrapComponent getMapping(ScrapComponent cc) 
{
   return component_map.get(cc);
}


boolean isAdded(ScrapComponent cc)
{
   return add_components.contains(cc);
}



/********************************************************************************/
/*                                                                              */
/*      Add methods                                                             */
/*                                                                              */
/********************************************************************************/

void addAssociation(ScrapComponent c1,ScrapComponent c2) 
{
   add_components.add(c1);
   used_components.add(c1);
   if (c2 != null) used_components.add(c2);
}



void addMapping(ScrapComponent c1,ScrapComponent c2) 
{
   component_map.putIfAbsent(c1,c2);
   component_map.putIfAbsent(c2,c1);
   used_components.add(c1);
   used_components.add(c2);
}


}       // end of class ScrapMergeData




/* end of ScrapMergeData.java */

