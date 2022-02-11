package org.energyweb.ddhub.dto;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DDHub {

	@NotNull
	@NotEmpty
	@Size(max = 200, message = "The maximum length is 200 characters")
	@FormParam("fqcn")
	@PartType(MediaType.TEXT_PLAIN)
	private String fqcn;

	@JsonIgnore
	public String streamName() {
		String[] streamName = fqcn.split(Pattern.quote("."));
		Collections.reverse(Arrays.asList(streamName));
		return String.join("_", streamName);
	}

}