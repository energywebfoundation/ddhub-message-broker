package org.energyweb.ddhub.helper;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class Recipients {
	@NonNull
	private int total;
	@NonNull
	private int sent;
	@NonNull
	private int failed;
}
