/********************************************************************************/
/*                                                                              */
/*              SwiftTokenGenerator.java                                        */
/*                                                                              */
/*      AST based token generation                                              */
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



package edu.brown.cs.spur.swift;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ExportsDirective;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.ModuleModifier;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.OpensDirective;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ProvidesDirective;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.RequiresDirective;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.UsesDirective;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.WildcardType;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompSymbol;
import edu.brown.cs.ivy.jcomp.JcompType;

class SwiftTokenGenerator extends ASTVisitor implements SwiftConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private         List<SwiftCodeToken>    ct_list;

private final static String LID = "$l$";
private final static String VID = "$v$";
private final static String GID = "$p$"; //General ID
private final static String TID = "$t$";
private final static String MID = "$m$";


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SwiftTokenGenerator()
{ }


/********************************************************************************/
/*                                                                              */
/*      Work methods                                                            */
/*                                                                              */
/********************************************************************************/

List<SwiftCodeToken> getCTs(ASTNode n)
{
   ct_list =  new ArrayList<>();
   
   if (n != null)  n.accept(this);
   
   return ct_list;
}


/********************************************************************************/
/*                                                                              */
/*      Type-specific methods                                                   */
/*                                                                              */
/********************************************************************************/

@Override public boolean visit(AnonymousClassDeclaration n)
{
   addKey("{");
   addList(n.bodyDeclarations());
   addKey("}");
   return false;
}


@Override public boolean visit(AnnotationTypeDeclaration n)
{
   addList(n.modifiers());
   addKey("@");
   addKey("interface");
   addNode(n.getName());
   addKey("(");
   addList(n.bodyDeclarations());
   addKey("}");
   return false;
}


@Override public boolean visit(EnumDeclaration n)
{
   addList(n.modifiers());
   addKey("enum");
   addNode(n.getName());
   addList(n.superInterfaceTypes(),"implements",",",null);
   addKey("{");
   addList(n.enumConstants(),null,",",null);
   addList(n.bodyDeclarations(),";",null,null);
   addKey("}");
   return false;
}



@Override public boolean visit(TypeDeclaration n)
{
   addList(n.modifiers());
   if (n.isInterface()) {
      addKey("interface");
      addNode(n.getName());
      addList(n.typeParameters(),"<",",",">");
      addList(n.superInterfaceTypes(),"extends",",",null);
    }
   else {
      addKey("class");
      addNode(n.getName());
      addList(n.typeParameters(),"<",",",">");
      addNode(n.getSuperclassType(),"extends",null);
      addList(n.superInterfaceTypes(),"implements",",",null);
    }
   addKey("{");
   addList(n.bodyDeclarations(),";",null,null);
   addKey("}");
   return false;
}



@Override public boolean visit(AnnotationTypeMemberDeclaration n)
{
   addList(n.modifiers());
   addNode(n.getType());
   addNode(n.getName());
   addNode(n.getDefault(),"default",null);
   addKey(";");
   return false;
}


@Override public boolean visit(EnumConstantDeclaration n)
{
   addList(n.modifiers());
   addNode(n.getName());
   addList(n.arguments(),"(",",",")");
   addNode(n.getAnonymousClassDeclaration());
   return false;
}


@Override public boolean visit(FieldDeclaration n)
{
   addList(n.modifiers());
   addNode(n.getType());
   addList(n.fragments(),null,",",null);
   addKey(";");
   return false;
}


@Override public boolean visit(Initializer n)
{
   addKey("static");
   addNode(n.getBody());
   return false;
}


@Override public boolean visit(MethodDeclaration n)
{
   addList(n.modifiers());
   addList(n.typeParameters(),"<",",",">");
   addNode(n.getReturnType2());
   addNode(n.getName());
   addKey("(");
   if (n.getReceiverQualifier() != null || n.getReceiverType() != null) {
      addNode(n.getReceiverType());
      addNode(n.getReceiverQualifier());
      addKey(",");
    }
   addList(n.parameters(),null,",",null);
   addKey(")");
   addList(n.extraDimensions());
   addList(n.thrownExceptionTypes(),"throws",",",null);
   if (n.getBody() != null) addNode(n.getBody());
   else addKey(";");
   
   return false;
}


@Override public boolean visit(CatchClause n) 
{
   addKey("catch");
   addKey("(");
   addNode(n.getException());
   addKey(")");
   addNode(n.getBody());
   return false;
}


@Override public boolean visit(CompilationUnit n)
{
   addNode(n.getPackage());
   addList(n.imports());
   if (n.getModule() != null) addNode(n.getModule());
   else if (n.types().size() > 0) {
      addList(n.types());
    }
   else addKey(";");
   
   return false;
}


@Override public boolean visit(Dimension n)
{
   addList(n.annotations());
   addKey("[");
   addKey("]");
   return false;
}


@Override public boolean visit(MarkerAnnotation n)
{
   addKey("@");
   addNode(n.getTypeName());
   return false;
}


@Override public boolean visit(NormalAnnotation n)
{
   addKey("@");
   addNode(n.getTypeName()); 
   addList(n.values(),"(",",",")");
   return false;
}


@Override public boolean visit(SingleMemberAnnotation n)
{
   addKey("@");
   addNode(n.getTypeName());
   addKey("(");
   addNode(n.getValue());
   addKey(")");
   return false;
}


@Override public boolean visit(ArrayAccess n)
{
   addNode(n.getArray());
   addKey("[");
   addNode(n.getIndex());
   addKey("]");
   return false;
}



@Override public boolean visit(ArrayCreation n)
{
   addKey("new");
   addNode(n.getType());
   for (Object o : n.dimensions()) {
      addKey("[");
      addNode((ASTNode) o);
      addKey("]");
    }
   addNode(n.getInitializer());
   return false;
}


@Override public boolean visit(ArrayInitializer n)
{
   addList(n.expressions(),"{",",","}");
   return false;
}


@Override public boolean visit(Assignment n)
{
   addNode(n.getLeftHandSide());
   addKey(n.getOperator().toString());
   addNode(n.getRightHandSide());
   return false;
}


@Override public boolean visit(BooleanLiteral n)
{
   addKey(LID);
   return false;
}


@Override public boolean visit(CastExpression n)
{
   addKey("(");
   addNode(n.getType());
   addKey(")");
   addNode(n.getExpression());
   return false;
}


@Override public boolean visit(CharacterLiteral n)
{
   addKey(LID);
   return false;
}


@Override public boolean visit(ClassInstanceCreation n)
{
   addNode(n.getExpression(),null,".");
   addKey("new");
   addList(n.typeArguments(),"<",",",">");
   addNode(n.getType());
   addKey("(");
   addList(n.arguments(),null,",",null);
   addKey(")");
   addNode(n.getAnonymousClassDeclaration());
   return false;
}


@Override public boolean visit(ConditionalExpression n)
{
   addNode(n.getExpression());
   addKey("?");
   addNode(n.getThenExpression());
   addKey(":");
   addNode(n.getElseExpression());
   return false;
}


@Override public boolean visit(FieldAccess n)
{
   addNode(n.getExpression());
   addKey(".");
   addNode(n.getName());
   return false;
}


@Override public boolean visit(InfixExpression n)
{
   String op = n.getOperator().toString();
   addNode(n.getLeftOperand());
   addKey(op);
   addNode(n.getRightOperand());
   addList(n.extendedOperands(),op,op,null);
   return false;
}



@Override public boolean visit(InstanceofExpression n)
{
   addNode(n.getLeftOperand());
   addKey("instanceof");
   addNode(n.getRightOperand());
   return false;
}


@Override public boolean visit(LambdaExpression n)
{
   addKey("(");
   addList(n.parameters(),null,",",null);
   addKey("->");
   addNode(n.getBody());
   return false;
}



@Override public boolean visit(MethodInvocation n)
{
   addNode(n.getExpression(),null,".");
   addList(n.typeArguments(),"<",",",">");
   addNode(n.getName());
   addKey("(");
   addList(n.arguments(),null,",",null);
   addKey(")");
   return false;
}


@Override public boolean visit(CreationReference n)
{
   addNode(n.getType());
   addKey("::");
   addList(n.typeArguments(),"<",",",">");
   addKey("new");
   return false;
}



@Override public boolean visit(ExpressionMethodReference n)
{
   addNode(n.getExpression());
   addKey("::");
   addList(n.typeArguments(),"<",",",">");
   addNode(n.getName());
   return false;
}



@Override public boolean visit(SuperMethodReference n)
{
   addNode(n.getQualifier(),null,".");
   addKey("super");
   addKey("::");
   addList(n.typeArguments(),"<",",",">");
   addNode(n.getName());
   return false;
}


@Override public boolean visit(TypeMethodReference n)
{
   addNode(n.getType());
   addKey("::");
   addList(n.typeArguments(),"<",",",">");
   addNode(n.getName());
   return false;
}


@Override public boolean visit(QualifiedName n)
{
   addNode(n.getQualifier());
   addKey(".");
   addNode(n.getName());
   return false;
}



@Override public boolean visit(SimpleName n)
{
   String ntyp = getNameType(n);
   if (ntyp != null) addKey(ntyp);
   return false;
}



private String getNameType(ASTNode n)
{
   JcompSymbol js = JcompAst.getDefinition(n);
   if (js == null) js = JcompAst.getReference(n);
   
   if (js != null) {
      switch (js.getSymbolKind()) {
         default :
         case ANNOTATION :
         case ANNOTATION_MEMBER :
         case NONE :
         case PACKAGE :
            return GID;
         case CLASS :
         case ENUM :
         case INTERFACE :
            return TID;
         case CONSTRUCTOR :
         case METHOD :
            return MID;
         case FIELD :
         case LOCAL :
            return VID;
       }
    }
   
   if (js == null) {
      JcompType jt = JcompAst.getJavaType(n);
      if (jt != null) {
         return TID;
       }
    }
   
   ASTNode par = n.getParent();
   StructuralPropertyDescriptor spd = n.getLocationInParent();
   
   switch (par.getNodeType()) {
      case ASTNode.METHOD_DECLARATION :
      case ASTNode.METHOD_INVOCATION :
      case ASTNode.SUPER_METHOD_INVOCATION :
         return MID;
      case ASTNode.TYPE_PARAMETER :
      case ASTNode.ANNOTATION_TYPE_DECLARATION :
      case ASTNode.QUALIFIED_TYPE :
      case ASTNode.TYPE_LITERAL :
      case ASTNode.SIMPLE_TYPE :
      case ASTNode.NAME_QUALIFIED_TYPE :
         return TID;
      case ASTNode.QUALIFIED_NAME :
         if (spd == QualifiedName.QUALIFIER_PROPERTY) 
            return GID;
         return getNameType(par);
      case ASTNode.PACKAGE_DECLARATION :
      case ASTNode.IMPORT_DECLARATION :
      case ASTNode.MEMBER_VALUE_PAIR :
      case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION :
         return GID;
      case ASTNode.SUPER_CONSTRUCTOR_INVOCATION :
      case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
      case ASTNode.ENUM_CONSTANT_DECLARATION :
         return VID;
      default :
         if (par instanceof Expression) return VID;
         if (par instanceof Statement) return VID;
         return VID;
    }
}


@Override public boolean visit(NullLiteral n)
{
   addKey(LID);
   return false;
}


@Override public boolean visit(NumberLiteral n)
{
   addKey(LID);
   return false;
}


@Override public boolean visit(ParenthesizedExpression n)
{
   addNode(n.getExpression(),"(",")");
   return false;
}


@Override public boolean visit(PostfixExpression n)
{
   addNode(n.getOperand());
   addKey(n.getOperator().toString());
   return false;
}


@Override public boolean visit(PrefixExpression n)
{
   addKey(n.getOperator().toString());
   addNode(n.getOperand());
   return false;
}


@Override public boolean visit(StringLiteral n)
{
   addKey(LID);
   return false;
}


@Override public boolean visit(SuperFieldAccess n)
{
   addNode(n.getQualifier(),null,".");
   addKey("super");
   addNode(n.getName());
   return false;
}


@Override public boolean visit(SuperMethodInvocation n)
{
   addNode(n.getQualifier(),null,".");
   addKey("super");
   addList(n.typeArguments(),"<",",",">");
   addNode(n.getName());
   addKey("(");
   addList(n.arguments(),null,",",null);
   addKey(")");
   return false;
}



@Override public boolean visit(SwitchExpression n)
{
   addKey("switch");
   addKey("(");
   addNode(n.getExpression());
   addKey("(");
   addList(n.statements());
   addKey("}");
   return false;
}



// @Override public boolean visit(TextBlock n)
// {
   // addKey(LID);
   // return false;
// }


@Override public boolean visit(ThisExpression n)
{
   addNode(n.getQualifier(),null,".");
   addKey("this");
   return false;
}



@Override public boolean visit(TypeLiteral n)
{
   addNode(n.getType());
   addKey(".");
   addKey("class");
   return false;
}


@Override public boolean visit(VariableDeclarationExpression n)
{
   addList(n.modifiers());
   addNode(n.getType());
   addList(n.fragments(),null,",",null);
   return false;
}



@Override public boolean visit(ImportDeclaration n)
{
   addKey("import");
   if (n.isStatic()) addKey("static");
   addNode(n.getName());
   if (n.isOnDemand()) {
      addKey(".");
      addKey("*");
    }
   addKey(";");
   return false;
}


@Override public boolean visit(MemberRef n)
{
   addNode(n.getQualifier());
   addKey("#");
   addNode(n.getName());
   return false;
}



@Override public boolean visit(MemberValuePair n)
{
   addNode(n.getName());
   addKey("=");
   addNode(n.getValue());
   return false;
}



@Override public boolean visit(MethodRef n)
{
   addNode(n.getQualifier());
   addKey("#");
   addNode(n.getName());
   addKey("(");
   addList(n.parameters(),null,",",null);
   return false;
}


@Override public boolean visit(MethodRefParameter n)
{
   addNode(n.getType());
   if (n.isVarargs()) addKey("...");
   addNode(n.getName());
   return false;
}



@Override public boolean visit(Modifier n)
{
   addKey(n.getKeyword().toString());
   return false;
}



@Override public boolean visit(ModuleDeclaration n)
{
   addList(n.annotations());
   if (n.isOpen()) addKey("open");
   addKey("module");
   addNode(n.getName());
   addKey("{");
   addList(n.moduleStatements());
   addKey("}");
   return false;
}


@Override public boolean visit(ExportsDirective n)
{
   addKey("exports");
   addNode(n.getName());
   addList(n.modules(),"to",",",null);
   addKey(";");
   return false;
}


@Override public boolean visit(OpensDirective n)
{
   addKey("opens");
   addNode(n.getName());
   addList(n.modules(),"to",",",null);
   addKey(";");
   return false;
}



@Override public boolean visit(ProvidesDirective n)
{
   addKey("provides");
   addNode(n.getName());
   addList(n.implementations(),"with",",",";");
   return false;
}


@Override public boolean visit(RequiresDirective n)
{
   addKey("requires");
   addList(n.modifiers());
   addNode(n.getName());
   addKey(";");
   return false;
}


@Override public boolean visit(UsesDirective n)
{
   addKey("uses");
   addNode(n.getName());
   addKey(";");
   return false;
}



@Override public boolean visit(ModuleModifier n)
{
   addKey(n.getKeyword().toString());
   return false;
}



@Override public boolean visit(PackageDeclaration n)
{
   addList(n.annotations());
   addKey("package");
   addNode(n.getName());
   addKey(";");
   return false;
}




@Override public boolean visit(AssertStatement n)
{
   addKey("assert");
   addNode(n.getExpression());
   addNode(n.getMessage(),":",null);
   addKey(";");
   return false;
}



@Override public boolean visit(Block n)
{
   addKey("{");
   addList(n.statements());
   addKey("}");
   return false;
}


@Override public boolean visit(BreakStatement n)
{
   addKey("break");
   addNode(n.getLabel());
   addKey(";");
   return false;
}


@Override public boolean visit(ConstructorInvocation n)
{
   addList(n.typeArguments(),"<",",",">");
   addKey("this");
   addKey("(");
   addList(n.arguments(),null,",",null);
   addKey(")");
   addKey(";");
   return false;
}



@Override public boolean visit(ContinueStatement n)
{
   addKey("continue");
   addNode(n.getLabel());
   addKey(";");
   return false;
}


@Override public boolean visit(DoStatement n)
{
   addKey("do");
   addNode(n.getBody());
   addKey("while");
   addKey("(");
   addNode(n.getExpression());
   addKey(")");
   addKey(";");
   return false;
}



@Override public boolean visit(EmptyStatement n)
{
   addKey(";");
   return false;
}



@Override public boolean visit(EnhancedForStatement n)
{
   addKey("for");
   addKey("(");
   addNode(n.getParameter());
   addKey(":");
   addNode(n.getExpression());
   addKey(")");
   addNode(n.getBody());
   return false;
}



@Override public boolean visit(ExpressionStatement n)
{
   addNode(n.getExpression());
   addKey(";");
   return false;
}



@Override public boolean visit(ForStatement n)
{
   addKey("for");
   addKey("(");
   addList(n.initializers(),null,",",null);
   addKey(";");
   addNode(n.getExpression());
   addKey(";");
   addList(n.updaters(),null,",",null);
   addKey(")");
   addNode(n.getBody());
   return false;
}



@Override public boolean visit(IfStatement n)
{
   addKey("if");
   addKey("(");
   addNode(n.getExpression());
   addKey(")");
   addNode(n.getThenStatement());
   addNode(n.getElseStatement(),"else",null);
   return false;
}


@Override public boolean visit(LabeledStatement n)
{
   addNode(n.getLabel());
   addKey(":");
   addNode(n.getBody());
   return false;
}


@Override public boolean visit(ReturnStatement n)
{
   addKey("return");
   addNode(n.getExpression());
   addKey(";");
   return false;
}


@Override public boolean visit(SuperConstructorInvocation n)
{
   addNode(n.getExpression(),null,".");
   addList(n.typeArguments(),"<",",",">");
   addKey("super");
   addKey("(");
   addList(n.arguments(),null,",",null);
   addKey(")");
   addKey(";");
   return false;
}


@SuppressWarnings("deprecation") 
@Override public boolean visit(SwitchCase n)
{
   addKey("case");
   if (n.isDefault()) addKey("default");
   else {
      try {
         addList(n.expressions(),null,",",null);
       }
      catch (Throwable t) {
         addNode(n.getExpression());
       }
    }
   try {
      if (n.isSwitchLabeledRule()) addKey("->");
      else addKey(":");
    }
   catch (Throwable t) {
      addKey(":");
    }
   return false;
}


@Override public boolean visit(SwitchStatement n)
{
   addKey("switch");
   addKey("(");
   addNode(n.getExpression());
   addKey(")");
   addKey("{");
   addList(n.statements());
   addKey("}");
   return false;
}



@Override public boolean visit(SynchronizedStatement n)
{
   addKey("synchronized");
   addKey("(");
   addNode(n.getExpression());
   addKey(")");
   addNode(n.getBody());
   return false;
}



@Override public boolean visit(ThrowStatement n)
{
   addKey("throw");
   addNode(n.getExpression());
   addKey(";");
   return false;
}


@Override public boolean visit(TryStatement n)
{
   addKey("try");
   addList(n.resources(),"(",null,")");
   addNode(n.getBody());
   addList(n.catchClauses());
   addNode(n.getFinally(),"finally",null);
   return false;
}



@Override public boolean visit(TypeDeclarationStatement n)
{
   addNode(n.getDeclaration());
   return false;
}


@Override public boolean visit(VariableDeclarationStatement n)
{
   addList(n.modifiers());
   addNode(n.getType());
   addList(n.fragments(),null,",",";");
   return false;
}


@Override public boolean visit(WhileStatement n)
{
   addKey("while");
   addKey("(");
   addNode(n.getExpression());
   addKey(")");
   addNode(n.getBody());
   return false;
}



@Override public boolean visit(NameQualifiedType n)
{
   addNode(n.getQualifier());
   addKey(".");
   addList(n.annotations());
   addNode(n.getName());
   return false;
}



@Override public boolean visit(PrimitiveType n)
{
   addKey(TID);
   return false;
}


@Override public boolean visit(QualifiedType n)
{
   addNode(n.getQualifier());
   addKey(".");
   addList(n.annotations());
   addNode(n.getName());
   return false;
}


@Override public boolean visit(SimpleType n)
{
   addList(n.annotations());
   addNode(n.getName());
   return false;
}


@Override public boolean visit(WildcardType n)
{
   addList(n.annotations());
   addKey("?");
   String t = (n.isUpperBound() ? "extends" : "super");
   addNode(n.getBound(),t,null);
   return false;
}


@Override public boolean visit(ArrayType n)
{
   addNode(n.getElementType());
   addList(n.dimensions());
   return false;
}



@Override public boolean visit(IntersectionType n)
{
   addList(n.types(),null,"&",null);
   return false;
}



@Override public boolean visit(ParameterizedType n)
{
   addNode(n.getType());
   addList(n.typeArguments(),"<",",",">");
   return false;
}



@Override public boolean visit(UnionType n)
{
   addList(n.types(),null,"|",null);
   return false;
}



@Override public boolean visit(TypeParameter n)
{
   addList(n.modifiers());
   addNode(n.getName());
   addList(n.typeBounds(),"extends","&",null);
   return false;
}


@Override public boolean visit(SingleVariableDeclaration n)
{
   addList(n.modifiers());
   addNode(n.getType());
   addList(n.varargsAnnotations());
   if (n.isVarargs()) addKey("...");
   addNode(n.getName());
   addList(n.extraDimensions());
   addNode(n.getInitializer(),"=",null);
   return false;
}



@Override public boolean visit(VariableDeclarationFragment n)
{
   addNode(n.getName());
   addList(n.extraDimensions());
   addNode(n.getInitializer(),"=",null);
   return false;
}




/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

private void addKey(String k)
{
   ct_list.add(new SwiftCodeToken(k));
}

private void addNode(ASTNode n)
{
   if (n != null) n.accept(this);
}

private void addNode(ASTNode n,String pfx,String sfx)
{
   if (n ==  null) return;
   if (pfx != null) addKey(pfx);
   n.accept(this);
   if (sfx != null) addKey(sfx);
}


private void addList(List<?> l)
{
   addList(l,null,null,null);
}


private void addList(List<?> l,String pfx,String sep,String sfx)
{
   if (l == null) return;
   
   int ct = 0;
   for (Object o : l) {
      if (ct != 0 && sep != null) addKey(sep);
      else if (ct == 0 && pfx != null) addKey(pfx);
      ASTNode n = (ASTNode) o;
      n.accept(this);
      ++ct;
    }
   if (ct > 0 && sfx != null) addKey(sfx);
}

}       // end of class SwiftTokenGenerator




/* end of SwiftTokenGenerator.java */

