package com.sap.sailing.landscape.ui.client;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.sap.sailing.landscape.ui.shared.SailingAnalyticsProcessDTO;
import com.sap.sailing.landscape.ui.shared.SailingApplicationReplicaSetDTO;
import com.sap.sse.common.Builder;

public class LinkBuilder implements Builder<LinkBuilder, SafeHtml> {

    static public enum pathModes {
        InstanceSearch, InstanceByAmiIdSearch, ImageSearch, AmiSearch, Hostname,
        ReplicaLinks, Version, MasterHost, TargetGroupSearch, AutoScalingGroupSearch, publicIp, privateIp
    };

    private pathModes pathMode;
    private String region;
    private String instanceId;
    private SailingApplicationReplicaSetDTO<String> replicaSet;
    private String targetGroupName;
    private String autoScalingGroupName;
    private String amiId;
    private String publicIp;
    private String privateIp;

    LinkBuilder setPathMode(pathModes mode) {
        pathMode = mode;
        return self();
    }

    LinkBuilder setTargetGroupName(String name) {
        this.targetGroupName = name;
        return self();
    }

    LinkBuilder setAutoScalingGroupName(String name) {
        this.autoScalingGroupName = name;
        return self();
    }

    LinkBuilder setInstanceId(String instanceId) {
        this.instanceId = instanceId;
        return self();
    }
    
    LinkBuilder setAmiId(String amiId) {
        this.amiId = amiId;
        return self();
    }

    LinkBuilder setRegion(String region) {
        this.region = region;
        return self();
    }

    LinkBuilder setPublicIp(String ip) {
        this.publicIp = ip;
        return self();
    }
    
    LinkBuilder setPrivateIp(String ip) {
        this.privateIp = ip;
        return self();
    }
    
    LinkBuilder setReplicaSet(SailingApplicationReplicaSetDTO<String> replicaSet) {
        this.replicaSet = replicaSet;
        return self();
    }

    private String getEc2ConsoleLinkForInstanceId(String instanceId) {
        return getEc2ConsoleBaseUrlForSelectedRegion() + "#Instances:search=" + instanceId;
    }
    
    private String getEc2ConsoleLinkForInstancesUsingImageWithId(String imageId) {
        return getEc2ConsoleBaseUrlForSelectedRegion() + "#Instances:imageId="+imageId;
    }

    private String getEc2ConsoleLinkForTargetGroupName(String name) {
        return getEc2ConsoleBaseUrlForSelectedRegion() + "#TargetGroups:search=" + name;
    }

    private String getEc2ConsoleLinkForAutoScalingGroupName(String name) {
        return getEc2ConsoleBaseUrlForSelectedRegion() + "#AutoScalingGroupDetails:id="+name+";view=details";
    }

    private String getEc2ConsoleLinkForAmiId(String amiId) {
        return getEc2ConsoleBaseUrlForSelectedRegion() + "#Images:visibility=owned-by-me;search=" + amiId;
    }

    private String getEc2ConsoleBaseUrlForSelectedRegion() {
        return "https://" + region + ".console.aws.amazon.com/ec2/v2/home?region=" + region;
    }

    private void appendEc2Link(final SafeHtmlBuilder builder, final String ec2Link, final String text) {
        builder.appendHtmlConstant("<a target=\"_blank\" href=\"" + ec2Link + "\">");
        builder.appendEscaped(text);
        builder.appendHtmlConstant("</a>");
    }

    private void appendEc2InstanceLink(final SafeHtmlBuilder builder, final String instanceId) {
        final String ec2Link = getEc2ConsoleLinkForInstanceId(instanceId);
        appendEc2Link(builder, ec2Link, instanceId);
    }

    private void appendEc2InstanceByAmiIdLink(final SafeHtmlBuilder builder, final String amiId) {
        final String ec2Link = getEc2ConsoleLinkForInstancesUsingImageWithId(amiId);
        appendEc2Link(builder, ec2Link, amiId);
    }

    private void appendEc2AmiLink(final SafeHtmlBuilder builder, final String amiId) {
        final String ec2Link = getEc2ConsoleLinkForAmiId(amiId);
        appendEc2Link(builder, ec2Link, amiId);
    }

    private void appendEc2TargetGroupLink(final SafeHtmlBuilder builder, final String name) {
        final String ec2Link = getEc2ConsoleLinkForTargetGroupName(name);
        appendEc2Link(builder, ec2Link, name);
    }

    private void appendEc2AutoScalingGroupLink(final SafeHtmlBuilder builder, final String name) {
        final String ec2Link = getEc2ConsoleLinkForAutoScalingGroupName(name);
        appendEc2Link(builder, ec2Link, name);
    }

    private String getGwtStatusLink(final String host, int port) {
        return (port == 443 ? "https" : "http") + "://" + host + ":" + port + "/gwt/status";
    }

    /**
     * Checks if an attribute is null and throws an exception if so. 
     *
     * @param attr The variable name to check for null.
     * @param name The human readable name of the variable.
     * @throws Exception This stores the value which is not set.
     */
    private void checkAttribute(Object attr, String name) throws Exception {
        if (attr == null) {
            throw new Exception(name + " needs to be given!");
        }
    }

    @Override
    public SafeHtml build() {
        SafeHtmlBuilder builder = new SafeHtmlBuilder();
        try {
            switch (pathMode) {
            case ReplicaLinks:
                checkAttribute(replicaSet, "Replicaset");
                for (final SailingAnalyticsProcessDTO replica : replicaSet.getReplicas()) {
                    final String gwtStatusLink = getGwtStatusLink(replica.getHost().getPublicIpAddress(),
                            replica.getPort());
                    builder.appendHtmlConstant("<a target=\"_blank\" href=\"" + gwtStatusLink + "\">");
                    builder.appendEscaped(replica.getHost().getPublicIpAddress() + ":" + replica.getPort());
                    builder.appendHtmlConstant("</a>");
                    final String replicaInstanceId = replica.getHost().getInstanceId();
                    builder.appendEscaped(" (");
                    builder.appendEscaped(replica.getServerName());
                    builder.appendEscaped(", ");
                    appendEc2InstanceLink(builder, replicaInstanceId);
                    builder.appendEscaped(")");
                    builder.appendHtmlConstant("<br>");
                }
                break;
            case AmiSearch:
                checkAttribute(replicaSet, "Replicaset");
                checkAttribute(region, "Region");
                if (replicaSet.getAutoScalingGroupAmiId() != null) {
                    appendEc2AmiLink(builder, replicaSet.getAutoScalingGroupAmiId());
                }

                break;
            case Hostname:
                checkAttribute(replicaSet, "Replicaset");
                final String hostnameLink = "https://" + replicaSet.getHostname();
                builder.appendHtmlConstant("<a target=\"_blank\" href=\"" + hostnameLink + "\">");
                builder.appendEscaped(replicaSet.getHostname());
                builder.appendHtmlConstant("</a>");
                break;
            case ImageSearch:
                break;
            case InstanceSearch:
                checkAttribute(region, "Region");
                appendEc2InstanceLink(builder, instanceId);
                break;
            case InstanceByAmiIdSearch:
                appendEc2InstanceByAmiIdLink(builder, amiId);
                break;
            case Version:
                checkAttribute(replicaSet, "Replicaset");
                final String version = replicaSet.getVersion();
                final String releaseNotesLink = replicaSet.getReleaseNotesLink();
                appendEc2Link(builder, releaseNotesLink, version);
                break;
            case MasterHost:
                checkAttribute(replicaSet, "Replicaset");
                final String gwtStatusLink = getGwtStatusLink(replicaSet.getMaster().getHost().getPublicIpAddress(),
                        replicaSet.getMaster().getPort());
                builder.appendHtmlConstant("<a target=\"_blank\" href=\"" + gwtStatusLink + "\">");
                builder.appendEscaped(replicaSet.getMaster().getHost().getPublicIpAddress());
                builder.appendHtmlConstant("</a>");
                break;
            case TargetGroupSearch:
                checkAttribute(region, "Region");
                checkAttribute(targetGroupName, "Target Group Name");
                appendEc2TargetGroupLink(builder, targetGroupName);
                break;
            case AutoScalingGroupSearch:
                checkAttribute(region, "Region");
                checkAttribute(autoScalingGroupName, "Auto-Scaling Group Name");
                appendEc2AutoScalingGroupLink(builder, autoScalingGroupName);
                break;
            case publicIp:
                checkAttribute(publicIp, "Public Ip");
                final String ipLink = "http://" + publicIp;
                builder.appendHtmlConstant("<a target=\"_blank\" href=\"" + ipLink + "\">");
                builder.appendEscaped(publicIp);
                builder.appendHtmlConstant("</a>");
                break;
            case privateIp:
                checkAttribute(privateIp, "Private Ip");
                final String privateIpLink = "http://" + privateIp;
                builder.appendHtmlConstant("<a target=\"_blank\" href=\"" + privateIpLink + "\">");
                builder.appendEscaped(privateIp);
                builder.appendHtmlConstant("</a>");
                break;
            }
        } catch (Exception e) {
            builder.appendHtmlConstant("<a target=\"_blank\" >");
            builder.appendEscaped(e.getMessage());
            builder.appendHtmlConstant("</a>");
        }

        return builder.toSafeHtml();
    }
}
