package com.sap.sse.landscape.impl;

import com.sap.sse.common.impl.NamedImpl;
import com.sap.sse.landscape.Release;

public abstract class AbstractRelease extends NamedImpl implements Release {
    private static final long serialVersionUID = 4872094283926485605L;
    
    public AbstractRelease(String name) {
        super(name);
    }
}
