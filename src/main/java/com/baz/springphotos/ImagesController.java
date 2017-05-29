package com.baz.springphotos;


import org.apache.tomcat.util.http.fileupload.FileItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.java.Log;

/**
 * Created by arahis on 5/21/17.
 */
@RestController
@RequestMapping("/images")
@Log
public class ImagesController {

    private final String BASE_IMAGES_URL = "http://localhost:8080/images";

    private List<Image> images = new ArrayList<>(); // dao imitation

    //autofilling with local images for test purpose
    /*@PostConstruct
    private void init() {
        //TODO: remove
        try {
            Path path = Paths.get(
                    "/home/arahis/Pictures/green_power_wallpaper_by_rocan64-d3hifbd.jpg");
            byte[] bytes = Files.readAllBytes(path);
            String name = "1.jpg";
            String originalName = "1.jpg";
            String contentType = "image/jpeg";
            MultipartFile mfile = new MockMultipartFile(name, originalName, contentType, bytes);
            uploadImage(mfile);
            //2nd imange
            path = Paths.get(
                    "/home/arahis/Pictures/39721762-power-wallpapers.png");
            bytes = Files.readAllBytes(path);
            name = "2.png";
            originalName = "2.png";
            contentType = "image/png";
            mfile = new MockMultipartFile(name, originalName, contentType, bytes);
            uploadImage(mfile);
            //3rd image
            path = Paths.get(
                    "/home/arahis/Pictures/27324811-power-wallpapers.jpg");
            bytes = Files.readAllBytes(path);
            name = "3.jgp";
            originalName = "3.jpg";
            contentType = "image/jpeg";
            mfile = new MockMultipartFile(name, originalName, contentType, bytes);
            uploadImage(mfile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //
    }*/

    @RequestMapping(method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity uploadImage(@RequestParam("image") MultipartFile uploadImage) {
        long id;
        if (uploadImage.isEmpty()) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        log.info("uploaded file type: " + uploadImage.getContentType()
                + ", " + uploadImage.getName());
        try {
            if (ImageIO.read(uploadImage.getInputStream()) == null) {
                return new ResponseEntity(HttpStatus.BAD_REQUEST);
            }
            id = System.currentTimeMillis();
            String uri = BASE_IMAGES_URL + "/" + id;
            log.info("image uri: " + uri);
            images.add(new Image(id, uploadImage.getBytes(),
                    uploadImage.getName(), uploadImage.getContentType(), uri));
        } catch (IOException e) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity(id, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{id}",
            method = RequestMethod.GET)
    public ResponseEntity getImageById(@PathVariable("id") long id) {
        HttpHeaders headers = new HttpHeaders();
        Image responseImage = findImg(id);
        if (responseImage == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        headers.setContentType(MediaType.parseMediaType(responseImage.getMimeType()));
        headers.setContentLength(responseImage.getBytes().length);

        return new ResponseEntity(responseImage.getBytes(), headers, HttpStatus.OK);
    }


    @RequestMapping(method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getAllImages() {

        List<ImageJson> allImages = new ArrayList<>();

        for (Image img : images) {
            ImageJson imageJson = new ImageJson(img.getId(), img.getUri());
            allImages.add(imageJson);
        }

        return new ResponseEntity(new ImagesJson(allImages), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity deleteImagesByIds(@RequestBody(required = true) long[] ids) {

        if (ids.length == 0) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        removeImages(ids);

        return new ResponseEntity(HttpStatus.OK);
    }


    @RequestMapping(value = "download/{format}",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity getImagesArchiveByIds(@PathVariable("format") String fileFormat,
                                                @RequestBody(required = true) long[] ids) { //TODO: required = true

        if (!fileFormat.equals("zip")) {
            return new ResponseEntity(HttpStatus.NOT_IMPLEMENTED);
        }
        if (ids.length == 0) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        try {
            ByteArrayResource resource = zipImages(ids);
            return new ResponseEntity(resource, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // code below should be in service/logic layer, but our case is simplified
    private ByteArrayResource zipImages(long[] ids) throws IOException {
        OutputStream outputStream = new FileOutputStream("images.zip");
        ZipOutputStream zipOS = new ZipOutputStream(outputStream);
        for (int i = 0; i < ids.length; i++) {
            long imageId = ids[i];
            zipOS.putNextEntry(new ZipEntry("image" + imageId));
            zipOS.write(findImg(imageId).getBytes());
            zipOS.closeEntry();
        }
        zipOS.finish();
        zipOS.flush();
        zipOS.close();
        outputStream.close();
        return new ByteArrayResource(Files.readAllBytes(Paths.get("images.zip")));
    }

    private void removeImages(long[] ids) {
        for (int i = 0; i < ids.length; i++) {
            Image imgToRemove = findImg(ids[i]);
            if (imgToRemove != null) {
                images.remove(imgToRemove);
            }
        }
    }


    private Image findImg(long id) {
        Image image = null;
        for (Image img : images) {
            if (img.getId() == id) {
                image = img;
            }
        }
        return image;
    }
}
