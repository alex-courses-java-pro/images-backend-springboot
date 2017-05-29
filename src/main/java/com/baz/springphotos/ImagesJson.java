package com.baz.springphotos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by arahis on 5/23/17.
 */
@Getter
@Setter
@AllArgsConstructor
public class ImagesJson {
    private List<ImageJson> images;
}
