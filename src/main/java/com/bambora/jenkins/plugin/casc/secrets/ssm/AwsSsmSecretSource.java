package com.bambora.jenkins.plugin.casc.secrets.ssm;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.AWSSimpleSystemsManagementException;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException;
import hudson.Extension;
import io.jenkins.plugins.casc.SecretSource;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class AwsSsmSecretSource extends SecretSource {

    public static final String CASC_SSM_PREFIX = "CASC_SSM_PREFIX";
    public static final String SECRETS_MANAGER_PREFIX = "/aws/reference/secretsmanager/";

    private static final Logger LOG = Logger.getLogger(AwsSsmSecretSource.class.getName());

    @Override
    public Optional<String> reveal(String key) {
        String resolveKey = getResolveKey(key);
        try {
            GetParameterRequest request = new GetParameterRequest();
            request.withName(resolveKey).withWithDecryption(true);
            GetParameterResult result = getClient().getParameter(request);
            return Optional.of(result.getParameter().getValue());
        } catch (ParameterNotFoundException e) {
            LOG.info("Could not find secret: " + resolveKey);
            return Optional.empty();
        } catch (AWSSimpleSystemsManagementException e) {
            LOG.log(Level.SEVERE, "Error getting ssm secret: " + resolveKey, e);
            return Optional.empty();
        } catch (SdkClientException e) {
            LOG.log(Level.SEVERE, "Error building sdk: " + resolveKey, e);
            return Optional.empty();
        }
    }

    private AWSSimpleSystemsManagement getClient() {
        AWSSimpleSystemsManagementClientBuilder builder = AWSSimpleSystemsManagementClientBuilder.standard();
        builder.setCredentials(new DefaultAWSCredentialsProviderChain());
        return builder.build();
    }

    private String getSystemProperty() {
        return System.getenv(CASC_SSM_PREFIX);
    }

    // Visible for testing
    public String getResolveKey(String key) {
        String prefix =  getSystemProperty();
        if (prefix != null) {
            if (key.startsWith(SECRETS_MANAGER_PREFIX)) {
                return SECRETS_MANAGER_PREFIX + prefix + key.substring(SECRETS_MANAGER_PREFIX.length());
            } else {
                return prefix + key;
            }
        }
        return key;
    }
}
