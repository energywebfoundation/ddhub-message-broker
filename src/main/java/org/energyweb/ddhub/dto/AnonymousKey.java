package org.energyweb.ddhub.dto;

import javax.validation.constraints.NotEmpty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AnonymousKey {
	@NotEmpty
	private String anonymousKey;
}
