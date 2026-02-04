package com.sap.sse.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.scalablevalue.KadaneExtremeSubsequenceFinder;
import com.sap.sse.common.scalablevalue.ScalableDouble;

public abstract class KadaneExtremeSubsequenceFinderTest {
    private static final double EPSILON = 0.00000001;
    protected KadaneExtremeSubsequenceFinder<Double, Double, ScalableDouble> finder;
    
    @Test
    public void testSimplePositiveSequence() {
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(2));
        finder.add(new ScalableDouble(3));
        assertEquals(6.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(2, finder.getEndIndexOfMaxSumSequence());
    }

    @Test
    public void testSimplePositiveSequenceWithInsertInTheMiddle() {
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(3));
        finder.add(1, new ScalableDouble(2));
        assertEquals(6.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(2, finder.getEndIndexOfMaxSumSequence());
    }

    @Test
    public void testSimpleSequenceWithPositiveAndNegative() {
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(2));
        finder.add(new ScalableDouble(3));
        finder.add(new ScalableDouble(-4));
        finder.add(new ScalableDouble(5));
        finder.add(new ScalableDouble(6));
        finder.add(new ScalableDouble(-5));
        assertEquals(13.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(5, finder.getEndIndexOfMaxSumSequence());
    }
    
    @Test
    public void testSimplePositiveSequenceWithLaterNegativeInsertInTheMiddle() {
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(2));
        finder.add(new ScalableDouble(3));
        finder.add(new ScalableDouble(4));
        finder.add(new ScalableDouble(5));
        assertEquals(15.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(4, finder.getEndIndexOfMaxSumSequence());
        finder.add(3, new ScalableDouble(-7));
        assertEquals(9.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(4, finder.getStartIndexOfMaxSumSequence());
        assertEquals(5, finder.getEndIndexOfMaxSumSequence());
    }
    
    @Test
    public void testRemoveFromMiddleOfPositiveSequence() {
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(2));
        finder.add(new ScalableDouble(3));
        finder.add(new ScalableDouble(4));
        finder.add(new ScalableDouble(5));
        assertEquals(15.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(4, finder.getEndIndexOfMaxSumSequence());
        finder.remove(2);
        assertEquals(12.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(3, finder.getEndIndexOfMaxSumSequence());
    }
    
    /**
     * A white-box test for how we remove from the {@link TreeSet} that holds the best max sum sub-sequence end node.
     * Should the wrong node be removed, the structure would become inconsistent and hold a node as "best" that no
     * longer exists.
     */
    @Test
    public void testTwoEquallyGoodPositiveSequencesThenRemovingFromOne() {
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(2));
        finder.add(new ScalableDouble(3));
        finder.add(new ScalableDouble(4));
        finder.add(new ScalableDouble(5));
        // now a "very negative" one that starts a new positive sequence after it
        finder.add(new ScalableDouble(-100));
        // now the second positive sequence with equal max sum as the first one
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(2));
        finder.add(new ScalableDouble(3));
        finder.add(new ScalableDouble(4));
        finder.add(new ScalableDouble(5));
        assertEquals(15.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertTrue(finder.getStartIndexOfMaxSumSequence() == 0 || finder.getStartIndexOfMaxSumSequence() == 6);
        assertTrue(finder.getEndIndexOfMaxSumSequence() == 4 || finder.getEndIndexOfMaxSumSequence() == 10);
        finder.remove(3);
        assertEquals(15.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(5, finder.getStartIndexOfMaxSumSequence());
        assertEquals(9, finder.getEndIndexOfMaxSumSequence());
        finder.remove(8); // removes the 4.0 from the second sequence, resulting again in two equal max sum sub-sequences:
        assertEquals(11.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertTrue(finder.getStartIndexOfMaxSumSequence() == 0 || finder.getStartIndexOfMaxSumSequence() == 5);
        assertTrue(finder.getEndIndexOfMaxSumSequence() == 3 || finder.getEndIndexOfMaxSumSequence() == 8);
        finder.remove(7); // removes the 3.0 from the second sequence
        assertEquals(11.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(3, finder.getEndIndexOfMaxSumSequence());
    }
    
    @Test
    public void testRemoveFromBeginningOfPositiveSequence() {
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(2));
        finder.add(new ScalableDouble(3));
        finder.add(new ScalableDouble(4));
        finder.add(new ScalableDouble(5));
        assertEquals(15.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(4, finder.getEndIndexOfMaxSumSequence());
        finder.remove(0);
        finder.remove(0);
        finder.remove(0);
        assertEquals(9.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(1, finder.getEndIndexOfMaxSumSequence());
    }
}
