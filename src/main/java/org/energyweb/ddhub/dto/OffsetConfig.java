package org.energyweb.ddhub.dto;

import javax.annotation.Nonnegative;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OffsetConfig {
	@Nonnegative
	@Min(value = 2,  message = "The mininum offset value = 2")
	@Max(value = 30,  message = "The maximum offset value = 30")
	private Integer offsetInDays;
}
