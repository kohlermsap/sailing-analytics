package com.sap.sse.landscape.impl;

import java.net.MalformedURLException;
import java.net.URL;

import com.sap.sse.landscape.Release;

public class GithubRelease extends AbstractRelease implements Release {
    private static final long serialVersionUID = -8587383557709591724L;
    private final String downloadURL;
    private final String releaseNotesURL;

    public GithubRelease(String name, String downloadURL, String releaseNotesURL) {
        super(name);
        this.downloadURL = downloadURL;
        this.releaseNotesURL = releaseNotesURL;
    }

    @Override
    public URL getReleaseNotesURL() {
        try {
            return new URL(releaseNotesURL);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public URL getDeployableArchiveURL() {
        try {
            return new URL(downloadURL);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
