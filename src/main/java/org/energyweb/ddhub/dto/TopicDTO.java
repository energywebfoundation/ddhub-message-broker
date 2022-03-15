package org.energyweb.ddhub.dto;

import java.util.HashMap;
import java.util.Set;

import javax.json.bind.JsonbBuilder;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

@Data
public class TopicDTO {

	public enum SchemaType {
		JSD7,
		XSD6,
		XML
	}

	private String id;
	@NotNull
	@NotEmpty
	private String name;
	@NotNull
	private SchemaType schemaType;
	@NotNull
	@NotEmpty
	@Getter(AccessLevel.NONE)
	private String schema;
	@NotNull
	@Pattern(regexp = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$", message = "Required Semantic Versions")
	private String version;
	@NotNull
	@NotEmpty
	private String owner;
	@Valid
	private Set<@NotEmpty String> tags;
	@JsonIgnore
	private String did;
	
	public HashMap getSchema() {
		return jsonParser();
	}

	private HashMap stringParser() {
		HashMap map = new HashMap();
		if(schemaType == SchemaType.XML) {
			map.put("XML", schema);
		}
		else{
			map.put("STRING", schema);
		}
		schema = JsonbBuilder.create().toJson(map);
		return jsonParser();
	}

	private HashMap jsonParser() {
		try {
			JSONParser parser = new JSONParser();
			return (HashMap)parser.parse(schema);
		} catch (ParseException e) {
			return stringParser();
		}
	}
}