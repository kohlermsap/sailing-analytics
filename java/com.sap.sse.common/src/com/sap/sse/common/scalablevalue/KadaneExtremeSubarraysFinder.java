package com.sap.sse.common.scalablevalue;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * In a sequence of {@link ComparableScalableValueWithDistance} objects, tells the contiguous sub-sequence with the
 * greatest and the contiguous sub-sequence with the least sum, according to the
 * {@link ComparableScalableValueWithDistance#add(ScalableValue) add} and the
 * {@link ComparableScalableValueWithDistance#compareTo(Object) compareTo} methods.
 * <p>
 * 
 * The sequence is mutable. In particular, elements can be added at any position, also before the start or after the
 * end, and elements can be removed at least from the beginning of the sequence. Updating the sub-sequences with minimal
 * and maximal sums happens with complexity O(1) when adding to the end of the sequence, so with constant effort
 * regardless the size of the sequence. When inserting into or removing from the sequence at arbitrary positions,
 * constant effort can no longer be guaranteed as changes may need to get propagated onwards to following elements.
 * <p>
 * 
 * See also <a href="https://en.wikipedia.org/wiki/Maximum_subarray_problem">here</a> for a description of the
 * algorithm.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class KadaneExtremeSubarraysFinder<ValueType, AveragesTo extends Comparable<AveragesTo>, T extends ComparableScalableValueWithDistance<ValueType, AveragesTo>>
implements Serializable {
    private static final long serialVersionUID = 2109193559337714286L;
    
    /**
     * The elements constituting the full sequence in which to find the contiguous sub-sequences
     */
    private final List<T> sequence;
    
    /**
     * The element at index <tt>i</tt> holds the maximum value of the sum of any contiguous sub-sequence ending at index
     * <tt>i</tt>.
     */
    private final List<AveragesTo> maxSumEndingAt;
    
    /**
     * The maximum of the sums of any contiguous sub-sequence
     */
    private AveragesTo maxSum;
    
    /**
     * Index of the first element in {@link #sequence} of the contiguous sub-sequence having the maximum sum
     */
    private int startIndexInclusiveOfMaxSumSequence;
    
    /**
     * Index of the element after the last element in {@link #sequence} of the contiguous sub-sequence having the maximum sum
     */
    private int endIndexExclusiveOfMaxSumSequence;
    
    /**
     * The element at index <tt>i</tt> holds the minimum value of the sum of any contiguous sub-sequence ending at index
     * <tt>i</tt>.
     */
    private final List<AveragesTo> minSumEndingAt;
    
    private AveragesTo minSum;
    
    /**
     * Index of the first element in {@link #sequence} of the contiguous sub-sequence having the minium sum
     */
    private int startIndexInclusiveOfMinSumSequence;
    
    /**
     * Index of the element after the last element in {@link #sequence} of the contiguous sub-sequence having the minimum sum
     */
    private int endIndexExclusiveOfMinSumSequence;
    
    public KadaneExtremeSubarraysFinder() {
        sequence = new LinkedList<>();
        maxSumEndingAt = new LinkedList<>();
        minSumEndingAt = new LinkedList<>();
        maxSum = null;
        minSum = null;
        startIndexInclusiveOfMaxSumSequence = -1;
        endIndexExclusiveOfMaxSumSequence = -1;
        startIndexInclusiveOfMinSumSequence = -1;
        endIndexExclusiveOfMinSumSequence = -1;
    }
}
