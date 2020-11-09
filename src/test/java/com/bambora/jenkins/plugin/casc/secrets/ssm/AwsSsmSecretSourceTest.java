package com.bambora.jenkins.plugin.casc.secrets.ssm;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.AWSSimpleSystemsManagementException;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AwsSsmSecretSource.class)
public class AwsSsmSecretSourceTest {

    private AwsSsmSecretSource underTest;

    @Before
    public void setup() {
        underTest = PowerMockito.spy(new AwsSsmSecretSource());
    }

    @Test
    public void notFound() throws Exception{
        AWSSimpleSystemsManagement client = Mockito.mock(AWSSimpleSystemsManagement.class);
        PowerMockito.doReturn(client)
                .when(underTest, "getClient");
        Mockito.when(client.getParameter(Mockito.any())).thenThrow(ParameterNotFoundException.class);

        Assert.assertEquals(Optional.empty(), underTest.reveal("parameter"));
    }

    @Test
    public void found() throws Exception{
        AWSSimpleSystemsManagement client = Mockito.mock(AWSSimpleSystemsManagement.class);
        PowerMockito.doReturn(client)
                .when(underTest, "getClient");

        GetParameterResult result = new GetParameterResult();
        result.setParameter(new Parameter().withName("parameter").withValue("value"));

        Mockito.when(client.getParameter(Mockito.any())).thenReturn(result);

        Assert.assertEquals(Optional.of("value"), underTest.reveal("parameter"));
    }

    @Test
    public void error() throws Exception{
        AWSSimpleSystemsManagement client = Mockito.mock(AWSSimpleSystemsManagement.class);
        PowerMockito.doReturn(client)
                .when(underTest, "getClient");

        Mockito.when(client.getParameter(Mockito.any())).thenThrow(AWSSimpleSystemsManagementException.class);

        Assert.assertEquals(Optional.empty(), underTest.reveal("parameter"));
    }

    @Test
    public void prefixFound() throws Exception{
        PowerMockito.doReturn("prefix.")
                .when(underTest, "getSystemProperty");
        AWSSimpleSystemsManagement client = Mockito.mock(AWSSimpleSystemsManagement.class);
        PowerMockito.doReturn(client)
                .when(underTest, "getClient");

        GetParameterResult result = new GetParameterResult();
        result.setParameter(new Parameter().withName("prefix.parameter").withValue("value"));

        Mockito.when(client.getParameter(Mockito.any())).thenReturn(result);

        Assert.assertEquals(Optional.of("value"), underTest.reveal("prefix.parameter"));
    }

    @Test
    public void errorMissingRegion() throws Exception{
        AWSSimpleSystemsManagement client = Mockito.mock(AWSSimpleSystemsManagement.class);
        PowerMockito.doReturn(client)
                .when(underTest, "getClient");

        Mockito.when(client.getParameter(Mockito.any())).thenThrow(SdkClientException.class);

        Assert.assertEquals(Optional.empty(), underTest.reveal("parameter"));
    }

    @Test
    public void resolveKey() throws Exception{
        Assert.assertEquals(underTest.getResolveKey("parameter"), "parameter");
    }

    @Test
    public void resolveKeyWithCascPrefix() throws Exception{
        Assert.assertEquals(underTest.getResolveKey(AwsSsmSecretSource.SECRETS_MANAGER_PREFIX + "parameter"), AwsSsmSecretSource.SECRETS_MANAGER_PREFIX + "parameter");
    }

    @Test
    public void resolveKeyWithSecretsManagerPrefix() throws Exception{
        PowerMockito.doReturn("prefix.").when(underTest, "getSystemProperty");
        Assert.assertEquals(underTest.getResolveKey("parameter"), "prefix.parameter");
    }

    @Test
    public void resolveKeyWithCascAndSecretsManagerPrefixes() throws Exception{
        PowerMockito.doReturn("prefix.").when(underTest, "getSystemProperty");
        Assert.assertEquals(underTest.getResolveKey(AwsSsmSecretSource.SECRETS_MANAGER_PREFIX + "parameter"), AwsSsmSecretSource.SECRETS_MANAGER_PREFIX + "prefix.parameter");
    }
}
