package gsrs.module.substance.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageInfo {

    private boolean hasData = false;
    private byte[] imageData = null;
    private String format = null;
}
