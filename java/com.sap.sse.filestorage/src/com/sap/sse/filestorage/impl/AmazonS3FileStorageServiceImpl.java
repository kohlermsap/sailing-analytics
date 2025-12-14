package com.sap.sse.filestorage.impl;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.osgi.framework.BundleContext;

import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.filestorage.FileStorageService;
import com.sap.sse.filestorage.FileStorageServiceProperty;
import com.sap.sse.filestorage.InvalidPropertiesException;
import com.sap.sse.filestorage.OperationFailedException;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * For testing purposes configure the access credentials as follows: To link this service to an AWS account, create the
 * following file: ~/.aws/credentials and add credentials to it (get the access id and secret key from
 * https://console.aws.amazon.com/iam/home?#security_credential).
 * 
 * @author Fredrik Teschke
 * @author Axel Uhl
 *
 */
public class AmazonS3FileStorageServiceImpl extends BaseFileStorageServiceImpl implements FileStorageService {
    private static final long serialVersionUID = -2406798172882732531L;
    public static final String NAME = "Amazon S3";

    private static final Logger logger = Logger.getLogger(AmazonS3FileStorageServiceImpl.class.getName());

    private static final String retrievalProtocol = "https";
    private static final String regionRetrievalHost = "s3.dualstack.eu-west-1.amazonaws.com";

    private final FileStorageServicePropertyImpl accessId = new FileStorageServicePropertyImpl("accessId", false,
            /* isPassword */ false, "s3AccessIdDesc");
    private final FileStorageServicePropertyImpl accessKey = new FileStorageServicePropertyImpl("accessKey", false,
            /* isPassword */ true, "s3AccessKeyDesc");
    private final FileStorageServicePropertyImpl bucketName = new FileStorageServicePropertyImpl("bucketName", true,
            /* isPassword */ false, "s3BucketNameDesc");

    public AmazonS3FileStorageServiceImpl(BundleContext bundleContext) {
        super(NAME, "s3Desc", bundleContext);
        addProperties(accessId, accessKey, bucketName);
    }

    private S3Client createS3Client() throws InvalidPropertiesException {
        final AwsCredentialsProvider credsProvider;
        // first try to use properties
        if (accessId.getValue() != null && accessKey.getValue() != null) {
            credsProvider = ()->AwsBasicCredentials.create(accessId.getValue(), accessKey.getValue());
        } else {
            // if properties are empty, read credentials from ~/.aws/credentials
            try {
                credsProvider = ProfileCredentialsProvider.create();
            } catch (Exception e) {
                throw new InvalidPropertiesException(
                        "credentials in ~/.aws/credentials seem to be invalid (tried this as fallback because properties were empty)",
                        e);
            }
        }
        return S3Client.builder().credentialsProvider(credsProvider).build();
    }

    private URI getUri(String key) {
        try {
            return new URI(retrievalProtocol, regionRetrievalHost, "/" + bucketName.getValue() + "/" + key, null);
        } catch (URISyntaxException e) {
            logger.log(Level.WARNING, "Could not create URI for uploaded file with key " + key, e);
            return null;
        }
    }

    @Override
    public URI storeFile(final InputStream is, String fileExtension, long lengthInBytes)
            throws InvalidPropertiesException, OperationFailedException, UnauthorizedException {
        final String key = getKey(fileExtension);
        return getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredDomainType.FILE_STORAGE, new TypeRelativeObjectIdentifier(key),
                key, () -> {
                    final PutObjectRequest request = PutObjectRequest.builder().contentLength(lengthInBytes).
                            bucket(bucketName.getValue()).key(key).acl(ObjectCannedACL.PUBLIC_READ).build();
                    createS3Client().putObject(request, RequestBody.fromInputStream(is, lengthInBytes));
                    URI uri = getUri(key);
                    logger.info("Stored file " + uri);
                    return uri;
                });
    }

    @Override
    public void removeFile(URI uri) throws InvalidPropertiesException, OperationFailedException, UnauthorizedException {
        String key = uri.getPath().substring(uri.getPath().lastIndexOf("/")+1);
        getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(SecuredDomainType.FILE_STORAGE.getQualifiedObjectIdentifier(
                new TypeRelativeObjectIdentifier(key)), () -> {
                    S3Client s3Client = createS3Client();
                    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName.getValue()).key(key).build());
                    logger.info("Removed file " + uri);
                });
    }

    @Override
    public void testProperties() throws InvalidPropertiesException {
        S3Client s3 = createS3Client();
        if (bucketName.getValue().equals("")) {
            throw new InvalidPropertiesException("empty bucketname is not allowed");
        }
        // test if credentials are valid
        // TODO seems to even work if credentials are not valid if bucket is publicly visible
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucketName.getValue()).build());
        } catch (NoSuchBucketException nsbe) {
            throw new InvalidPropertiesException("invalid bucket", new Pair<FileStorageServiceProperty, String>(
                    bucketName, "bucket does not exist"));
        } catch (Exception e) {
            throw new InvalidPropertiesException("invalid credentials or not enough access rights for the bucket: " + e.getCause(), e,
                    new Pair<FileStorageServiceProperty, String>(accessId, "seems to be invalid"),
                    new Pair<FileStorageServiceProperty, String>(accessKey, "seems to be invalid"));
        }
    }

    @Override
    public void doPermissionCheckForGetFile(URI uri) throws UnauthorizedException {
        String key = uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.FILE_STORAGE.getStringPermissionForTypeRelativeIdentifier(DefaultActions.READ,
                        new TypeRelativeObjectIdentifier(key)));
    }
}
