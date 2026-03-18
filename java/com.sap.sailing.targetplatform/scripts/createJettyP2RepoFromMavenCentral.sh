#!/bin/bash
#
# Creates a local p2 repository for Jetty 9.4.x from Maven Central artifacts.
#
# Use this when the official Eclipse Jetty p2 repository at
#   https://download.eclipse.org/jetty/updates/jetty-bundles-9.x/
# does not yet contain the latest security patch release.
#
# The Jetty jars on Maven Central are already proper OSGi bundles (with
# Bundle-SymbolicName, etc.) and their -sources.jar files already carry
# Eclipse-SourceBundle headers, so we can publish them directly into a
# p2 repository.
#
# Prerequisites:
#   - An Eclipse installation accessible via the "eclipse" command on PATH
#     (needs the p2 publisher: org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher)
#     Typically any Eclipse IDE or the Equinox SDK will have this.
#   - curl and a POSIX shell
#
# Usage:
#   ./createJettyP2RepoFromMavenCentral.sh [VERSION]
#
#   VERSION defaults to 9.4.58.v20250814 if not supplied.
#
# After running, the p2 repository will be at:
#   /tmp/jetty-p2-repo-<VERSION>/repository
#
# You can then either:
#   a) Point your target platform definition at this local directory, or
#   b) Upload it to your p2 server (see uploadAndActivateJettyP2Repo.sh)
#
set -euo pipefail

VERSION="${1:-9.4.58.v20250814}"
echo "=== Building Jetty p2 repository for version ${VERSION} ==="

MAVEN_CENTRAL="https://repo1.maven.org/maven2"
WORK_DIR="/tmp/jetty-p2-repo-${VERSION}"
REPO_DIR="${WORK_DIR}/repository"
PLUGINS_DIR="${WORK_DIR}/staging/plugins"
FEATURES_DIR="${WORK_DIR}/staging/features/org.eclipse.jetty.bundles.f_${VERSION}"

rm -rf "${WORK_DIR}"
mkdir -p "${PLUGINS_DIR}" "${FEATURES_DIR}"

# ---------------------------------------------------------------------------
# Define the bundle list.
# Format: "maven-group-path|artifact-id|osgi-bundle-symbolic-name"
#
# The OSGi symbolic names must match what the existing target platform
# definition and feature.xml files reference.
# ---------------------------------------------------------------------------
BUNDLES="\
org/eclipse/jetty|jetty-annotations|org.eclipse.jetty.annotations
org/eclipse/jetty|jetty-client|org.eclipse.jetty.client
org/eclipse/jetty|jetty-continuation|org.eclipse.jetty.continuation
org/eclipse/jetty|jetty-deploy|org.eclipse.jetty.deploy
org/eclipse/jetty|jetty-http|org.eclipse.jetty.http
org/eclipse/jetty|jetty-io|org.eclipse.jetty.io
org/eclipse/jetty|jetty-jaas|org.eclipse.jetty.jaas
org/eclipse/jetty|jetty-jmx|org.eclipse.jetty.jmx
org/eclipse/jetty|jetty-jndi|org.eclipse.jetty.jndi
org/eclipse/jetty|jetty-plus|org.eclipse.jetty.plus
org/eclipse/jetty|jetty-proxy|org.eclipse.jetty.proxy
org/eclipse/jetty|jetty-rewrite|org.eclipse.jetty.rewrite
org/eclipse/jetty|jetty-security|org.eclipse.jetty.security
org/eclipse/jetty|jetty-server|org.eclipse.jetty.server
org/eclipse/jetty|jetty-servlet|org.eclipse.jetty.servlet
org/eclipse/jetty|jetty-util|org.eclipse.jetty.util
org/eclipse/jetty|jetty-util-ajax|org.eclipse.jetty.util.ajax
org/eclipse/jetty|jetty-webapp|org.eclipse.jetty.webapp
org/eclipse/jetty|jetty-xml|org.eclipse.jetty.xml
org/eclipse/jetty/websocket|websocket-api|org.eclipse.jetty.websocket.api
org/eclipse/jetty/websocket|websocket-client|org.eclipse.jetty.websocket.client
org/eclipse/jetty/websocket|websocket-common|org.eclipse.jetty.websocket.common
org/eclipse/jetty/websocket|websocket-server|org.eclipse.jetty.websocket.server
org/eclipse/jetty/websocket|websocket-servlet|org.eclipse.jetty.websocket.servlet
org/eclipse/jetty/osgi|jetty-osgi-boot|org.eclipse.jetty.osgi.boot
org/eclipse/jetty/osgi|jetty-osgi-boot-warurl|org.eclipse.jetty.osgi.boot.warurl
org/eclipse/jetty|apache-jsp|org.eclipse.jetty.apache-jsp
org/eclipse/jetty/osgi|jetty-osgi-boot-jsp|org.eclipse.jetty.osgi.boot.jsp"

# ---------------------------------------------------------------------------
# Download bundles and source bundles
# ---------------------------------------------------------------------------
echo ""
echo "--- Downloading bundles from Maven Central ---"
DOWNLOAD_ERRORS=0

while IFS='|' read -r GROUP ARTIFACT BSN; do
  [ -z "${GROUP}" ] && continue

  JAR_URL="${MAVEN_CENTRAL}/${GROUP}/${ARTIFACT}/${VERSION}/${ARTIFACT}-${VERSION}.jar"
  SRC_URL="${MAVEN_CENTRAL}/${GROUP}/${ARTIFACT}/${VERSION}/${ARTIFACT}-${VERSION}-sources.jar"

  echo "  ${ARTIFACT} ..."
  # Download main bundle
  if ! curl -sfL -o "${PLUGINS_DIR}/${BSN}_${VERSION}.jar" "${JAR_URL}"; then
    echo "    ERROR: Failed to download ${JAR_URL}"
    DOWNLOAD_ERRORS=$((DOWNLOAD_ERRORS + 1))
    continue
  fi

  # Download source bundle (non-fatal if missing)
  if ! curl -sfL -o "${PLUGINS_DIR}/${BSN}.source_${VERSION}.jar" "${SRC_URL}"; then
    echo "    WARNING: No source jar for ${ARTIFACT}"
    rm -f "${PLUGINS_DIR}/${BSN}.source_${VERSION}.jar"
  fi
done <<EOF
${BUNDLES}
EOF

if [ ${DOWNLOAD_ERRORS} -gt 0 ]; then
  echo ""
  echo "ERROR: ${DOWNLOAD_ERRORS} bundle(s) failed to download. Aborting."
  exit 1
fi

echo ""
echo "  Downloaded $(ls "${PLUGINS_DIR}"/*.jar 2>/dev/null | wc -l) jar files."

# ---------------------------------------------------------------------------
# Create the feature.xml
# This wraps all the downloaded bundles into a single installable feature
# called "org.eclipse.jetty.bundles.f" — matching the existing p2 repo.
# ---------------------------------------------------------------------------
echo ""
echo "--- Generating feature.xml ---"

cat > "${FEATURES_DIR}/feature.xml" <<FEATURE_HEADER
<?xml version="1.0" encoding="UTF-8"?>
<feature
      id="org.eclipse.jetty.bundles.f"
      label="Jetty Bundles"
      version="${VERSION}">

   <description>
      Jetty ${VERSION} bundles for OSGi, built from Maven Central artifacts.
   </description>

   <license url="http://www.apache.org/licenses/LICENSE-2.0">
      Apache License 2.0 / Eclipse Public License 1.0
   </license>

FEATURE_HEADER

while IFS='|' read -r GROUP ARTIFACT BSN; do
  [ -z "${GROUP}" ] && continue
  cat >> "${FEATURES_DIR}/feature.xml" <<PLUGIN_ENTRY
   <plugin
         id="${BSN}"
         download-size="0"
         install-size="0"
         version="${VERSION}"
         unpack="false"/>

PLUGIN_ENTRY

  # Also add the source plugin if we downloaded it
  if [ -f "${PLUGINS_DIR}/${BSN}.source_${VERSION}.jar" ]; then
    cat >> "${FEATURES_DIR}/feature.xml" <<SOURCE_ENTRY
   <plugin
         id="${BSN}.source"
         download-size="0"
         install-size="0"
         version="${VERSION}"
         unpack="false"/>

SOURCE_ENTRY
  fi
done <<EOF
${BUNDLES}
EOF

echo "</feature>" >> "${FEATURES_DIR}/feature.xml"

# ---------------------------------------------------------------------------
# Also create a source feature (org.eclipse.jetty.bundles.f.source)
# that references the source bundles – matching the existing p2 structure.
# ---------------------------------------------------------------------------
SOURCE_FEATURE_DIR="${WORK_DIR}/staging/features/org.eclipse.jetty.bundles.f.source_${VERSION}"
mkdir -p "${SOURCE_FEATURE_DIR}"

cat > "${SOURCE_FEATURE_DIR}/feature.xml" <<SOURCE_FEATURE_HEADER
<?xml version="1.0" encoding="UTF-8"?>
<feature
      id="org.eclipse.jetty.bundles.f.source"
      label="Jetty Bundles (Sources)"
      version="${VERSION}">

   <description>
      Source bundles for Jetty ${VERSION}, built from Maven Central artifacts.
   </description>

   <license url="http://www.apache.org/licenses/LICENSE-2.0">
      Apache License 2.0 / Eclipse Public License 1.0
   </license>

SOURCE_FEATURE_HEADER

while IFS='|' read -r GROUP ARTIFACT BSN; do
  [ -z "${GROUP}" ] && continue
  if [ -f "${PLUGINS_DIR}/${BSN}.source_${VERSION}.jar" ]; then
    cat >> "${SOURCE_FEATURE_DIR}/feature.xml" <<SOURCE_PLUGIN_ENTRY
   <plugin
         id="${BSN}.source"
         download-size="0"
         install-size="0"
         version="${VERSION}"
         unpack="false"/>

SOURCE_PLUGIN_ENTRY
  fi
done <<EOF
${BUNDLES}
EOF

echo "</feature>" >> "${SOURCE_FEATURE_DIR}/feature.xml"

# ---------------------------------------------------------------------------
# Publish the p2 repository using Eclipse's FeaturesAndBundlesPublisher
# ---------------------------------------------------------------------------
echo ""
echo "--- Publishing p2 repository ---"
echo "    Source:      ${WORK_DIR}/staging"
echo "    Destination: ${REPO_DIR}"

eclipse -nosplash -verbose \
  -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher \
  -metadataRepository "file:${REPO_DIR}" \
  -artifactRepository "file:${REPO_DIR}" \
  -source "${WORK_DIR}/staging" \
  -publishArtifacts \
  -configs gtk.linux.x86_64

echo ""
echo "=== SUCCESS ==="
echo ""
echo "p2 repository created at: ${REPO_DIR}"
echo ""
echo "To use it locally, update your target platform definition to point to:"
echo "  file:${REPO_DIR}"
echo ""
echo "To upload to sapsailing.com and update all workspace references, run:"
echo "  ./uploadAndActivateJettyP2Repo.sh ${VERSION}"