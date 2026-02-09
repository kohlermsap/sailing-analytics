package com.sap.sse.common.scalablevalue;

public interface HasConfidenceAndIsScalable<ValueType, BaseType, RelativeTo> extends IsScalable<ValueType, BaseType>,
        HasConfidence<ValueType, BaseType, RelativeTo> {
}
