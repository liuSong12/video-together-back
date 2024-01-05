package com.videotogether.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadFile {
    private String fileName;
    private String fileExtension;
    private String fileHash;
    private MultipartFile fileChunk;
    private String fileHashIndex;
}
