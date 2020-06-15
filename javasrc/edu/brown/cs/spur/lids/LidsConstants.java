/********************************************************************************/
/*                                                                              */
/*              LidsConstants.java                                              */
/*                                                                              */
/*      LIbrary Discovery via Search constants                                  */
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



package edu.brown.cs.spur.lids;



public interface LidsConstants
{


abstract class LidsLibrary {
   abstract public String getName();
   abstract public String getVersion();
   abstract public String getId();
   abstract public String getFullId();
   
   public String getGroup() {
      String s = getId();
      int idx = s.indexOf(":");
      if (idx > 0) s = s.substring(0,idx);
      return s;
    }
   
   @Override public boolean equals(Object o) {
      if (o instanceof LidsLibrary) {
         LidsLibrary ll = (LidsLibrary) o;
         return getFullId().equals(ll.getFullId());
       }
      return false;
    }
   
   @Override public int hashCode() {
      return getFullId().hashCode();
    }
   
   @Override public String toString() {
      return getId() + "@" + getName() + "@" + getVersion();
    }
   
}       // end of inner class LidsLibrary




}       // end of interface LidsConstants




/* end of LidsConstants.java */

