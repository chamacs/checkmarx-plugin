package com.checkmarx.jenkins;

import com.checkmarx.jenkins.exception.CxCredException;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cx.restclient.common.ErrorMessage;
import hudson.model.Item;
import hudson.model.Run;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;


//resolve between global or specific and username+pssd or credential manager
public class CxConnectionDetails {

    private String serverUrl;
    private String username;
    private String encryptedPassword;
    private Boolean isProxy;
    private Boolean isScaProxy;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return encryptedPassword;
    }

    public void setPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public Boolean isProxy() {
        return isProxy;
    }

    public void setProxy(Boolean proxy) {
        isProxy = proxy;
    }
    public Boolean isScaProxy() {
        return isScaProxy;
    }

    public void setScaProxy(Boolean scaProxy) {
        isScaProxy = scaProxy;
    }

    @NotNull
    private static CxConnectionDetails getCxCredentials(Run<?, ?> run, CxConnectionDetails ret, String credentialsId, String username, String passwordPlainText) {
        return getCxCredentials(username, passwordPlainText, credentialsId, ret, getCredentialsById(credentialsId, run));
    }

    public static CxConnectionDetails resolveCred(CxScanBuilder cxScanBuilder, CxScanBuilder.DescriptorImpl descriptor, Run<?, ?> run) {
        CxConnectionDetails ret = new CxConnectionDetails();
        cxScanBuilder.setGenerateXmlReport((cxScanBuilder.getGenerateXmlReport() == null) ? true : cxScanBuilder.getGenerateXmlReport());
        if (cxScanBuilder.isUseOwnServerCredentials()) {
            ret.setServerUrl(cxScanBuilder.getServerUrl());
            ret.setProxy(cxScanBuilder.getIsProxy());
            return getCxCredentials(run, ret, cxScanBuilder.getCredentialsId(), cxScanBuilder.getUsername(), cxScanBuilder.getPasswordPlainText());
        } else {
            ret.setServerUrl(descriptor.getServerUrl());
            ret.setProxy(descriptor.getIsProxy());
            return getCxCredentials(run, ret, descriptor.getCredentialsId(), descriptor.getUsername(), descriptor.getPasswordPlainText());
        }
    }

    public static CxConnectionDetails resolveCred(boolean useOwnServerCredentials, String serverUrl, String username, String pssd, String credId, boolean isProxy, CxScanBuilder.DescriptorImpl descriptor, Item item) throws CxCredException {

        CxConnectionDetails ret = new CxConnectionDetails();
        if (useOwnServerCredentials) {
            ret.setServerUrl(serverUrl);
            ret.setProxy(isProxy);
            return getCxCredentials(username, pssd, credId, ret, getCredentialsById(credId, item));
        } else {
            ret.setServerUrl(descriptor.getServerUrl());
            ret.setProxy(descriptor.getIsProxy());
            return getCxCredentials(descriptor.getUsername(), descriptor.getPasswordPlainText(), descriptor.getCredentialsId(), ret, getCredentialsById(descriptor.getCredentialsId(), item));
        }
    }

    @NotNull
    private static CxConnectionDetails getCxCredentials(String username, String pssd, String credId, CxConnectionDetails ret, UsernamePasswordCredentials credentialsById) {
        if (StringUtils.isNotEmpty(credId)) {
            UsernamePasswordCredentials c = credentialsById;
            ret.setUsername(c != null ? c.getUsername() : "");
            ret.setPassword(c != null ? Aes.encrypt(c.getPassword().getPlainText(), ret.getUsername()) : "");
            return ret;
        } else {
            ret.setUsername(StringUtils.defaultString(username));
            ret.setPassword(Aes.encrypt(StringUtils.defaultString(pssd), ret.getUsername()));
            return ret;
        }
    }

    static UsernamePasswordCredentials getCredentialsById(String credentialsId, Run<?, ?> run) {
        return CredentialsProvider.findCredentialById(
                credentialsId,
                StandardUsernamePasswordCredentials.class,
                run,
                Collections.emptyList());
    }

    static UsernamePasswordCredentials getCredentialsById(String credentialsId, Item item) {
        List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(
                StandardUsernamePasswordCredentials.class,
                item,
                null,
                Collections.emptyList());

        return CredentialsMatchers.firstOrNull(credentials, CredentialsMatchers.withId(credentialsId));
    }

    public static void validateCxCredentials(CxConnectionDetails credentials) throws CxCredException {
        if (StringUtils.isEmpty(credentials.getServerUrl()) ||
                StringUtils.isEmpty(credentials.getUsername()) ||
                StringUtils.isEmpty((Aes.decrypt(credentials.getPassword(), credentials.getUsername())))) {
            throw new CxCredException(ErrorMessage.CHECKMARX_SERVER_CONNECTION_FAILED.getErrorMessage());
        }
    }

}