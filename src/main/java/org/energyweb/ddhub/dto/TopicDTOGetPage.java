package org.energyweb.ddhub.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TopicDTOGetPage {

	@JsonProperty(value = "total")
	private long count;
	private long limit;
	private int page;
	private List<TopicDTOGet> records;
}