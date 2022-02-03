package org.energyweb.ddhub.dto;

import java.io.InputStream;

import javax.validation.constraints.NotNull;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MultipartBody extends DDHub {
    @NotNull
    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public InputStream file;

    @NotNull
    @FormParam("fileName")
    @PartType(MediaType.TEXT_PLAIN)
    public String fileName;

}
