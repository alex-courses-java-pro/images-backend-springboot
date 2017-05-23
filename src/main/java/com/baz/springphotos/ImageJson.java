package com.baz.springphotos;

import java.util.Base64;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by arahis on 5/22/17.
 */
@Getter
@Setter
@AllArgsConstructor
public class ImageJson {
    private long id;
    private String uri;
}
