// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloudian.cloudstack;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.log4j.Logger;

import com.cloud.utils.nio.TrustAllManager;
import com.cloudian.client.GroupInfo;
import com.cloudian.client.UserInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CloudianClient {
    private static final Logger LOG = Logger.getLogger(CloudianClient.class);

    private final HttpClient httpClient;
    private final String baseUrl;

    public CloudianClient(final String baseUrl, final String username, final String password, final boolean validateSSlCertificate) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        this.baseUrl = baseUrl;

        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
        final CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, credentials);

        if (!validateSSlCertificate) {
            final SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null,  new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    return true;
                }
            });
            final SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());

            final SSLContext sslcontext = SSLUtils.getSSLContext();
            sslcontext.init(null, new X509TrustManager[]{new TrustAllManager()}, new SecureRandom());
            final SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext, NoopHostnameVerifier.INSTANCE);

            this.httpClient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .setSSLSocketFactory(factory)
                    .build();
        } else {
            this.httpClient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .build();
        }
    }

    private HttpResponse delete(final String path) throws IOException {
        return httpClient.execute(new HttpDelete(baseUrl + path));
    }

    private HttpResponse get(final String path) throws IOException {
        return httpClient.execute(new HttpGet(baseUrl + path));
    }

    private HttpResponse post(final String path, final Object item) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(item);
        final StringEntity entity = new StringEntity(json);
        final HttpPost request = new HttpPost(baseUrl + path);
        request.setHeader("Content-type", "application/json");
        request.setEntity(entity);
        return httpClient.execute(request);
    }

    private HttpResponse put(final String path, final Object item) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(item);
        final StringEntity entity = new StringEntity(json);
        final HttpPut request = new HttpPut(baseUrl + path);
        request.setHeader("Content-type", "application/json");
        request.setEntity(entity);
        return httpClient.execute(request);
    }

    public UserInfo addUser(final UserInfo user) {
        try {
            final HttpResponse response = put("/user", user);
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.getEntity().getContent(), UserInfo.class);
        } catch (final IOException e) {
            LOG.error("Failed to add Cloudian user due to:", e);
        }
        return null;
    }

    public UserInfo listUser(final String userId, final String groupId) {
        try {
            final HttpResponse response = get(String.format("/user?userId=%s&groupId=%s", userId, groupId));
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.getEntity().getContent(), UserInfo.class);
        } catch (final IOException e) {
            LOG.error("Failed to list Cloudian user due to:", e);
        }
        return null;
    }

    public List<UserInfo> listUsers(final String groupId) {
        try {
            final HttpResponse response = get(String.format("/user/list?groupId=%s&userType=all&userStatus=active", groupId));
            final ObjectMapper mapper = new ObjectMapper();
            return Arrays.asList(mapper.readValue(response.getEntity().getContent(), UserInfo[].class));
        } catch (final IOException e) {
            LOG.error("Failed to list Cloudian users due to:", e);
        }
        return null;
    }

    public boolean updateUser(final UserInfo user) {
        try {
            final HttpResponse response = post("/user", user);
            return response.getStatusLine().getStatusCode() == 200;
        } catch (final IOException e) {
            LOG.error("Failed to update Cloudian user due to:", e);
        }
        return false;
    }

    public boolean removeUser(final String userId, final String groupId) {
        try {
            final HttpResponse response = delete(String.format("/user?userId=%s&groupId=%s", userId, groupId));
            return response.getStatusLine().getStatusCode() == 200;
        } catch (final IOException e) {
            LOG.error("Failed to remove Cloudian user due to:", e);
        }
        return false;
    }

    public GroupInfo addGroup(final GroupInfo group) {
        try {
            final HttpResponse response = put("/group", group);
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.getEntity().getContent(), GroupInfo.class);
        } catch (final IOException e) {
            LOG.error("Failed to add Cloudian group due to:", e);
        }
        return null;
    }

    public GroupInfo listGroup(final String groupId) {
        try {
            final HttpResponse response = get(String.format("/group?groupId=%s", groupId));
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.getEntity().getContent(), GroupInfo.class);
        } catch (final IOException e) {
            LOG.error("Failed to list Cloudian group due to:", e);
        }
        return null;
    }

    public List<GroupInfo> listGroups() {
        try {
            final HttpResponse response = get("/group/list");
            final ObjectMapper mapper = new ObjectMapper();
            return Arrays.asList(mapper.readValue(response.getEntity().getContent(), GroupInfo[].class));
        } catch (final IOException e) {
            LOG.error("Failed to list Cloudian groups due to:", e);
        }
        return null;
    }

    public boolean updateGroup(final GroupInfo group) {
        try {
            final HttpResponse response = post("/group", group);
            return response.getStatusLine().getStatusCode() == 200;
        } catch (final IOException e) {
            LOG.error("Failed to remove group due to:", e);
        }
        return false;
    }

    public boolean removeGroup(final String groupId) {
        try {
            final HttpResponse response = delete(String.format("/group?groupId=%s", groupId));
            return response.getStatusLine().getStatusCode() == 200;
        } catch (final IOException e) {
            LOG.error("Failed to remove group due to:", e);
        }
        return false;
    }

}
