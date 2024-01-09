package com.videotogether.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.gson.Gson;
import com.videotogether.anno.AnnoUpdateUser;
import com.videotogether.pojo.Message;
import com.videotogether.pojo.UploadFile;
import com.videotogether.pojo.Video;
import com.videotogether.service.impl.VideoServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping
public class UploadController {
    private final int BUFFER_SIZE = 1024 * 1024 * 5;
    @Autowired
    private Gson gson;
    @Autowired
    private VideoServiceImpl videoService;

    @PostMapping("/upload")
    public String upload(
            @RequestParam String fileHash,
            @RequestParam String fileHashIndex,
            @RequestParam MultipartFile fileChunk
    ) throws Exception {
        File hashPathDir = new File("upload", fileHash);
        if (!hashPathDir.exists()) {
            hashPathDir.mkdirs();
        }
        File targetFile = new File(hashPathDir, fileHashIndex);
        saveChunk(fileChunk, targetFile);
        return "上传成功";
    }

    public void saveChunk(MultipartFile multipartFile, File targetFile) throws Exception {
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(targetFile));
        BufferedInputStream bufferedInputStream = new BufferedInputStream(multipartFile.getInputStream());
        int len;
        byte[] bytes = new byte[BUFFER_SIZE];
        while ((len = bufferedInputStream.read(bytes)) != -1) {
            bufferedOutputStream.write(bytes, 0, len);
        }
        bufferedInputStream.close();
        bufferedOutputStream.close();
    }


    @AnnoUpdateUser
    @PostMapping("/merge")
    public String mergeRequest(@RequestBody UploadFile uploadFile) {
        new Thread(() -> {
            File hashDir = new File("upload", uploadFile.getFileHash());
            String fileName = uploadFile.getFileName();
            try {
                Video video = new Video(null, uploadFile.getFileHash()+uploadFile.getFileExtension(), fileName,null);
                videoService.save(video);
            }catch (Exception ignored){
                ignored.printStackTrace();
            }
            int i = fileName.lastIndexOf(".");
            File targetFile;
            if (i == -1) {
                targetFile = new File("upload", uploadFile.getFileHash());
            } else {
                String extension = fileName.substring(i);
                targetFile = new File("upload", uploadFile.getFileHash() + extension);
            }
            try {
                mergeFiles(hashDir.listFiles(), targetFile);
                //这里通知可以边看边切
                String json = gson.toJson(new Message(null, uploadFile.getFileHash(), null, "completeUpload", null));
                SoketController.sendAllMessage(json);
                sliceFile(uploadFile.getFileHash(), targetFile);
                //告诉前端切片完成
                String json2 = gson.toJson(new Message(null, uploadFile.getFileHash(), null, "completeSlice",null));
                SoketController.sendAllMessage(json2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
        return uploadFile.getFileHash();
    }

    @GetMapping("/filter")
    public String filterRepeat(@RequestParam("fileHash") String fileHash, @RequestParam("fileExtension") String fileExtension) {
        File dir = new File("upload", fileHash);
        if (dir.exists()) {
            if (dir.isFile()) {
                return new PreUpload(false, fileHash, null).retrunObj();
            } else {
                return new PreUpload(true, null, Arrays.asList(dir.list())).retrunObj();
            }
        } else {
            File file = new File("upload", fileHash + fileExtension);
            if (file.exists()) {
                return new PreUpload(false, fileHash, null).retrunObj();
            } else {
                return new PreUpload(true, fileHash, null).retrunObj();
            }
        }

    }

    class PreUpload {
        private Boolean needUpload;
        private String fileHash;
        private List<String> fileChunks;

        public PreUpload(Boolean needUpload, String fileHash, List<String> fileChunks) {
            this.needUpload = needUpload;
            this.fileChunks = fileChunks;
            this.fileHash = fileHash;
        }

        public String retrunObj() {
            return gson.toJson(this);
        }
    }

    /**
     * 这里是获取ts切片的
     * @param indexName
     * @param response
     * @throws Exception
     */
    @GetMapping("/{indexName}")
    public void sliceIndex(@PathVariable("indexName") String indexName, HttpServletResponse response) throws Exception {
        String hashDir = indexName.split("-")[0];
        File file = new File("slicevideo\\" + hashDir, indexName);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(response.getOutputStream());
        byte[] bytes = new byte[BUFFER_SIZE];
        int len;
        while ((len = bufferedInputStream.read(bytes)) != -1) {
            bufferedOutputStream.write(bytes, 0, len);
        }
        bufferedInputStream.close();
        bufferedOutputStream.close();
    }


    @AnnoUpdateUser
    @GetMapping("/getPoster")
    public void getPoster(@RequestParam String posterName, HttpServletResponse response) throws Exception {
        int i = posterName.lastIndexOf(".");
        String hashName = posterName;
        if (i != -1) {
            hashName = posterName.substring(0, i);
        }
        File file = new File("upload", hashName + ".jpg");
        BufferedInputStream bufferedInputStream;
        BufferedOutputStream bufferedOutputStream;
        InputStream inputStream;
        if (!file.exists()) {
            inputStream = new ClassPathResource("/static/cover.jpg").getInputStream();
        } else {
            inputStream = new FileInputStream(file);
        }

        bufferedInputStream = new BufferedInputStream(inputStream);
        bufferedOutputStream = new BufferedOutputStream(response.getOutputStream());
        byte[] bytes = new byte[BUFFER_SIZE];
        int len;
        while ((len = bufferedInputStream.read(bytes)) != -1) {
            bufferedOutputStream.write(bytes, 0, len);
        }
        bufferedInputStream.close();
        bufferedOutputStream.close();

    }

    @AnnoUpdateUser
    @GetMapping("/startView")
    public void sliceVideo(@RequestParam String videoName, HttpServletResponse response) throws Exception {
        int i = videoName.lastIndexOf(".");
        String hashName = videoName;
        if (i != -1) {
            hashName = videoName.substring(0, i);
        }
        File file = new File("slicevideo\\" + hashName, hashName + "-.m3u8");

        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(response.getOutputStream());
        byte[] bytes = new byte[BUFFER_SIZE];
        int len;
        while ((len = bufferedInputStream.read(bytes)) != -1) {
            bufferedOutputStream.write(bytes, 0, len);
        }
        response.setHeader("Content-Type", "application/x-mpegURL");
        bufferedInputStream.close();
        bufferedOutputStream.close();
    }


    /**
     * @param fileHash   切分文件夹下面hash文件夹
     * @param targetFile upload文件夹下面目标文件名
     */
    private void sliceFile(String fileHash, File targetFile) throws Exception {
        File sliceDir = new File("slicevideo", fileHash);
        if (!sliceDir.exists()) {
            sliceDir.mkdirs();
        }
        File file = new File(sliceDir, fileHash + "-.m3u8");
        if (file.exists()) {
            return;
        }

        /**
         *
         * ffmpeg -i 1.mp4 -c:v libx264 -c:a aac -strict -2 -f hls -hls_list_size 0 1.m3u8 -segment_time 10 ${path.join(outputPath, "hash-%04d.ts")}
         */
        String substring = targetFile.getName().substring(0, targetFile.getName().lastIndexOf(".")) + ".jpg";
        File img = new File(targetFile.getParentFile(), substring);
        String toTs = "ffmpeg -y -i " + targetFile.getAbsolutePath() + " -c:v libx264 -c:a aac -strict -2 -f hls -hls_list_size 0 " + file.getAbsolutePath();
        String getCover = "ffmpeg -ss 10 -t 0.001 -i " + targetFile.getAbsolutePath() + " -y -f image2 " + img.getAbsolutePath();


        try {
            Process process2 = new ProcessBuilder().command(getCover.split(" ")).redirectErrorStream(true).start();
            process2.getOutputStream().close();
            InputStream inputStream = process2.getInputStream();
            int data;
            while ((data = inputStream.read()) != -1) {
                System.out.print((char) data);
            }
            inputStream.close();
            InputStream errorStream = process2.getErrorStream();
            int err;
            while ((err = errorStream.read()) != -1) {
                System.out.print((char) err);
            }
            errorStream.close();

            Process process = new ProcessBuilder().command(toTs.split(" ")).redirectErrorStream(true).start();
            process.getOutputStream().close();
            InputStream inputStream1 = process.getInputStream();
            int d;
            while ((d = inputStream1.read()) != -1) {
                System.out.print((char) d);
            }
            inputStream1.close();
            InputStream errorStream1 = process.getErrorStream();
            int err1;
            while ((err1 = errorStream1.read()) != -1) {
                System.out.print((char) err1);
            }
            errorStream1.close();

        } catch (Exception e) {
        }


    }


    public void mergeFiles(File[] files, File targetFile) throws Exception {
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                String s1 = o1.getName().split("-")[1];
                String s2 = o2.getName().split("-")[1];
                return Integer.parseInt(s1) - Integer.parseInt(s2);
            }
        });
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(targetFile));
        byte[] bytes = new byte[BUFFER_SIZE];
        File parentFile = files[0].getParentFile();
        int len;
        for (File file : files) {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
            while ((len = bufferedInputStream.read(bytes)) != -1) {
                bufferedOutputStream.write(bytes, 0, len);
            }
            bufferedInputStream.close();
            file.delete();
        }
        boolean delete = parentFile.delete();
        bufferedOutputStream.close();
        if (!delete) {
            for (File file : files) {
                file.delete();
            }
            parentFile.delete();
        }
    }

    /**
     *
     * @param name
     * @return
     */
    @AnnoUpdateUser
    @GetMapping("/getVideoList")
    public List<Map<String,String>> getVideoList( @RequestParam(value = "name",required = false) String name) {
        File uploadDir = new File("upload");
        File[] files = uploadDir.listFiles();
        List<Map<String,String>> fileList = new ArrayList<>();
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".mp4")) {
                LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Video::getVideoHash, file.getName());
                Video video = videoService.getOne(wrapper);
                if(video!=null){
                    HashMap<String, String> map = new HashMap<>();
                    map.put("videName", video.getVideoName());
                    map.put("fileHashName", video.getVideoHash());
                    map.put("uploadTime",video.getUploadTime().toString());
                    fileList.add(map);
                }
            }
        }

        Collections.sort(fileList, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> o1, Map<String, String> o2) {
                return o2.get("uploadTime").compareTo(o1.get("uploadTime"));
            }
        });

        if(name!=null){
            return fileList.stream().filter(v->v.get("videName").contains(name)).collect(Collectors.toList());
        }else {
            return fileList;
        }
    }


}
