package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.storage.s3.AWSS3Test;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class S3TestController {

    private final AWSS3Test awsS3Test;

    @GetMapping("/test/s3/all")
    public String testAll() {
        awsS3Test.runAllTests();
        return "S3 전체 테스트 완료! 콘솔 로그를 확인하세요.";
    }

    @GetMapping("/test/s3/upload")
    public String testUpload() {
        awsS3Test.testUpload();
        return "업로드 테스트 완료! 콘솔 로그를 확인하세요.";
    }

    @GetMapping("/test/s3/download")
    public String testDownload() {
        awsS3Test.testDownload();
        return "다운로드 테스트 완료! 콘솔 로그를 확인하세요.";
    }

    @GetMapping("/test/s3/presigned")
    public String testPresigned() {
        awsS3Test.testPresignedUrl();
        return "PresignedURL 테스트 완료! 콘솔 로그를 확인하세요.";
    }
}

