package org.energyweb.ddhub.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopicDTOPage {

	private long count;
	private long limit;
	private int page;
	private List<TopicDTO> records;
}