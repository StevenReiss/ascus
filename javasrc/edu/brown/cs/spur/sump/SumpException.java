/********************************************************************************/
/*                                                                              */
/*              SumpException.java                                              */
/*                                                                              */
/*      Excetion for use with SUMP                                              */
/*                                                                              */
/********************************************************************************/



package edu.brown.cs.spur.sump;



public class SumpException extends Exception implements SumpConstants
{



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static final long serialVersionUID = 1;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SumpException(String msg)
{
   super(msg);
}


SumpException(String msg,Throwable t)
{
   super(msg,t);
}



}       // end of class SumpException




/* end of SumpException.java */
