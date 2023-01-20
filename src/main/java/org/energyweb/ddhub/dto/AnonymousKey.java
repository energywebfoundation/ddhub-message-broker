package org.energyweb.ddhub.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AnonymousKey {
	@NotEmpty
	@Size(min = 1, max = 255, message = "The length between 1-255 characters")
	private String anonymousKey;
}
