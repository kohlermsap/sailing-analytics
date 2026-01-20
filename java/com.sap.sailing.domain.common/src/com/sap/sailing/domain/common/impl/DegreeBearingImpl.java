package com.sap.sailing.domain.common.impl;

import java.io.ObjectStreamException;

import com.sap.sse.common.AbstractBearing;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.settings.GwtIncompatible;

/**
 * This class is marked as deprecated because it was refactored to {@link com.sap.sse.common.impl.DegreeBearingImpl}
 * which shall be used instead. Nevertheless it needs to exist due to serialization issues which can be resolved with a
 * new general data import at each server instance. The serialization progress of this class is therefore adjusted by
 * the use of {@link #readResolve()} which returns the refactored {@link com.sap.sse.common.impl.DegreeBearingImpl}
 * object. Deserialization of this class now points to the refactored {@link com.sap.sse.common.impl.DegreeBearingImpl}.
 * 
 * 
 * @author Maximilian Gro� (D064866)
 *
 */
@Deprecated
@GwtIncompatible
public class DegreeBearingImpl extends AbstractBearing implements Bearing {
    private static final long serialVersionUID = -8045400378221073451L;
    private final double bearingDeg;

    @Deprecated
    public DegreeBearingImpl(double bearingDeg) {
        super();
        this.bearingDeg = bearingDeg - 360 * (int) (bearingDeg / 360.);
    }

    @Deprecated
    @Override
    public double getDegrees() {
        return bearingDeg;
    }

    private Object readResolve() throws ObjectStreamException {
        return new com.sap.sse.common.impl.DegreeBearingImpl(bearingDeg);
    }

}
