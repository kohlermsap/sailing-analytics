#!/bin/bash
if [ -z "${1}" -o "-h" = "${1}" ]; then
  echo "Usage: ${0} {MONGO_URI} {NEW_API_TOKEN} [ {OLD_API_TOKEN_TO_REPLACE} ]"
  echo ""
  echo "Sets a new API token in the MONGO DB whose URI is given. This affects"
  echo "the tracTracApiToken field in the CONNECTIVITY_PARAMS_FOR_RACES_TO_BE_RESTORED"
  echo "collection."
  echo "If the optional OLD_API_TOKEN_TO_REPLACE is provided, only those records will"
  echo "be updated that have an existing tracTracApiToken equal to the old API token."
else
  MONGO_URI="${1}"
  TOKEN="${2}"
  OLD_TOKEN="${3}"
  if [ -n "${OLD_TOKEN}" ]; then
    FILTER_FOR_OLD_TOKEN=', "tracTracApiToken": "'${OLD_TOKEN}'"'
  else
    FILTER_FOR_OLD_TOKEN=""
  fi
  mongosh --quiet --eval 'EJSON.stringify(db.CONNECTIVITY_PARAMS_FOR_RACES_TO_BE_RESTORED.updateMany({"type": "TRAC_TRAC"'"${FILTER_FOR_OLD_TOKEN}"'}, {$set: {"tracTracApiToken": "'${TOKEN}'"}}, {multi: true}))' "${MONGO_URI}"
fi
