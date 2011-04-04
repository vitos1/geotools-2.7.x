/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2006-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.filter.text.cql2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.filter.IllegalFilterException;
import org.geotools.filter.text.commons.ICompiler;
import org.geotools.filter.text.commons.IToken;
import org.geotools.filter.text.commons.Result;
import org.geotools.filter.text.commons.TokenAdapter;
import org.geotools.filter.text.generated.parsers.CQLParser;
import org.geotools.filter.text.generated.parsers.Node;
import org.geotools.filter.text.generated.parsers.ParseException;
import org.geotools.filter.text.generated.parsers.TokenMgrError;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Not;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.DistanceBufferOperator;


/**
 * CQL Compiler. This class extend the CQLParser generated by javacc with
 * semantic actions.
 * <p>
 * The "build..." methods implement that semantic actions making a filter for
 * each syntax rules recognized.
 * </p>
 * 
 * <p>
 * Warning: This component is not published. It is part of module implementation. 
 * Client module should not use this feature.
 * </p>
 * 
 * TODO This module should use the new geometry API, more info in http://docs.codehaus.org/display/GEOTOOLS/GeomeryFactoryFinder+Proposal
 * 
 * 
 * @author Mauricio Pazos (Axios Engineering)
 * 
 *
 * @source $URL$
 * @version $Id$
 * @since 2.5
 */
public class CQLCompiler extends CQLParser implements ICompiler{

    private static final String ATTRIBUTE_PATH_SEPARATOR = "/";

    /** cql expression to compile */
    private final String source;

    private CQLFilterBuilder builder;

    /**
     * new instance of CQL Compiler
     * @param cqlSource 
     * @param filterFactory
     */
    public CQLCompiler(final String cqlSource, final FilterFactory filterFactory) {
        
        super(new StringReader(cqlSource));

        assert cqlSource != null: "cqlSource cannot be null";
        assert filterFactory != null: "filterFactory cannot be null";
        
        this.source = cqlSource;
        this.builder =  new CQLFilterBuilder(cqlSource, filterFactory);
    }
    
    /** 
     * compile source to produce a Filter. The filter
     * result must be retrieved with {@link #getFilter()}.
     */
    public void compileFilter() throws CQLException{
        try {
            super.FilterCompilationUnit();
        } catch (TokenMgrError tokenError){
            throw new CQLException(tokenError.getMessage(),getTokenInPosition(0), this.source);
        } catch (CQLException e) {
            throw e;
        } catch (ParseException e) {
            throw new CQLException(e.getMessage(), getTokenInPosition(0), e.getCause(), this.source);
        }
    }
    
    /** 
     * compile source to produce a Expression
     */
    public void compileExpression() throws CQLException{
        try {
            super.ExpressionCompilationUnit();
        } catch (TokenMgrError tokenError){
            throw new CQLException(tokenError.getMessage(),getTokenInPosition(0), this.source);
        } catch (CQLException e) {
            throw e;
        } catch (ParseException e) {
            throw new CQLException(e.getMessage(), getTokenInPosition(0), e.getCause(), this.source);
        }
    }
    
    
    public void compileFilterList() throws CQLException{
        try {
            super.FilterListCompilationUnit();
        } catch (TokenMgrError tokenError){
            throw new CQLException(tokenError.getMessage(),getTokenInPosition(0), this.source);
        } catch (CQLException e) {
            throw e;
        } catch (ParseException e) {
            throw new CQLException(e.getMessage(), getTokenInPosition(0), e.getCause(), this.source);
        }
    }
    
    /**
     * @return the cql source
     */
    public final String getSource(){
        return this.source;
    }

    /**
     * Return the filter resultant of compiling process
     * @return Filter
     * @throws CQLException 
     */
    public final Filter getFilter() throws CQLException {
        return this.builder.getFilter(); 
    }
    /**
     * Return the expression resultant of compiling process
     * @return Expression
     * @throws CQLException 
     */
    public final Expression getExpression() throws CQLException {

        return this.builder.getExpression();
    }

    /**
     * Returns the list of Filters built as the result of calling
     * {@link #MultipleCompilationUnit()}
     * 
     * @return List<Filter> 
     * @throws CQLException
     *             if a ClassCastException occurs while casting a built item to
     *             a Filter.
     */
    public final List<Filter> getFilterList() throws CQLException {
        
        return this.builder.getFilterList();
    }

    public IToken getTokenInPosition(int index ){
        return TokenAdapter.newAdapterFor( super.getToken(index));
    }
    public final void jjtreeOpenNodeScope(Node n) {
    }

    public final void jjtreeCloseNodeScope(Node n) throws ParseException {
        
        try {
            Object built = build(n);
           
            IToken tokenAdapter = TokenAdapter.newAdapterFor(token);
            Result r = new Result(built, tokenAdapter, n.getType());
            this.builder.pushResult(r );
            
        } finally {
            n.dispose();
        }
    }
    /**
     * This method is called when the parser close a node. Here is built the
     * filters an expressions recognized in the parsing process.
     * 
     * @param cqlNode a Node instance
     * @return Filter or Expression
     * @throws CQLException
     */
    private Object build(Node cqlNode) throws CQLException {
        switch (cqlNode.getType()) {
        // Literals
        // note, these should never throw because the parser grammar
        // constrains input before we ever reach here!
        case JJTINTEGERNODE:
            return this.builder.buildLiteralInteger(getToken(0).image);

        case JJTFLOATINGNODE:
            return  this.builder.buildLiteralDouble(getToken(0).image);

        case JJTSTRINGNODE:
            return this.builder.buildLiteralString(getToken(0).image);
            // ----------------------------------------
            // Identifier
            // ----------------------------------------
        case JJTIDENTIFIER_NODE:
            return this.builder.buildIdentifier(JJTIDENTIFIER_PART_NODE);

        case JJTIDENTIFIER_PART_NODE:
            return this.builder.buildIdentifierPart(getTokenInPosition(0));

            // ----------------------------------------
            // attribute
            // ----------------------------------------
        case JJTSIMPLE_ATTRIBUTE_NODE:
            return this.builder.buildSimpleAttribute();

        case JJTCOMPOUND_ATTRIBUTE_NODE:
            return this.builder.buildCompoundAttribute(JJTSIMPLE_ATTRIBUTE_NODE, ATTRIBUTE_PATH_SEPARATOR);

            // ----------------------------------------
            // function
            // ----------------------------------------
        case JJTFUNCTION_NODE:
            return this.builder.buildFunction(JJTFUNCTIONNAME_NODE);

        case JJTFUNCTIONNAME_NODE:
            return cqlNode; // used as mark of function name in stack

        case JJTFUNCTIONARG_NODE:
            return cqlNode; // used as mark of args in stack

            // Math Nodes
        case JJTADDNODE:
        case JJTSUBTRACTNODE:
        case JJTMULNODE:
        case JJTDIVNODE:
            return buildBinaryExpression(cqlNode.getType());

            // Boolean expression
        case JJTBOOLEAN_AND_NODE:
            return buildLogicFilter(JJTBOOLEAN_AND_NODE);

        case JJTBOOLEAN_OR_NODE:
            return buildLogicFilter(JJTBOOLEAN_OR_NODE);

        case JJTBOOLEAN_NOT_NODE:
            return buildLogicFilter(JJTBOOLEAN_NOT_NODE);

            // ----------------------------------------
            // between predicate actions
            // ----------------------------------------
        case JJTBETWEEN_NODE:
            return this.builder.buildBetween();

        case JJTNOT_BETWEEN_NODE:
            return this.builder.buildNotBetween();

            // ----------------------------------------
            // Compare predicate actions
            // ----------------------------------------
        case JJTCOMPARISSONPREDICATE_EQ_NODE:
        case JJTCOMPARISSONPREDICATE_GT_NODE:
        case JJTCOMPARISSONPREDICATE_LT_NODE:
        case JJTCOMPARISSONPREDICATE_GTE_NODE:
        case JJTCOMPARISSONPREDICATE_LTE_NODE:
            return buildBinaryComparasionOperator(cqlNode.getType());

        case JJTCOMPARISSONPREDICATE_NOT_EQUAL_NODE:

            Filter eq = buildBinaryComparasionOperator(JJTCOMPARISSONPREDICATE_EQ_NODE);
            Not notFilter = this.builder.buildNotFilter(eq);

            return notFilter;

            // ----------------------------------------
            // Text predicate (Like)
            // ----------------------------------------
        case JJTLIKE_NODE:
            return this.builder.buildLikeFilter();

        case JJTNOT_LIKE_NODE:
            return this.builder.buildNotLikeFilter();

            // ----------------------------------------
            // Null predicate
            // ----------------------------------------
        case JJTNULLPREDICATENODE:
            return this.builder.buildPropertyIsNull();

        case JJTNOTNULLPREDICATENODE:
            return this.builder.buildPorpertyNotIsNull();

            // ----------------------------------------
            // temporal predicate actions
            // ----------------------------------------
        case JJTDATETIME_NODE:
            return this.builder.buildDateTimeExpression(getTokenInPosition(0));

        case JJTDURATION_DATE_NODE:
            return this.builder.buildDurationExpression(getTokenInPosition(0));

        case JJTPERIOD_BETWEEN_DATES_NODE:
            return this.builder.buildPeriodBetweenDates();

        case JJTPERIOD_WITH_DATE_DURATION_NODE:
            return this.builder.buildPeriodDateAndDuration();

        case JJTPERIOD_WITH_DURATION_DATE_NODE:
            return this.builder.buildPeriodDurationAndDate();

        case JJTTPBEFORE_DATETIME_NODE:
            return buildTemporalPredicateBefore();

        case JJTTPAFTER_DATETIME_NODE:
            return buildTemporalPredicateAfter();

        case JJTTPDURING_PERIOD_NODE:
            return buildTemporalPredicateDuring();

        case JJTTPBEFORE_OR_DURING_PERIOD_NODE:
            return buildTemporalPredicateBeforeOrDuring();

        case JJTTPDURING_OR_AFTER_PERIOD_NODE:
            return buildTemporalPredicateDuringOrAfter();

            // ----------------------------------------
            // existence predicate actions
            // ----------------------------------------
        case JJTEXISTENCE_PREDICATE_EXISTS_NODE:
            return this.builder.buildPropertyExists();

        case JJTEXISTENCE_PREDICATE_DOESNOTEXIST_NODE:

            Filter filter = this.builder.buildPropertyExists();
            Filter filterPropNotExist = this.builder.buildNotFilter(filter);

            return filterPropNotExist;

            // ----------------------------------------
            // routine invocation Geo Operation
            // ----------------------------------------
        case JJTROUTINEINVOCATION_GEOOP_EQUAL_NODE:
        case JJTROUTINEINVOCATION_GEOOP_DISJOINT_NODE:
        case JJTROUTINEINVOCATION_GEOOP_INTERSECT_NODE:
        case JJTROUTINEINVOCATION_GEOOP_TOUCH_NODE:
        case JJTROUTINEINVOCATION_GEOOP_CROSS_NODE:
        case JJTROUTINEINVOCATION_GEOOP_WITHIN_NODE:
        case JJTROUTINEINVOCATION_GEOOP_CONTAIN_NODE:
        case JJTROUTINEINVOCATION_GEOOP_OVERLAP_NODE:
            return buildBinarySpatialOperator(cqlNode.getType());

        case JJTROUTINEINVOCATION_GEOOP_BBOX_NODE:
        case JJTROUTINEINVOCATION_GEOOP_BBOX_SRS_NODE:
            return buildBBox(cqlNode.getType());

        case JJTROUTINEINVOCATION_GEOOP_RELATE_NODE:
            throw new CQLException(
                    "Unsupported geooperation RELATE (is not implemented by GeoTools)",
                    getTokenInPosition(0), this.source);

            // ----------------------------------------
            // routine invocation RelGeo Operation
            // ----------------------------------------
        case JJTTOLERANCE_NODE:
            return this.builder.buildTolerance();

        case JJTDISTANCEUNITS_NODE:
            return this.builder.buildDistanceUnit(getTokenInPosition(0));

        case JJTROUTINEINVOCATION_RELOP_BEYOND_NODE:
        case JJTROUTINEINVOCATION_RELOP_DWITHIN_NODE:
            return buildDistanceBufferOperator(cqlNode.getType());

            // ----------------------------------------
            // Geometries:
            // ----------------------------------------
        case JJTWKTNODE:
            return this.builder.buildGeometry(TokenAdapter.newAdapterFor(cqlNode.getToken()));

        case JJTENVELOPETAGGEDTEXT_NODE:
            return this.builder.buildEnvelop(TokenAdapter.newAdapterFor(cqlNode.getToken()));

        case JJTINCLUDE_NODE:
            return Filter.INCLUDE;

        case JJTEXCLUDE_NODE:
            return Filter.EXCLUDE;

        case JJTTRUENODE:
            return this.builder.buildTrueLiteral();

        case JJTFALSENODE:
            return this.builder.buildFalseLiteral(); 
        }

        return null;
    }

    private org.opengis.filter.expression.BinaryExpression buildBinaryExpression(
            int nodeType) throws CQLException {
        
        org.opengis.filter.expression.BinaryExpression expr = null;

        switch (nodeType) {
        case JJTADDNODE:
            expr =  this.builder.buildAddExpression();

            break;

        case JJTSUBTRACTNODE:
            expr = this.builder.buildSubtractExression();

            break;

        case JJTMULNODE:
            
            expr = this.builder.buildMultiplyExpression();
            break;

        case JJTDIVNODE:
            
            expr = this.builder.buildDivideExpression();            
            break;

        default:
            break;
        }

        return expr;
    }

    private org.opengis.filter.Filter buildLogicFilter(int nodeType)
            throws CQLException {
        try {

            org.opengis.filter.Filter logicFilter;

            switch (nodeType) {
            case JJTBOOLEAN_AND_NODE:
                
                logicFilter = this.builder.buildAndFilter();
                break;

            case JJTBOOLEAN_OR_NODE:
                
                logicFilter = this.builder.buildOrFilter();

                break;

            case JJTBOOLEAN_NOT_NODE:
                
                logicFilter = this.builder.buildNotFilter();
                

                break;

            default:
                throw new CQLException(
                        "Expression not supported. And, Or, Not is required",
                        getTokenInPosition(0), this.source);
            }

            return logicFilter;
        } catch (IllegalFilterException ife) {
            throw new CQLException("Exception building LogicFilter",
                    getTokenInPosition(0), ife, this.source);
        }
    }



    /**
     * Creates Binary Spatial Operator
     * 
     * @param tipeNode
     * 
     * @return BinarySpatialOperator 
     * @throws CQLException
     */
    private BinarySpatialOperator buildBinarySpatialOperator(final int nodeType)
            throws CQLException {

        BinarySpatialOperator filter = null;

        switch (nodeType) {
        case JJTROUTINEINVOCATION_GEOOP_EQUAL_NODE:
            filter = this.builder.buildSpatialEqualFilter();
            break;
        case JJTROUTINEINVOCATION_GEOOP_DISJOINT_NODE:
            filter = this.builder.buildSpatialDisjointFilter();
            break;
        case JJTROUTINEINVOCATION_GEOOP_INTERSECT_NODE:
            filter = this.builder.buildSpatialIntersectsFilter();
            break;
        case JJTROUTINEINVOCATION_GEOOP_TOUCH_NODE:
            filter = this.builder.buildSpatialTouchesFilter();
            break;

        case JJTROUTINEINVOCATION_GEOOP_CROSS_NODE:
            filter = this.builder.buildSpatialCrossesFilter();
            break;
        case JJTROUTINEINVOCATION_GEOOP_WITHIN_NODE:
            filter = this.builder.buildSpatialWithinFilter();
            break;
        case JJTROUTINEINVOCATION_GEOOP_CONTAIN_NODE:
            filter = this.builder.buildSpatialContainsFilter();
            break;
        case JJTROUTINEINVOCATION_GEOOP_OVERLAP_NODE:
            filter = this.builder.buildSpatialOverlapsFilter();
            break;
        default:
            throw new CQLException("Binary spatial operator unexpected");
        }

        return filter;
    }

    private org.opengis.filter.spatial.BBOX buildBBox(int nodeType)
            throws CQLException {

            if (nodeType == JJTROUTINEINVOCATION_GEOOP_BBOX_SRS_NODE) {
                return this.builder.buildBBoxWithCRS();
            } else {
                return this.builder.buildBBox();
            }
    }

    /**
     * Builds Distance Buffer Operator
     * 
     * @param nodeType
     * @return DistanceBufferOperator dwithin and beyond filters
     * @throws CQLException
     */
    private DistanceBufferOperator buildDistanceBufferOperator(
            final int nodeType) throws CQLException {

        DistanceBufferOperator filter = null;

        switch (nodeType) {
        case JJTROUTINEINVOCATION_RELOP_DWITHIN_NODE:
            filter = this.builder.buildSpatialDWithinFilter();
            break;

        case JJTROUTINEINVOCATION_RELOP_BEYOND_NODE:

            filter = this.builder.buildSpatialBeyondFilter();
            break;

        default:
            throw new CQLException("Binary spatial operator unexpected");
        }

        return filter;
    }


    private org.opengis.filter.Filter buildTemporalPredicateBeforeOrDuring()
            throws CQLException {
        org.opengis.filter.Filter filter = null;

        Result node = this.builder.peekResult();

        switch (node.getNodeType()) {
        case JJTPERIOD_BETWEEN_DATES_NODE:
        case JJTPERIOD_WITH_DATE_DURATION_NODE:
        case JJTPERIOD_WITH_DURATION_DATE_NODE:
            filter = this.builder.buildPropertyIsLTELastDate();
            break;

        default:
            throw new CQLException(
                    "unexpeted date time expression in temporal predicate.",
                    node.getToken(), this.source);
        }

        return filter;
    }

    private org.opengis.filter.Filter buildTemporalPredicateDuringOrAfter()
            throws CQLException {
        org.opengis.filter.Filter filter = null;

        Result node = this.builder.peekResult();

        switch (node.getNodeType()) {
        case JJTPERIOD_BETWEEN_DATES_NODE:
        case JJTPERIOD_WITH_DATE_DURATION_NODE:
        case JJTPERIOD_WITH_DURATION_DATE_NODE:
            filter = this.builder.buildPropertyIsGTEFirstDate();

            break;

        default:
            throw new CQLException(
                    "unexpeted date time expression in temporal predicate.",
                    node.getToken(), this.source);
        }

        return filter;
    }





    /**
     * Build the convenient filter for before date and before period filters
     * 
     * @param nodeType
     * 
     * @return Filter
     * @throws CQLException
     */
    private org.opengis.filter.Filter buildTemporalPredicateBefore()
            throws CQLException {
        org.opengis.filter.Filter filter = null;

        // analyzes if the last build is period or date
        Result node = this.builder.peekResult();

        switch (node.getNodeType()) {
        case JJTDATETIME_NODE:
            filter = buildBinaryComparasionOperator(JJTCOMPARISSONPREDICATE_LT_NODE);

            break;

        case JJTPERIOD_BETWEEN_DATES_NODE:
        case JJTPERIOD_WITH_DATE_DURATION_NODE:
        case JJTPERIOD_WITH_DURATION_DATE_NODE:
            filter = this.builder.buildPropertyIsLTFirsDate();

            break;

        default:
            throw new CQLException(
                    "unexpeted date time expression in temporal predicate.",
                    node.getToken(), this.source);
        }

        return filter;
    }

    /**
     * Build the convenient filter for during period filters
     * 
     * @return Filter
     * @throws CQLException
     */
    private Object buildTemporalPredicateDuring() throws CQLException {
        org.opengis.filter.Filter filter = null;

        // determines if the node is period or date
        Result node = this.builder.peekResult();

        switch (node.getNodeType()) {
        case JJTPERIOD_BETWEEN_DATES_NODE:
        case JJTPERIOD_WITH_DATE_DURATION_NODE:
        case JJTPERIOD_WITH_DURATION_DATE_NODE:
            filter = this.builder.buildPropertyBetweenDates();

            break;

        default:
            throw new CQLException(
                    "unexpeted period expression in temporal predicate.", node
                            .getToken(), this.source);
        }

        return filter;
    }


    /**
     * build filter for after date and after period
     * 
     * @return a filter
     * @throws CQLException
     */
    private org.opengis.filter.Filter buildTemporalPredicateAfter()
            throws CQLException {
        org.opengis.filter.Filter filter = null;

        // determines if the node is period or date
        Result result = this.builder.peekResult();

        switch (result.getNodeType()) {
        case JJTDATETIME_NODE:
            filter = buildBinaryComparasionOperator(JJTCOMPARISSONPREDICATE_GT_NODE);

            break;

        case JJTPERIOD_BETWEEN_DATES_NODE:
        case JJTPERIOD_WITH_DURATION_DATE_NODE:
        case JJTPERIOD_WITH_DATE_DURATION_NODE:
            filter = this.builder.buildPropertyIsGTLastDate();

            break;

        default:
            throw new CQLException(
                    "unexpeted date time expression in temporal predicate.",
                    result.getToken(), this.source);
        }

        return filter;
    }



    /**
     * Builds a compare filter
     * 
     * @param filterTipa
     * 
     * @return BinaryComparisonOperator
     * @throws CQLException
     */
    private BinaryComparisonOperator buildBinaryComparasionOperator(
            int filterType) throws CQLException {

        switch (filterType) {
        case JJTCOMPARISSONPREDICATE_EQ_NODE:
            return this.builder.buildEquals();

        case JJTCOMPARISSONPREDICATE_GT_NODE:
            return this.builder.buildGreater();

        case JJTCOMPARISSONPREDICATE_LT_NODE:
            return this.builder.buildLess();

        case JJTCOMPARISSONPREDICATE_GTE_NODE:
            return this.builder.buildGreaterOrEqual();

        case JJTCOMPARISSONPREDICATE_LTE_NODE:
            return this.builder.buildLessOrEqual();

        default:
            throw new CQLException("unexpeted filter type.");
        }
    }
    /**
     * On line cql interpreter
     * @param args
     * @throws ParseException
     */
    public static void main(String args[]) throws ParseException {

        System.out.println("Expecting a predicate (q - quit).");
        while (true) {

            try {
                InputStreamReader reader = new InputStreamReader(System.in);
                BufferedReader buf = new BufferedReader(reader);
                String source = buf.readLine();
                if ("q".equals(source)) {
                    System.out.println("bye.");
                    break;
                } 

                FilterFactory ff = CommonFactoryFinder
                            .getFilterFactory((Hints) null);
                CQLCompiler compiler = new CQLCompiler(source, ff);
                    
                compiler.compileFilter();

                Filter filter = compiler.getFilter();
                    
                System.out.println("Result: " + filter);

            } catch (CQLException e) {
                System.out.println("Parsing error.");
                System.out.println(e.getSyntaxError());
                //e.printStackTrace();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
}