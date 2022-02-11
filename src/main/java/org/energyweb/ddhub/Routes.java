package org.energyweb.ddhub;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.energyweb.ddhub.dto.MessageDTO;
import org.energyweb.ddhub.dto.MultipartBody;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.google.gson.Gson;

@ApplicationScoped
public class Routes extends RouteBuilder {

        @ConfigProperty(name = "AZURE_ACCOUNT_NAME")
        String azureAccountName;

        @ConfigProperty(name = "AZURE_ACCESS_KEY")
        String azureAccessKey;

        @ConfigProperty(name = "DDHUB_CONTEXT_URL")
        String ddhubContextURL;

        public Routes() {

        }

        @Override
        public void configure() throws Exception {
                StorageSharedKeyCredential credential = new StorageSharedKeyCredential(azureAccountName,
                                azureAccessKey);
                String uri = String.format("https://%s.blob.core.windows.net", azureAccountName);
                BlobServiceClient client = new BlobServiceClientBuilder()
                                .endpoint(uri)
                                .credential(credential)
                                .buildClient();

                getContext().getRegistry().bind("client", client);

                from("direct:azureupload")
                                .process(e -> {

                                        MultipartBody multipartBody = (MultipartBody) e.getIn().getBody();
                                        MessageDTO messageDTO = new MessageDTO();
                                        messageDTO.setFqcn(multipartBody.getFqcn());
                                        messageDTO.setTopicId(multipartBody.getTopicId());
                                        e.setProperty("multipartBody", multipartBody);
                                        e.setProperty("messageDTO", messageDTO);

                                        String key = multipartBody.getFileName();
                                        byte[] bytes = multipartBody.getFile().readAllBytes();
                                        e.getIn().setHeader("CamelAzureStorageBlobBlobName",
                                                        messageDTO.storageName() + key);
                                        e.getIn().setBody(bytes);
                                })
                                .to("azure-storage-blob://{{AZURE_ACCOUNT_NAME}}/{{AZURE_CONTAINER_NAME}}?operation=uploadBlockBlob&serviceClient=#client")
                                .process(e -> {
                                        MessageDTO messageDTO = (MessageDTO) e.getProperty("messageDTO");
                                        MultipartBody multipartBody = (MultipartBody) e.getProperty("multipartBody");
                                        JsonObjectBuilder builder = Json.createObjectBuilder();
                                        JsonObject jsonObject = builder
                                                        .add("filename", multipartBody.getFileName())
                                                        .add("download", ddhubContextURL + "/message/download"
                                                                        + "?fileId="
                                                                        + URLEncoder.encode(multipartBody.getFileName(),
                                                                                        StandardCharsets.UTF_8
                                                                                                        .toString()))
                                                        .add("signature", multipartBody.getSignature())
                                                        .build();
                                        messageDTO.setPayload(jsonObject.toString());
                                        e.getIn().setBody(new Gson().toJson(messageDTO));
                                })
                                .setHeader(Exchange.HTTP_METHOD, simple("POST"))
                                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                                // .setHeader("Accept", constant("application/json"))
                                .to("netty-http:http://127.0.0.1:{{quarkus.http.port}}/message?throwExceptionOnFailure=true");

                from("direct:azuredownload")
                                .to("azure-storage-blob://{{AZURE_ACCOUNT_NAME}}/{{AZURE_CONTAINER_NAME}}?operation=getBlob&serviceClient=#client");

        }
}
