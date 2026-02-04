package com.sap.sse.common.test;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sse.common.scalablevalue.KadaneExtremeSubsequenceFinderLinkedListImpl;

public class KadaneExtremeSubsequenceFinderLinkedListTest extends KadaneExtremeSubsequenceFinderTest {
    @BeforeEach
    public void setUp() {
        finder = new KadaneExtremeSubsequenceFinderLinkedListImpl<>();
    }
}
