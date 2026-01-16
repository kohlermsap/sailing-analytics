package com.sap.sse.common.impl;

import com.sap.sse.common.Renamable;

public class RenamableImpl implements Renamable {
    private static final long serialVersionUID = -4815125282671451300L;
    private String name;
    
    public RenamableImpl(String name) {
        super();
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void setName(String newName) {
        this.name = newName;
    }

}
