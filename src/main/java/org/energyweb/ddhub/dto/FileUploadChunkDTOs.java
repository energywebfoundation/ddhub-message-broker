package org.energyweb.ddhub.dto;

import java.io.File;

import javax.validation.constraints.NotNull;
import javax.ws.rs.FormParam;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FileUploadChunkDTOs extends FileUploadDTOs {

	@NotNull
	@FormParam("fileSize")
	private Float fileSize;
	
	@NotNull
	@FormParam("chunkSize")
	private Float chunkSize;
	
	@NotNull
	@FormParam("currentChunkIndex")
	private Integer currentChunkIndex;
	
	@NotNull
	@FormParam("fileChecksum")
	private String fileChecksum;
	
	private File tempFile;
	
}
