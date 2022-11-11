package org.energyweb.ddhub.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExtChannelDTO {
	private List<@Valid @NotNull AnonymousKey> anonymousKeys = new ArrayList<AnonymousKey>();
}
