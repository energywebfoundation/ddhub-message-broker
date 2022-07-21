package org.energyweb.ddhub;

import java.util.Arrays;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.JsonbBuilder;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.energyweb.ddhub.dto.FileUploadDTO;
import org.energyweb.ddhub.dto.FileUploadDTOs;
import org.energyweb.ddhub.dto.MessageDTOs;
import org.jboss.logging.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;

@ApplicationScoped
public class Routes extends RouteBuilder {

        @ConfigProperty(name = "BLOB_STORAGE_ACCOUNT_NAME")
        String azureAccountName;

        @ConfigProperty(name = "BLOB_STORAGE_ACCESS_KEY")
        String azureAccessKey;

        @ConfigProperty(name = "DDHUB_CONTEXT_URL")
        String ddhubContextURL;

        @Inject
        Logger logger;

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

                                        FileUploadDTOs multipartBody = (FileUploadDTOs) e.getIn().getBody();
                                        MessageDTOs messageDTO = new MessageDTOs();
                                        messageDTO.setFqcns(Arrays.asList(multipartBody.getFqcns().split(",")));
                                        messageDTO.setFqcn(multipartBody.getOwnerdid());
                                        messageDTO.setClientGatewayMessageId(multipartBody.getClientGatewayMessageId());
                                        messageDTO.setTopicId(multipartBody.getTopicId());
                                        messageDTO.setSignature(multipartBody.getSignature());
                                        messageDTO.setTopicVersion(multipartBody.getTopicVersion());
                                        messageDTO.setTransactionId(multipartBody.getTransactionId());
                                        messageDTO.setPayloadEncryption(multipartBody.isPayloadEncryption());
                                        messageDTO.setFromUpload(true);
                                        e.setProperty("multipartBody", multipartBody);
                                        e.setProperty("messageDTO", messageDTO);

                                        String key = multipartBody.getFileName();
                                        byte[] bytes = multipartBody.getFile().readAllBytes();
                                        e.getIn().setHeader("CamelAzureStorageBlobBlobName",
                                                        messageDTO.storageName() + key);
                                        e.getIn().setBody(bytes);
                                })
                                .to("azure-storage-blob://{{BLOB_STORAGE_ACCOUNT_NAME}}/{{BLOB_CONTAINER_NAME}}?operation=uploadBlockBlob&serviceClient=#client")
                                .process(e -> {
                                        MessageDTOs messageDTO = (MessageDTOs) e.getProperty("messageDTO");
                                        FileUploadDTO multipartBody = (FileUploadDTO) e.getProperty("multipartBody");
                                        JsonObjectBuilder builder = Json.createObjectBuilder();
                                        JsonObject jsonObject = builder
                                                        .add("fileId", multipartBody.getFileName())
                                                        .build();
                                        messageDTO.setPayload(jsonObject.toString());
                                        e.getIn().setHeader("Authorization", e.getProperty("token"));
                                        e.getIn().setBody(JsonbBuilder.create().toJson(messageDTO));
                                })
                                .setHeader(Exchange.HTTP_METHOD, simple("POST"))
                                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                                .doTry()
                                .to("netty-http:http://127.0.0.1:{{quarkus.http.port}}/messages?throwExceptionOnFailure=true")
                                .doCatch(Exception.class)
                                .process(e -> {
                                        Exception exception = e.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                                        logger.info(exception);
                                        JSONParser parser = new JSONParser();
                                        JSONObject json = (JSONObject) parser
                                                        .parse(new String((byte[]) e.getIn().getBody()));
                                        throw new RuntimeException(json.get("returnMessage").toString());
                                })
                                .endDoTry();

                from("direct:azuredownload")
                                .to("azure-storage-blob://{{BLOB_STORAGE_ACCOUNT_NAME}}/{{BLOB_CONTAINER_NAME}}?operation=getBlob&serviceClient=#client");

        }
}
