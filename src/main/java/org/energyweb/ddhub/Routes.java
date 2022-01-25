package org.energyweb.ddhub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.activation.DataHandler;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;

import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.energyweb.ddhub.dto.MultipartBody;

/**
 * Camel route definitions.
 */
public class Routes extends RouteBuilder {

        public Routes() {

        }

        @Override
        public void configure() throws Exception {
                // from("seda:readtopicnats")
                // // .pollEnrich().simple("nats:${header.topic}?servers=localhost:4222");
                // .pollEnrich().simple("kafka:${header.topic}");

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

                from("direct:c")
                                .process(e -> {
                                        MultipartBody multipartBody = (MultipartBody) e.getIn().getBody();
                                        String key = multipartBody.fileName;
                                        byte[] bytes = multipartBody.file.readAllBytes();
                                        e.getIn().setHeader("CamelAzureStorageBlobBlobName", key);
                                        e.getIn().setBody(bytes);
                                })
                                .to("azure-storage-blob://vcaemo/vcfile?operation=uploadBlockBlob&serviceClient=#client")
                                .setBody(constant("Successfull Job"));

        }
}
