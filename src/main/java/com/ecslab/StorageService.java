package com.ecslab;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageService {
    /** Stores the file and returns the key (relative path) used to retrieve it. */
    String upload(MultipartFile file) throws IOException;

    /** Returns the full URL for a previously uploaded key. */
    String getUrl(String key);
}
