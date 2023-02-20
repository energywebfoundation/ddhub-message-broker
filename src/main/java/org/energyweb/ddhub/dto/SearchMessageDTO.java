package org.energyweb.ddhub.dto;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.json.bind.annotation.JsonbDateFormat;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerInfo;
import io.nats.client.api.ConsumerConfiguration.Builder;
import io.opentelemetry.extension.annotations.WithSpan;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SearchMessageDTO {

	@JsonIgnore
	private String fqcn;

	@Size(min = 1, max = 255, message = "The length between 1-255 characters")
	@Pattern(regexp = "^(?!(did|DID|dID|dId|DiD|DId|Did):).+(\\w*)", message = "DIDs format identify")
	private String anonymousRecipient;

	@Valid
	private List<@NotNull @NotEmpty @Pattern(regexp = "^[0-9a-fA-F]+$", message = "Required Hexdecimal string") String> topicId;

	@NotNull
	@NotEmpty
    @Valid
    private List<@NotNull @NotEmpty @Pattern(regexp = "^[0-9a-fA-F]+$", message = "Required Hexdecimal string") String> fqcnTopicList;

	@NotNull
	@NotEmpty
	@Valid
	private List<@NotNull @NotNull String> senderId;

	@Pattern(regexp = "^[a-zA-Z0-9\\-:.>*]+$", message = "Required Alphanumeric string")
	@Size(max=247, message = "The maximum length is 247 characters")
	private String clientId = "mb-default";

	private int amount = 1;
	
	private boolean ack = false;

	@JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
	private LocalDateTime from;

	@JsonIgnore
	public String subjectAll() {
		return streamName().concat(".").concat("*");
	}

	@JsonIgnore
	public String subjectName(String topicId) {
		return streamName().concat(".").concat(topicId);
	}

	@JsonIgnore
	public String streamName() {
		String[] streamName = fqcn.split(java.util.regex.Pattern.quote("."));
		Collections.reverse(Arrays.asList(streamName));
		return String.join("_", streamName);
	}

	public String findDurable() {
		List<String> _clientId = new ArrayList<>();
		_clientId.addAll(Arrays.asList(clientId.split("[.>*]")));
		_clientId.removeIf(String::isEmpty);
		if(from != null) {
		    _clientId.add("#:"+Long.toHexString(from.toEpochSecond(ZoneOffset.UTC)));
		}
		return String.join(":", _clientId);
	}

	public int fetchAmount(long totalAckPending) {
        int fetchAmount = amount;
        if(totalAckPending > 0 && totalAckPending > amount) {
            fetchAmount = (int) totalAckPending;
        }
        return (fetchAmount > MessageAckDTOs.MAX_FETCH_AMOUNT)?MessageAckDTOs.MAX_FETCH_AMOUNT:fetchAmount;
    }

	

	@WithSpan("manageSearchDateClientId")
	public void manageSearchDateClientId(JetStreamManagement jsm){
		try {
        	
			if(this.getFrom() == null ) {
				return;
			}
			String durable = this.findDurable();
			HashSet<String> checkDuplicate = new HashSet<>();
			if(checkConsumerExist(jsm, this.streamName(), durable)) {
				checkDuplicate.add(durable);
			}
			jsm.getConsumerNames(this.streamName()).stream().filter(c->c.startsWith(durable.split(":#:")[0]+ ":#:") ).forEach(id ->{
				if(checkDuplicate.add(id)) {
					deleteConsumer(jsm, this.streamName(), id);
				}
			});
			
		} catch (IOException | JetStreamApiException e) {
		}
	}

	@WithSpan("updateClientSingleTopic")
	public void updateClientSingleTopic(JetStreamManagement jsm, Duration duration) {
		try {
			ConsumerInfo info = jsm.getConsumerInfo(this.streamName(), this.findDurable());

			if(info.getConsumerConfiguration().getAckWait().equals(duration)) return;
			
			Builder builder = ConsumerConfiguration.builder(info.getConsumerConfiguration());
			builder.ackWait((duration));
			
			jsm.addOrUpdateConsumer(this.streamName(), builder.build());
		} catch (IOException | JetStreamApiException e) {
		}
		
	}
	
	@WithSpan("checkConsumerExist")
	public boolean checkConsumerExist(JetStreamManagement jsm, String streamName, String consumer)
	{
		boolean isExist = false;
		try {
			jsm.getConsumerInfo(streamName, consumer);
			isExist = true;
		} catch (IOException | JetStreamApiException e) {
		}
		
		return isExist;
	}
	
	@WithSpan("deleteConsumer")
	public void deleteConsumer(JetStreamManagement jsm, String streamName, String consumer) {
		try {
			jsm.deleteConsumer(streamName, consumer);
		} catch (IOException | JetStreamApiException e) {
		}
	}

	public String anonymousFqcnRule(String DID) {
		return (anonymousRecipient == null)?DID:anonymousRecipient;
	}

	public boolean validateSearchByTopicId() {
		return this.getFqcnTopicList() != null && !this.getFqcnTopicList().isEmpty() && this.getFqcnTopicList().size() > 1 && this.getTopicId() != null && !this.getTopicId().isEmpty() && this.getTopicId().size() == 1;
	}

	
}
