package org.energyweb.ddhub.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConfigDTO {
	private long msgExpired;
	private long msgMaxSize;
	private long fileMaxSize;
	private long natsMaxClientidSize;
}
