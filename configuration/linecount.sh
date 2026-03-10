#!/bin/sh
wc --total=only -l `find com.sap.* */com.sap.* -type f \( -name '*.java' -o -name '*.gwt.xml' -o -name '*.ini' -o -name '*.html' -o -name '*.css' -o -name '*.gss' \) \! -ipath '*/generated/*' \! -ipath '*/gen/*' \! -ipath '*/bin/*' \! -ipath '*/generated-sources/*' \! -ipath '*/.generated/*' \! -name R.java \! -ipath './java/com.sap.sailing.gwt.ui/com.sap.sailing.gwt.*' 2>/dev/null`
