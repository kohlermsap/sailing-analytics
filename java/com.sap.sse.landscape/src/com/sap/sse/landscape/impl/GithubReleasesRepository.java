package com.sap.sse.landscape.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.ReleaseRepository;
import com.sap.sse.util.HttpUrlConnectionHelper;

/**
 * Assumes a public GitHub repository where releases can be freely downloaded from
 * <code>https://github.com/{owner}/{repo}/releases/download/{release-name}</code>.
 * 
 * @author Axel Uhl (d043530)
 */
public class GithubReleasesRepository extends AbstractReleaseRepository implements ReleaseRepository {
    private final static Logger logger = Logger.getLogger(GithubReleasesRepository.class.getName());
    private final static String GITHUB_API_BASE_URL = "https://api.github.com";
    private final static String GITHUB_BASE_URL = "https://github.com";
    private final String owner;
    private final String repositoryName;
    
    public GithubReleasesRepository(String owner, String repositoryName, String defaultReleaseNamePrefix) {
        super(defaultReleaseNamePrefix);
        this.owner = owner;
        this.repositoryName = repositoryName;
    }
    
    private String getRepositoryPath() {
        return owner+"/"+repositoryName;
    }
    
    private String getReleasesURL() {
        return GITHUB_API_BASE_URL+"/repos/"+getRepositoryPath()+"/releases?per_page=100"; // TODO for unauthenticated requests there is a harsh rate limit of 60 requests per hour...
    }

    @Override
    public Release getRelease(String releaseName) {
        return new GithubRelease(releaseName, GITHUB_BASE_URL+"/"+getRepositoryPath()+"/releases/download/"+releaseName+"/"+releaseName+Release.ARCHIVE_EXTENSION,
                GITHUB_BASE_URL+"/"+getRepositoryPath()+"/releases/download/"+releaseName+"/"+Release.RELEASE_NOTES_FILE_NAME);
    }

    @Override
    protected Iterable<Release> getAvailableReleases() {
        final List<Release> result = new LinkedList<>();
        try {
            String nextPageURL = getReleasesURL();
            do {
                final URLConnection connection = HttpUrlConnectionHelper.redirectConnection(new URL(nextPageURL));
                final InputStream index = (InputStream) connection.getContent();
                final String linkHeader = connection.getHeaderField("link");
                final JSONArray releasesJson = (JSONArray) new JSONParser().parse(new InputStreamReader(index));
                addAllReleasesTo(releasesJson, result);
                nextPageURL = getNextPageURL(linkHeader);
            } while (nextPageURL != null);
        } catch (IOException | ParseException e) {
            logger.warning("Exception trying to find releases: "+e.getMessage());
        }
        return result;
    }

    private void addAllReleasesTo(JSONArray releasesJson, List<Release> result) {
        for (final Object releaseObject : releasesJson) {
            final JSONObject releaseJson = (JSONObject) releaseObject;
            final String name = releaseJson.get("name").toString();
            String archiveDownloadURL = null;
            String releaseNotesURL = null;
            for (final Object archiveAsset : (JSONArray) releaseJson.get("assets")) {
                final JSONObject archiveAssetJson = (JSONObject) archiveAsset;
                if (archiveAssetJson.get("content_type").equals("application/x-tar")) {
                    archiveDownloadURL = archiveAssetJson.get("browser_download_url").toString();
                } else if (archiveAssetJson.get("name").equals(Release.RELEASE_NOTES_FILE_NAME)) {
                    releaseNotesURL = archiveAssetJson.get("browser_download_url").toString();
                }
            }
            result.add(new GithubRelease(name, archiveDownloadURL, releaseNotesURL));
        }
    }

    private static final Pattern nextPagePattern = Pattern.compile(".*<([^<]*)>; rel=\"next\".*");
    String getNextPageURL(String linkHeader) {
        final String result;
        final Matcher m = nextPagePattern.matcher(linkHeader);
        if (m.matches()) {
            result = m.group(1);
        } else {
            result = null;
        }
        return result;
    }
}
