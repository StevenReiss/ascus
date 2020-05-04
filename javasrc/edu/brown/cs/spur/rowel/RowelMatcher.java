/********************************************************************************/
/*                                                                              */
/*              RowelMatcher.java                                               */
/*                                                                              */
/*      Match two sets (bipartite matching) for matching items in various ways  */
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
/* Copyright (c) 2012 Kevin L. Stern
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package edu.brown.cs.spur.rowel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.brown.cs.spur.rowel.RowelConstants.RowelMatch;

public class RowelMatcher<T extends RowelMatch> implements RowelConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<T> source_set;
private List<T> target_set;

private double [][]     cost_matrix;
private int             row_count;
private int             col_count;
private int             row_col_dim;
private double []       label_by_worker;
private double []       label_by_job;
private int []          min_slack_worker_by_job;
private double []       min_slack_value_by_job;
private int []          match_job_by_worker;
private int []          match_worker_by_job;
private int []          parent_worker_by_committed_job;
private boolean []      committed_workers;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public RowelMatcher(Collection<T> src,Collection<T> tgt)
{
   source_set = new ArrayList<>(src);
   target_set = new ArrayList<>(tgt);
}


/********************************************************************************/
/*                                                                              */
/*      Gale Shapley matching algorithm                                         */
/*                                                                              */
/********************************************************************************/

public Map<T,T> bestMatch(RowelMatchType mt)
{
   setup(mt);
   
   reduce();
   computeInitialFeasibleSolution();
   greedyMatch();
   
   int w = fetchUnmatchedWorker();
   while (w < row_col_dim) {
      initializePhase(w);
      executePhase();
      w = fetchUnmatchedWorker();
    }
   
   Map<T,T> rslt = new HashMap<>();
   for (w = 0; w < row_count; ++w) {
      int j = match_job_by_worker[w];
      if (j >= 0 && j < col_count) {
         rslt.put(source_set.get(w),target_set.get(j));
       }
    }
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Initialization                                                          */
/*                                                                              */
/********************************************************************************/

private void setup(RowelMatchType mt)
{
   row_count = source_set.size();
   col_count = target_set.size();
   row_col_dim = Math.max(row_count,col_count);
   cost_matrix = new double[row_col_dim][row_col_dim];
   for (int i = 0; i < source_set.size(); ++i) {
      RowelMatch src = source_set.get(i);
      for (int j = 0; j < target_set.size(); ++j) {
         RowelMatch tgt = target_set.get(i);
         double score = src.getMatchScore(tgt,mt);
         cost_matrix[i][j] = score;
       }
    }
   label_by_worker = new double[row_col_dim];
   label_by_job = new double[row_col_dim];
   min_slack_worker_by_job = new int[row_col_dim];
   min_slack_value_by_job = new double[row_col_dim];
   committed_workers = new boolean[row_col_dim];
   parent_worker_by_committed_job = new int[row_col_dim];
   match_job_by_worker = new int[row_col_dim];
   Arrays.fill(match_job_by_worker,-1);
   match_worker_by_job = new int[row_col_dim];
   Arrays.fill(match_worker_by_job,-1);
}



/********************************************************************************/
/*                                                                              */
/*      Reduce the cost matrix to normalize each row                            */
/*                                                                              */
/********************************************************************************/

private void reduce()
{
   for (int w = 0; w < row_col_dim; ++w) {
      double min = Double.POSITIVE_INFINITY;
      for (int j = 0; j < row_col_dim; ++j) {
         if (cost_matrix[w][j] < min) min = cost_matrix[w][j];
       }
      for (int j = 0; j < row_col_dim; ++j) {
         cost_matrix[w][j] -= min;
       }
    }
   double [] min = new double[row_col_dim];
   Arrays.fill(min,Double.POSITIVE_INFINITY);
   for (int w = 0; w < row_col_dim; ++w) {
      for (int j = 0; j < row_col_dim; ++j) {
         if (cost_matrix[w][j] < min[j]) min[j] = cost_matrix[w][j];
       }
    }
   for (int w = 0; w < row_col_dim; ++w) {
      for (int j = 0; j < row_col_dim; ++j) {
         cost_matrix[w][j] -= min[j];
       }
    }
}


/********************************************************************************/
/*                                                                              */
/*      Compute initial feasible solution                                       */
/*                                                                              */
/********************************************************************************/

private void computeInitialFeasibleSolution()
{
   Arrays.fill(label_by_job,Double.POSITIVE_INFINITY);
   for (int w = 0; w < row_col_dim; ++w) {
      for (int j = 0; j < row_col_dim; ++j) {
         if (cost_matrix[w][j] < label_by_job[j]) {
            label_by_job[j] = cost_matrix[w][j];
          }
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*       Find a valid matching greedily                                         */
/*                                                                              */
/********************************************************************************/

private void greedyMatch()
{
   for (int w = 0; w < row_col_dim; ++w) {
      for (int j = 0; j < row_col_dim; ++j) {
         if (match_job_by_worker[w] == -1 && match_worker_by_job[j] == -1 &&
               cost_matrix[w][j] - label_by_worker[w] - label_by_job[j] == 0) {
            match(w,j);
          }
       }
    }
}



private void match(int w,int j)
{
   match_job_by_worker[w] = j;
   match_worker_by_job[j] = w;
}



/********************************************************************************/
/*                                                                              */
/*      Initialization phase                                                    */
/*                                                                              */
/********************************************************************************/

private void initializePhase(int w)
{
   Arrays.fill(committed_workers,false);
   Arrays.fill(parent_worker_by_committed_job,-1);
   committed_workers[w] = true;
   for (int j = 0; j < row_col_dim; ++j) {
      min_slack_value_by_job[j] = cost_matrix[w][j] - label_by_worker[w] -
         label_by_job[j];
      min_slack_worker_by_job[j] = w;
    }
}



/********************************************************************************/
/*                                                                              */
/*      Execution phase                                                         */
/*                                                                              */
/********************************************************************************/

private void executePhase()
{
   while (true) {
      int minslackworker = -1;
      int minslackjob = -1;
      double minslackvalue = Double.POSITIVE_INFINITY;
      for (int j = 0; j < row_col_dim; ++j) {
         if (parent_worker_by_committed_job[j] == -1) {
            if (min_slack_value_by_job[j] < minslackvalue) {
               minslackvalue = min_slack_value_by_job[j];
               minslackworker = min_slack_worker_by_job[j];
               minslackjob = j;
             }
          }
       }
      if (minslackvalue > 0) {
         updateLabeling(minslackvalue);
       }
      parent_worker_by_committed_job[minslackjob] = minslackworker;
      if (match_worker_by_job[minslackjob] == -1) {
         // augmenting path found
         int committedjob = minslackjob;
         int parentworker = parent_worker_by_committed_job[committedjob];
         while (true) {
            int temp = match_job_by_worker[parentworker];
            match(parentworker,committedjob);
            committedjob = temp;
            if (committedjob == -1) break;
            parentworker = parent_worker_by_committed_job[committedjob];
          }
         return;
       }
      else {
         // update slack values
         int worker = match_worker_by_job[minslackjob];
         committed_workers[worker] = true;
         for (int j = 0; j < row_col_dim; ++j) {
            if (parent_worker_by_committed_job[j] == -1) {
               double slack = cost_matrix[worker][j] - label_by_worker[worker] -
                  label_by_job[j];
               if (min_slack_value_by_job[j] > slack) {
                  min_slack_value_by_job[j] = slack;
                  min_slack_worker_by_job[j] = worker;
                }
             }
          }
       }
    }
}




/********************************************************************************/
/*                                                                              */
/*      Execution helpers                                                       */
/*                                                                              */
/********************************************************************************/

private int fetchUnmatchedWorker() 
{
   int w;
   for (w = 0; w < row_col_dim; ++w) {
      if (match_job_by_worker[w] == -1) break;
    }
   return w;
}



private void updateLabeling(double slack)
{
   for (int w = 0; w < row_col_dim; ++w) {
      if (committed_workers[w]) {
         label_by_worker[w] += slack;
       }
    }
   for (int j = 0; j < row_col_dim; ++j) {
      if (parent_worker_by_committed_job[j] != -1) {
         label_by_job[j] -= slack;
       }
      else {
         min_slack_value_by_job[j] -= slack;
       }
    }
}



}       // end of class RowelMatcher




/* end of RowelMatcher.java */

