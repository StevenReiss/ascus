/********************************************************************************/
/*                                                                              */
/*              StareDriver.java                                                */
/*                                                                              */
/*      Driver for handling transformation and repair                           */
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



package edu.brown.cs.spur.stare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StareDriver implements StareConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<StareSolution>     solution_set;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public StareDriver()
{
   solution_set = new ArrayList<>();
}



/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

public void addInitialSolution(StareCandidateSolution scs)
{
   solution_set.add(new StareSolutionBase(scs));
}


public void addInitialSolutions(Collection<? extends StareCandidateSolution> scss)
{
   for (StareCandidateSolution scs : scss) addInitialSolution(scs);
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

public void process()
{
   for (StareSolution sc : solution_set) {
      System.err.println("MATCH: \n" + sc.getCoseResult().getEditText());
      sc.generateCode();
    }
}



}       // end of class StareDriver




/* end of StareDriver.java */

