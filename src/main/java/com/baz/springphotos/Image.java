package com.baz.springphotos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by arahis on 5/23/17.
 */
@Getter
@Setter
@AllArgsConstructor
public class Image {
    private long id;
    private byte[] bytes;
    private String name;
    private String mimeType;
    private String uri;
}
