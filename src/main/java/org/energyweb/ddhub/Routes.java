package org.energyweb.ddhub;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.storage.blob.BlobConstants;
import org.bson.Document;
import org.energyweb.ddhub.dto.MessageDTO;
import org.energyweb.ddhub.dto.MultipartBody;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.google.gson.Gson;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.IndexOptions;

/**
 * Camel route definitions.
 */
public class Routes extends RouteBuilder {

	 
        public Routes() {
        	
        }

        @Override
        public void configure() throws Exception {

                String azureAccountName = "vcaemo";
                String azureAccessKey = "lS5Zh7D4CMGcwFVOrJQzfUzRgV5B9Hetrn3iOXEf/G64+MHuC/tuXdpx5K83LqjbIgEgKyIrM/83tUdyANeVlA==";
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
                                        e.setProperty("messageDTO", messageDTO);
                                        e.setProperty("signature", multipartBody.getSignature());
                                        String key = multipartBody.fileName;
                                        byte[] bytes = multipartBody.file.readAllBytes();
                                        e.getIn().setHeader("CamelAzureStorageBlobBlobName", key);
                                        e.getIn().setBody(bytes);
                                })
                                .to("azure-storage-blob://vcaemo/vcfile?operation=uploadBlockBlob&serviceClient=#client")
                                .to("azure-storage-blob://vcaemo/vcfile?operation=downloadLink&serviceClient=#client")
                                .process(e -> {
                                        JsonObjectBuilder builder = Json.createObjectBuilder();
                                        JsonObject jsonObject = builder
                                                        .add("filename", e.getMessage().getHeader(
                                                                        BlobConstants.BLOB_NAME, String.class))
                                                        .add("download", e.getMessage().getHeader(
                                                                        BlobConstants.DOWNLOAD_LINK, String.class))
                                                        .add("signature", e.getProperty("signature").toString())
                                                        .build();
                                        MessageDTO messageDTO = (MessageDTO) e.getProperty("messageDTO");
                                        messageDTO.setPayload(jsonObject.toString());
                                        e.getIn().setBody(new Gson().toJson(messageDTO));
                                }).process(e -> {

                                })
                                .setHeader(Exchange.HTTP_METHOD, simple("POST"))
                                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                                // .setHeader("Accept", constant("application/json"))
                                .to("netty-http:http://127.0.0.1:{{quarkus.http.port}}/message?throwExceptionOnFailure=false");

        }
}
